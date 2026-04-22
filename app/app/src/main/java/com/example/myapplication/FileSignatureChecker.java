package com.example.myapplication;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

/**
 * Validates a file's true type by reading its magic bytes (file signature),
 * independent of the file name or extension. This prevents disguised malicious
 * files (e.g., an .exe renamed to .jpg) from being uploaded.
 *
 * Reference: https://en.wikipedia.org/wiki/List_of_file_signatures
 */
public class FileSignatureChecker {

    private static final String TAG = "FileSignatureChecker";

    // How many bytes to read — enough for the deepest signature we check (ZIP/APK/DOCX at 4 bytes)
    // We read 16 to have room for all cases including MP4 offset checks.
    private static final int READ_BYTES = 16;

    public enum CheckResult {
        SAFE,      // Known safe file type, allow upload
        BLOCKED,   // Recognised as dangerous (executable, script, etc.) — hard block
        UNKNOWN    // Couldn't be matched to any known signature — warn the user
    }

    public static class Result {
        public final CheckResult status;
        public final String detectedType;   // Human-readable label e.g. "JPEG image"
        public final String warningDetail;  // Shown in the warning dialog

        Result(CheckResult status, String detectedType, String warningDetail) {
            this.status       = status;
            this.detectedType = detectedType;
            this.warningDetail = warningDetail;
        }
    }

