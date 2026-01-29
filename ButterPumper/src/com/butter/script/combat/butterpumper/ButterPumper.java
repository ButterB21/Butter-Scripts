package com.butter.script.combat.butterpumper;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.ui.tabs.TabManager;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import javafx.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@ScriptDefinition(name = "Butter Pumper", author = "Butter", version = 1.2, description = "Blast Furnace Pumper", skillCategory = SkillCategory.COMBAT)

public class ButterPumper extends Script {
    public ButterPumper(Object scriptCore) {
        super(scriptCore);
    }

    private final String scriptVersion = "1.2";

    private Timer playerAnimationTimer = new Timer();
    private Timer randomActionTimer = new Timer();
    private Timer lastXPGainedTimer = new Timer();

    private XPTracker strengthTracker;
    private int startingStrengthLevel = 0;
    private double strengthXPSnapshot = 0;
    private double currentStrengthXP = 0;
    private double startingStrengthXP = 0;

    private int minAfkDelayMs = 1 * 60000;
    private int maxAfkDurationMs = 5 * 60000;
    private int randomActionTimeout = RandomUtils.uniformRandom(minAfkDelayMs, maxAfkDurationMs);
    private int animationDelay = RandomUtils.uniformRandom(4000, 10000);
    private int noXPGainedTimeout = RandomUtils.uniformRandom(3 * 60000, 7 * 60000);

    private int primaryWorld = 303;
    private boolean hopBetween = false;
    private int failedPumpInteraction = 0;
    private long scriptStartTime = 0;

    @Override
    public void onStart() {
        log(ButterPumper.class, "Version" + scriptVersion);
        checkForUpdates();

        UI ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);

        primaryWorld = ui.getSelectedPrimaryWorldNumber();
        hopBetween = ui.isHopBetweenWorldsEnabled();
        minAfkDelayMs = ui.getMinAfkMinutes() * 60000;
        maxAfkDurationMs = ui.getMaxAfkMinutes() * 60000;

        // --- ensure we start on the selected primary world ---
        Integer currentWorld = getCurrentWorld();
        if (currentWorld == null || currentWorld != primaryWorld) {
            getProfileManager().forceHop(worlds ->
                    worlds.stream()
                            .filter(w -> w.getId() == primaryWorld)
                            .findFirst()
                            .orElse(worlds.get(0))
            );
        }

        scriptStartTime = System.currentTimeMillis();
        this.strengthTracker = getXPTrackers().get(SkillType.STRENGTH);
        if (strengthTracker != null) {
            strengthXPSnapshot = strengthTracker.getStartXp();
        }

