package cnsl.augear;

import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.util.Log;

import java.io.IOException;

/**
 * Created by leeyg on 2016-02-02.
 */
public class TimerRecorder {
    private static final String LOG_TAG = "TimerRecorder";
    private static final long timeWindow = 5000;
    private static String fileRoot = null;
    private static int fileCount = -1;
    private static int fileKeepCount = 100;
    private static String prefix = "/AugEar/RecFile_";
    private static String extension = ".wav";

    private Recorder mRecorder = null;
    private Timer mTimer = null;

    public TimerRecorder(String fileRoot){
        mRecorder = new Recorder();
        mTimer = new Timer(timeWindow);
        this.fileRoot = fileRoot;
    }

    public void startRecordingToFile() {
        mRecorder.startRecordingToFile(getNextFileName());
        mTimer.start();
    }

    public void stopRecording() {
        mRecorder.stopRecording();
        mTimer.cancel();
    }

    public String getCurrFileName(){
        return fileRoot + prefix + fileCount + extension;
    }

    public String getNextFileName(){
        fileCount = (fileCount + 1) % fileKeepCount;
        return fileRoot + prefix + fileCount + extension;
    }


    private class Timer extends CountDownTimer{
        public Timer(long startTime){
            super(startTime, startTime - 1);
        }

        @Override
        public void onFinish() {
            stopRecording();
            startRecordingToFile();
            this.start();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //do nothing
        }
    }


    private class Recorder {
        MediaRecorder innerRecorder = null;

        public void startRecordingToFile(String fileName) {
            Log.i(LOG_TAG,fileName);
            innerRecorder = new MediaRecorder();

            innerRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            innerRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            innerRecorder.setOutputFile(fileName);
            innerRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                innerRecorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            innerRecorder.start();
        }

        public void stopRecording() {
            if(innerRecorder != null){
                innerRecorder.stop();
                innerRecorder.reset();
                innerRecorder.release();
                innerRecorder = null;
            }
            else{
                Log.e(LOG_TAG, "Recorder does not exists.");
            }
        }
    }



}
