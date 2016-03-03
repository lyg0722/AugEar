package cnsl.augear;

/**
 * Created by leeyg on 2016-02-24.
 */
public class SharedConstants {
    public static final int BYTE_RECORD = 1;
    public static final int STOP_SEARCHING = 2;
    public static final int REQUEST_CONNECTION = 3;
    public static final int DISCONNECTED = 4;

    public static final String KEY_LOGTAG = "log";
    public static final String KEY_MSG = "byte";

    public static final int MODE_SERVER = 1;
    public static final int MODE_CLIENT = 2;

    public static final int PORT = 8988;
    public static final String SERVER_ADDRESS = "02:0a:f5:54:8c:80";    // YG dragon board
    public static final String CLIENT_ADDRESS = "ce:fa:00:e3:46:23";    // YG phone
//    public static final String SERVER_ADDRESS = "ce:fa:00:e3:46:23";
//    public static final String CLIENT_ADDRESS = "02:0a:f5:54:8c:80";
    public static  final int SOCKET_TIMEOUT = 5000;

    public static final int DEFAULT_BUFFER_SIZE = 3584;
    public static int CURRENT_BUFFER_SIZE = DEFAULT_BUFFER_SIZE;
}
