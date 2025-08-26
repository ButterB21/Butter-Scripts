package moths;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import moths.data.MothData;
import moths.tasks.BuyJars;
import moths.tasks.CatchMoth;
import moths.tasks.HandleBank;
import moths.tasks.Task;
import moths.ui.UI;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(
    name = "Moths",
    description = "A script for catching & banking moths.",
    version = 2.1,
    author = "Butter",
    skillCategory = SkillCategory.HUNTER)

public class Moths extends Script {
    public Moths(Object scriptCore) {
        super(scriptCore);
    }

    private final String scriptVersion = "2.1";

    private UI ui;
    private final List<Task> tasks = new ArrayList<>();
    public static int mothsCaught = 0;
    public static int jarsBought = 0;
    public static boolean catchMothTask = true;
    public static boolean bankTask = true;
    public static boolean activateRestocking = false;

    private String method;
    private static final String MODE_CATCH_MOTHS = "Catch & Bank";
    private static final String MODE_ONLY_BUY_AND_BANK = "Only Buy & Bank Jars";
    private static final String MODE_ONLY_CATCH = "Only Catch (No bank)";
    private boolean isCatch;
    private boolean isRestockOnly;
    private boolean isCatchOnly;

    private static final Font ARIEL = new Font("Arial", Font.PLAIN, 14);
    @Override
    public void onStart() {
        ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Moth Catcher Setup", false);

        new Thread(() -> moths.util.UpdaterA.checkForUpdates(this, scriptVersion), "moths-updater").start();

        method = ui.getSelectedMethod();
        isCatch = MODE_CATCH_MOTHS.equalsIgnoreCase(method) || MODE_ONLY_CATCH.equalsIgnoreCase(method);
        isRestockOnly = MODE_ONLY_BUY_AND_BANK.equalsIgnoreCase(method);
        isCatchOnly = MODE_ONLY_CATCH.equalsIgnoreCase(method);

        catchMothTask = isCatch;
        bankTask = !isCatchOnly; // never bank in Only Catch mode
        activateRestocking = isRestockOnly; // BuyJars only in restock-only mode; HandleBank toggles it for Catch+Restock
        log(CatchMoth.class, "Starting Moths script with method: " + method
                + "\nCatch Moth Task: " + catchMothTask
                + "\nBank Task: " + bankTask
                + "\nActivate Restocking: " + activateRestocking);

        tasks.add(new CatchMoth(this, ui));
        tasks.add(new HandleBank(this, ui));
        tasks.add(new BuyJars(this, ui));
    }

    @Override
    public int poll() {
        for (Task task : tasks) {
            if (task.activate()) {
                task.execute();
                return 0;
            }
        }
        return 0;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{MothData.fromUI(ui).getMothRegion(), MothData.fromUI(ui).getBankRegion()};
    }

    @Override
    public boolean promptBankTabDialogue() {
        return !MODE_ONLY_CATCH.equalsIgnoreCase(ui.getSelectedMethod());
    }

    @Override
    public void onPaint(Canvas c) {
        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        f.setDecimalFormatSymbols(s);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        int xpPerMoth = MothData.fromUI(ui).getXpPerMoth();
        int mothsPerHour = elapsed > 0 ? (int) ((mothsCaught * 3600000L) / elapsed) : 0;
        int xpPerHour = elapsed >0 ? (int) ((mothsCaught * xpPerMoth * 3600000L) / elapsed) : 0;
        int y = 40;

        c.fillRect(5, y, 220, isRestockOnly ? 80 : 140, Color.BLACK.getRGB(), 0.8);
        c.drawRect(5, y, 220, isRestockOnly ? 80 : 140, Color.BLUE.getRGB());

        c.drawText("Method: " + method,10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Script version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        if (isRestockOnly) {
            c.drawText("Jars Bought: " + jarsBought,10, y += 20, Color.WHITE.getRGB(), ARIEL);
        } else {
            c.drawText("Moths caught: " + f.format(mothsCaught), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("XP/Hr: " + f.format(xpPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Total XP: " + f.format(mothsCaught * xpPerMoth), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Moths/hr: " + f.format(mothsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        }
    }

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/jar/Moths.jar");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("Moths.jar") || n.startsWith("Moths-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "Moths-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/Moths/jar/Moths.jar");
                try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }

                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();

            } catch (Exception e) {
                log("UPDATE", "❌ Error downloading new version: " + e.getMessage());
            }
        } else {
            log("SCRIPTVERSION", "✅ You are running the latest version (v" + scriptVersion + ").");
        }
    }


    private String getLatestVersion(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            if (c.getResponseCode() != 200) return null;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    if (l.trim().startsWith("version")) {
                        return l.split("=")[1].replace(",", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }


    public static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}
