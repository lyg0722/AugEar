package cnsl.augear;

import android.media.AudioFormat;
import android.media.MediaRecorder;

/**
 * Created by leeyg on 2016-01-25.
 */
public class Constants {
    protected static final int REC1_SOURCE = MediaRecorder.AudioSource.MIC;
    protected static final int REC1_SAMPLERATE = 44100;
    protected static final int REC1_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    protected static final int REC1_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    protected static final int RECORD_TIME = 400; // in ms
}
