package com.example.hp.letsshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SendOrReceive extends AppCompatActivity {

    TextView appName;
    ImageButton sendButton;
    ImageButton receiveButton;

    TextView discoveryText;
    ListView peersListView;
    WifiManager wifiManager;
    static WifiP2pManager mWifiP2pManager;
   static WifiP2pManager.Channel mChannel;
    BroadcastReceiver mBroadcastReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    String serverOrClient="";
   static String serverIpAddress="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendorreceive);
        initialise();
        addOnClickListeners();
    }
    public void initialise()
    {
        //UI components
        appName = (TextView)findViewById(R.id.appName);
        sendButton = (ImageButton)findViewById((R.id.sendButton));
        receiveButton = (ImageButton)findViewById(R.id.receiveButton);

        discoveryText = (TextView)findViewById(R.id.discoveryState);
        discoveryText.setText("Discovery has not started yet");
        //WiFi State change
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        peersListView = (ListView)findViewById(R.id.peerListView);

        //for WifiDirectBroadcastReceiver
        mWifiP2pManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this,getMainLooper(),null);
        mBroadcastReceiver = new WiFiDirectBroadcastReceiver(mWifiP2pManager,mChannel,this);
        mIntentFilter = new IntentFilter();
        addIntentFilterActions();
    }

    public void addIntentFilterActions()
    {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    }
    public void addOnClickListeners()
    {
        final Context context = this;
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiManager.isWifiEnabled() == false)
                {

                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(SendOrReceive.this,R.style.MyDialogTheme);
                    dlgAlert.setMessage("Your WiFi needs to be turned on to proceed");
                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
                                }
                            });
                    dlgAlert.setTitle("Let's Share");
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();

                }
                else{
                    discoveryText.setText("Discovering.......");
                    serverOrClient = "server";
                    startSearch();}
            }
        });
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiManager.isWifiEnabled() == false)
                {

                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(SendOrReceive.this,R.style.MyDialogTheme);
                    dlgAlert.setMessage("Your WiFi needs to be turned on to proceed");
                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
                                }
                            });
                    dlgAlert.setTitle("Let's Share");
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();

                }
                else {
                    discoveryText.setText("Discovering.......");
                    startSearch();
                serverOrClient="client";}
            }
        });

        peersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                if (serverOrClient.equals("client"))
                {
                    config.groupOwnerIntent=0;
                }
                else {
                    config.groupOwnerIntent=15;
                }

                mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(SendOrReceive.this,"Connected",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(SendOrReceive.this,"Connection Failed",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    public static String getIp()
    {
        return serverIpAddress;
    }
    WifiP2pManager.ConnectionInfoListener mConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress address = info.groupOwnerAddress;
            serverIpAddress = address.getHostAddress().toString();
            if(info.groupFormed && info.isGroupOwner)
            {

                Toast.makeText(SendOrReceive.this,"Server",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SendOrReceive.this,ServerSocketThread.class);
                startActivity(intent);
            }
            else if(info.groupFormed)
            {
                Toast.makeText(SendOrReceive.this,"Client",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SendOrReceive.this,Client.class);
                startActivity(intent);
            }
        }
    };

    public void startSearch()
    {
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {


            }
        });
    }
    WifiP2pManager.PeerListListener peerlistListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {
            if(!peersList.getDeviceList().equals(peers))
            {
                peers.clear();
                peers.addAll(peersList.getDeviceList());

                deviceNameArray = new String[peersList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peersList.getDeviceList().size()];

                int index = 0;
                for (WifiP2pDevice device : peersList.getDeviceList())
                {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index+=1;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                peersListView.setAdapter(adapter);
            }
            if(peers.size()==0)
            {
                Toast.makeText(SendOrReceive.this,"No peers available",Toast.LENGTH_SHORT).show();
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);

    }

}
