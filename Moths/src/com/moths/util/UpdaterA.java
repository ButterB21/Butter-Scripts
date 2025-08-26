package moths.util;

import com.osmb.api.script.Script;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdaterA {
    // Raw URLs (case-sensitive)
    private static final String RAW_VERSION_URL =
            "https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/VERSION";
    private static final String RAW_JAR_URL =
            "https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/jar/Moths.jar";

    private UpdaterA() {}

    public static void checkForUpdates(Script script, String currentVersion) {
        try {
            String latest = fetchPlainText(RAW_VERSION_URL);
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

                script.log("UPDATE", "✅ Updated to v" + latest + ". Please restart the script.");
                script.stop();
            } else {
                script.log("SCRIPTVERSION", "✅ You are running the latest version (v" + currentVersion + ").");
            }
        } catch (Exception e) {
            script.log("UPDATE", "⚠ Update check failed: " + e.getMessage());
        }
    }

    private static String fetchPlainText(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(4000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "moths-updater");
            if (c.getResponseCode() != 200) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String s = r.readLine();
                return s != null ? s.trim() : null;
            }
        } catch (Exception e) {
            return null;
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
            if (script != null) script.log("UPDATE", "Downloaded from " + current);
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
}