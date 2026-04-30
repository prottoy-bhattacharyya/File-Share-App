package com.example.myapplication.Activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.webkit.MimeTypeMap;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.LocalDb.AppDatabase;
import com.example.myapplication.LocalDb.LocalFile;
import com.example.myapplication.R;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class ReceivedFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<SharedFile> fileListFull = new ArrayList<>();
    private SearchView searchView;
    private String uniqueText;
    private TextView tv_unique_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_files);

        recyclerView   = findViewById(R.id.recycler_view);
        searchView     = findViewById(R.id.search_view);
        tv_unique_text = findViewById(R.id.tv_unique_text_value);

        uniqueText = getIntent().getStringExtra("unique_text");
        if (uniqueText == null) {
            uniqueText = "";
            tv_unique_text.setVisibility(View.GONE);
        } else {
            tv_unique_text.append(uniqueText);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LayoutAnimationController animation =
                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down);
        recyclerView.setLayoutAnimation(animation);

        loadFiles();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    // ── Load files from Room DB + file system ─────────────────────────────────

    private void loadFiles() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // Fetch all LocalFile records.
            // If a specific uniqueText was passed in, filter by it;
            // otherwise load every record so all received files are shown.
            List<LocalFile> dbFiles;
            if (uniqueText.isEmpty()) {
                dbFiles = db.fileDao().getAllFiles();
            } else {
                dbFiles = db.fileDao().getFilesByCode(uniqueText);
            }

            // Build a map: fileName → uniqueText for badge display
            // (a filename might appear under multiple codes — keep the first/most recent)
            Map<String, String> fileNameToCode = new HashMap<>();
            Set<String> allowedFileNames = new HashSet<>();
            for (LocalFile f : dbFiles) {
                if (f.fileName != null && !f.fileName.isEmpty()) {
                    allowedFileNames.add(f.fileName);
                    // putIfAbsent: keep the first (most-recent-first order from getAllFiles)
                    if (!fileNameToCode.containsKey(f.fileName)) {
                        fileNameToCode.put(f.fileName, f.uniqueText != null ? f.uniqueText : "");
                    }
                }
            }

            fileListFull = fetchFilesFromFolder(allowedFileNames, fileNameToCode);

            runOnUiThread(() -> {
                adapter = new FileAdapter(this, new ArrayList<>(fileListFull));
                recyclerView.setAdapter(adapter);

                if (fileListFull.isEmpty()) {
                    findViewById(R.id.empty_message).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.empty_message).setVisibility(View.GONE);
                    recyclerView.scheduleLayoutAnimation();
                }
            });
        });
    }

    // ── Scan the download folder and match against DB records ─────────────────

    private List<SharedFile> fetchFilesFromFolder(Set<String> allowedNames,
                                                  Map<String, String> fileNameToCode) {
        List<SharedFile> list = new ArrayList<>();

        File folder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "File Share App");

        if (!folder.exists() || !folder.isDirectory()) return list;

        File[] files = folder.listFiles();
        if (files == null) return list;

        // If no DB records exist yet, fall back to showing all files (no badge)
        boolean noFilter = allowedNames.isEmpty();

        for (File file : files) {
            if (!file.isFile()) continue;
            if (!noFilter && !allowedNames.contains(file.getName())) continue;

            Uri fileUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);
            String mimeType = getMimeType(file);

            // Look up the unique code for this file; empty string if unknown
            String code = fileNameToCode.getOrDefault(file.getName(), "");

            list.add(new SharedFile(file.getName(), file.length(), fileUri, mimeType, code));
        }
        return list;
    }

    private String getMimeType(File file) {
        String extension = "";
        int dot = file.getName().lastIndexOf('.');
        if (dot > 0) extension = file.getName().substring(dot + 1);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return mimeType != null ? mimeType : "*/*";
    }

    // ── Data model ────────────────────────────────────────────────────────────

    public static class SharedFile {
        private final String name;
        private final long size;
        private final Uri uri;
        private final String mimeType;
        private final String uniqueText; // code badge shown on the right

        public SharedFile(String name, long size, Uri uri, String mimeType, String uniqueText) {
            this.name       = name;
            this.size       = size;
            this.uri        = uri;
            this.mimeType   = mimeType;
            this.uniqueText = uniqueText;
        }

        public String getName()       { return name; }
        public long   getSize()       { return size; }
        public Uri    getUri()        { return uri; }
        public String getMimeType()   { return mimeType; }
        public String getUniqueText() { return uniqueText; }
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder>
            implements Filterable {

        private final Context context;
        private List<SharedFile> filesDisplayed;

        FileAdapter(Context context, List<SharedFile> files) {
            this.context        = context;
            this.filesDisplayed = files;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<SharedFile> filtered = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) {
                        filtered.addAll(fileListFull);
                    } else {
                        String pattern = constraint.toString().toLowerCase().trim();
                        for (SharedFile item : fileListFull) {
                            if (item.getName().toLowerCase().contains(pattern))
                                filtered.add(item);
                        }
                    }
                    FilterResults results = new FilterResults();
                    results.values = filtered;
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filesDisplayed.clear();
                    filesDisplayed.addAll((List<SharedFile>) results.values);
                    notifyDataSetChanged();
                }
            };
        }

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull FileViewHolder h, int position) {
            SharedFile file = filesDisplayed.get(position);

            h.nameTxt.setText(file.getName());
            h.sizeTxt.setText(Formatter.formatShortFileSize(context, file.getSize()));

            // ── Unique-text badge ─────────────────────────────────────────
            String code = file.getUniqueText();
            if (code != null && !code.isEmpty()) {
                h.cardUniqueText.setVisibility(View.VISIBLE);
                h.tvUniqueText.setText(code);
            } else {
                h.cardUniqueText.setVisibility(View.GONE);
            }

            // ── Thumbnail ─────────────────────────────────────────────────
            int iconResId = getIconForFileType(file.getMimeType(), file.getName());
            if (file.getMimeType().startsWith("image/")) {
                Glide.with(context)
                        .load(file.getUri())
                        .placeholder(R.drawable.ic_image)
                        .into(h.imgThumb);
            } else {
                h.imgThumb.setImageResource(iconResId);
            }

            // ── Open on tap ───────────────────────────────────────────────
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(file.getUri(), file.getMimeType());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, "No app to open this file", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return filesDisplayed.size(); }

        class FileViewHolder extends RecyclerView.ViewHolder {
            TextView         nameTxt, sizeTxt, tvUniqueText;
            ImageView        imgThumb;
            MaterialCardView cardUniqueText;

            FileViewHolder(@NonNull View v) {
                super(v);
                nameTxt        = v.findViewById(R.id.fileName);
                sizeTxt        = v.findViewById(R.id.fileSize);
                imgThumb       = v.findViewById(R.id.thumbnail);
                tvUniqueText   = v.findViewById(R.id.fileUniqueText);
                cardUniqueText = v.findViewById(R.id.cardUniqueText);
            }
        }

        private int getIconForFileType(String mimeType, String fileName) {
            if (mimeType == null) mimeType = "*/*";
            if (mimeType.startsWith("image/"))   return R.drawable.ic_image;
            if (mimeType.startsWith("audio/"))   return R.drawable.ic_audio;
            if (mimeType.equals("application/pdf")) return R.drawable.ic_pdf;
            String name = fileName.toLowerCase();
            if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                    name.endsWith(".doc") || name.endsWith(".docx") ||
                    name.endsWith(".xls") || name.endsWith(".xlsx") ||
                    name.endsWith(".txt")) return R.drawable.ic_docs;
            return R.drawable.ic_unknown;
        }
    }
}