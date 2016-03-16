package cnsl.augear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by leeyg on 2016-02-22.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "WifiBroadcastReceiver";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;
    private Handler mHandler;
    private int mode;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        super();
        mManager = manager;
        mChannel = channel;
        mActivity = activity;
        mHandler = mActivity.getHandler();
        mode = mActivity.getMode();
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Toast.makeText(context, "Wifi P2P is enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Wifi P2P is disabled", Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if (mManager != null) {
//                if (mode == SharedConstants.MODE_SERVER){
                    Message msg = new Message();
                    msg.what = SharedConstants.REQUEST_CONNECTION;
                    mHandler.sendMessage(msg);
//                }
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            MainActivity.log(LOG_TAG, "Change of connection broadcast is received.");
            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                MainActivity.log(LOG_TAG, "Connection Change: New connection.");
                // we are connected with the other device, request connection
                // info to find group owner IP
                mManager.requestConnectionInfo(mChannel, mActivity);
            } else {
                // It's a disconnect
                MainActivity.log(LOG_TAG, "Connection Change: Disconnection.");
                Message.obtain(mHandler, SharedConstants.DISCONNECTED).sendToTarget();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            MainActivity.log(LOG_TAG, "Change of device's wifi state broadcast is received.");
        }
    }

    public void setMode(int mode){
        this.mode = mode;
    }
}
