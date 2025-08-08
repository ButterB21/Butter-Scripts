package moths;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;
import moths.data.MothData;
import moths.tasks.CatchMoth;
import moths.tasks.HandleBank;
import moths.tasks.Task;
import moths.ui.UI;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
    name = "Moths",
    description = "A script for catching moths.",
    version = 1.0,
    author = "Butter",
    skillCategory = SkillCategory.HUNTER)

public class Moths extends Script {
    public Moths(Object scriptCore) {
        super(scriptCore);
    }

    private UI ui;
    private final List<Task> tasks = new ArrayList<>();
    public static int mothsCaught = 0;
    public static boolean shouldCatchMoths = true;
    public static boolean shouldBank = true;

//
//    @Override
//    public void onPaint(Canvas c) {
//        DecimalFormat f = new DecimalFormat("#,###");
//        DecimalFormatSymbols s = new DecimalFormatSymbols();
//        f.setDecimalFormatSymbols(s);
//
//        long now = System.currentTimeMillis();
//        long elapsed = now - startTime;
//        String runtime = formatTime(elapsed);
//
//        int mothsPerHour = elapsed > 0 ? (int) ((oresBought * 3600000L) / elapsed) : 0;
//        int y = 40;
//
//        c.fillRect(5, y, 220, 110, Color.BLACK.getRGB(), 1);
//        c.drawRect(5, y, 220, 110, Color.WHITE.getRGB());
//
//        c.drawText("Ores bought: " + f.format(oresBought), 10, y += 25, Color.WHITE.getRGB(), ARIEL);
//        c.drawText("Ores/hr: " + f.format(mothsPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
//        c.drawText("Runtime: " + runtime, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
//        c.drawText("Script version: 1.5", 10, y += 20, Color.WHITE.getRGB(), ARIEL);
//    }

    @Override
    public void onStart() {
        ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Moth Catcher Setup", false);

        log(Moths.class, "Selected Method: " + ui.selectedMethod);
        log(Moths.class, "Selected Moth Type: " + Arrays.toString(ui.getMothItemIdsToCatch()));
        log(Moths.class, "Restocking: " + ui.isRestocking);

        shouldCatchMoths = true; // create get/set later?
        shouldBank = true;
        tasks.add(new CatchMoth(this, ui));
        tasks.add(new HandleBank(this, ui));
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
        // Define regions to prioritise
        return new int[]{MothData.MOONLIGHT_MOTH.getMothRegion(), MothData.MOONLIGHT_MOTH.getBankRegion()};
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }
}
