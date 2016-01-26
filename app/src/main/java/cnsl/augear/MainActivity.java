package cnsl.augear;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.IOException;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;

public class MainActivity extends AppCompatActivity {
//    private int source1 = MediaRecorder.AudioSource.MIC;
//
//    private AudioRecord aRecorder = null;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);
//
//        int buffersize = AudioRecord.getMinBufferSize(Constants.REC1_SAMPLERATE, Constants.REC1_CHANNELS, Constants.REC1_ENCODING);
//
//        aRecorder = new AudioRecord(Constants.REC1_SOURCE, Constants.REC1_SAMPLERATE, Constants.REC1_CHANNELS, Constants.REC1_ENCODING, buffersize);
//
//        aRecorder.startRecording();
//        boolean isRecording = true;
//
//        Log.i(this.getClass().getName(), "this is log");
//    }

    private CountDownTimer timer;

    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;

    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton   mPlayButton = null;
    private MediaPlayer mPlayer = null;

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);



        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();

        timer = new CountDownTimer(Constants.RECORD_TIME, Constants.RECORD_TIME-1) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                stopRecording();
            }
        };
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    class RecordButton extends Button {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends Button {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }



    public MainActivity() {
        mFileName = Environment.getExternalStorageDirectory().getPath();
        mFileName += "/audiorecordtest.wav";

    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);


        LinearLayout ll = new LinearLayout(this);
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        mPlayButton = new PlayButton(this);
        ll.addView(mPlayButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));
        setContentView(ll);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
}
