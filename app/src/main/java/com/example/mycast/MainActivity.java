package com.example.mycast;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mycast.Tunnel.TunnelServer;
import com.example.mycast.rtsp.RtspServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 665;
    private static final int REQUEST_CODE_CAPTURE = 667;
    private static final int REQUEST_CODE_OPTION = 669;
    private final IntentFilter intentFilter = new IntentFilter();

    WifiP2pManager.Channel channel;
    WifiP2pManager manager;
    List<WifiP2pDevice> peers;
    WifiP2pDevice peer;
    DeviceAdapter deviceAdapter;
    WifiP2pInfo wifiP2pInfo;

    MediaProjectionManager mpManager;
    MediaProjection mediaProjection;
    RtspServer mRtspServer;
    public static TunnelServer TheTunnelServer;

    public static class MediaData {
        public byte[] data;
        public long ts;
    }

    private static int queuesize = 600;
    public static ArrayBlockingQueue<MediaData> videoQueue = new ArrayBlockingQueue<>(queuesize);
    public static ArrayBlockingQueue<MediaData> audioQueue = new ArrayBlockingQueue<>(queuesize);

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
            if (!refreshedPeers.equals(peers)) {
                peers.clear();
                peers.addAll(refreshedPeers);
                deviceAdapter.notifyDataSetChanged();

                if (peers.isEmpty()) {
                    showToast("未检测到P2P设备");
                }

                // If an AdapterView is backed by this data, notify it
                // of the change. For instance, if you have a ListView of
                // available peers, trigger an update.
//                ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

                // Perform any other updates needed based on the new list of
                // peers connected to the Wi-Fi P2P network.
            }
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    showToast("WIFI P2P ENABLED");
                } else {
                    showToast("WIFI P2P DISABLED");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                manager.requestPeers(channel, peerListListener);

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    showToast("已连接p2p设备");
                    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            wifiP2pInfo = info;
                            showNetworkInfo();
                            doCast();
                        }
                    });
                } else {
                    showToast("与p2p设备未连接");
                    wifiP2pInfo = null;
                }

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
//                showToast("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            }
        }
    };

    ServiceConnection mRtspServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRtspServer = ((RtspServer.LocalBinder) service).getService();
            mRtspServer.addCallbackListener(mRtspCallbackListener);
            mRtspServer.start();
            showNetworkInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

        @Override
        public void onError(RtspServer server, Exception e, int error) {
            // We alert the user that the port is already used by another app.
            if (error == RtspServer.ERROR_BIND_FAILED) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Port already in use !")
                        .setMessage("You need to choose another port for the RTSP server !")
                        .show();
            }
        }

        @Override
        public void onMessage(RtspServer server, int message) {
            if (message == RtspServer.MESSAGE_STREAMING_STARTED) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showToast("RTSP STREAM STARTED");
                    }
                });
            } else if (message == RtspServer.MESSAGE_STREAMING_STOPPED) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showToast("RTSP STREAM STOPPED");
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);

        mpManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        peers = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(peers);
        deviceAdapter.setClickListener(new DeviceAdapter.OnClickListener() {
            @Override
            public void onItemClick(int position) {
                peer = peers.get(position);
                connect();
            }
        });
        RecyclerView rv_deviceList = findViewById(R.id.rvDeviceList);
        rv_deviceList.setAdapter(deviceAdapter);
        rv_deviceList.setLayoutManager(new LinearLayoutManager(this));

        doCheckPermission();
        showNetworkInfo();
    }

    protected void connect() {
        WifiP2pConfig config = new WifiP2pConfig();
        if (peer != null) {
            config.deviceAddress = peer.deviceAddress;
            config.wps.setup = WpsInfo.KEYPAD;
            config.wps.pin = "31415926";
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    showToast("connect onSuccess");
                }

                @Override
                public void onFailure(int reason) {
                    showToast("connect onFailure " + reason);
                }
            });
        }
    }

    public void checkPermission(View view) {
        doCheckPermission();
    }

    public void doCheckPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_NETWORK_STATE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.WAKE_LOCK,
