package com.example.myapplication;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ReceivedFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private List<SharedFile> fileListFull = new ArrayList<>(); // Original backup
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_files);

        recyclerView = findViewById(R.id.recycler_view);
        searchView = findViewById(R.id.search_view); // Ensure this ID exists in your XML

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFiles();

        // Implement the search listener
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
        // Pass a COPY of the list to the adapter
        adapter = new FileAdapter(this, new ArrayList<>(fileListFull));
        recyclerView.setAdapter(adapter);

        if (fileListFull.isEmpty()) {
            Toast.makeText(this, "No files found", Toast.LENGTH_SHORT).show();
        }
    }

    private List<SharedFile> fetchFilesFromFolder() {
        List<SharedFile> list = new ArrayList<>();
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                MediaStore.Downloads.EXTERNAL_CONTENT_URI :
                MediaStore.Files.getContentUri("external");

        String[] projection = {
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads._ID,
                MediaStore.Downloads.MIME_TYPE
        };

        String selection = MediaStore.Downloads.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{"Download/File Share App/%"};

        try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, MediaStore.Downloads.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE);
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE);

                while (cursor.moveToNext()) {
                    Uri contentUri = ContentUris.withAppendedId(collection, cursor.getLong(idCol));
                    list.add(new SharedFile(
                            cursor.getString(nameCol),
                            cursor.getLong(sizeCol),
                            contentUri,
                            cursor.getString(mimeCol)
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
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

    // --- ADAPTER UPDATED WITH FILTERABLE ---
    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> implements Filterable {
        private final Context context;
        private List<SharedFile> filesDisplayed; // The filtered list

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
                        filteredList.addAll(fileListFull); // Show all if empty
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

            Glide.with(context)
                    .load(file.getUri())
                    .placeholder(R.drawable.ic_loading)
                    .error(R.drawable.ic_docs)
                    .into(holder.imgThumb);

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
    }
}