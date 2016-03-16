package cnsl.augear;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {
    private static final String LOG_TAG = "MainActivity";
    private static final int TIMEOUT = 3000;

    private static int MODE = -1;
    private static boolean mRecorder_onAir = false;
    private static String logText = "";
    private static TextView mLogView = null;
    private static boolean isConnectionBusy = false;
    private static boolean isConnected = false;
    private static boolean isAnimRunning = false;

    private TextView mRecordButton = null;
    private TextView mModeText = null;
    private TextView mTextBox = null;
    private ImageView mRefreshButton = null;
    private ImageView mRightBox = null;
    private ImageView mLeftBox = null;
    private ImageView coloredBox = null;
    private Recorder mRecorder = null;
    private DataQueue mDataQueue = null;
    private MainHandler mHandler;

    private WifiP2pManager mManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private String hostAddress;
    private ProgressDialog progressDialog = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setContentView(R.layout.main);

        mHandler = new MainHandler(this);
        mDataQueue = new DataQueue(mHandler);

        // Log view
        mLogView = (TextView) findViewById(R.id.logview);
        mLogView.setMovementMethod(new ScrollingMovementMethod());

        // wifi init.
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        deletePersistentGroups();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Recorder init
        mRecorder = new Recorder(mHandler, mDataQueue);
        mRecordButton = (TextView) findViewById(R.id.btn1);

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
                // server cannot use button
                mRecordButton.setBackgroundColor(Color.rgb(255, 255, 255));
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

                mRecordButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(isConnected){
                            toggleRecorder();
                        }
                    }
                });
            }
        };
        AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
        adBuilder.setMessage("Choose mode")
                .setCancelable(false)
                .setPositiveButton("Server", serverClick)
                .setNegativeButton("Client", clientClick)
                .show();


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
                } else {
                    MainActivity.log(MainActivity.LOG_TAG, "Step 1. Not connected. Start discovery.");
                    discoverPeers();
                }
            }
        });

        mTextBox = (TextView) findViewById(R.id.textbox);
        mRightBox = (ImageView) findViewById(R.id.rightbox);
        mLeftBox = (ImageView) findViewById(R.id.leftbox);
    }

    private void deletePersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mManager, mChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
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

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        log(LOG_TAG, info.toString());

        // InetAddress from WifiP2pInfo struct.
        if (info.groupFormed) {
            if(info.isGroupOwner){
                // server
                MainActivity.log(LOG_TAG, "Step 3. This is server. start serverAsyncTask");
                new ServerAsyncTask(mHandler, mDataQueue).execute();
            }else{
                // The other device acts as the client.
                MainActivity.log(LOG_TAG, "Step 3. This is client. set hostAdress.");
                hostAddress = info.groupOwnerAddress.getHostAddress();
                mRecorder.setHostAddress(hostAddress);
            }
        }
        isConnected = true;
    }

    protected void toggleRecorder() {
        if (mRecorder_onAir) {
            // recording -> stop
            mRecorder.stopRecording();
            mRecorder = new Recorder(mHandler, mDataQueue);
            mRecorder.setHostAddress(hostAddress);
            mRecordButton.setText("START RECOGNITION");
        } else {
            // stop -> recording
            if (MODE == SharedConstants.MODE_SERVER) {
//                mRecorder.startRecordingToFile("rec_server.wav");
                log(LOG_TAG, "server recording start");
                mRecorder.startLocalStreaming();
                findViewById(R.id.frag_container).bringToFront();
                findViewById(R.id.frag_container).setVisibility(View.VISIBLE);
                getFragmentManager().beginTransaction().add(R.id.frag_container, new CountdownFragment(), SharedConstants.FRAG_TAG).commit();
            } else {
                mRecorder.startWifiStreaming();
            }
            mRecordButton.setText("STOP RECOGNITION");
        }
        mRecorder_onAir = !mRecorder_onAir;
    }

    private void blinkBox(int box){
        if(!isAnimRunning){
            isAnimRunning = true;
            if(box == SharedConstants.LEFT){
                coloredBox = mLeftBox;
            }else if(box == SharedConstants.RIGHT){
                coloredBox = mRightBox;
            }else {
                log(LOG_TAG, "wrong box number.");
                return;
            }

            mTextBox.setText("Warning!!!");

            coloredBox.setBackgroundColor(Color.rgb(255, 0, 0));
            Animation anim1 = new AlphaAnimation(0.0f, 1.0f);
            anim1.setDuration(150);
            anim1.setRepeatCount(20);   // 150ms x 20 = 3sec
            anim1.setStartOffset(0);
            anim1.setRepeatMode(Animation.REVERSE);
            coloredBox.startAnimation(anim1);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message.obtain(mHandler, SharedConstants.RETURN_TO_NORMAL).sendToTarget();
                    isAnimRunning = false;
                }
            }).start();
        }else {
            log(LOG_TAG, "Anim Running");
        }
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
                            config.groupOwnerIntent = 1;
                            config.deviceAddress = SharedConstants.SERVER_ADDRESS;
                        }else if(MODE == SharedConstants.MODE_SERVER){
                            config.groupOwnerIntent = 14; // Should be a number between 1-15(>0).
                            config.deviceAddress = SharedConstants.CLIENT_ADDRESS;
//                            mManager.createGroup(mChannel, null);
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
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
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
                case SharedConstants.LOCAL_STREAMING:
                    toggleRecorder();
                    break;
                case SharedConstants.CALIBRATION_FINISHED:
                    findViewById(R.id.frag_container).setVisibility(View.INVISIBLE);
                    getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentByTag(SharedConstants.FRAG_TAG)).commit();
                    break;
                case SharedConstants.RETURN_TO_NORMAL:
                    mTextBox.setText("Normal");
                    coloredBox.setBackgroundColor(Color.rgb(255,255,255));
                    break;
                case SharedConstants.STATUS_UPDATE:
                    blinkBox(msg.getData().getInt(SharedConstants.KEY_DIRECTION));
                    break;
                default:
                    break;
            }
        }
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
}
