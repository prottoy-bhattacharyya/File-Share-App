package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class directTransferActivity extends AppCompatActivity {

    private static final String TAG = "WiFiDirectApp";
    private static final int PORT = 8888;
    private static final String SHARE_FOLDER = "wifi direct share";

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private String[] peerNames;
    private WifiP2pDevice[] peerDevices;

    private TextView statusText, progressText;
    private ProgressBar progressBar;
    private ListView peerListView, receivedFilesListView;
    private Button btnDiscover, btnPickFile;

    private List<String> receivedFileNames = new ArrayList<>();
    private ArrayAdapter<String> receivedFilesAdapter;

    private boolean isGroupOwner = false;
    private InetAddress ownerAddress;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null){
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                            if (uri != null) {
                                sendFile(uri);
                            }
                        }
                    }
                    else {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            sendFile(uri);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_transfer);

        statusText = findViewById(R.id.status_text);
        progressText = findViewById(R.id.progress_text);
        progressBar = findViewById(R.id.progress_bar);
        peerListView = findViewById(R.id.peer_list);
        receivedFilesListView = findViewById(R.id.received_files_list);
        btnDiscover = findViewById(R.id.btn_discover);
        btnPickFile = findViewById(R.id.btn_pick_file);

        receivedFilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, receivedFileNames);
        receivedFilesListView.setAdapter(receivedFilesAdapter);

        loadReceivedFiles();

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver();

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        btnDiscover.setOnClickListener(v -> discoverPeers());
        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            filePickerLauncher.launch(intent);
        });

        peerListView.setOnItemClickListener((parent, view, position, id) -> {
            WifiP2pDevice device = peerDevices[position];
            connectToPeer(device);
        });

        receivedFilesListView.setOnItemClickListener((parent, view, position, id) -> {
            String fileName = receivedFileNames.get(position);
            File file = new File(getShareDirectory(), fileName);
            openFile(file);
        });

        // Start server thread to listen for incoming files
        new Thread(new ServerThread()).start();
    }

    private File getShareDirectory() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SHARE_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void loadReceivedFiles() {
        File dir = getShareDirectory();
        File[] files = dir.listFiles();
        receivedFileNames.clear();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    receivedFileNames.add(file.getName());
                }
            }
        }
        Collections.sort(receivedFileNames);
        receivedFilesAdapter.notifyDataSetChanged();
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            String fileName = file.getName();
            String extension = "";
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1);
            }
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType == null) mimeType = "*/*";

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            showToast("Cannot open file: " + e.getMessage());
        }
    }

    private void discoverPeers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                statusText.setText("Discovery Started");
            }

            @Override
            public void onFailure(int reasonCode) {
                statusText.setText("Discovery Failed: " + reasonCode);
            }
        });
    }

    private void connectToPeer(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(directTransferActivity.this, "Connecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(directTransferActivity.this, "Connect Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFile(Uri uri) {
        if (ownerAddress == null) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new ClientThread(uri)).start();
    }

    private class ServerThread implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                serverSocket.setReuseAddress(true);
                while (true) {
                    Socket client = serverSocket.accept();
                    receiveFile(client);
                }
            } catch (Exception e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }

        private void receiveFile(Socket socket) {
            new Thread(() -> {
                try {
                    updateUIStatus("Receiving metadata...");
                    InputStream is = socket.getInputStream();
                    java.io.DataInputStream dis = new java.io.DataInputStream(is);

                    // Read metadata
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    updateUIStatus("Receiving " + fileName + " (" + fileSize + " bytes)");

                    File file = new File(getShareDirectory(), fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalReceived = 0;

                    showProgress(true);
                    while (totalReceived < fileSize && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalReceived))) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        totalReceived += bytesRead;
                        updateProgress((int) ((totalReceived * 100) / fileSize));
                    }
                    bos.flush();
                    bos.close();
                    socket.close();

                    updateUIStatus("Received: " + fileName);
                    showToast("File received successfully!");
                    showProgress(false);

                    // Refresh file list on UI thread
                    new Handler(Looper.getMainLooper()).post(directTransferActivity.this::loadReceivedFiles);
                } catch (Exception e) {
                    updateUIStatus("Receive Failed: " + e.getMessage());
                    showProgress(false);
                    Log.e(TAG, "Receive error", e);
                }
            }).start();
        }
    }

    private class ClientThread implements Runnable {
        private Uri uri;

        ClientThread(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void run() {
            try {
                updateUIStatus("Connecting to receiver...");
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ownerAddress, PORT), 5000);

                updateUIStatus("Sending metadata...");
                OutputStream os = socket.getOutputStream();
                java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

                // Get file info
                String fileName = "file_" + System.currentTimeMillis();
                long fileSize = 0;

                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex);
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
                    cursor.close();
                }

                // Send metadata
                dos.writeUTF(fileName);
                dos.writeLong(fileSize);
                dos.flush();

                updateUIStatus("Sending " + fileName + "...");
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedInputStream bis = new BufferedInputStream(is);

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalSent = 0;

                showProgress(true);
                while ((bytesRead = bis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    if (fileSize > 0) {
                        int progress = (int) ((totalSent * 100) / fileSize);
                        updateProgress(progress);
                    }
                }
                dos.flush();
                socket.close();
                bis.close();

                updateUIStatus("Sent " + fileName + " successfully!");
                showToast("All files sent successfully!");
                showProgress(false);

            } catch (Exception e) {
                updateUIStatus("Send Failed: " + e.getMessage());
                showProgress(false);
                Log.e(TAG, "Send error", e);
            }
        }
    }

    private void updateUIStatus(String status) {
        new Handler(Looper.getMainLooper()).post(() -> statusText.setText("Status: " + status));
    }

    private void updateProgress(int progress) {
        new Handler(Looper.getMainLooper()).post(() -> {
            progressBar.setProgress(progress);
            progressText.setText(progress + "%");
        });
    }

    private void showProgress(boolean show) {
        new Handler(Looper.getMainLooper()).post(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressText.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(directTransferActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Toast.makeText(directTransferActivity.this, "Please enable Wi-Fi", Toast.LENGTH_LONG).show();
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (ActivityCompat.checkSelfPermission(directTransferActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                manager.requestPeers(channel, peerListListener);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                manager.requestConnectionInfo(channel, connectionInfoListener);
            }
        }
    }

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                peerNames = new String[peerList.getDeviceList().size()];
                peerDevices = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    peerNames[index] = device.deviceName;
                    peerDevices[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(directTransferActivity.this, android.R.layout.simple_list_item_1, peerNames);
                peerListView.setAdapter(adapter);
            }
        }
    };

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            ownerAddress = info.groupOwnerAddress;
            isGroupOwner = info.isGroupOwner;

            if (info.groupFormed) {
                statusText.setText(isGroupOwner ? "Group Owner (Receiver)" : "Connected to Group Owner (Sender)");
                if (!isGroupOwner && ownerAddress != null) {
                    // We are the client, ownerAddress is the receiver
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}