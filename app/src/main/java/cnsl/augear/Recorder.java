package cnsl.augear;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by leeyg on 2016-02-03.
 */
public class Recorder {
    private static final String LOG_TAG = "Recorder";
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int HEADER_SIZE = 0x2c;
    private static final int RECORDER_BPS = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int WAVE_CHANNEL = 1;  // mono
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    private final int BUFFER_SIZE;

    private AudioRecord mAudioRecord;
    private boolean mIsRecording;
    private String mFileName;
    private String mHostAddress;
    private int mAudioLen = 0;

    private Handler mHandler;

    public Recorder(Handler handler) {
        mHandler = handler;
        BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        mFileName = null;           // temporarily null because we don't use this.
        mIsRecording = false;
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, BUFFER_SIZE);

        SharedConstants.CURRENT_BUFFER_SIZE = BUFFER_SIZE;
        sendMsg("buffer size: "+SharedConstants.CURRENT_BUFFER_SIZE);
    }

    public void startRecordingToFile(String fileName) {
        mAudioRecord.startRecording();
        mIsRecording = true;
        mFileName = fileName;

        MainActivity.log(LOG_TAG, "start recording to file");

        new Thread(new Runnable() {
            @Override
            public void run() {
                writeToFile();
            }
        },"AudioRecorder Thread").start();
    }

    /**
     * BPS가 16인 경우 음성의 sample하나(frame)당 16비트(2바이트)를 이용하여 표현한다는 말임.
     * 주어진 데이터에서 두개씩 묶으면 하나의 샘플을 표현함.
     */
    private void writeToFile(){
        byte[] data = new byte[SharedConstants.CURRENT_BUFFER_SIZE];
        File waveFile = new File(Environment.getExternalStorageDirectory()+"/"+ mFileName);
        File tempFile = new File(Environment.getExternalStorageDirectory()+"/"+"temp_server.bak");

        BufferedInputStream mBIStream;
        BufferedOutputStream mBOStream = null;

        try {
            mBOStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            sendMsg("target file not found. check parent directory.");
            e1.printStackTrace();
        }

        int read = 0;
        if (null != mBOStream) {
            try {
                while (mIsRecording) {
                    read = mAudioRecord.read(data, 0, SharedConstants.CURRENT_BUFFER_SIZE);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        mBOStream.write(data);
                    }
                }
                mBOStream.flush();
                mBOStream.close();
                mAudioLen = (int)tempFile.length();

                data = new byte[SharedConstants.CURRENT_BUFFER_SIZE];
                mBIStream = new BufferedInputStream(new FileInputStream(tempFile));
                mBOStream = new BufferedOutputStream(new FileOutputStream(waveFile));
                mBOStream.write(getFileHeader());
                while (mBIStream.read(data) != -1) {
                    mBOStream.write(data);
                }
                mBOStream.flush();
                mBIStream.close();
                mBOStream.close();

            } catch (IOException e1) {
                sendMsg("IO exception occurred.");
                e1.printStackTrace();
            }
        }
    }

    public void startStreaming(){
        // search for 'MIC로 녹음하면 WAVE 파일로 변환해주는 소스' in NAVER.
        // wav file format 설명: http://crystalcube.co.kr/123
        mAudioRecord.startRecording();
        mIsRecording = true;

        sendMsg("Start streaming.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = new Socket();
                byte byteBuffer[]  = new byte[SharedConstants.CURRENT_BUFFER_SIZE]; // buffer of buffer

                try {
                    sendMsg("Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(mHostAddress, SharedConstants.PORT)), SharedConstants.SOCKET_TIMEOUT);
                    sendMsg("Client socket - " + socket.isConnected());
                    OutputStream outputStream = socket.getOutputStream();
                    BufferedOutputStream bOutputStream = new BufferedOutputStream(outputStream);

                    int audioRead = 0;
                    while (mIsRecording) {
                        audioRead = mAudioRecord.read(byteBuffer, 0, SharedConstants.CURRENT_BUFFER_SIZE);
                        if (AudioRecord.ERROR_INVALID_OPERATION != audioRead) {
                            bOutputStream.write(byteBuffer);
                        }
                    }
                    sendMsg("Input done. close.");
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    sendMsg("IO exception occurred.");
                }

                /**
                 * Clean up any open sockets when done
                 * transferring or if an exception occurred.
                 */
                finally {
                    sendMsg("In the finally of streaming.");
                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                //catch logic
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void stopRecording() {
        MainActivity.log(LOG_TAG, "stop recording.");

        if (mAudioRecord != null) {
            mIsRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private byte[] getFileHeader() {
        byte[] header = new byte[HEADER_SIZE];
        int totalDataLen = mAudioLen + 40;
        long byteRate = RECORDER_BPS * RECORDER_SAMPLERATE * WAVE_CHANNEL /8;
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = (byte)1;  // format = 1 (PCM방식)
        header[21] = 0;
        header[22] = (byte) WAVE_CHANNEL;
        header[23] = 0;
        header[24] = (byte) (RECORDER_SAMPLERATE & 0xff);
        header[25] = (byte) ((RECORDER_SAMPLERATE >> 8) & 0xff);
        header[26] = (byte) ((RECORDER_SAMPLERATE >> 16) & 0xff);
        header[27] = (byte) ((RECORDER_SAMPLERATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (RECORDER_BPS * WAVE_CHANNEL / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPS;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte)(mAudioLen & 0xff);
        header[41] = (byte)((mAudioLen >> 8) & 0xff);
        header[42] = (byte)((mAudioLen >> 16) & 0xff);
        header[43] = (byte)((mAudioLen >> 24) & 0xff);
        return header;
    }

    public static byte[] getFileHeader(int audioLen){
        byte[] header = new byte[HEADER_SIZE];
        int totalDataLen = audioLen + 40;
        long byteRate = RECORDER_BPS * RECORDER_SAMPLERATE * WAVE_CHANNEL /8;
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = (byte)1;  // format = 1 (PCM방식)
        header[21] = 0;
        header[22] = (byte) WAVE_CHANNEL;
        header[23] = 0;
        header[24] = (byte) (RECORDER_SAMPLERATE & 0xff);
        header[25] = (byte) ((RECORDER_SAMPLERATE >> 8) & 0xff);
        header[26] = (byte) ((RECORDER_SAMPLERATE >> 16) & 0xff);
        header[27] = (byte) ((RECORDER_SAMPLERATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (RECORDER_BPS * WAVE_CHANNEL / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPS;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte)(audioLen & 0xff);
        header[41] = (byte)((audioLen >> 8) & 0xff);
        header[42] = (byte)((audioLen >> 16) & 0xff);
        header[43] = (byte)((audioLen >> 24) & 0xff);
        return header;
    }

    private void sendMsg(String msg){
        Bundle bundle = new Bundle();
        bundle.putString(SharedConstants.KEY_MSG, msg);
        bundle.putString(SharedConstants.KEY_LOGTAG, LOG_TAG);

        Message msgObj = new Message();
        msgObj.what = SharedConstants.BYTE_RECORD;
        msgObj.setData(bundle);
        mHandler.sendMessage(msgObj);
    }

    /*
    This is used only for testing.
     */
    public boolean inputToOutput(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();

        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            sendMsg("Time taken to transfer all bytes is : "+endTime);

        } catch (IOException e) {
            sendMsg("IO Exception occurred.");
            return false;
        }
        return true;
    }

    public void setHostAddress(String address){
        mHostAddress = address;
    }
}