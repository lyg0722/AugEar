package cnsl.augear;

import android.media.AudioFormat;
import android.media.AudioRecord;

/**
 * Created by leeyg on 2016-02-24.
 */
public class SharedConstants {
    public static final int BYTE_RECORD = 1;
    public static final int STOP_SEARCHING = 2;
    public static final int REQUEST_CONNECTION = 3;
    public static final int DISCONNECTED = 4;
    public static final int LOCAL_STREAMING = 5;
    public static final int CALIBRATION_FINISHED = 6;
    public static final int RETURN_TO_NORMAL = 7;
    public static final int STATUS_UPDATE = 8;

    public static final String KEY_LOGTAG = "log";
    public static final String KEY_MSG = "byte";
    public static final String KEY_DIRECTION = "dir";

    public static final int MODE_SERVER = 1;
    public static final int MODE_CLIENT = 2;

    public static final int RIGHT = 1;      // server MIC
    public static final int LEFT = 2;       // client MIC

    public static final String FRAG_TAG = "CD";

    public static final int PORT = 8988;

    private static final String YG_PHONE = "ce:fa:00:e3:46:23";
    private static final String BG_PHONE = "ce:fa:00:e3:46:23";    // BG phone
    private static final String YG_DB_OLD = "02:0a:f5:54:8c:80";    // YG dragon board old
    private static final String YG_DB_NEW = "02:0a:f5:20:88:a0";    // YG dragon board new
    private static final String BG_DB = "02:0a:f5:2c:88:a0";    // BG dragon board

    public static final String SERVER_ADDRESS = YG_PHONE;
    public static final String CLIENT_ADDRESS = BG_DB;
    public static  final int SOCKET_TIMEOUT = 5000;

    public static final int CALIBRATION_LENGTH_IN_MS = 3000;    // This must be bigger than the expected wifi latency. (150ms)
    public static final int TIME_TO_PRESERVE_RECORD_IN_MS = 1000;      // This plus wifi latency is latency of the AugEar to the danger.

    public static final short DANGER_THRESHOLD = 1000;

    // Audio related constants
    public static final int RECORDER_BPS = 16;
    public static final int RECORDER_SAMPLERATE = 44100;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int CURRENT_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)*4;
}
