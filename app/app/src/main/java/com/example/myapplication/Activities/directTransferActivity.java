package com.example.myapplication.Activities;

import android.Manifest;
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
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class directTransferActivity extends AppCompatActivity {

    private static final String TAG = "FileShareApp-Direct";
    private static final int PORT = 8888;
    private static final int SOCKET_TIMEOUT_MS = 10_000;

    // Wi-Fi P2P
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    // Peer state
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private boolean isGroupOwner = false;
    private InetAddress ownerAddress = null;

    // UI references
    private TextView statusText, statusSubtitle, progressText, transferLabel;
    private ProgressBar statusSpinner;
    private LinearProgressIndicator progressBar;
    private MaterialCardView progressCard;
    private LinearLayout peersContainer, emptyPeers, filesContainer, emptyFiles;
    private MaterialButton btnDiscover, btnPickFile;

    // Thread management
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicBoolean serverRunning = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    // File queue for multi-file send
    private final List<Uri> pendingUris = new ArrayList<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);

    // ── File picker ────────────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                pendingUris.clear();
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                        if (uri != null) pendingUris.add(uri);
                    }
                } else if (result.getData().getData() != null) {
                    pendingUris.add(result.getData().getData());
                }
                if (!pendingUris.isEmpty()) sendNextFile();
            });

    // ── Permission launcher ────────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
                boolean allGranted = !grants.containsValue(false);
                if (allGranted) startDiscovery();
                else showToast("Location permission is required for Wi-Fi Direct");
            });

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_transfer);
        bindViews();
        setupWifiDirect();
        startServer();
