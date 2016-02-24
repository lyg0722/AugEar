package cnsl.augear;

import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by leeyg on 2016-02-02.
 */
public class Timer {
    private static final String LOG_TAG = "Timer";
    private static final String APP_DIR = "/AugEar/";
    private static final long timeWindow = 5000;

    private static int fileCount = -1;
    private static int fileKeepCount = 100;
    private static String prefix = "RecFile_";
    private static String extension = ".wav";

    private CountDownTimer mTimer = null;
    private Recorder mRecorder1 = null;
    private Recorder mRecorder2 = null;
    private Recorder[] recorderArray = new Recorder[2];
    private FeatureExtractor fExtractor = null;

    private String fileRoot = null;
    private File rawAudioFile = null;
    private int currRecorderNo;
    private int nextRecorderNo;


    /**
     * Record and decode every specified time interval.
     */
    public Timer(){
        mRecorder1 = new Recorder();
        mRecorder2 = new Recorder();
//        fExtractor = new FeatureExtractor();

        currRecorderNo = 0;
        nextRecorderNo = 1;
        recorderArray[currRecorderNo] = mRecorder1;
        recorderArray[nextRecorderNo] = mRecorder2;

        fileRoot = Environment.getExternalStorageDirectory().getPath() + APP_DIR;
        Log.i(LOG_TAG, "file name: " + fileRoot);

        mTimer = new CountDownTimer(timeWindow, timeWindow/2+2){    // call onTick around after the half of the recording time
            @Override
            public void onFinish() {
                recorderArray[currRecorderNo].stopRecording();
                recorderArray[nextRecorderNo].startRecordingToFile();

                this.start();

                rawAudioFile = new File(getFilePath(getCurrFileName()));
//                fExtractor.makeFeatureFile(rawAudioFile, getCurrFileName());
                currRecorderNo = nextRecorderNo;
                nextRecorderNo = (currRecorderNo == 1) ? 0 : 1;
            }

            @Override
            public void onTick(long millisUntilFinished) {
                recorderArray[nextRecorderNo].prepare(getFilePath(getNextFileName()));
            }
        };

        recorderArray[currRecorderNo].prepare(getFilePath(getNextFileName()));
    }

    public void startRecordingToFile() {
        recorderArray[currRecorderNo].startRecordingToFile();
        mTimer.start();
    }

    public void stopRecording() {
        recorderArray[currRecorderNo].stopRecording();
        mTimer.cancel();
        recorderArray[nextRecorderNo].stopRecording();
        recorderArray[nextRecorderNo].prepare(getFilePath(getNextFileName()));
    }

    public String getFilePath(String fileName){
        return fileRoot + fileName;
    }

    public String getCurrFileName(){
        return prefix + fileCount + extension;
    }

    public String getNextFileName(){
        fileCount = (fileCount + 1) % fileKeepCount;
        return prefix + fileCount + extension;
    }
}
