package moths.util;

import com.osmb.api.script.Script;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SimpleUpdater {
    private static final int TIMEOUT_MS = 3000;

    // TODO: Point to the Java source file that contains your version field/annotation.
    private static final String SOURCE_URL =
            "https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/src/com/moths/Moths.java";

    // The raw JAR to download when a newer version is found
    private static final String JAR_URL =
            "https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/jar/Moths.jar";

    // The base name used for jar files in ~/.osmb/Scripts
    private static final String BASE_NAME = "Moths";

    private SimpleUpdater() {}

    public static void checkForUpdates(Script script, String currentVersion) {
        String latest = getLatestVersion(SOURCE_URL);
        if (latest == null) {
            script.log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(currentVersion, latest) < 0) {
            script.log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                if (!dir.exists()) dir.mkdirs();

                File[] old = dir.listFiles((d, n) -> n.equals(BASE_NAME + ".jar") || n.startsWith(BASE_NAME + "-"));
                if (old != null) for (File f : old) if (f.delete()) script.log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, BASE_NAME + "-" + latest + ".jar");
                URL url = new URL(JAR_URL);
                try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }

                script.log("UPDATE", "✅ Downloaded: " + out.getName());
                script.stop();
            } catch (Exception e) {
                script.log("UPDATE", "❌ Error downloading new version: " + e.getMessage());
            }
        } else {
            script.log("SCRIPTVERSION", "✅ You are running the latest version (v" + currentVersion + ").");
        }
    }

    private static String getLatestVersion(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("User-Agent", "moths-updater");
            if (c.getResponseCode() != 200) return null;

            // Read the whole file (cheap) and try a few patterns
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    sb.append(l).append('\n');
                    // Fast path: check line-wise
                    String v = extractFromLine(l);
                    if (v != null) return v;
                }
            }
            // Fallback: search full text (handles annotations spanning lines)
            return extractFromSource(sb.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String extractFromLine(String line) {
        String[] pats = new String[] {
                "\\bversion\\s*=\\s*\"?([0-9]+(?:\\.[0-9]+)*)\"?",
                "\\bscriptVersion\\s*=\\s*\"?([0-9]+(?:\\.[0-9]+)*)\"?"
        };
        for (String p : pats) {
            Matcher m = Pattern.compile(p).matcher(line);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    private static String extractFromSource(String src) {
        // Handles annotation or multi-line cases, e.g. @ScriptDefinition(... version = 2.3, ...)
        Matcher m = Pattern.compile("\\bversion\\s*=\\s*\"?([0-9]+(?:\\.[0-9]+)*)\"?", Pattern.DOTALL).matcher(src);
        if (m.find()) return m.group(1);
        m = Pattern.compile("\\bscriptVersion\\s*=\\s*\"?([0-9]+(?:\\.[0-9]+)*)\"?", Pattern.DOTALL).matcher(src);
        if (m.find()) return m.group(1);
        return null;
    }

    public static int compareVersions(String v1, String v2) {
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