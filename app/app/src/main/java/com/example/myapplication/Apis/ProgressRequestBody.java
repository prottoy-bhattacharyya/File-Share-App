package com.example.myapplication.Apis;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * A custom OkHttp RequestBody that wraps a File and fires a ProgressListener
 * callback with the number of bytes written so far and the total file size.
 *
 * OkHttp calls writeTo() on a background thread, so the listener must post
 * any UI updates to the main thread (e.g. via runOnUiThread or Handler).
 */
public class ProgressRequestBody extends RequestBody {

    public interface ProgressListener {
        /**
         * @param bytesWritten bytes sent so far
         * @param contentLength total file size in bytes
         */
        void onProgress(long bytesWritten, long contentLength);
    }

    private static final int BUFFER_SIZE = 8192; // 8 KB chunks — matches OkHttp's default

    private final File file;
    private final MediaType mediaType;
    private final ProgressListener listener;

    public ProgressRequestBody(File file, MediaType mediaType, ProgressListener listener) {
        this.file      = file;
        this.mediaType = mediaType;
        this.listener  = listener;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long total       = file.length();
        long uploaded    = 0;
        byte[] buffer    = new byte[BUFFER_SIZE];
        int   bytesRead;

        try (FileInputStream fis = new FileInputStream(file)) {
            while ((bytesRead = fis.read(buffer)) != -1) {
                sink.write(buffer, 0, bytesRead);
                uploaded += bytesRead;
                // Flush so OkHttp actually sends the bytes rather than buffering
                sink.flush();
                listener.onProgress(uploaded, total);
            }
        }
    }
}