        randomActionTimer.reset();
        randomActionTimeout = RandomUtils.uniformRandom(minAfkDelayMs, maxAfkDurationMs);
        playerAnimationTimer.reset();
        lastXPGainedTimer.reset();
    }

    @Override
    public Map<SkillType, XPTracker> getXPTrackers() {
        return super.getXPTrackers();
    }

    @Override
    public int poll() {
        WorldPosition playerPos = getWorldPosition();
        if (playerPos == null) {
            log("Could not get player position");
            return 0;
        }

        if (playerPos.getRegionID() != 7757) {
            log("Not in Blast Furnace region. Stopping script...");
            stop();
            return 0;
        }

        RSObject pumpObj = getObjectManager().getClosestObject(playerPos, "Pump");
        if (pumpObj == null) {
            log("Could not find pump object");
            return 0;
        }

        Polygon pumpPoly = pumpObj.getConvexHull();
        if (pumpPoly == null) {
            log("Could not get pump polygon");
            return 0;
        }

        if (getWidgetManager().insideGameScreenFactor(pumpPoly, List.of(ChatboxComponent.class)) < 0.7) {
            log(ButterPumper.class, "Pump not fully on screen, walking to it...");
            walkToObject(pumpObj);
            return 0;
        }

        if (getPixelAnalyzer().isPlayerAnimating(0.4)) {
            playerAnimationTimer.reset();
        }

        double distanceToPump = pumpObj.distance(playerPos);
        if (distanceToPump == 0) {
            if (strengthTracker != null) {
                currentStrengthXP = strengthTracker.getXp();
                if (currentStrengthXP > strengthXPSnapshot) {
                    strengthXPSnapshot = strengthTracker.getXp();
                    lastXPGainedTimer.reset();
                    return 0;
                }
            } else {
                strengthTracker = getXPTrackers().get(SkillType.STRENGTH);
                log(ButterPumper.class, "Attempting to reacquire strength tracker...");
                log(ButtonGroup.class, "Last xp gained: " + lastXPGainedTimer.timeElapsed() / 1000 + "s");
            }

            // Check for no XP gained
            if (lastXPGainedTimer.timeElapsed() > noXPGainedTimeout && hopBetween) {
                noXPGGainedHandler();
                return 0;
            }

            if (playerAnimationTimer.timeElapsed() < animationDelay) {
                // random action every 1-5 minutes (default values)
                if (randomActionTimer.timeElapsed() > randomActionTimeout) {
                    randomActionTimer.reset();
                    randomActionTimeout = RandomUtils.uniformRandom(minAfkDelayMs, maxAfkDurationMs);
                    log(ButterPumper.class, "Performing random action!");
                    openRandomTab();
                }
                return 0;
            }
            log(ButterPumper.class, "Animation delay hit, re-interacting with pump!");
            animationDelay = RandomUtils.uniformRandom(4000, 8000);
        }

        if (failedPumpInteraction > 3) {
            log(ButterPumper.class, "Failed to find/interact with pump multiple times. Hopping worlds...");
            failedPumpInteraction = 0;

            if (hopBetween) {
               hopWorlds();
            }
            return 0;
        }

        if (!pumpObj.interact("operate")) { // implement some kind of tap() for randomization too?
            failedPumpInteraction++;
            log("Could not interact with pump");
            return 0;
        }

        pollFramesHuman(() -> {
            WorldPosition currPlayerPos = getWorldPosition();
            if (currPlayerPos == null) {
                return false;
            }
            return pumpObj.distance(currPlayerPos) == 0;
        }, RandomUtils.uniformRandom(5000, 10000));

        return 0;
    }

    private void walkToObject(RSObject object) {
        int breakDistance = RandomUtils.uniformRandom(2, 5);
        WalkConfig.Builder builder = new WalkConfig.Builder().breakDistance(breakDistance).tileRandomisationRadius(2);
        builder.breakCondition(() -> {
            WorldPosition currentPosition = getWorldPosition();
            if (currentPosition == null) {
                return false;
            }

            log(ButterPumper.class, "Walking to object...");
            Polygon objectPoly = object.getConvexHull();

            return object.distance(currentPosition) <= breakDistance || (objectPoly != null && getWidgetManager().insideGameScreenFactor(objectPoly, List.of(ChatboxComponent.class)) >= 0.7);
        });
        getWalker().walkTo(object, builder.build());
    }

    public void openRandomTab() {
        Tab.Type[] tabValues = Tab.Type.values();
        int randomIndex = RandomUtils.uniformRandom(0, tabValues.length - 1);

        TabManager tabManager = getWidgetManager().getTabManager();
        if (tabManager == null) {
            return;
        }

        Tab.Type tabToOpen = tabValues[randomIndex];
        tabManager.openTab(tabToOpen);

        pollFramesHuman(() -> {
            Tab.Type currentTab = tabManager.getActiveTab();
            if (currentTab == null || tabToOpen == null) {
                return false;
            }
            log(ButterPumper.class, "Current tab: " + currentTab.name() + ", Desired tab: " + tabToOpen.name());

            return currentTab == tabToOpen;
        }, RandomUtils.uniformRandom(3000, 8000));

        if (tabManager.getActiveTab() != null) {
            int closeTab = RandomUtils.uniformRandom(100);
            if (closeTab < 85) { // 85% chance to close tab
                tabManager.closeContainer();
                log(ButterPumper.class, "Closing tab...");
                pollFramesHuman(() -> true, RandomUtils.uniformRandom(3000, 8000));
            }
        }
    }

    private void hopWorlds() {
        Integer currentWorld = getCurrentWorld();
        if (currentWorld == null) {
            return;
        }

        if (getCurrentWorld() == 302) {
            getProfileManager().forceHop(worlds ->
                    worlds.stream()
                            .filter(w -> w.getId() == 303)
                            .findFirst()
                            .orElse(worlds.get(0))
            );
        } else {
            getProfileManager().forceHop(worlds ->
                    worlds.stream()
                            .filter(w -> w.getId() == 302)
                            .findFirst()
                            .orElse(worlds.get(0))
            );
        }
    }

    private void noXPGGainedHandler(){
        lastXPGainedTimer.reset();
        noXPGainedTimeout = RandomUtils.uniformRandom(3 * 60000, 7 * 60000);

        if (hopBetween) {
            log(ButterPumper.class, "No XP gained for a while, hopping worlds to fix potential issues.");
            hopWorlds();
        }
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsedTime = System.currentTimeMillis() - scriptStartTime;
        String runtime = formatRuntime(elapsedTime);
        String xpGainedText = "";
        int currentStrengthLevel = 0;
        double xpGained = 0;
        int xpPerHour = 0;

        if (strengthTracker != null) {
            currentStrengthXP = strengthTracker.getXp();
            xpGained = strengthTracker.getXpGained();
            xpPerHour = strengthTracker.getXpPerHour();
            String xpPerHourText =
                    xpPerHour >= 1_000_000 ? String.format("%.1fm", xpPerHour / 1_000_000.0) :
                            xpPerHour >= 1_000    ? String.format("%dk",  xpPerHour / 1_000) :
                                    String.valueOf(xpPerHour);

            xpGainedText = String.format("%.0f (%s/hr)", xpGained, xpPerHourText);

            currentStrengthLevel = strengthTracker.getLevel();
            if (startingStrengthXP == 0) {
                startingStrengthXP = strengthTracker.getStartXp();
                startingStrengthLevel = strengthTracker.getLevel();
            }
        }

        int levelsGained = currentStrengthLevel - startingStrengthLevel;
        String currentLevelText = (levelsGained > 0)
                ? (currentStrengthLevel + " (+" + levelsGained + ")")
                : String.valueOf(currentStrengthLevel);


        long millisUntilRandom = getMillisUntilRandomAction();
        String randomActionText = millisUntilRandom >= 0
                ? formatMillisShort(millisUntilRandom)
                : "N/A";

        final int panelX = 8;
        final int panelY = 68;
        final int panelW = 250;
        final int topPad = 20;
        final int bottomPad = 10;
        final int lineGap = 16;

        final int shadow = 0x33000000;   // 20% shadow
        final int bg     = 0x880B1020;   // ~53% transparent deep navy
        final int border = 0x66FFFFFF;   // 40% white

        Font titleFont = new Font("SansSerif", Font.BOLD, 13);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 12);
        Font valueFont = new Font("SansSerif", Font.BOLD, 12);

        int panelH = topPad + (6 * lineGap) + bottomPad;

        // Background
        c.fillRect(panelX + 2, panelY + 2, panelW, panelH, shadow);
        c.fillRect(panelX, panelY, panelW, panelH, bg);
        c.drawRect(panelX, panelY, panelW, panelH, border);

        String titleName = "ButterPumper";
        String titleVersion = " v" + scriptVersion;

        int nameWidth = c.getFontMetrics(titleFont).stringWidth(titleName);
        int versionWidth = c.getFontMetrics(titleFont).stringWidth(titleVersion);

        int totalTitleWidth = nameWidth + versionWidth;
        int titleX = panelX + (panelW - totalTitleWidth) / 2;
        int y = panelY + topPad;

        c.drawText(titleName, titleX, y, 0xFFFFFFFF, titleFont);

        int versionX = titleX + nameWidth;
        c.drawText(titleVersion, versionX, y, 0xFF00FF00, titleFont);

        y += lineGap;
        y = drawLine(c, y, "Runtime:", runtime, labelFont, valueFont);
        y = drawLine(c, y, "Strength lvl:", currentLevelText, labelFont, valueFont);
        y = drawLine(c, y, "XP Gained:", xpGainedText, labelFont, valueFont);
        y = drawLine(c, y, "Time until random action:", randomActionText, labelFont, valueFont);
        y = drawLine(c, y, "Primary world:", String.valueOf(primaryWorld), labelFont, valueFont);
        drawLine(c, y, "Hop between worlds:", hopBetween ? "Yes" : "No", labelFont, valueFont);
    }
    private int drawLine(Canvas c, int y, String labelText, String valueText, Font labelFont, Font valueFont) {
        final int lineGap = 16;
        c.drawText(labelText, 8 + 10, y, -4934476, labelFont);
        int valW = c.getFontMetrics(valueFont).stringWidth(valueText);
        c.drawText(valueText, 8 + 250 - 10 - valW, y, -1, valueFont);
        return y + lineGap;
    }

    private long getMillisUntilRandomAction() {
        if (randomActionTimer == null) {
            return -1; // unknown / not started
        }

        long elapsed = randomActionTimer.timeElapsed(); // ms elapsed since last reset
        long remaining = randomActionTimeout - elapsed;
        return Math.max(0, remaining); // don’t go negative
    }

    private String formatMillisShort(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private String formatRuntime(long millis) {
        long seconds = millis / 1000;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[] { 7757 };
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

    private boolean checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/ButterB21/Butter-Scripts/refs/heads/main/ButterPumper/src/com/butter/script/combat/butterpumper/ButterPumper.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return false;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "❌ New version v" + latest + " found! Please update your script from github.");
            return true;
        }

        log("SCRIPTVERSION", "✅ You are running the latest version (v" + scriptVersion + ").");
        return true;
    }

    private String getLatestVersion(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);

            if (c.getResponseCode() != 200) {
                return null;
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.trim().startsWith("version")) {
                        return line.split("=")[1].replace(",", "").trim();
                    }
                }
            }
        } catch (Exception e) {
            log("VERSION", "❌ ⚠ Error fetching latest version: " + e.getMessage());
        }
        return null;
    }
}
