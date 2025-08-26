package moths.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class JarVersion {
    private JarVersion() {}

    public static class Result {
        public final String version;     // null if not found
        public final String diag;        // diagnostics to log
        public Result(String version, String diag) {
            this.version = version;
            this.diag = diag;
        }
    }

    public static Result fetchVersionFromJarUrl(String jarUrl) {
        StringBuilder diag = new StringBuilder();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(jarUrl).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(8000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "moths-updater");
            int code = c.getResponseCode();
            diag.append("HTTP=").append(code).append(", CT=").append(c.getContentType()).append('\n');
            if (code != 200) {
                return new Result(null, diag.append("Non-200 response").toString());
            }

            try (InputStream raw = new BufferedInputStream(c.getInputStream())) {
                // Peek header to ensure it's a ZIP (JAR)
                raw.mark(4);
                byte[] sig = new byte[4];
                int read = raw.read(sig);
                raw.reset();
                boolean looksZip = read >= 2 && sig[0] == 'P' && sig[1] == 'K';
                if (!looksZip) {
                    // Sometimes Git LFS pointer text or HTML is returned
                    byte[] firstBytes = new byte[Math.max(64, read)];
                    raw.read(firstBytes, 0, Math.min(firstBytes.length, read));
                    diag.append("Not a ZIP. First bytes: ").append(bytesToAscii(firstBytes, read)).append('\n');
                    return new Result(null, diag.toString());
                }

                // Now parse ZIP to locate manifest or pom.properties
                raw.reset();
                try (ZipInputStream zis = new ZipInputStream(raw)) {
                    ZipEntry e;
                    Manifest manifest = null;
                    String implVersion = null;
                    String specVersion = null;
                    String bundleVersion = null;
                    String genericVersion = null;
                    String pomVersion = null;

                    while ((e = zis.getNextEntry()) != null) {
                        String name = e.getName();
                        if ("META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
                            // Read full manifest
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            copy(zis, bos);
                            manifest = new Manifest(new ByteArrayInputStream(bos.toByteArray()));

                            implVersion = getAttr(manifest, "Implementation-Version");
                            specVersion = getAttr(manifest, "Specification-Version");
                            bundleVersion = getAttr(manifest, "Bundle-Version");
                            genericVersion = getAttr(manifest, "Version");
                            diag.append("Manifest found. Impl=")
                                    .append(implVersion).append(", Spec=")
                                    .append(specVersion).append(", Bundle=")
                                    .append(bundleVersion).append(", Version=")
                                    .append(genericVersion).append('\n');
                        } else if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties")) {
                            Properties p = new Properties();
                            p.load(zis);
                            pomVersion = trimOrNull(p.getProperty("version"));
                            diag.append("pom.properties found: ").append(pomVersion).append('\n');
                        }
                        zis.closeEntry();
                    }

                    String chosen = firstNonNull(trimOrNull(implVersion),
                            trimOrNull(specVersion),
                            trimOrNull(bundleVersion),
                            trimOrNull(genericVersion),
                            trimOrNull(pomVersion));
                    return new Result(chosen, diag.toString());
                }
            }
        } catch (Exception e) {
            diag.append("Exception: ").append(e.getClass().getSimpleName())
                    .append(": ").append(e.getMessage());
            return new Result(null, diag.toString());
        }
    }

    private static String getAttr(Manifest m, String key) {
        if (m == null) return null;
        String v = m.getMainAttributes().getValue(key);
        if (v == null) {
            // try some common variants
            v = m.getMainAttributes().getValue(key.toLowerCase());
            if (v == null) v = m.getMainAttributes().getValue(key.toUpperCase());
        }
        return trimOrNull(v);
    }

    private static String firstNonNull(String... a) {
        for (String s : a) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }

    private static String bytesToAscii(byte[] b, int len) {
        if (len <= 0) len = Math.min(b.length, 32);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len && i < b.length; i++) {
            int c = b[i] & 0xff;
            if (c >= 32 && c < 127) sb.append((char) c);
            else sb.append(String.format("\\x%02X", c));
        }
        return sb.toString();
    }
}