//        refreshFileList();

        btnDiscover.setOnClickListener(v -> checkPermissionsAndDiscover());
        btnPickFile.setOnClickListener(v -> {
            if (ownerAddress == null) {
                showToast("Connect to a device first");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            filePickerLauncher.launch(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serverRunning.set(false);
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        executor.shutdownNow();
        if (channel != null) channel.close();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI Setup
    // ══════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        statusText     = findViewById(R.id.status_text);
        statusSubtitle = findViewById(R.id.status_subtitle);
        statusSpinner  = findViewById(R.id.status_spinner);
        progressCard   = findViewById(R.id.progress_card);
        progressBar    = findViewById(R.id.progress_bar);
        progressText   = findViewById(R.id.progress_text);
        transferLabel  = findViewById(R.id.transfer_label);
        peersContainer = findViewById(R.id.peers_container);
        emptyPeers     = findViewById(R.id.empty_peers);
//        filesContainer = findViewById(R.id.files_container);
//        emptyFiles     = findViewById(R.id.empty_files);
        btnDiscover    = findViewById(R.id.btn_discover);
        btnPickFile    = findViewById(R.id.btn_pick_file);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Wi-Fi Direct setup
    // ══════════════════════════════════════════════════════════════════════════

    private void setupWifiDirect() {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private void checkPermissionsAndDiscover() {
        List<String> needed = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                        != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        if (needed.isEmpty()) {
            startDiscovery();
        } else {
            permissionLauncher.launch(needed.toArray(new String[0]));
        }
    }

    // ── Discovery ──────────────────────────────────────────────────────────────

    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        setStatus("Discovering…", "Looking for nearby devices");
        setSpinner(true);
        peers.clear();
        renderPeerList();

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                setStatus("Discovering…", "Scanning for nearby devices");
            }
            @Override public void onFailure(int reason) {
                setSpinner(false);
                setStatus("Discovery failed", reasonText(reason));
            }
        });
    }

    private void connectToPeer(WifiP2pDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        setStatus("Connecting…", "Connecting to " + device.deviceName);
        setSpinner(true);

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() { /* connection result comes via broadcast */ }
            @Override public void onFailure(int reason) {
                setSpinner(false);
                setStatus("Connection failed", reasonText(reason));
                showToast("Could not connect to " + device.deviceName);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Server (receive side)
    // ══════════════════════════════════════════════════════════════════════════

    private void startServer() {
        if (serverRunning.getAndSet(true)) return;
        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                Log.d(TAG, "Server listening on port " + PORT);
                while (serverRunning.get()) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.execute(() -> receiveFile(client));
                    } catch (Exception e) {
                        if (serverRunning.get()) Log.e(TAG, "Accept error", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Server init error", e);
            }
        });
    }

    private void receiveFile(Socket socket) {
        try {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            java.io.DataInputStream dis = new java.io.DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            String fileName = dis.readUTF();
            long fileSize   = dis.readLong();

            setStatus("Receiving", fileName);
            showProgressCard(true, "Receiving " + fileName);

            File outFile = new File(getShareDirectory(), sanitizeFileName(fileName));
            try (FileOutputStream fos = new FileOutputStream(outFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buf = new byte[65536];
                long received = 0;
                int n;
                while (received < fileSize &&
                        (n = dis.read(buf, 0, (int) Math.min(buf.length, fileSize - received))) != -1) {
                    bos.write(buf, 0, n);
                    received += n;
                    final int pct = (int) (received * 100 / fileSize);
                    updateProgress(pct, "Receiving " + fileName);
                }
                bos.flush();
            }
            socket.close();

            runOnUiThread(() -> {
                showProgressCard(false, null);
                setStatus("Transfer complete", "Received " + fileName);
//                refreshFileList();
                showToast("Received: " + fileName);
            });

        } catch (Exception e) {
            Log.e(TAG, "Receive error", e);
            runOnUiThread(() -> {
                showProgressCard(false, null);
                setStatus("Receive failed", e.getMessage());
            });
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Client (send side) — sequential multi-file queue
    // ══════════════════════════════════════════════════════════════════════════

    private void sendNextFile() {
        if (pendingUris.isEmpty()) {
            isSending.set(false);
            runOnUiThread(() -> setStatus("All files sent", "Transfer complete"));
            showToast("All files sent!");
            return;
        }
        if (!isSending.compareAndSet(false, true)) return;
        Uri uri = pendingUris.remove(0);
        executor.execute(new SendTask(uri));
    }

    private class SendTask implements Runnable {
        private final Uri uri;
        SendTask(Uri uri) { this.uri = uri; }

        @Override
        public void run() {
            String fileName = "file_" + System.currentTimeMillis();
            long fileSize = 0;

            try (android.database.Cursor c = getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int ni = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    int si = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (ni >= 0) fileName = c.getString(ni);
                    if (si >= 0) fileSize = c.getLong(si);
                }
            } catch (Exception e) {
                Log.w(TAG, "Cursor query failed", e);
            }

            final String finalName = fileName;
            final long finalSize = fileSize;
            runOnUiThread(() -> {
                setStatus("Sending", finalName);
                showProgressCard(true, "Sending " + finalName);
            });

            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(ownerAddress, PORT), SOCKET_TIMEOUT_MS);
                socket.setSoTimeout(SOCKET_TIMEOUT_MS * 6);

                java.io.DataOutputStream dos = new java.io.DataOutputStream(
                        new BufferedOutputStream(socket.getOutputStream()));

                dos.writeUTF(finalName);
                dos.writeLong(finalSize);
                dos.flush();

                try (InputStream is = getContentResolver().openInputStream(uri);
                     BufferedInputStream bis = new BufferedInputStream(is)) {
                    byte[] buf = new byte[65536];
                    int n;
                    long sent = 0;
                    while ((n = bis.read(buf)) != -1) {
                        dos.write(buf, 0, n);
                        sent += n;
                        if (finalSize > 0) {
                            final int pct = (int) (sent * 100 / finalSize);
                            updateProgress(pct, "Sending " + finalName);
                        }
                    }
                    dos.flush();
                }
                socket.close();

                isSending.set(false);
                runOnUiThread(() -> showProgressCard(false, null));
                sendNextFile();

            } catch (Exception e) {
                Log.e(TAG, "Send error", e);
                isSending.set(false);
                runOnUiThread(() -> {
                    showProgressCard(false, null);
                    setStatus("Send failed", e.getMessage());
                    showToast("Failed to send: " + finalName);
                });
            } finally {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // File list
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshFileList() {
        File dir = getShareDirectory();
        List<File> files = new ArrayList<>();
        File[] listing = dir.listFiles();
        if (listing != null) {
            for (File f : listing) if (f.isFile()) files.add(f);
        }
        Collections.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        runOnUiThread(() -> {
            filesContainer.removeAllViews();
            if (files.isEmpty()) {
                emptyFiles.setVisibility(View.VISIBLE);
                filesContainer.setVisibility(View.GONE);
            } else {
                emptyFiles.setVisibility(View.GONE);
                filesContainer.setVisibility(View.VISIBLE);
                LayoutInflater inflater = LayoutInflater.from(this);
                for (File f : files) {
                    View card = inflater.inflate(R.layout.item_file, filesContainer, false);
                    TextView name = card.findViewById(R.id.fileName);
                    TextView size = card.findViewById(R.id.fileSize);
                    ImageView icon = card.findViewById(R.id.thumbnail);
                    name.setText(f.getName());
                    size.setText(formatSize(f.length()));
                    icon.setImageResource(iconForFile(f.getName()));
                    card.setOnClickListener(v -> openFile(f));
                    filesContainer.addView(card);
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Peer list rendering
    // ══════════════════════════════════════════════════════════════════════════

    private void renderPeerList() {
        runOnUiThread(() -> {
            peersContainer.removeAllViews();
            setSpinner(false);
            if (peers.isEmpty()) {
                emptyPeers.setVisibility(View.VISIBLE);
                peersContainer.setVisibility(View.GONE);
                setStatus("No devices found", "Make sure the other device has Wi-Fi on");
            } else {
                emptyPeers.setVisibility(View.GONE);
                peersContainer.setVisibility(View.VISIBLE);
                setStatus("Found " + peers.size() + " device(s)", "Tap a device to connect");
                for (WifiP2pDevice device : peers) {
                    MaterialCardView card = buildPeerCard(device);
                    peersContainer.addView(card);
                }
            }
        });
    }

    private MaterialCardView buildPeerCard(WifiP2pDevice device) {
        // Inflate item_file and repurpose as a peer card row
        View row = LayoutInflater.from(this).inflate(R.layout.item_file, peersContainer, false);
        TextView name = row.findViewById(R.id.fileName);
        TextView sub  = row.findViewById(R.id.fileSize);
        ImageView icon = row.findViewById(R.id.thumbnail);
        View badge = row.findViewById(R.id.fileUniqueText); // repurpose hidden field

        name.setText(device.deviceName.isEmpty() ? device.deviceAddress : device.deviceName);
        sub.setText(deviceStatusText(device.status));
        icon.setImageResource(R.drawable.ic_share);
        if (badge != null) badge.setVisibility(View.GONE);

        row.setOnClickListener(v -> connectToPeer(device));
        return (MaterialCardView) row; // item_file root IS a MaterialCardView
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Broadcast receiver
    // ══════════════════════════════════════════════════════════════════════════

    private class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        setStatus("Wi-Fi Direct unavailable", "Please enable Wi-Fi");
                        showToast("Please enable Wi-Fi Direct");
                    }
                    break;
                }
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    if (ActivityCompat.checkSelfPermission(directTransferActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) break;
                    manager.requestPeers(channel, peerListListener);
                    break;
                }
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    manager.requestConnectionInfo(channel, connectionInfoListener);
                    break;
                }
            }
        }
    }

    private final WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                renderPeerList();
            }
        }
    };

    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            if (info.groupFormed) {
                ownerAddress = info.groupOwnerAddress;
                isGroupOwner = info.isGroupOwner;
                setSpinner(false);
                if (isGroupOwner) {
                    setStatus("Connected — Receiving mode", "Waiting to receive files");
                } else {
                    setStatus("Connected — Sending mode", "Tap Send Files to share");
                }
            } else {
                ownerAddress = null;
                setStatus("Disconnected", "Tap Discover to find devices");
            }
        }
    };

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private File getShareDirectory() {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                getResources().getString(R.string.download_folder));
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            String ext = "";
            int dot = file.getName().lastIndexOf('.');
            if (dot > 0) ext = file.getName().substring(dot + 1).toLowerCase();
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mime == null) mime = "*/*";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            showToast("Cannot open file");
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._\\-() ]", "_");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private int iconForFile(String name) {
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) ext = name.substring(dot + 1).toLowerCase();
        switch (ext) {
            case "pdf":  return R.drawable.ic_pdf;
            case "jpg": case "jpeg": case "png": case "gif": case "webp":
                return R.drawable.ic_image;
            case "mp3": case "wav": case "ogg": case "m4a":
                return R.drawable.ic_audio;
            default:     return R.drawable.ic_docs;
        }
    }

    private String deviceStatusText(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:   return "Available";
            case WifiP2pDevice.INVITED:     return "Invited";
            case WifiP2pDevice.CONNECTED:   return "Connected";
            case WifiP2pDevice.FAILED:      return "Failed";
            case WifiP2pDevice.UNAVAILABLE: return "Unavailable";
            default: return "Unknown";
        }
    }

    private String reasonText(int reason) {
        switch (reason) {
            case WifiP2pManager.ERROR:            return "Internal error";
            case WifiP2pManager.P2P_UNSUPPORTED:  return "Wi-Fi Direct not supported";
            case WifiP2pManager.BUSY:             return "System busy, try again";
            default: return "Error code " + reason;
        }
    }

    // ── UI update helpers ──────────────────────────────────────────────────────

    private void setStatus(String title, String subtitle) {
        runOnUiThread(() -> {
            statusText.setText(title);
            statusSubtitle.setText(subtitle);
        });
    }

    private void setSpinner(boolean show) {
        runOnUiThread(() -> statusSpinner.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    private void showProgressCard(boolean show, String label) {
        runOnUiThread(() -> {
            progressCard.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show && label != null) {
                transferLabel.setText(label);
                progressBar.setProgress(0);
                progressText.setText("0%");
            }
        });
    }

    private void updateProgress(int pct, String label) {
        runOnUiThread(() -> {
            progressBar.setProgressCompat(pct, true);
            progressText.setText(pct + "%");
            if (label != null) transferLabel.setText(label);
        });
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(directTransferActivity.this, msg, Toast.LENGTH_SHORT).show());
    }
}