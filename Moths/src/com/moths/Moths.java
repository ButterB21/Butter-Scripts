package moths;

import com.moths.tasks.Task;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.trackers.experiencetracker.XPTracker;
import com.osmb.api.visual.drawing.Canvas;
import moths.data.MothData;
import moths.tasks.CatchMoth;
import moths.tasks.HandleBank;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(
    name = "Moths",
    description = "A script for catching moths in the game.",
    version = 1.0,
    author = "Butter",
    skillCategory = SkillCategory.HUNTER)

public class Moths extends Script {
    public Moths(Object scriptCore) {
        super(scriptCore);
    }

    private final List<Task> tasks = new ArrayList<>();
    public static int mothsCaught = 0;
    private static final Font ARIAL = new Font("Arial", Font.PLAIN, 14);
    private XPTracker xpTracker;

    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIAL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        if (xpTracker != null) {
            lines.add("Current XP: " + String.format("%,d", (long) xpTracker.getXp()));
            lines.add("XP Gained: " + String.format("%,d", (long) xpTracker.getXpGained()));
            lines.add("Xp per hour: " + String.format("%,d", (long) xpTracker.getXpPerHour(getStartTime())));
            lines.add("Logs Chopped: " + String.format("%,d", mothsCaught));
        }
        // Calculate max width and total height
        int maxWidth = 0;
        for (String line : lines) {
            int w = metrics.stringWidth(line);
            if (w > maxWidth) maxWidth = w;
        }
        int totalHeight = metrics.getHeight() * lines.size();
        int drawX = 10;
        // Draw background rectangle
        c.fillRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.BLACK.getRGB(), 0.8);
        c.drawRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.GREEN.getRGB());
        // Draw text lines
        int drawY = 40;
        for (String line : lines) {
            int color = Color.WHITE.getRGB();
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIAL);
        }
    }

    @Override
    public void onStart() {
        tasks.add(new CatchMoth(this));
        tasks.add(new HandleBank(this));
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
        return new int[]{MothData.MOONLIGHT_MOTH.getMothRegion(), MothData.MOONLIGHT_MOTH.getBankRegion()}; // Example region ID
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
