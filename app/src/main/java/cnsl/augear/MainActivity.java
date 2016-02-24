package cnsl.augear;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int TIMEOUT = 3000;
    private static final int PORT = 8888;

    private static int MODE_SERVER = 1;
    private static int MODE_CLIENT = 2;
    private static int MODE = -1;
    private static boolean IS_SERVER_RUNNING = false;
    private static boolean mRecorder_onAir = false;
    private static String logText = "";
    private static TextView mLogView = null;

    private TextView mRecordButton = null;
    private TextView mModeText = null;
    private ImageView mRefreshButton = null;
//    private Timer mRecorder = null;
    private Recorder mRecorder = null;
    private int const_count = 0;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;


    // USB Things
//    private UsbManager mUsbManager = null;
//    private UsbDevice mic1 = null;
//    private UsbDevice mic2 = null;
//    private UsbDevice mouse = null;
//    private UsbEndpoint mic1_Endpoint2 = null;
//    private UsbEndpoint mic1_Endpoint3 = null;
//    private UsbEndpoint mic2_EndpointFromMic = null;
//    private UsbInterface mic1_Interface0 = null;
//    private UsbInterface mic1_Interface1 = null;
//    private UsbInterface mic1_Interface2 = null;
//    private UsbInterface mic1_Interface3 = null;
//    private UsbInterface mic2_Interface = null;
//    private UsbDeviceConnection mic1_Connection = null;
//    private UsbDeviceConnection mic2_Connection = null;
//
//    private byte[] receivedByte = null;
    // USB Things

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setContentView(R.layout.main);

        // Log view
        mLogView = (TextView) findViewById(R.id.logview);
        mLogView.setMovementMethod(new ScrollingMovementMethod());

        // Choose mode: server or client
        mModeText = (TextView)findViewById(R.id.mode);

        DialogInterface.OnClickListener serverClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MODE = MODE_SERVER;
                mModeText.setText("SERVER");
            }
        };
        DialogInterface.OnClickListener clientClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MODE = MODE_CLIENT;
                mModeText.setText("CLIENT");
            }
        };

        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setMessage("Choose mode")
                .setCancelable(false)
                .setPositiveButton("Server", serverClick)
                .setNegativeButton("Client", clientClick)
                .show();

        // wifi init.
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // recorder init
//        mRecorder = new Timer();

        mRecorder = new Recorder();
        mRecorder.prepare("AugEarTMP");

        mRecordButton = (TextView) findViewById(R.id.btn1);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder_onAir) {
                    mRecordButton.setText("START RECOGNITION");
                    onRecord(mRecorder_onAir, mRecorder);
                    mRecorder = null;
                } else {
                    onRecord(mRecorder_onAir, mRecorder);
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

                // discovering peers
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Discovery failed : " + reason, Toast.LENGTH_SHORT).show();
                    }
                });
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
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.stopRecording();
        }
        unregisterReceiver(mReceiver);
    }

//    private void onRecord(boolean start, Timer mRecorder) {
    private void onRecord(boolean start, Recorder mRecorder) {
        if (!start) {
            mRecorder.startRecordingToFile();
        } else {
            mRecorder.stopRecording();
        }
    }

    public void onWifiConnected(){
        if(MODE == MODE_SERVER){
            if(!IS_SERVER_RUNNING){
                new ServerAsyncTask(this).execute();
            }

        }
        else if(MODE == MODE_CLIENT){

        }
        else{
            Toast.makeText(this, "Need to select mode", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

        private final Context mContext;

        public ServerAsyncTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d(MainActivity.LOG_TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(MainActivity.LOG_TAG, "Server: connection done");

                InputStream inputstream = client.getInputStream();
                while(mRecorder_onAir){
                    MainActivity.log(LOG_TAG, "Input stream --->>> " + inputstream.read());
                }
                serverSocket.close();
                IS_SERVER_RUNNING = false;
                return null;
            } catch (IOException e) {
                Log.e(MainActivity.LOG_TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {

            // do what is needed to do

//            if (result != null) {
//                statusText.setText("File copied - " + result);
//                Intent intent = new Intent();
//                intent.setAction(android.content.Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//                context.startActivity(intent);
//            }
        }
    }

    public static void log(String tag, String log){
        Log.i(tag, log);
        logText += "\n" + tag + "   " + log;
        mLogView.setText(logText);
    }


    // USB Permission
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(mUsbReceiver);
//
//        if(mic1_Connection!=null){
//            mic1_Connection.releaseInterface(mic1_Interface);
//            mic1_Connection.close();
//        }
//
//        if(mic2_Connection!=null){
//            mic2_Connection.releaseInterface(mic2_Interface);
//            mic2_Connection.close();
//        }
//    }
//    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
//    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
//
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (ACTION_USB_PERMISSION.equals(action)) {
//                synchronized (this) {
//                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        if(device != null){
//                            //call method to set up device communication
//                        }
//                    }
//                    else {
//                        Log.d(LOG_TAG, "permission denied for device " + device);
//                    }
//                }
//            }
//        }
//    };
//
//    class DataTransferThread extends Thread {
//        private byte[] buffer;
//        private UsbDeviceConnection connection;
//        private UsbEndpoint endpoint;
//
//        public DataTransferThread(byte[] bytes, UsbEndpoint endpoint,UsbDeviceConnection connection) {
//            this.buffer = bytes;
//            this.endpoint = endpoint;
//            this.connection = connection;
//        }
//
//        @Override
//        public void start() {
//            Log.d(LOG_TAG, "in start");
//
//            connection.bulkTransfer(endpoint, buffer, buffer.length, TIMEOUT);
//            for(byte b : buffer){
////                Log.d(LOG_TAG, "bytes transferred. - " + buffer.toString());
//                Log.d(LOG_TAG, "bytes transferred. - " + b);
//            }
//        }
//    }
    // USB Permission
}
