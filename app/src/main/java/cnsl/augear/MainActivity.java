package cnsl.augear;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int TIMEOUT = 3000;

    private static int MODE = -1;
    private static boolean IS_SERVER_RUNNING = false;
    private static boolean mRecorder_onAir = false;
    private static String logText = "";
    private static int logLineCount = 0;
    private static TextView mLogView = null;
    private static boolean isConnectionBusy = false;
    private static boolean isConnected = false;

    private TextView mRecordButton = null;
    private TextView mModeText = null;
    private ImageView mRefreshButton = null;
    private Recorder mRecorder = null;
    private int const_count = 0;
    private MainHandler mHandler;

    private WifiP2pManager mManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private WifiP2pInfo mConnectionInfo;
    private String hostAddress;
    private ProgressDialog progressDialog = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setContentView(R.layout.main);

        mHandler = new MainHandler(this);

        // Log view
        mLogView = (TextView) findViewById(R.id.logview);
        mLogView.setMovementMethod(new ScrollingMovementMethod());

        // wifi init.
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Choose mode: server or client
        mModeText = (TextView)findViewById(R.id.mode);
        DialogInterface.OnClickListener serverClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MODE = SharedConstants.MODE_SERVER;
                mModeText.setText("SERVER");
                if(mReceiver!=null){
                    ((WiFiDirectBroadcastReceiver)mReceiver).setMode(MODE);
                }
            }
        };
        DialogInterface.OnClickListener clientClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MODE = SharedConstants.MODE_CLIENT;
                mModeText.setText("CLIENT");
                if(mReceiver!=null){
                    ((WiFiDirectBroadcastReceiver)mReceiver).setMode(MODE);
                }
            }
        };
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setMessage("Choose mode")
                .setCancelable(false)
                .setPositiveButton("Server", serverClick)
                .setNegativeButton("Client", clientClick)
                .show();

        // Recorder init
        mRecorder = new Recorder(mHandler);
        mRecordButton = (TextView) findViewById(R.id.btn1);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder_onAir) {
                    // recording -> stop
                    mRecorder.stopRecording();
                    mRecorder = new Recorder(mHandler);
                    mRecorder.setHostAddress(hostAddress);
                    mRecordButton.setText("START RECOGNITION");
                } else {
                    // stop -> recording
                    if(MODE == SharedConstants.MODE_SERVER){
                        mRecorder.startRecordingToFile("recordTest.wav");
                    }else if(MODE == SharedConstants.MODE_CLIENT){
                        mRecorder.startStreaming();
                    }else{
                        Toast.makeText(getApplicationContext(), "Need to select mode", Toast.LENGTH_SHORT).show();
                    }
                    mRecordButton.setText("STOP RECOGNITION");
                }
                mRecorder_onAir = !mRecorder_onAir;
            }
        });

        mRefreshButton = (ImageView) findViewById(R.id.refreshicon);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Refresh wifi p2p connection", Toast.LENGTH_SHORT).show();
                if (isConnected) {
                    MainActivity.log(MainActivity.LOG_TAG, "Step 1-1. Canceling the current connection.");
                    mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            MainActivity.log(MainActivity.LOG_TAG, "Step 1-2. Disconnect and discover.");
                            isConnected = false;
                            isConnectionBusy = false;
                            discoverPeers();
                        }

                        @Override
                        public void onFailure(int reason) {
                            MainActivity.log(MainActivity.LOG_TAG, "Step 1-2. failed to disconnect.");
                            isConnected = false;
                            isConnectionBusy = false;
                        }
                    });
//                    mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
//                        @Override
//                        public void onSuccess() {
//                            MainActivity.log(MainActivity.LOG_TAG, "Disconnect and discover.");
//                            isConnected = false;
//                            discoverPeers();
//                        }
//                        @Override
//                        public void onFailure(int reason) {
//                            MainActivity.log(MainActivity.LOG_TAG, "failed to disconnect.");
//                        }
//                    });
                } else {
                    MainActivity.log(MainActivity.LOG_TAG, "Step 1. Not connected. Start discovery.");
                    discoverPeers();
                }

            }
        });



        // USB Things
