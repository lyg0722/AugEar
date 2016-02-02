package cnsl.augear;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";

    private TextView mRecordButton = null;
    private TimerRecorder mRecorder = null;
    private boolean mRecorder_onAir = false;
    private int const_count = 0;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setContentView(R.layout.main);

        String fileRoot = Environment.getExternalStorageDirectory().getPath();
        Log.i(LOG_TAG, "file name: "+fileRoot);

        mRecorder = new TimerRecorder(fileRoot);

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




//        ConfigurationManager cm = new ConfigurationManager();
//        FrontEnd fe = new FrontEnd();
//        StreamDataSource dataSource = new StreamDataSource(44100, );        // need to be specified
//        dataSource.setInputStream(innerRecorder);          // this is wrong. need to be changed.
//        fe.setDataSource(dataSource);                  // substitute mic to audio file
//
//        fe.getData();                           // output of the front end
//
//        try {
//            FeatureFileDumper extractor = new FeatureFileDumper(cm, fe.getName());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        // USB Things
//        UsbManager mUsbManager = (UsbManager) getSystemService(this.USB_SERVICE);
//        HashMap<String, UsbDevice> devlist = mUsbManager.getDeviceList();
//        Iterator<UsbDevice> devIter = devlist.values().iterator();
//
//        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        registerReceiver(mUsbReceiver, filter);
//
//        UsbDevice mic1 = null;
//        UsbDevice mic2 = null;
//        UsbDevice mouse = null;
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
//        Log.i(LOG_TAG, "mic1 protocol"+mic1.getDeviceProtocol());
//        Log.i(LOG_TAG, "mic1 interface count"+mic1.getInterfaceCount());
//        Log.i(LOG_TAG, "mic2 "+mic2.getDeviceId());
//
//
//        mUsbManager.requestPermission(mic1, mPermissionIntent);
//        mUsbManager.requestPermission(mic2, mPermissionIntent);
//        mUsbManager.requestPermission(mouse, mPermissionIntent);
//
//
//        byte[] bytes = {0,1,2,3,4};
//        int TIMEOUT = 0;
//        boolean forceClaim = true;
//
//        UsbInterface mic1_Interface = mic1.getInterface(2);
//        UsbInterface mic2_Interface = mic2.getInterface(2);
//        UsbInterface mouse_Interface = mic2.getInterface(2);
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
//        StringBuilder builder2 = new StringBuilder();
//        for (int i=0; i<mouse.getInterfaceCount(); i++)
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
//            builder2.append("Int. " + i + " EP count: " + mic1.getInterface(i).getEndpointCount() +
//                    " || EP direction: " + epDirString + " || EP type: " + epTypeString + "\n");
//
//        }
//        Log.i(LOG_TAG, builder2.toString());
//
//        UsbEndpoint mic1_EndpointFromMic = mic1_Interface.getEndpoint(0);
//        UsbEndpoint mic2_EndpointFromMic = mic2_Interface.getEndpoint(0);
//
//        UsbDeviceConnection mic1_Connection = mUsbManager.openDevice(mic1);
//        UsbDeviceConnection mic2_Connection = mUsbManager.openDevice(mic2);
//
//        mic1_Connection.claimInterface(mic1_Interface, forceClaim);
//        mic2_Connection.claimInterface(mic2_Interface, forceClaim);
//
//        UsbRequest requestRecord = new UsbRequest();
//        requestRecord.initialize(mic1_Connection, mic1_EndpointFromMic);
//
//        mic1_Connection.bulkTransfer(mic1_EndpointFromMic, bytes, bytes.length, TIMEOUT); //do in another thread
//        mic2_Connection.bulkTransfer(mic2_Endpoint, bytes, bytes.length, TIMEOUT); //do in another thread
//
        // USB Things
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.stopRecording();
        }
    }

    private void onRecord(boolean start, TimerRecorder mRecorder) {
        if (!start) {
            mRecorder.startRecordingToFile();
        } else {
            mRecorder.stopRecording();
        }
    }

    // USB Permission
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d(LOG_TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };
    // USB Permission

}
