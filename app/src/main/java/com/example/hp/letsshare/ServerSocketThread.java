package com.example.hp.letsshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerSocketThread extends AppCompatActivity{

    Socket socketForServer;
    ServerSocket serverSocket;
    final int SocketServerPORT = 8080;
    final int REQUEST_CODE = 48;
    ImageButton sendButton;
    ImageButton selectButton;
    Uri uri;
    TextView selectEdit;
    TextView sendStatus;
    int width;
    int height;
    int size;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serversocketthread);
        initialise();
        addOnClickListeners();

        ServerThread serverThread = new ServerThread();
        serverThread.start();
    }
    public void initialise()
    {
        sendButton = (ImageButton)findViewById(R.id.sendButton);
        selectButton = (ImageButton)findViewById(R.id.selectButton);
        selectEdit = (TextView)findViewById(R.id.selectEditText);
        selectEdit.setText("Nothing has been selected");

        sendStatus = (TextView)findViewById(R.id.sendStatus);
    }
    public void addOnClickListeners()
    {
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch();
                if(uri!=null)
                {
                    ServerSocketThread.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                           selectEdit.setText(uri.getPath());
                        }});

                }
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(uri == null)
                {
                    ServerSocketThread.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(ServerSocketThread.this,R.style.MyDialogTheme);
                            dlgAlert.setMessage("You need to select something first");
                            dlgAlert.setPositiveButton("Ok",null);
                            dlgAlert.setTitle("Let's Share");
                            dlgAlert.setCancelable(true);
                            dlgAlert.create().show();
                        }
                    });
                }
                else {
                    ServerSocketThread.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                           sendStatus.setText("Sending.........");
                        }});

                FileTxThread fileTxThread = new FileTxThread(socketForServer);
                fileTxThread.start();}
            }
        });
    }

    private void startSearch()
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent,REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK)
        {
            if(data!=null){
                uri = data.getData();
                //path = uri.toString();
            }
        }
    }

    public class ServerThread extends Thread
    {
        @Override
        public void run()
        {
            socketForServer=null;
            try {
                serverSocket = new ServerSocket(SocketServerPORT);

                while (true) {
                    socketForServer = serverSocket.accept();

                    ServerSocketThread.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(ServerSocketThread.this,
                                    "connected",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (socketForServer != null) {
                    try {
                        socketForServer.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }}
    public class FileTxThread extends Thread {
        Socket socket;
        FileTxThread(Socket socket){
            this.socket= socket;
        }
        @Override
        public void run() {
            ContentResolver cr = getContentResolver();

            try {
                InputStream is = cr.openInputStream(uri);
                final Bitmap bitmap= MediaStore.Images.Media.getBitmap( ServerSocketThread.this.getContentResolver(),uri);

                //converting bitmap to byte array
                byte[] bytes = null;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                bytes = stream.toByteArray();
                bitmap.recycle();
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(bytes);
                oos.flush();
                final String sentMsg = "File sent to: " + socket.getInetAddress();
                ServerSocketThread.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ServerSocketThread.this,
                                sentMsg,
                                Toast.LENGTH_LONG).show();
                    }});

            } catch (FileNotFoundException e) {
                //Toast.makeText(getApplicationContext(),"file not found",Toast.LENGTH_SHORT).show();
                ServerSocketThread.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(ServerSocketThread.this,
                                "File Found",
                                Toast.LENGTH_LONG).show();
                    }});
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                ServerSocketThread.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        selectEdit.setText("Nothing has been selected yet");
                        sendStatus.setText("Sent Successfully");
                    }});

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
}