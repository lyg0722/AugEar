package cnsl.augear;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by leeyg on 2016-02-03.
 */
public class Recorder {
    private static final String LOG_TAG = "Recorder";

    private final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final int WAVE_CHANNEL_MONO = 1;
    private final int HEADER_SIZE = 0x2c;
    private final int RECORDER_BPS = 16;
    private final int RECORDER_SAMPLERATE = 44100;
    private final int BUFFER_SIZE;
    private final String TEMP_FILE_NAME = "temp.bak";

    private AudioRecord mAudioRecord;
    private boolean mIsRecording;
    private BufferedInputStream mBIStream;
    private BufferedOutputStream mBOStream;
    private String mFileName;
    private int mAudioLen = 0;
    private Thread mRecordingThread;

    public Recorder() {
        BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        mFileName = null;
        mIsRecording = false;
        mRecordingThread = null;

        MainActivity.log(LOG_TAG, "created");
    }

    public void prepare(String fileName){
        mFileName = fileName;
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, BUFFER_SIZE);
    }

    public void startRecordingToFile() {
        mAudioRecord.startRecording();
        mIsRecording = true;

        MainActivity.log(LOG_TAG, "start recording to file");

        mRecordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeToFile();
            }
        },"AudioRecorder Thread");
    }

    private void writeToFile(){
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] data = new byte[BUFFER_SIZE];
        File waveFile = new File(Environment.getExternalStorageDirectory()+"/"+ mFileName);
        File tempFile = new File(Environment.getExternalStorageDirectory()+"/"+TEMP_FILE_NAME);

        MainActivity.log(LOG_TAG, "write to file");

        try {
            mBOStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        int read = 0;
        int len = 0;
        if (null != mBOStream) {
            try {
                while (mIsRecording) {
                    read = mAudioRecord.read(data, 0, BUFFER_SIZE);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
//                        mBOStream.write(data);
//                        Log.i(LOG_TAG, "buffer data --->> " + buffer.toString());
                        MainActivity.log(LOG_TAG, "buffer data --->> " + buffer.toString());
                    }
                }

//                mBOStream.flush();
                mAudioLen = (int)tempFile.length();
                mBIStream = new BufferedInputStream(new FileInputStream(tempFile));
                mBOStream.close();
//                mBOStream = new BufferedOutputStream(new FileOutputStream(waveFile));
//                mBOStream.write(getFileHeader());
                len = HEADER_SIZE;
                while ((read = mBIStream.read(

                )) != -1) {
//                    mBOStream.write(buffer);
//                    Log.i(LOG_TAG, "buffer data --->> " + buffer.toString());
                    MainActivity.log(LOG_TAG, "buffer data --->> " + buffer.toString());
                }
//                mBOStream.flush();
                mBIStream.close();
                mBOStream.close();

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    public void startStreaming(){
        mAudioRecord.startRecording();
        mIsRecording = true;

        mRecordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendToNetwork();
            }
        },"AudioRecorder Thread");
    }

    private void sendToNetwork(){
        byte[] data = new byte[BUFFER_SIZE];

        int read = 0;
        while(mIsRecording){
            read = mAudioRecord.read(data, 0, BUFFER_SIZE);
            if(mAudioRecord.ERROR_INVALID_OPERATION != read){
                // send to network using 'data'
                // search for 'MIC로 녹음하면 WAVE 파일로 변환해주는 소스' in NAVER.
                // wav file format 설명: http://crystalcube.co.kr/123
            }
        }
    }

    public void stopRecording() {
        if (null != mAudioRecord) {
            mIsRecording = false;
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
            mRecordingThread = null;
        }
    }

    private byte[] getFileHeader() {
        byte[] header = new byte[HEADER_SIZE];
        int totalDataLen = mAudioLen + 40;
        long byteRate = RECORDER_BPS * RECORDER_SAMPLERATE * WAVE_CHANNEL_MONO/8;
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
        header[22] =  WAVE_CHANNEL_MONO;
        header[23] = 0;
        header[24] = (byte) (RECORDER_SAMPLERATE & 0xff);
        header[25] = (byte) ((RECORDER_SAMPLERATE >> 8) & 0xff);
        header[26] = (byte) ((RECORDER_SAMPLERATE >> 16) & 0xff);
        header[27] = (byte) ((RECORDER_SAMPLERATE >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) RECORDER_BPS * WAVE_CHANNEL_MONO/8;  // block align
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

    //    private MediaRecorder innerRecorder = null;
//    private boolean isPrepared = false;
//
//    public void prepare(String fileName){
//        innerRecorder = new MediaRecorder();
//
//        innerRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        innerRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
//        innerRecorder.setOutputFile(fileName);
//        innerRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        innerRecorder.setAudioSamplingRate(44100);
//
//        try {
//            innerRecorder.prepare();
//            isPrepared = true;
//        } catch (IOException e) {
//            Log.e(LOG_TAG, "prepare() failed");
//        }
//    }
//
//    public void startRecordingToFile() {
//        innerRecorder.start();
//    }
//
//    public void stopRecording() {
//        if(innerRecorder != null){
//            innerRecorder.stop();
//            innerRecorder.reset();
//            innerRecorder.release();
//            innerRecorder = null;
//
//            isPrepared = false;
//        }
//        else{
//            Log.e(LOG_TAG, "Recorder does not exists.");
//        }
//    }
//
//    public void release(){
//        if(innerRecorder != null){
//            innerRecorder.reset();
//            innerRecorder.release();
//            innerRecorder = null;
//
//            isPrepared = false;
//        }
//        else{
//            Log.e(LOG_TAG, "Recorder does not exists.");
//        }
//    }
//
//    public boolean isPrepared(){
//        return isPrepared;
//    }
}