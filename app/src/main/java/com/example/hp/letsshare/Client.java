package com.example.hp.letsshare;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends AppCompatActivity {

    TextView receiveStatus;

    final int portNumber = 8080;

    String address;

    private int requestCode;
    private int grantResults[];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        initialise();

        address = SendOrReceive.getIp();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            //if you dont have required permissions ask for it (only required for API 23+)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            onRequestPermissionsResult(requestCode, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, grantResults);
        }

        ClientThread clientThread = new ClientThread(address, portNumber);
        clientThread.start();
    }

    @Override // android recommended class to handle permissions
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("permission", "granted");
                } else {

                    // permission denied.
                    Client.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(Client.this,
                                    "Permission Denied",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                    //app cannot function without this permission for now so close it...
                    onDestroy();
                }
                return;
            }
        }
    }

    public void initialise() {
        receiveStatus = (TextView) findViewById(R.id.receiveStatus);
        Client.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
               receiveStatus.setText("Receiving........");
            }});

    }

    public class ClientThread extends Thread {
        String address;
        int port;

        public ClientThread(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            try {
                socket = new Socket(address, portNumber);
                File fileDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "LetsShare");
                if (fileDir.exists() == false) {
                    fileDir.mkdirs();
                }
                File file = new File(fileDir, "test.jpg");
                file.createNewFile();
                InputStream is = socket.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                FileOutputStream fos = null;
                byte[] bytes = null;

                try {
                    bytes = (byte[]) ois.readObject();
                } catch (ClassNotFoundException e) {

                }catch (EOFException e)
                {

                }
                finally {
                    ois.close();
                }
                Client.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        receiveStatus.setText("Saving........");
                    }});
                final Bitmap myBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);


                try {

                    fos = new FileOutputStream(file);
                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();


                    Client.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            receiveStatus.setText("Finished.........");
                        }
                    });
                    scanMedia(file.toString());
                    //File toSet = new File(file.toString());
                    //imageView.setImageBitmap(BitmapFactory.decodeFile(toSet.getAbsolutePath()));
                } catch (Exception e) {
                    Client.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(Client.this,
                                    "sorry",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                final String eMsg = "Something wrong from client: " + e.getMessage();
                Client.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Client.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });

            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                        deletePersistentGroups();
                        disconnect();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        private void scanMedia(String path) {
            File file = new File(path);
            Uri uri = Uri.fromFile(file);
            Intent scanFileIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            sendBroadcast(scanFileIntent);
        }
    }

    public static void disconnect() {
        if (SendOrReceive.mWifiP2pManager != null && SendOrReceive.mChannel != null) {
            SendOrReceive.mWifiP2pManager.requestGroupInfo(SendOrReceive.mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && SendOrReceive.mWifiP2pManager != null && SendOrReceive.mChannel != null
                            && group.isGroupOwner()) {
                        SendOrReceive.mWifiP2pManager.removeGroup(SendOrReceive.mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onFailure(int reason) {

                            }
                        });
                    }
                }
            });
        }
    }
    private void deletePersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(SendOrReceive.mWifiP2pManager, SendOrReceive.mChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}