//        mUsbManager = (UsbManager) getSystemService(this.USB_SERVICE);
//        HashMap<String, UsbDevice> devlist = mUsbManager.getDeviceList();
//        Iterator<UsbDevice> devIter = devlist.values().iterator();
//
//        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        registerReceiver(mUsbReceiver, filter);
//
//        while (devIter.hasNext()){
//            UsbDevice device = devIter.next();
//            int deviceId = device.getDeviceId();
//            if(deviceId == 1005) mic1 = device;
//            else if(deviceId == 1006) mic2 = device;
//            else mouse = device;
//
//            Log.i(LOG_TAG,"devId="+device.getDeviceId()+" / devName="+device.getDeviceName()+" / prodId="+device.getProductId()+" / prodName="+device.getProductName());
//        }
//        Log.i(LOG_TAG, "mic1 class"+mic1.getDeviceClass());
//        Log.i(LOG_TAG, "mic1 protocol" + mic1.getDeviceProtocol());
//        Log.i(LOG_TAG, "mic1 interface count"+mic1.getInterfaceCount());
////        Log.i(LOG_TAG, "mic2 "+mic2.getDeviceId());
//
//        mUsbManager.requestPermission(mic1, mPermissionIntent);
////        mUsbManager.requestPermission(mic2, mPermissionIntent);
//
//
//        byte[] bytes = new byte[30];
//        boolean forceClaim = true;
//
//        mic1_Interface0 = mic1.getInterface(0);
//        mic1_Interface1 = mic1.getInterface(1);
//        mic1_Interface2 = mic1.getInterface(2);
//        mic1_Interface3 = mic1.getInterface(3);
////        mic2_Interface = mic2.getInterface(2);
//
//        StringBuilder builder = new StringBuilder();
//        for (int i=0; i<mic1.getInterfaceCount(); i++)
//        {
//            String epDirString = "No endpoints";
//            String epTypeString = "No endpoints";
//
//            if (mic1.getInterface(i).getEndpointCount() > 0)
//            {
//                epDirString = String.valueOf(mic1.getInterface(i).getEndpoint(0).getDirection());
//                epTypeString = String.valueOf(mic1.getInterface(i).getEndpoint(0).getType());
//            }
//
//            builder.append("Int. " + i + " EP count: " + mic1.getInterface(i).getEndpointCount() +
//                    " || EP direction: " + epDirString + " || EP type: " + epTypeString + "\n");
//
//        }
//        Log.i(LOG_TAG, builder.toString());
//
//        mic1_Endpoint2 = mic1_Interface2.getEndpoint(0);        // isochronous
//        mic1_Endpoint3 = mic1_Interface3.getEndpoint(0);        // interrupt
////        mic2_EndpointFromMic = mic2_Interface.getEndpoint(0);
//
//        Log.d(LOG_TAG, "ep get");
//
//        mic1_Connection = mUsbManager.openDevice(mic1);
////        mic2_Connection = mUsbManager.openDevice(mic2);
//        Log.d(LOG_TAG, "open dev");
//
////        mic1_Connection.claimInterface(mic1_Interface2, forceClaim);
////        mic2_Connection.claimInterface(mic2_Interface, forceClaim);
//        Log.d(LOG_TAG, "claim interface");
//
////        UsbRequest requestRecord = new UsbRequest();
////        requestRecord.initialize(mic1_Connection, mic1_Endpoint3);
//
////        mic1_Connection.bulkTransfer(mic1_Endpoint3, bytes, bytes.length, TIMEOUT); //do in another thread
////        mic2_Connection.bulkTransfer(mic2_EndpointFromMic, bytes, bytes.length, TIMEOUT); //do in another thread
//
//        Log.d(LOG_TAG, "start");
//
//        Log.d(LOG_TAG, "interface 0 + bulktransfer");
//        mic1_Connection.claimInterface(mic1_Interface0, forceClaim);
//        mic1_Connection.controlTransfer()
//        Thread th = new DataTransferThread(new byte[30], mic1_Endpoint3, mic1_Connection);
//        th.start();
//        Log.d(LOG_TAG, "done");

        // USB Things
    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) {
            mRecorder.stopRecording();
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        mConnectionInfo = info;
        isConnected = true;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // InetAddress from WifiP2pInfo struct.
        hostAddress = info.groupOwnerAddress.getHostAddress();

        if (info.groupFormed && info.isGroupOwner) {
            // server
            if(!IS_SERVER_RUNNING){
                MainActivity.log(LOG_TAG, "Step 3. This is server. start serverAsyncTask");
                new ServerAsyncTask(this, mHandler).execute();
            }
        } else if (info.groupFormed) {
            // The other device acts as the client.
            MainActivity.log(LOG_TAG, "Step 3. This is client. set hostAdress.");
            mRecorder.setHostAddress(hostAddress);
        }
    }

    private void discoverPeers(){
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(this, "Press back to cancel", "finding peers", true, true);

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Step 2. Discovery initiated successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Step 2. Failed to initiate discovery: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void log(String tag, String log){
        // TODO: logLineCount 이용하여 log 길이 일정하게 유지하기(메모리부족)
        Log.i(tag, log);
        logText += "\n" + tag + "   " + log;
        mLogView.setText(logText);
    }

    public int getMode(){
        return MODE;
    }

    public Handler getHandler(){
        return mHandler;
    }


    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {
        private final Context mContext;
        private Handler mHandler;

        public ServerAsyncTask(Context context, Handler handler) {
            mContext = context;
            mHandler = handler;
            sendMsg("ServerAsyncTask init.");
        }

        @Override
        protected String doInBackground(Void... params) {
            BufferedInputStream tmpInputStream;
            BufferedOutputStream bOutStream = null;
            int bufferSize = SharedConstants.CURRENT_BUFFER_SIZE;
            int audioLen = 0;
            byte byteBuffer[]  = new byte[bufferSize]; // buffer of buffer
            String TEMP_FILE_NAME = "temp.bak";
            String RESULT_FILE_NAME = "result.wav";
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
                    sendMsg("Server: Socket opened");
                    Socket client = serverSocket.accept();
                    sendMsg("Server: connection done");

                    InputStream inputstream = client.getInputStream();
                    BufferedInputStream bIS = new BufferedInputStream(inputstream);
                    int numBytesRead = 0;
                    while ((numBytesRead = bIS.read(byteBuffer)) != -1) {
                        bOutStream.write(byteBuffer, 0, numBytesRead);
                    }
                    bOutStream.flush();
                    bOutStream.close();
                    inputstream.close();
                    bIS.close();
                    audioLen = (int) tempFile.length();

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

                    sendMsg("file write done.");
                    serverSocket.close();
                    IS_SERVER_RUNNING = false;
                } catch (IOException e) {
                    try {
                        if (serverSocket != null) serverSocket.close();
                        sendMsg("Server: Socket closed");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    IS_SERVER_RUNNING = false;
                    sendMsg(e.getMessage());
                }
            }
            return null;
        }

//        @Override
//        protected String doInBackground(Void... params) {
//            int bufferSize = SharedConstants.CURRENT_BUFFER_SIZE*11/10;
//            byte byteBuffer[]  = new byte[bufferSize]; // buffer of buffer
//            ServerSocket serverSocket = null;
//
//            try {
//                serverSocket = new ServerSocket(SharedConstants.PORT);
//                sendMsg("Server: Socket opened");
//                Socket client = serverSocket.accept();
//                sendMsg("Server: connection done");
//
//                InputStream inputstream = client.getInputStream();
//                BufferedInputStream br = new BufferedInputStream(inputstream);
//                while(br.read(byteBuffer) != -1){
//                    String str = "";
//                    for(byte bytes:byteBuffer){
//                        str += bytes + " ";
//                    }
//                    byteBuffer = new byte[bufferSize]; // initialize manually for the last line.
//                    sendMsg("Input stream --->>> " + str);
//                }
//                sendMsg("reading done.");
//                inputstream.close();
//                br.close();
//                serverSocket.close();
//                IS_SERVER_RUNNING = false;
//                return null;
//            } catch (IOException e) {
//                try {
//                    if(serverSocket != null) serverSocket.close();
//                    sendMsg("Server: Socket closed");
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//                IS_SERVER_RUNNING = false;
//                sendMsg(e.getMessage());
//                return null;
//            }
//        }

        @Override
        protected void onPostExecute(String result) {

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
    }

    private class MainHandler extends Handler {
        Context mContext;

        public MainHandler(Context c){
            super();
            mContext = c;
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);

            switch(msg.what){
                case SharedConstants.BYTE_RECORD:
                    MainActivity.log(msg.getData().getString(SharedConstants.KEY_LOGTAG), msg.getData().getString(SharedConstants.KEY_MSG));
                    break;
                case SharedConstants.STOP_SEARCHING:
                    mManager.stopPeerDiscovery(mChannel, null);
                    MainActivity.log(MainActivity.LOG_TAG, "stop searching msg received and called a method.");
                    break;
                case SharedConstants.REQUEST_CONNECTION:
                    MainActivity.log(MainActivity.LOG_TAG, "Step 4. connection requested. busy connection? "+isConnectionBusy);
                    if(!isConnectionBusy){
                        isConnectionBusy = true;
                        WifiP2pConfig config = new WifiP2pConfig();
                        if(MODE == SharedConstants.MODE_CLIENT){
                            config.groupOwnerIntent = 0;
                            config.deviceAddress = SharedConstants.SERVER_ADDRESS;
                        }else if(MODE == SharedConstants.MODE_SERVER){
                            config.groupOwnerIntent = 15; // Should be a number between 1-15(>0).
                            config.deviceAddress = SharedConstants.CLIENT_ADDRESS;
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        progressDialog = ProgressDialog.show(mContext, "Press back to cancel",
                                "Connecting to :" + config.deviceAddress, true, true);

                        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                // Success logic, we also get broadcast, so things are implemented there.
                                // See onConnectionInfoAvailable
                                MainActivity.log(LOG_TAG, "Step 5. Wifi direct Connected successfully");
                            }
                            @Override
                            public void onFailure(int reason) {
                                //failure logic
                                MainActivity.log(LOG_TAG, "Step 5. Failed to connection : " + reason);
                                isConnectionBusy = false;
                            }
                        });
                    }
                    break;
                case SharedConstants.DISCONNECTED:
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    isConnected = false;
                    break;
                default:
                    break;
            }
        }
    }
}
