package com.example.hp.letsshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private SendOrReceive mActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, SendOrReceive mActivity)
    {

        this.mChannel = mChannel;
        this.mWifiP2pManager = mManager;
        this.mActivity = mActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
        {
            //Checks if the wifi is turned on or off just to show a toast
            //Not necessary for this app
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action))
        {

            if(mWifiP2pManager==null)
            {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected())
            {
                mWifiP2pManager.requestConnectionInfo(mChannel,mActivity.mConnectionInfoListener);
            }
        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
        {

            if(mActivity!=null)
            {
                mWifiP2pManager.requestPeers(mChannel,mActivity.peerlistListener);
            }

        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))
        {

        }
    }
}