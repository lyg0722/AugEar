package cnsl.augear;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by leeyg on 2016-03-05.
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */
public class ServerAsyncTask extends AsyncTask<Void, Void, String> {
    private static final String LOG_TAG = "ServerAsyncTask";
    private Handler mainHandler;
    private DataQueue mDataQueue;

    public ServerAsyncTask(Handler handler, DataQueue dataQueue) {
        mainHandler = handler;
        mDataQueue = dataQueue;

        sendLog("ServerAsyncTask init.");
    }

    // This is for test. You need to modify the other doInBackground which is commentized below.
    protected String doInBackgroundTest(Void... params) {
        BufferedInputStream tmpInputStream;
        BufferedOutputStream bOutStream = null;
        int bufferSize = SharedConstants.CURRENT_BUFFER_SIZE;
        int audioLen = 0;
        byte byteBuffer[]  = new byte[bufferSize];
        String TEMP_FILE_NAME = "temp_client.bak";
        String RESULT_FILE_NAME = "rec_client.wav";
        ServerSocket serverSocket = null;

        File waveFile = new File(Environment.getExternalStorageDirectory()+"/"+ RESULT_FILE_NAME);
        File tempFile = new File(Environment.getExternalStorageDirectory()+"/"+TEMP_FILE_NAME);

        try {
            bOutStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (null != bOutStream) {
            try {
                serverSocket = new ServerSocket(SharedConstants.PORT);
                sendLog("Server: Socket opened");
                Socket client = serverSocket.accept();  // Waits for an incoming request and blocks until the connection is opened.
                sendLog("Server: connection done start recording");
                toggleLocalStreaming();

                InputStream inputstream = client.getInputStream();
                BufferedInputStream bIS = new BufferedInputStream(inputstream);
                int numBytesRead = 0;
                int count = 0;
                long prevTime = System.currentTimeMillis();
                long currTime;
                while ((numBytesRead = bIS.read(byteBuffer)) != -1) {
                    if(count++%30==0){
                        currTime = System.currentTimeMillis();
                        sendLog("numBytesRead: " + numBytesRead + " / avg time per loop(ms): " + (currTime - prevTime) / 10);
                        prevTime = currTime;
                    }
                    bOutStream.write(byteBuffer, 0, numBytesRead);
                }
                bOutStream.flush();
                bOutStream.close();
                inputstream.close();
                bIS.close();
                audioLen = (int) tempFile.length();

                toggleLocalStreaming();

                byteBuffer  = new byte[bufferSize];
                tmpInputStream = new BufferedInputStream(new FileInputStream(tempFile));
                bOutStream = new BufferedOutputStream(new FileOutputStream(waveFile));
                bOutStream.write(Recorder.getFileHeader(audioLen));
                while ((numBytesRead = tmpInputStream.read(byteBuffer)) != -1) {
                    bOutStream.write(byteBuffer, 0 , numBytesRead);
                }
                bOutStream.flush();
                tmpInputStream.close();
                bOutStream.close();

                sendLog("file write done.");
                serverSocket.close();
            } catch (IOException e) {
                try {
                    if (serverSocket != null) serverSocket.close();
                    sendLog("Server: Socket closed");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                sendLog(e.getMessage());
            }
        }
        return null;
    }

    // This is for use.
    @Override
    protected String doInBackground(Void... params) {
        // TODO:Need to handle restart of recording.
        int bufferSize = SharedConstants.CURRENT_BUFFER_SIZE;
        byte[] clientByteBuffer  = new byte[bufferSize];
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(SharedConstants.PORT);
            sendLog("Server: Socket opened");
            Socket client = serverSocket.accept();
            sendLog("Server: connection done");
            toggleLocalStreaming();

            InputStream inputstream = client.getInputStream();
            BufferedInputStream bIS = new BufferedInputStream(inputstream);
            int numBytesRead = 0;
            while ((numBytesRead = bIS.read(clientByteBuffer)) != -1 && (mDataQueue.offerClientCal(clientByteBuffer, numBytesRead)) != -1) {
            }
            sendLog("send clientCal.");
            mDataQueue.calibrate();

            int totRead = 0;
            while ((numBytesRead = bIS.read(clientByteBuffer)) != -1) {
                mDataQueue.offerClientQueue(clientByteBuffer, numBytesRead);
                totRead += numBytesRead;
//                sendLog("Client total read (numbytesRead): "+totRead + "("+numBytesRead+")");
            }
            sendLog("streaming stopped.");
            toggleLocalStreaming();
            mDataQueue.reset();
            inputstream.close();
            bIS.close();
            serverSocket.close();
        } catch (IOException e) {
            try {
                if(serverSocket != null) serverSocket.close();
                sendLog("Server: Socket closed");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            sendLog(e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {

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

    private void toggleLocalStreaming(){
        Message msgObj = new Message();
        msgObj.what = SharedConstants.LOCAL_STREAMING;
        mainHandler.sendMessage(msgObj);
    }
}
