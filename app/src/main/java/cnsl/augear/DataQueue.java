package cnsl.augear;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by leeyg on 2016-03-07.
 */
public class DataQueue {
    private static final String LOG_TAG = "DataQueue";

    private int calibrationLength;
    private short[] serverCal;
    private short[] clientCal;
    private int serverCalSize;
    private int clientCalSize;

    private int queueLength;
    private int queueBufferLength;
    private short[] serverQueue;
    private short[] clientQueue;
    private short serverQueueMax;
    private short clientQueueMax;
    private ArrayBlockingQueue<Short> serverQueueBuffer;
    private ArrayBlockingQueue<Short> clientQueueBuffer;

    private Handler mainHandler;

    private boolean isRunning;
    private int updatePeriod;
    private int passedFrames;
    private Thread updater;

    private int serverCount;
    private int clientCount;

    public DataQueue(Handler handler){
        calibrationLength = (SharedConstants.CALIBRATION_LENGTH_IN_MS) * SharedConstants.RECORDER_SAMPLERATE / 1000;
        queueLength = (SharedConstants.TIME_TO_PRESERVE_RECORD_IN_MS) * SharedConstants.RECORDER_SAMPLERATE / 1000;
        queueBufferLength = queueLength * 2;
        mainHandler = handler;
        updatePeriod = queueLength;

        reset();

        sendLog("cal Len: " + calibrationLength + "   queue Len: " + queueLength);

        serverCount = 0;
        clientCount = 0;
    }

    public int offerServerCal(byte[] array, int sizeInBytes){
        int result = offerCalCommon(byteArrayToShortArray(array), sizeInBytes, serverCal, serverCalSize, serverQueue);
        serverCalSize = (result==-1) ? calibrationLength : result;
        return result;
    }

    public int offerClientCal(byte[] array, int sizeInBytes){
        int result = offerCalCommon(byteArrayToShortArray(array), sizeInBytes, clientCal, clientCalSize, clientQueue);
        clientCalSize  = (result==-1) ? calibrationLength : result;
        return result;
    }

    private int offerCalCommon(short[] array, int sizeInBytes, short[] calArray, int calSize, short[] queue){
        int sizeInShorts = sizeInBytes/2;

        if(calSize+sizeInShorts > calibrationLength){
            int overFlowSize = calSize+sizeInShorts-calibrationLength;
            int i;
            for(i=0; i<sizeInShorts-overFlowSize; i++){
                calArray[i+calSize] = array[i];
            }
            for(int j=0; i<overFlowSize; i++){
                queue[j] = array[i+j];
            }
            return -1;
        }else{
            for(int i=0; i<sizeInShorts; i++){
                calArray[i+calSize] = array[i];
            }
            return calSize + sizeInShorts;
        }
    }

    public void offerServerQueue(byte[] array, int sizeInBytes){
        serverCount++;
        offerToQueueBuffer(byteArrayToShortArray(array), sizeInBytes, serverQueueBuffer);
        setQueues();
    }

    public void offerClientQueue(byte[] array, int sizeInBytes){
        clientCount++;
        offerToQueueBuffer(byteArrayToShortArray(array), sizeInBytes, clientQueueBuffer);
        setQueues();
    }

    private void offerToQueueBuffer(short[] array, int sizeInBytes, ArrayBlockingQueue queueBuffer){
        int minSize;
        int sizeInShorts = sizeInBytes/2;

        for(int i=0;i<sizeInShorts;i++){
            if(!queueBuffer.offer(array[i])){
                sendLog("queue buffer length: "+queueBuffer.size() +  "   / Queue Length: "+queueLength+"\npassedFrames : " + passedFrames);
                sendLog(passedFrames + " poll finished successfully. \n serverCount/clientCount: " + serverCount + " / "+clientCount);
                sendLog("incoming too fast.");
            }
        }

        minSize = getMinQueueBufferSize();
        if(minSize >= queueLength){
            passedFrames += minSize - queueLength;
        }
//        sendLog("queue buffer length: "+queueBuffer.size() +  "   / Queue Length: "+queueLength+"\npassedFrames : " + passedFrames + "  / minSize: " + minSize);
        while(minSize > queueLength){
            serverQueueBuffer.poll();
            clientQueueBuffer.poll();
            minSize--;
        }
//        sendLog(passedFrames + " poll finished successfully. \n serverCount/clientCount: " + serverCount + " / "+clientCount);
    }

