package moths.util;

import com.osmb.api.script.Script;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class UpdaterA {
    // Raw URLs (case-sensitive)
    private static final String RAW_VERSION_URL =
            "https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/VERSION";
    private static final String RAW_JAR_URL =
            "https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/jar/Moths.jar";

    private UpdaterA() {}

    public static void checkForUpdates(Script script, String currentVersion) {
        try {
            String latest = fetchPlainText(script, RAW_VERSION_URL);
            if (latest == null || latest.isBlank()) {
                script.log("VERSION", "⚠ Could not fetch latest version info.");
                return;
            }
            latest = latest.trim();

            if (compareVersions(currentVersion, latest) < 0) {
                script.log("VERSION", "⏬ New version v" + latest + " available. Downloading...");
                File dir = new File(System.getProperty("user.home"), ".osmb/Scripts");
                if (!dir.exists() && !dir.mkdirs()) {
                    script.log("UPDATE", "❌ Cannot create directory: " + dir);
                    return;
                }

                // Delete old jars
                File[] old = dir.listFiles((d, n) -> n.equals("Moths.jar") || n.startsWith("Moths-"));
                if (old != null) for (File f : old) if (f.delete()) script.log("UPDATE", "🗑 Deleted old: " + f.getName());

                // Download new jar
                File out = new File(dir, "Moths-" + latest + ".jar");
                downloadWithRedirects(script, RAW_JAR_URL, out);

                // Post-download verification and logging
                long size = out.length();
                String sha256 = computeSha256(out);
                VerifyResult vr = verifyJar(out);

                script.log("UPDATE", "Saved " + out.getAbsolutePath() + " (" + humanSize(size) + ")");
                if (sha256 != null) script.log("UPDATE", "SHA-256: " + sha256);
                script.log("UPDATE", "Jar verify: " + (vr.ok ? "OK" : "FAILED") + (vr.diag.isBlank() ? "" : " — " + vr.diag));

                if (!vr.ok) {
                    // Clean up a bad download
                    if (out.delete()) script.log("UPDATE", "❌ Removed invalid JAR.");
                    return;
                }

                script.log("UPDATE", "✅ Updated to v" + latest + ". Please restart the script.");
                script.stop();
            } else {
                script.log("SCRIPTVERSION", "✅ You are running the latest version (v" + currentVersion + ").");
            }
        } catch (Exception e) {
            script.log("UPDATE", "⚠ Update check failed: " + e.getMessage());
        }
    }

    private static String fetchPlainText(Script script, String urlStr) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(6000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", "moths-updater");
            c.setRequestProperty("Accept", "text/plain");

            // Optional: token for private repos or rate limits
            String token = System.getenv("GITHUB_TOKEN");
            if (token == null || token.isBlank()) token = System.getProperty("github.token", "");
            if (token != null && !token.isBlank()) c.setRequestProperty("Authorization", "Bearer " + token.trim());

            int code = c.getResponseCode();
            String ct = c.getContentType();
            if (code != 200) {
                script.log("UPDATE", "VERSION GET HTTP " + code + " CT=" + ct + " URL=" + urlStr);
                return null;
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line = r.readLine();
                if (line == null) {
                    script.log("UPDATE", "VERSION file empty at " + urlStr);
                    return null;
                }
                return line.trim();
            }
        } catch (Exception e) {
            script.log("UPDATE", "VERSION fetch error: " + e.getMessage() + " URL=" + urlStr);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static void downloadWithRedirects(Script script, String urlStr, File out) throws IOException {
        int maxRedirects = 7;
        String current = urlStr;

        for (int i = 0; i < maxRedirects; i++) {
            HttpURLConnection c = (HttpURLConnection) new URL(current).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod("GET");
            c.setConnectTimeout(8000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "moths-updater");

            // Optional token (works for private raw)
            String token = System.getenv("GITHUB_TOKEN");
            if (token == null || token.isBlank()) token = System.getProperty("github.token", "");
            if (token != null && !token.isBlank()) c.setRequestProperty("Authorization", "Bearer " + token.trim());

            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                if (loc == null || loc.isEmpty()) throw new IOException("Redirect with no Location from: " + current);
                current = loc;
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + code + " for: " + current);

            try (InputStream in = c.getInputStream(); FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
            }
            script.log("UPDATE", "Downloaded from " + current);
            return;
        }
        throw new IOException("Too many redirects for: " + urlStr);
    }

    private static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? parseIntSafe(a[i]) : 0;
            int n2 = i < b.length ? parseIntSafe(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    private static String computeSha256(File file) {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, md)) {
                byte[] buf = new byte[8192];
                while (dis.read(buf) != -1) { /* drain */ }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private static VerifyResult verifyJar(File file) {
        // Basic checks: ZIP signature and presence of manifest
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            fis.mark(4);
            byte[] sig = new byte[4];
            int r = fis.read(sig);
            fis.reset();
            boolean isZip = r >= 2 && sig[0] == 'P' && sig[1] == 'K';
            if (!isZip) return new VerifyResult(false, "Not a ZIP/JAR (wrong signature)");

            boolean manifestSeen = false;
            int count = 0;
            try (ZipInputStream zis = new ZipInputStream(fis)) {
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    count++;
                    if ("META-INF/MANIFEST.MF".equalsIgnoreCase(e.getName())) manifestSeen = true;
                    zis.closeEntry();
                    if (count > 5 && manifestSeen) break; // we don't need to scan entire JAR
                }
            }
            if (!manifestSeen) return new VerifyResult(false, "Manifest not found");
            if (count == 0) return new VerifyResult(false, "Empty ZIP");
            return new VerifyResult(true, "Entries=" + count + ", Manifest=" + (manifestSeen ? "yes" : "no"));
        } catch (Exception e) {
            return new VerifyResult(false, "Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.2f MB", mb);
    }

    private static class VerifyResult {
        final boolean ok;
        final String diag;
        VerifyResult(boolean ok, String diag) { this.ok = ok; this.diag = diag == null ? "" : diag; }
    }
}