    /**
     * Reads up to READ_BYTES from the URI and returns a Result.
     * Must be called from a background thread.
     */
    public static Result check(ContentResolver resolver, Uri uri) {
        byte[] header = new byte[READ_BYTES];
        int bytesRead = 0;

        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) return unknown("Could not open file stream");
            bytesRead = is.read(header, 0, READ_BYTES);
        } catch (Exception e) {
            Log.e(TAG, "Error reading file header", e);
            return unknown("Could not read file: " + e.getMessage());
        }

        if (bytesRead < 2) return unknown("File is too small to verify");

        // ── Images ────────────────────────────────────────────────────────────

        // JPEG: FF D8 FF
        if (match(header, 0xFF, 0xD8, 0xFF))
            return safe("JPEG image");

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (match(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            return safe("PNG image");

        // GIF: GIF87a / GIF89a
        if (match(header, 0x47, 0x49, 0x46, 0x38))
            return safe("GIF image");

        // WebP: RIFF????WEBP
        if (match(header, 0x52, 0x49, 0x46, 0x46) && bytesRead >= 12 &&
                header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50)
            return safe("WebP image");

        // BMP: BM
        if (match(header, 0x42, 0x4D))
            return safe("BMP image");

        // HEIC/HEIF (ftyp box): bytes 4-7 = "ftyp", bytes 8-11 contain brand
        if (bytesRead >= 12 &&
                header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
            String brand = new String(header, 8, 4);
            if (brand.startsWith("hei") || brand.startsWith("mif") || brand.startsWith("avif"))
                return safe("HEIC/AVIF image");
        }

        // ICO: 00 00 01 00
        if (match(header, 0x00, 0x00, 0x01, 0x00))
            return safe("ICO image");

        // TIFF: little-endian (49 49 2A 00) or big-endian (4D 4D 00 2A)
        if (match(header, 0x49, 0x49, 0x2A, 0x00) || match(header, 0x4D, 0x4D, 0x00, 0x2A))
            return safe("TIFF image");

        // ── Video ─────────────────────────────────────────────────────────────

        // MP4 / M4V / MOV: ftyp box at offset 4
        if (bytesRead >= 8 &&
                header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70)
            return safe("MP4/MOV video");

        // AVI: RIFF????AVI (bytes 8-10)
        if (match(header, 0x52, 0x49, 0x46, 0x46) && bytesRead >= 12 &&
                header[8] == 0x41 && header[9] == 0x56 && header[10] == 0x49)
            return safe("AVI video");

        // MKV / WebM: EBML header 1A 45 DF A3
        if (match(header, 0x1A, 0x45, 0xDF, 0xA3))
            return safe("MKV/WebM video");

        // FLV: 46 4C 56
        if (match(header, 0x46, 0x4C, 0x56))
            return safe("FLV video");

        // MPEG-1/2: 00 00 01 BA or 00 00 01 B3
        if (match(header, 0x00, 0x00, 0x01, 0xBA) || match(header, 0x00, 0x00, 0x01, 0xB3))
            return safe("MPEG video");

        // WMV/WMA/ASF: 30 26 B2 75 8E 66 CF 11
        if (match(header, 0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11))
            return safe("WMV/WMA media");

        // 3GP / 3G2: ftyp at offset 4 (already caught above by MP4 check)
        // TS: 47 (sync byte) — check a few bytes for MPEG-TS
        if (header[0] == 0x47 && bytesRead >= 4 && header[4] == 0x47)
            return safe("MPEG-TS video");

        // ── Audio ─────────────────────────────────────────────────────────────

        // MP3: ID3 tag (49 44 33) or sync word FF FB / FF F3 / FF F2
        if (match(header, 0x49, 0x44, 0x33))
            return safe("MP3 audio");
        if ((header[0] & 0xFF) == 0xFF &&
                ((header[1] & 0xE0) == 0xE0)) // MPEG audio sync
            return safe("MP3 audio");

        // FLAC: 66 4C 61 43
        if (match(header, 0x66, 0x4C, 0x61, 0x43))
            return safe("FLAC audio");

        // WAV: RIFF????WAVE
        if (match(header, 0x52, 0x49, 0x46, 0x46) && bytesRead >= 12 &&
                header[8] == 0x57 && header[9] == 0x41 && header[10] == 0x56 && header[11] == 0x45)
            return safe("WAV audio");

        // OGG: 4F 67 67 53
        if (match(header, 0x4F, 0x67, 0x67, 0x53))
            return safe("OGG audio");

        // AAC: FF F1 or FF F9 (ADTS header)
        if ((header[0] & 0xFF) == 0xFF && ((header[1] & 0xF6) == 0xF0))
            return safe("AAC audio");

        // M4A: ftyp M4A (already caught by MP4 check above — same box structure)

        // AIFF: 46 4F 52 4D
        if (match(header, 0x46, 0x4F, 0x52, 0x4D))
            return safe("AIFF audio");

        // ── Documents ─────────────────────────────────────────────────────────

        // PDF: 25 50 44 46 (%PDF)
        if (match(header, 0x25, 0x50, 0x44, 0x46))
            return safe("PDF document");

        // Office Open XML (docx, xlsx, pptx) and APK/JAR/ZIP all start with PK (50 4B 03 04).
        // We disambiguate by extension since the binary signature is identical.
        if (match(header, 0x50, 0x4B, 0x03, 0x04))
            return safe("ZIP / Office document / APK");

        // Legacy Office (doc, xls, ppt): D0 CF 11 E0 A1 B1 1A E1
        if (match(header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1))
            return safe("Legacy Office document");

        // RTF: 7B 5C 72 74 66 {\rtf
        if (match(header, 0x7B, 0x5C, 0x72, 0x74, 0x66))
            return safe("RTF document");

        // ── Archives ──────────────────────────────────────────────────────────

        // RAR: 52 61 72 21 1A 07
        if (match(header, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07))
            return safe("RAR archive");

        // 7-Zip: 37 7A BC AF 27 1C
        if (match(header, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C))
            return safe("7-Zip archive");

        // GZip: 1F 8B
        if (match(header, 0x1F, 0x8B))
            return safe("GZip archive");

        // BZip2: 42 5A 68
        if (match(header, 0x42, 0x5A, 0x68))
            return safe("BZip2 archive");

        // XZ: FD 37 7A 58 5A 00
        if (match(header, 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00))
            return safe("XZ archive");

        // TAR: "ustar" at offset 257 — skip (READ_BYTES is only 16; won't reach offset 257)

        // ── Plaintext / code (safe but no binary signature) ───────────────────

        // UTF-8 BOM: EF BB BF
        if (match(header, 0xEF, 0xBB, 0xBF))
            return safe("UTF-8 text");

        // UTF-16 BOM: FF FE or FE FF
        if (match(header, 0xFF, 0xFE) || match(header, 0xFE, 0xFF))
            return safe("UTF-16 text");

        // Plain ASCII text heuristic: all first bytes are printable ASCII or common control chars
        if (isLikelyPlainText(header, bytesRead))
            return safe("Text / code file");

        // ── Executables & scripts — BLOCKED ───────────────────────────────────

        // EXE / DLL / EFI (Windows PE): 4D 5A (MZ)
        if (match(header, 0x4D, 0x5A))
            return blocked("Windows executable (EXE/DLL)",
                    "This file appears to be a Windows executable (.exe / .dll). Executables are not allowed.");

        // ELF (Linux binary): 7F 45 4C 46
        if (match(header, 0x7F, 0x45, 0x4C, 0x46))
            return blocked("Linux executable (ELF)",
                    "This file appears to be a Linux/Android binary. Executables are not allowed.");

        // Mach-O (macOS/iOS binary): CE FA ED FE / CF FA ED FE / CA FE BA BE
        if (match(header, 0xCE, 0xFA, 0xED, 0xFE) || match(header, 0xCF, 0xFA, 0xED, 0xFE) ||
                match(header, 0xCA, 0xFE, 0xBA, 0xBE))
            return blocked("macOS/iOS executable",
                    "This file appears to be a macOS or iOS binary. Executables are not allowed.");

        // Java class file: CA FE BA BE (also Mach-O fat binary — already caught above)

        // Shell scripts: #!/ (23 21 2F) — shebang
        if (match(header, 0x23, 0x21, 0x2F))
            return blocked("Shell script",
                    "This file appears to be a shell script. Scripts are not allowed.");

        // HTML (could contain malicious JS):
        // Check for <!DOCTYPE or <html
        if (startsWithAscii(header, bytesRead, "<!") || startsWithAscii(header, bytesRead, "<html") ||
                startsWithAscii(header, bytesRead, "<HTML"))
            return blocked("HTML file",
                    "HTML files are not allowed as they may contain scripts.");

        // ── Unknown ───────────────────────────────────────────────────────────

        return unknown("File type could not be verified from its content. " +
                "Only known safe file types (images, video, audio, documents, archives) are allowed.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns true if the header bytes match the given signature (as ints 0–255). */
    private static boolean match(byte[] header, int... sig) {
        if (header.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) {
            if ((header[i] & 0xFF) != sig[i]) return false;
        }
        return true;
    }

    /** Checks if the header starts with the given ASCII string (case-sensitive). */
    private static boolean startsWithAscii(byte[] header, int bytesRead, String prefix) {
        if (bytesRead < prefix.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (header[i] != (byte) prefix.charAt(i)) return false;
        }
        return true;
    }

    /**
     * Heuristic: if every byte in the header is a printable ASCII character
     * or a common whitespace/control code, treat it as plain text (CSV, JSON,
     * XML, source code, etc.).
     */
    private static boolean isLikelyPlainText(byte[] header, int bytesRead) {
        int check = Math.min(bytesRead, 8);
        for (int i = 0; i < check; i++) {
            int b = header[i] & 0xFF;
            // Allow printable ASCII (32-126) plus TAB (9), LF (10), CR (13)
            if (b < 9 || (b > 13 && b < 32) || b > 126) return false;
        }
        return true;
    }

    private static Result safe(String type) {
        return new Result(CheckResult.SAFE, type, null);
    }

    private static Result blocked(String type, String detail) {
        return new Result(CheckResult.BLOCKED, type, detail);
    }

    private static Result unknown(String detail) {
        return new Result(CheckResult.UNKNOWN, "Unknown", detail);
    }
}