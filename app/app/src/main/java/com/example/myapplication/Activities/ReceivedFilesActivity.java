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
import com.example.myapplication.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReceivedFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<SharedFile> fileListFull = new ArrayList<>();
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_files);

        recyclerView = findViewById(R.id.recycler_view);
        searchView = findViewById(R.id.search_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFiles();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });
    }

    private void loadFiles() {
        fileListFull = fetchFilesFromFolder();

        adapter = new FileAdapter(this, new ArrayList<>(fileListFull));
        recyclerView.setAdapter(adapter);

        if (fileListFull.isEmpty()) {
            findViewById(R.id.empty_message).setVisibility(View.VISIBLE);
        }
    }

    private List<SharedFile> fetchFilesFromFolder() {
        List<SharedFile> list = new ArrayList<>();

        File folder = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/File Share App");
        File directory = new File(folder.toURI());

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {

                        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

                        String mimeType = getMimeType(file);

                        list.add(new SharedFile(
                                file.getName(),
                                file.length(),
                                fileUri,
                                mimeType
                        ));
                    }
                }
            }
        }
        return list;
    }

    private String getMimeType(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        String fileName = file.getName();
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        if (mimeType == null) mimeType = "*/*";
        return mimeType;
    }

    public static class SharedFile {
        private final String name;
        private final long size;
        private final Uri uri;
        private final String mimeType;

        public SharedFile(String name, long size, Uri uri, String mimeType) {
            this.name = name;
            this.size = size;
            this.uri = uri;
            this.mimeType = mimeType;
        }
        public String getName() { return name; }
        public long getSize() { return size; }
        public Uri getUri() { return uri; }
        public String getMimeType() { return mimeType; }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> implements Filterable {
        private final Context context;
        private List<SharedFile> filesDisplayed;

        public FileAdapter(Context context, List<SharedFile> files) {
            this.context = context;
            this.filesDisplayed = files;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<SharedFile> filteredList = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        filteredList.addAll(fileListFull);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        for (SharedFile item : fileListFull) {
                            if (item.getName().toLowerCase().contains(filterPattern)) {
                                filteredList.add(item);
                            }
                        }
                    }

                    FilterResults results = new FilterResults();
                    results.values = filteredList;
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
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            SharedFile file = filesDisplayed.get(position);
            holder.nameTxt.setText(file.getName());
            holder.sizeTxt.setText(Formatter.formatShortFileSize(context, file.getSize()));




            int iconResId = getIconForFileType(file.getMimeType(), file.getName());

            if (file.getMimeType().startsWith("image/")) {
                Glide.with(context).load(file.getUri()).placeholder(R.drawable.ic_image).into(holder.imgThumb);
            } else {
                holder.imgThumb.setImageResource(iconResId);
            }

            holder.itemView.setOnClickListener(v -> {
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
            TextView nameTxt, sizeTxt;
            ImageView imgThumb;
            public FileViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTxt = itemView.findViewById(R.id.fileName);
                sizeTxt = itemView.findViewById(R.id.fileSize);
                imgThumb = itemView.findViewById(R.id.thumbnail);
            }
        }

        private int getIconForFileType(String mimeType, String fileName) {
            if (mimeType == null) mimeType = "*/*";


            if (mimeType.startsWith("audio/")) return R.drawable.ic_audio;
            if (mimeType.equals("application/pdf")) return R.drawable.ic_pdf;

            String name = fileName.toLowerCase();
            if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")) return R.drawable.ic_docs;
            if (name.endsWith(".doc") || name.endsWith(".docx")) return R.drawable.ic_docs;
            if (name.endsWith(".xls") || name.endsWith(".xlsx")) return R.drawable.ic_docs;
            if (name.endsWith(".txt")) return R.drawable.ic_docs;

            return R.drawable.ic_docs;
        }
    }
}