    public void calibrate(){
        if(clientCalSize == calibrationLength && serverCalSize == calibrationLength){
            sendLog("start calibration. CCS/SCS: "+clientCalSize + "/" + serverCalSize);
            int offset;
            short clientMaxVal = -1;
            short serverMaxVal = -1;
            int clientMaxPos = -1;
            int serverMaxPos = -1;

            for (int i = 0; i < calibrationLength; i++) {
                if(clientCal[i] > clientMaxVal) {
                    clientMaxVal = clientCal[i];
                    clientMaxPos = i;
                }

                if(serverCal[i] > serverMaxVal) {
                    serverMaxVal = serverCal[i];
                    serverMaxPos = i;
                }
            }
            offset = clientMaxPos - serverMaxPos;
            sendLog("Calibration offset: "+ offset + "\nSever MaxVal / Client MaxVal: "+serverMaxVal+" / "+clientMaxVal);

            if(offset>0){
                while(offset>0){
                    clientQueueBuffer.poll();
                    offset--;
                }
            }else{
                while(offset<0){
                    serverQueueBuffer.poll();
                    offset++;
                }
            }
            setQueues();
            serverCal = null;
            clientCal = null;

            isRunning = true;
            Message.obtain(mainHandler, SharedConstants.CALIBRATION_FINISHED).sendToTarget();
            updater.start();
        }else{
            sendLog("Needs one more calibration data");
            return;
        }
    }

    public void reset(){
        serverCal = new short[calibrationLength];
        clientCal = new short[calibrationLength];
        serverCalSize = 0;
        clientCalSize = 0;

        serverQueue = new short[queueLength];
        clientQueue = new short[queueLength];
        serverQueueBuffer = new ArrayBlockingQueue<>(queueBufferLength);
        clientQueueBuffer = new ArrayBlockingQueue<>(queueBufferLength);

        isRunning = false;
        passedFrames = 0;

        updater = new Thread(new Runnable() {
            @Override
            public void run() {
                sendLog("Danger updater thread starts. "+isRunning);
                while(isRunning){
                    if(passedFrames > updatePeriod){
                        sendLog("update period");
                        if(isDanger()){
                            Bundle bundle = new Bundle();
                            bundle.putInt(SharedConstants.KEY_DIRECTION, getDirection());

                            Message msgObj = new Message();
                            msgObj.what = SharedConstants.STATUS_UPDATE;
                            msgObj.setData(bundle);
                            mainHandler.sendMessage(msgObj);
                        }
                        passedFrames = 0;
                    }
                }
                sendLog("Danger updater thread ends.");
            }
        });
    }

    public boolean isDanger() {
        // TODO: implement max instead of sort if time permits.
        Arrays.sort(serverQueue);
        Arrays.sort(clientQueue);

        serverQueueMax = serverQueue[serverQueue.length-1];
        clientQueueMax = clientQueue[clientQueue.length-1];

        sendLog("serverQueueMax: " + serverQueueMax + " || clientQueueMax: "+clientQueueMax);

        if(serverQueueMax > SharedConstants.DANGER_THRESHOLD){
            return true;
        }else if(clientQueueMax > SharedConstants.DANGER_THRESHOLD){
            return true;
        }else{
            return false;
        }
    }

    public int getDirection(){
        if(serverQueueMax > clientQueueMax){
            return SharedConstants.RIGHT;
        }else{
            return SharedConstants.LEFT;
        }
    }

    private void setQueues(){
        Iterator<Short> serverIter = serverQueueBuffer.iterator();
        Iterator<Short> clientIter = clientQueueBuffer.iterator();

        int iterCount = (getMinQueueBufferSize()<queueLength) ? getMinQueueBufferSize() : queueLength;
        int count = 0;

        while(iterCount>count && serverIter.hasNext() && clientIter.hasNext()){
            serverQueue[count] = serverIter.next();
            clientQueue[count] = clientIter.next();
            count++;
        }
    }

    private short[] byteArrayToShortArray(byte[] byteA){
        // if length of byteA is odd number, there may be error.
        if(byteA.length%2!=0){
            sendLog("Warning: damaged byteArray.");
        }

        short[] shorts = new short[byteA.length/2];
        ByteBuffer.wrap(byteA).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    private int getMinQueueBufferSize(){
        if(serverQueueBuffer.size() > clientQueueBuffer.size()){
            return clientQueueBuffer.size();
        }else{
            return serverQueueBuffer.size();
        }
    }

    private void sendLog(String msg){
        Bundle bundle = new Bundle();
        bundle.putString(SharedConstants.KEY_MSG, msg);
        bundle.putString(SharedConstants.KEY_LOGTAG, LOG_TAG);

        Message msgObj = new Message();
        msgObj.what = SharedConstants.BYTE_RECORD;
        msgObj.setData(bundle);
        mainHandler.sendMessage(msgObj);
    }
}