//                        Manifest.permission.SYSTEM_ALERT_WINDOW,
//                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_CODE_PERMISSIONS);

        if (mediaProjection == null) {
            Intent permissionIntent = mpManager.createScreenCaptureIntent();
            startActivityForResult(permissionIntent, REQUEST_CODE_CAPTURE);
        }
    }

    public void discoverPeers(View view) {
        peers.clear();
        peer = null;
        deviceAdapter.notifyDataSetChanged();
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank. Code for peer discovery goes in the
                // onReceive method, detailed below.
                showToast("discover onSuccess");
            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                showToast("discover onFailure");
            }
        });
    }

    public void cast(View view) {
        if (mRtspServer == null) {
            doCast();
        } else {
            stopCast();
        }
    }

    protected void doCast() {
        if (mRtspServer != null) {
            return;
        }
        bindService(new Intent(this, RtspServer.class),
                mRtspServiceConnection,
                Context.BIND_AUTO_CREATE);
        RecorderActivity.startCapturingStatic(mediaProjection);
        ((Button) findViewById(R.id.btnCast)).setText("Stop");

        TheTunnelServer = new TunnelServer();
        TheTunnelServer.start();
    }

    protected void stopCast() {
        if (mRtspServer == null) {
            return;
        }
        unbindService(mRtspServiceConnection);
        mRtspServer = null;
        RecorderActivity.stopRecordingAndOpenFileStatic(this);
        ((Button) findViewById(R.id.btnCast)).setText("Cast");

        TheTunnelServer.interrupt();
        TheTunnelServer = null;
    }

    public void option(View view) {
        Intent intent = new Intent(this, RecorderActivity.class);
        startActivity(intent);
    }

    public void showNetworkInfo() {
        TextView infoView = (TextView) findViewById(R.id.tvNetInfo);
        String infoText;

        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();

        infoText = "局域网ip地址： " + intIP2String(wifiInfo.getIpAddress()) + "\r\n";
        if (wifiP2pInfo != null) {
            infoText += "groupOwnerAddress： " + wifiP2pInfo.groupOwnerAddress + "\r\n";
        } else {
            infoText += "groupOwnerAddress： " + null + "\r\n";
        }
        infoText += "RTSP服务器是否运行： " + (mRtspServer != null) + "\r\n";
        infoText += "服务器端口： " + RtspServer.DEFAULT_RTSP_PORT + "\r\n";

        infoView.setText(infoText);
    }

    private String intIP2String(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showToast("缺少权限，请先授予权限");
                    showToast(permissions[i]);
                    return;
                }
            }
            showToast("已获得权限");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                mediaProjection = mpManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {
                    showToast("程序发生错误:MediaProjection");
                    return;
                }
            } else {
                showToast("获取MediaProjection失败");
            }
        }
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

//    public static long startTs = 0;
//    public static long numTs = 0;
//    public static void countTs(long ts) {
//        numTs++;
//        if (startTs == 0){
//            startTs = ts;
//            numTs = 0;
//        }else{
//            if (ts - startTs >= 1e9){
//                Log.i(TAG, "countTs() called with: ts = [" + ts + "]");
//                Log.i(TAG, "numTs: " + numTs);
//                startTs = 0;
//            }
//        }
//    }

    public static void putVideoData(byte[] buffer, long ts) {
        if (videoQueue.size() >= queuesize) {
            videoQueue.poll();
        }
        MediaData data = new MediaData();
        data.data = buffer;
        data.ts = ts;
        videoQueue.add(data);
    }

    public static void putAudioData(byte[] buffer, long ts) {
        if (audioQueue.size() >= queuesize) {
            audioQueue.poll();
        }
        MediaData data = new MediaData();
        data.data = buffer;
        data.ts = ts;
        audioQueue.add(data);
    }
}
