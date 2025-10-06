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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(
        name = "Moths",
        description = "A script for catching & banking moths.",
        version = 2.55,
        author = "Butter",
        skillCategory = SkillCategory.HUNTER)
public class Moths extends Script {
    public Moths(Object scriptCore) {
        super(scriptCore);
    }

    private final String scriptVersion = "2.55";

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

        new Thread(() -> moths.util.SimpleUpdater.checkForUpdates(this, scriptVersion), "moths-updater").start();
        method = ui.getSelectedMethod();
        isCatch = MODE_CATCH_MOTHS.equalsIgnoreCase(method) || MODE_ONLY_CATCH.equalsIgnoreCase(method);
        isRestockOnly = MODE_ONLY_BUY_AND_BANK.equalsIgnoreCase(method);
        isCatchOnly = MODE_ONLY_CATCH.equalsIgnoreCase(method);

        catchMothTask = isCatch;
        bankTask = !isCatchOnly;
        activateRestocking = isRestockOnly;

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
        // CHANGE: Multi-region support
        MothData data = MothData.fromUI(ui);
        int[] mothRegions = data.getAllMothRegions();
        int bankRegion = data.getBankRegion();
        boolean already = false;
        for (int r : mothRegions) {
            if (r == bankRegion) { already = true; break; }
        }
        if (already) return mothRegions;
        int[] combined = new int[mothRegions.length + 1];
        System.arraycopy(mothRegions, 0, combined, 0, mothRegions.length);
        combined[mothRegions.length] = bankRegion;
        return combined;
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
        int mothsPerHour = elapsed > 0 ? (int)((mothsCaught * 3600000L) / elapsed) : 0;
        int y = 40;

        boolean isRestockOnlyMode = MODE_ONLY_BUY_AND_BANK.equalsIgnoreCase(method);
        c.fillRect(5, y, 220, isRestockOnlyMode ? 80 : 140, Color.BLACK.getRGB(), 0.8);
        c.drawRect(5, y, 220, isRestockOnlyMode ? 80 : 140, Color.BLUE.getRGB());

        c.drawText("Method: " + method,10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Script version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        if (isRestockOnlyMode) {
            c.drawText("Jars Bought: " + f.format(jarsBought),10, y += 20, Color.WHITE.getRGB(), ARIEL);
        } else {
            c.drawText("Moths caught: " + f.format(mothsCaught), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
            c.drawText("Moths/hr: " + f.format(mothsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        }
    }
}