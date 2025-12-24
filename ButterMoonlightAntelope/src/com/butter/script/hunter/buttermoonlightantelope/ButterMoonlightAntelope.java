package com.butter.script.hunter.buttermoonlightantelope;

import com.butter.script.hunter.buttermoonlightantelope.data.Food;
import com.butter.script.hunter.buttermoonlightantelope.handler.BankHandler;
import com.butter.script.hunter.buttermoonlightantelope.handler.InventoryHandler;
import com.butter.script.hunter.buttermoonlightantelope.ui.UI;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.utils.CachedObject;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.visual.drawing.Canvas;
import javafx.scene.Scene;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.butter.script.hunter.buttermoonlightantelope.Constants.*;
import static com.butter.script.hunter.buttermoonlightantelope.util.MovementUtils.*;

@ScriptDefinition(
        name = "Butter Moonlight Antelope",
        description =  "A script for catching & banking Moonlight Antelope antlers & meat.",
        version = 1.2,
        author = "Butter",
        skillCategory = SkillCategory.HUNTER)
public class ButterMoonlightAntelope extends Script {
    public ButterMoonlightAntelope(Object scriptCore) {
        super(scriptCore);
    }

    private final String scriptVersion = "1.2";
    private long scriptStartTime = 0;

    private BankHandler bankHandler = null;
    private InventoryHandler inventoryHandler = null;
    private ItemGroupResult inventorySnapshot = null;
    public static final Set<Integer> ITEM_IDS_TO_RECOGNIZE = new HashSet<>();
    private final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();
    private long chatboxReadDelay = 0;
    private boolean chatboxInitialized = false;

    private int currNumLogs = 0;
    private int numAntelolpesCaught = 0;
    public static boolean dismantleForRespawnCircle = false;
    private boolean isAntelopeCaught = false;
    private int randLogsNeeded = RandomUtils.uniformRandom(1, 3);
    public static boolean isInvyFull = false;
    public static int runEnergyThreshold = RandomUtils.uniformRandom(30, 60);
    public static boolean pouchEmptied = false;


    // UI Options
    public static boolean shouldChiselAntlers = false;
    public static boolean needsFood = false;
    public static int hpPctToEatAt = -1;
    public static int randomizedHPPctToEatAt = -1;
    public static int selectedFoodItemID;
    private boolean allowHopOrBreak = true;
    public static int userHPLevel = -1;
    public static Food selectedFood = null;

    private final Predicate<RSObject> interactableRoots = roots -> {
        if (roots.getName() == null || roots.getActions() == null) {
            return false;
        }

        if (Arrays.stream(roots.getActions()).noneMatch(action -> "Take-log".equalsIgnoreCase(action))) {
            return false;
        }

        return roots.canReach();
    };

    private static final MenuHook spikedPitHook = menuEntries -> {
        for (MenuEntry entry : menuEntries) {
            if (entry.getAction().equalsIgnoreCase("jump")) {
                return entry;
            }
        }
        return null;
    };

    private final MenuHook setOrDismantleHook = menuEntries -> {
        for (MenuEntry entry : menuEntries) {
            if (entry.getAction().equalsIgnoreCase("dismantle")) {
                dismantleForRespawnCircle = true;
                return entry;
            }

            if (entry.getAction().equalsIgnoreCase("trap")) {
                return entry;
            }
        }
        return null;
    };

        public static final Predicate<RSObject> pitObjQuery = rsObject -> {
        if (rsObject.getName() == null || !rsObject.getName().equalsIgnoreCase("Pit")) {
            return false;
        }

        if (rsObject.getActions() == null || rsObject.getActions().length == 0) {
            return false;
        }

        return rsObject.canReach();
    };

    private static final MenuHook dismantleHook = menuEntries -> {
        for (MenuEntry entry : menuEntries) {
            if (entry.getAction().equalsIgnoreCase("dismantle")) {
                return entry;
            }
        }
        return null;
    };

    @Override
    public void onStart() {
        log(getClass(), "Starting ButterMoonlightAntelope v" + scriptVersion);
        if (!checkForUpdates()) {
            stop();
        }

        scriptStartTime = System.currentTimeMillis();

        UI ui = new UI();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Settings", false);

        // Check user settings
        selectedFoodItemID = ui.getSelectedFoodID();
        shouldChiselAntlers = ui.chisselAntlers();
        hpPctToEatAt = ui.getFoodEatPct();
        randomizedHPPctToEatAt = hpPctToEatAt;
        selectedFood = Food.getFood(selectedFoodItemID);
        if (selectedFood == null) {
            log(ButterMoonlightAntelope.class, "Selected food is null! Stopping script.");
            stop();
            return;
        }

        log(ButterMoonlightAntelope.class, "Selected Food: " + selectedFood.getName());

        ITEM_IDS_TO_RECOGNIZE.addAll(LOG_IDS);
        ITEM_IDS_TO_RECOGNIZE.addAll(ITEM_IDS_TO_DROP);
        ITEM_IDS_TO_RECOGNIZE.addAll(ITEM_IDS_TO_KEEP);
        ITEM_IDS_TO_RECOGNIZE.addAll(ITEM_IDS_TO_BANK);
        ITEM_IDS_TO_RECOGNIZE.addAll(ITEM_IDS_POUCHES);

        ITEM_IDS_TO_RECOGNIZE.add(selectedFoodItemID);
        ITEM_IDS_TO_KEEP.add(selectedFoodItemID);
        ITEM_IDS_TO_KEEP.addAll(LOG_IDS);
        ITEM_IDS_TO_KEEP.addAll(ITEM_IDS_POUCHES);
        this.bankHandler = new BankHandler(this);
        this.inventoryHandler = new InventoryHandler(this);
        currNumLogs = 0;
    }

    @Override
    public int poll() {
        if (userHPLevel < 10) {
            userHPLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.HITPOINTS).getBoostedLevel();
            log(ButterMoonlightAntelope.class, "Base HP Level: " + getWidgetManager().getSkillTab().getSkillLevel(SkillType.HITPOINTS).getLevel());
            log(ButterMoonlightAntelope.class, "boosted level: " + getWidgetManager().getSkillTab().getSkillLevel(SkillType.HITPOINTS).getBoostedLevel());
            return 0;
        }

        WorldPosition playerPosition = getWorldPosition();
        if (playerPosition == null) {
            log(ButterMoonlightAntelope.class, "Player pos is null!");
            return 0;
        }

        // Ensure player has: Teasing Stick/Hunter's spear, knife & chisel (optional)
        if (playerPosition.getRegionID() == BANK_REGION) {
            allowHopOrBreak = true;
            if (getWidgetManager().getBank().isVisible()) {
                bankHandler.handleBank();
                return 0;
            }

            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
            if (inventorySnapshot == null) {
                log(ButterMoonlightAntelope.class, "Inventory is null!");
                return 0;
            }

            if (inventorySnapshot.contains(selectedFoodItemID) && isFoodNeeded()) {
                eatFood();
                return 0;
            }

            if (inventorySnapshot.containsAny(ITEM_IDS_TO_DROP)) {
                getWidgetManager().getInventory().dropItems(ITEM_IDS_TO_DROP);
                return 0;
            }

            if (inventorySnapshot.containsAny(ITEM_IDS_TO_BANK) || isFoodNeeded()) {
                bankHandler.openBank();
                return 0;
            }

            currNumLogs = inventorySnapshot.getAmount(LOG_IDS);
            climbDownStairs(this);
            return 0;
        } else if (playerPosition.getRegionID() == MOONLIGHT_REGION) {
            allowHopOrBreak = false;
            if (getProfileManager().isDueToHop() || getProfileManager().isDueToBreak() || getProfileManager().isDueToAFK()) {
                log(ButterMoonlightAntelope.class, "Can hop/break value: " + allowHopOrBreak);
                if (playerInSafeArea(this)) {
                    allowHopOrBreak = true;
                }
                return 0;
            }

            if (isFoodNeeded()) {
                log(ButterMoonlightAntelope.class, "HP is low!");
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
                if (inventorySnapshot == null) {
                    log(ButterMoonlightAntelope.class, "Inventory is null!");
                    return 0;
                }

                if (inventorySnapshot.contains(selectedFoodItemID)) {
                    log(ButterMoonlightAntelope.class, "Eating food to restore HP...");
                    eatFood();
                    return 0;
                }

                inventoryHandler.climbUpStairs();
                return 0;
            }

            if (!MOONLIGHT_HUNT_AREA.contains(playerPosition)) {
                walkToArea(this, MOONLIGHT_HUNT_AREA);
                return 0;
            }

            UIResultList<WorldPosition> foreignPlayerPositions = getWidgetManager().getMinimap().getPlayerPositions();
            if (foreignPlayerPositions.isNotVisible()) {
                log(ButterMoonlightAntelope.class, "Minimap is not visible!");
                return 0;
            }

            if (foreignPlayerPositions.isFound()) {
                log(ButterMoonlightAntelope.class, "Player detected on minimap! Switching worlds...");
                if (playerInSafeArea(this)) {
                    getProfileManager().forceHop();
                }
                return 0;
            }

            // Check if there is a dialogue visible (when chiseling antlers)
            if (getWidgetManager().getDialogue().isVisible()) {
                log(ButterMoonlightAntelope.class, "Dialogue is visible!");
                DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
                if (dialogueType == DialogueType.ITEM_OPTION) {
                    log(ButterMoonlightAntelope.class, "Selecting moonlight antler bolts option...");
                    getWidgetManager().getDialogue().selectItem(ItemID.MOONLIGHT_ANTLER_BOLTS);
//                    return 0;
                } else if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    currNumLogs = 3;
                    getWidgetManager().getDialogue().continueChatDialogue();
                    return 0;
                }

                handleDialogue();
                return 0;
            }

            if (isInvyFull) {
                log(ButterMoonlightAntelope.class, "Handling inventory...");
                if (!playerInSafeArea(this)) {
                    return 0;
                }
                inventoryHandler.handleInventory();
                isInvyFull = false;
                return 0;
            }

            // maybe use num traps here?
            Map<RSObject, PixelAnalyzer.RespawnCircle> activePits = respawnCircleExists();
            if (activePits != null && !activePits.isEmpty()) {
                log(ButterMoonlightAntelope.class, "Active pit exists!");

                // dismantle traps only if no yellow circles (active traps) left?
                boolean useablePitExists = activePits.values().stream().anyMatch((respawnCircle) -> respawnCircle.getType() == PixelAnalyzer.RespawnCircle.Type.YELLOW);
                boolean dismantleNow = RandomUtils.uniformRandom(3) == 0; // 1 in 3 chance to skip setting up a new trap
                if (!useablePitExists || dismantleNow) {
                    if (anyTrapsToDismantle(activePits)) {
                        randLogsNeeded = RandomUtils.uniformRandom(1, 3);
                        return 0;
                    }
                }

                // Filter out green respawn circles
                activePits.entrySet().removeIf(pit -> pit.getValue().getType() == PixelAnalyzer.RespawnCircle.Type.GREEN);
                if (activePits.isEmpty()) {
                    log(ButterMoonlightAntelope.class, "Active pits is empty!");
                    return 0;
                }

                List<Rectangle> antelopePositions = getAntelopePositions();
                if (antelopePositions.isEmpty()) {
                    log(ButterMoonlightAntelope.class, "Moonlight Antelopes are NOT in a valid teasing area");
                    return 0;
                }

                log(ButterMoonlightAntelope.class, "Number of Moonlight antelopes found: " + antelopePositions.size());
                if (!teaseAntelope(antelopePositions)) {
                    log(ButterMoonlightAntelope.class, "Failed to tease antelope!");
                    return 0;
                }

                // Clear antelopes drawn
                getScreen().removeCanvasDrawable("selected_antelope");
                log(ButterMoonlightAntelope.class, "Antelope teased!");

                // Turn the key set into a list to index it
                List<RSObject> activeTrapList = new ArrayList<>(activePits.keySet());
                int randomTrapIndex = RandomUtils.uniformRandom(0, activeTrapList.size() - 1);
                RSObject activeTrap = activeTrapList.get(randomTrapIndex);
                Polygon activeTrapPoly = activeTrap.getConvexHull();
                if (activeTrapPoly == null) {
                    log(ButterMoonlightAntelope.class, "Active trap polygon is null!");
                    return 0;
                }

                if (getWidgetManager().insideGameScreenFactor(activeTrapPoly, List.of(ChatboxComponent.class)) < 0.4 || !activeTrap.isInteractableOnScreen()) {
                    walkToObject(this, activeTrap);
                }

                WorldPosition currPos = getWorldPosition();
                if (currPos == null) {
                    log(ButterMoonlightAntelope.class, "Current position is null!" );
                    return 0;
                }

                if (!VALID_PLAYER_POS.contains(currPos)) {
                    log(ButterMoonlightAntelope.class, "Player not in valid position to trap antelope! Walking to valid area...");
                    walkToArea(this, VALID_PLAYER_POS);
                }

                trapAntelope(activeTrap);
                return 0;
            }

            if (currNumLogs < randLogsNeeded && currNumLogs == 0) {
                log(ButterMoonlightAntelope.class, "currNumLogs: " + currNumLogs + ". randLogsNeeded: " + randLogsNeeded + " Getting more logs!");
                getLogs(randLogsNeeded);
            }

            List<RSObject> pits = unusedTraps();
            if (pits.isEmpty()) {
                log(ButterMoonlightAntelope.class, "No possible traps found!");
                return 0;
            }

            drawAvailablePits(pits);
            if (setTrap(pits)) {
                enableRunEnergy(this);
                log(ButterMoonlightAntelope.class, "Trap set!");
                return 0;
            }
        } else if (playerPosition.getRegionID() == 12850) {
            log(ButterMoonlightAntelope.class, "You died sucka! Wear a Ring of life next time?");
            stop();
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsedTime = System.currentTimeMillis() - scriptStartTime;
        String runtime = formatRuntime(elapsedTime);
        int antelopesCaught = numAntelolpesCaught;
        int boltsBanked = antelopesCaught * 12;

        // Calculate per hour rates
        double hoursElapsed = elapsedTime / 3600000.0; // Convert ms to hours
        int antelopesPerHour = hoursElapsed > 0 ? (int) (antelopesCaught / hoursElapsed) : 0;
        int boltsPerHour = hoursElapsed > 0 ? (int) (boltsBanked / hoursElapsed) : 0;

        final int panelX = 8;
        final int panelY = 68;
        final int panelW = 250;
        final int topPad = 20;
        final int bottomPad = 10;
        final int lineGap = 16;

        final int shadow = 0x15000000;   // ~8% black shadow
        final int bg     = 0x40000000;   // ~25% black (neutral, not navy)
        final int border = 0x50FFFFFF;   // ~31% white border

        Font titleFont = new Font("SansSerif", Font.BOLD, 13);
        Font labelFont = new Font("SansSerif", Font.PLAIN, 12);
        Font valueFont = new Font("SansSerif", Font.BOLD, 12);

        int panelH = topPad + (6 * lineGap) + bottomPad;

        // Background
        c.fillRect(panelX + 2, panelY + 2, panelW, panelH, shadow);
        c.fillRect(panelX, panelY, panelW, panelH, bg);
        c.drawRect(panelX, panelY, panelW, panelH, border);

        String titleName = "ButterMoonlightAntelope";
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
        y = drawLine(c, y,"Runtime:", runtime, labelFont, valueFont);
        y = drawLine(c, y,"Antelopes Caught:", formatWithPerHour(antelopesCaught, antelopesPerHour), labelFont, valueFont);
        y = drawLine(c, y,"Total Bolts Banked:", formatWithPerHour(boltsBanked, boltsPerHour), labelFont, valueFont);
        y = drawLine(c, y,"HP to eat or bank at (randomized):", String.valueOf((int) (randomizedHPPctToEatAt * userHPLevel / 100.0)), labelFont, valueFont);
    }

    private int drawLine(Canvas c, int y, String labelText, String valueText, Font labelFont, Font valueFont) {
        final int lineGap = 16;
        c.drawText(labelText, 8 + 10, y, -4934476, labelFont);
        int valW = c.getFontMetrics(valueFont).stringWidth(valueText);
        c.drawText(valueText, 8 + 250 - 10 - valW, y, -1, valueFont);
        return y + lineGap;
    }

    private String formatRuntime(long millis) {
        long seconds = millis / 1000;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    private String formatWithPerHour(int total, int perHour) {
        return String.format("%,d (%,d/hr)", total, perHour);
    }

    @Override
    public void onNewFrame() {
        updateChatBoxLines();
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[] {6291, 6191};
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public boolean canHopWorlds() {
        log(ButterMoonlightAntelope.class, "canHopWorlds: " + allowHopOrBreak);
        return allowHopOrBreak;
    }

    @Override
    public boolean canBreak() {
        return allowHopOrBreak;
    }

    @Override
    public void onRelog() {
        allowHopOrBreak = true;
    }

    private boolean isFoodNeeded() {
        int currentHpPct = getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        needsFood = currentHpPct <= randomizedHPPctToEatAt;
        log(ButterMoonlightAntelope.class, "Current HP%: " + currentHpPct + " Threshold: " + randomizedHPPctToEatAt);
        return needsFood;
    }

//    private boolean interactAndWaitForDialogue() {
//        log(MoonlightAntelope.class, "Chiseling antlers before dropping items...");
//
//        if (!inventorySnapshot.contains(ItemID.CHISEL)) {
//            log(MoonlightAntelope.class, "No chisel in inventory to chisel antlers!");
//            stop();
//            return false;
//        }
//
//        ItemSearchResult chisel = inventorySnapshot.getItem(ItemID.CHISEL);
//        ItemSearchResult randomAntler = inventorySnapshot.getRandomItem(ItemID.MOONLIGHT_ANTELOPE_ANTLER);
//        if (!chisel.isSelected()) {
//            if (!chisel.interact()) {
//                log(MoonlightAntelope.class, "Failed to interact with chisel!");
//                return false;
//            }
//        }
//
//        if (randomAntler.interact()) {
//            return pollFramesHuman(() -> {
//                Dialogue dialogue = getWidgetManager().getDialogue();
//                if (dialogue == null) {
//                    return false;
//                }
//                return dialogue.getDialogueType() == DialogueType.ITEM_OPTION;
//            }, RandomUtils.uniformRandom(2500, 5000));
//        }
//        return false;
//    }

    private void handleDialogue() {
        int randomAmountTimeout = RandomUtils.uniformRandom(3000, 3500);
        Timer amountChangeTimer = new Timer();
        AtomicInteger previousAmount = new AtomicInteger(-1);

        pollFramesHuman(() -> {
            // ---FROM OSMB ---
            DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    pollFramesUntil(() -> false, RandomUtils.uniformRandom(1000, 4000));
                    return true;
                }
            }

            if (amountChangeTimer.timeElapsed() > randomAmountTimeout) {
                log(ButterMoonlightAntelope.class, "amount Change Timer hit!");
                return true;
            }

            ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
            if (inventorySnapshot == null) {
                log(ButterMoonlightAntelope.class, "Inventory snapshot is null!");
                return false;
            }

            int currentAmount = inventorySnapshot.getAmount(ItemID.MOONLIGHT_ANTLER_BOLTS);
            if (currentAmount == 0) {
                log(ButterMoonlightAntelope.class, "No moonlight antler bolts found in inventory after chiseling!");
                return true;
            }

            if (currentAmount < previousAmount.get()) {
                log(ButterMoonlightAntelope.class, "Chiseling...");
                amountChangeTimer.reset();
                previousAmount.set(currentAmount);
            }
            return false;
        }, RandomUtils.uniformRandom(4000, 6000));
    }

    private void getLogs(int logsNeeded) {
        if (inventoryHandler.checkInvySpace()) {
            isInvyFull = true;
            return;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(LOG_IDS);
        if (inventorySnapshot == null) {
            return;
        }

        int logsInventorySnapshot = inventorySnapshot.getAmount(LOG_IDS);
        log(ButterMoonlightAntelope.class, "logsInventorySnapshot: " + logsInventorySnapshot);
        if (logsInventorySnapshot > 2) {
            log(ButterMoonlightAntelope.class, "Already have enough logs in inventory!");
            currNumLogs = logsInventorySnapshot;
            return;
        }
        currNumLogs = logsInventorySnapshot;

        List<RSObject> roots = getObjectManager().getObjects(interactableRoots);
        if (roots == null || roots.isEmpty()) {
            log(ButterMoonlightAntelope.class, "No roots found!");
            return;
        }

        for (int i = currNumLogs; i < logsNeeded; i++) {
            RSObject root = (RSObject) getUtils().getClosest(roots);
            Polygon rootPoly = root.getConvexHull();
            if (rootPoly == null) {
                return;
            }

            int randomTapOrInteract = RandomUtils.uniformRandom(2);
            if (randomTapOrInteract == 0) {
                if (!root.interact("Take-log")) {
                    log(ButterMoonlightAntelope.class, "Failed to take logs!");
                    continue;
                }
            } else {
                if (!getFinger().tapGameScreen(rootPoly.getResized(0.7))) {
                    log(ButterMoonlightAntelope.class, "Failed to tap root to take logs!");
                    continue;
                }
            }

            log(ButterMoonlightAntelope.class, "currNumLogs before taking logs: " + currNumLogs);
            pollFramesHuman(() -> {
                WorldPosition currPos = getWorldPosition();
                if (currPos == null) {
                    return false;
                }

                ensureChatboxVisible();

                return root.distance(currPos) == 1 && (currNumLogs > logsInventorySnapshot || isInvyFull);
            }, RandomUtils.uniformRandom(6000, 10000));
        }
    }

    private Map<RSObject, PixelAnalyzer.RespawnCircle> respawnCircleExists() {
        if (!getWidgetManager().getTabManager().closeContainer()) {
            log(ButterMoonlightAntelope.class, "Failed to close container!");
            return null;
        }

        List<RSObject> pitObjs = getObjectManager().getObjects(pitObjQuery);
        if (pitObjs == null || pitObjs.isEmpty()) {
            log(ButterMoonlightAntelope.class, "No pit objects found!");
            return null;
        }

        Map<RSObject, PixelAnalyzer.RespawnCircle> respawnCircles = getPixelAnalyzer().getRespawnCircleObjects(pitObjs, PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER, 100, 15);
        if (respawnCircles == null) {
            log(ButterMoonlightAntelope.class, "No active traps found!");
            return null;
        }

        drawSpikedPit(respawnCircles.keySet());
        return respawnCircles;
    }

    // do we want to also check each pit to see if it is a yellow or green circle, and go from there?
    // Check if respawn circle is inside the trap pit
    private boolean isRespawnCircleInTrapPit(RSObject pitTrap) {
        List<PixelAnalyzer.RespawnCircle> occupiedPitTrap = getPixelAnalyzer().findRespawnCircleTypes();

        if (occupiedPitTrap != null && !occupiedPitTrap.isEmpty()) {
            return occupiedPitTrap.stream().anyMatch((occupiedTrap) -> {
                Polygon pitPoly = pitTrap.getConvexHull();
                if (pitPoly == null) {
                    return false;
                }

                Rectangle circleBounds = occupiedTrap.getBounds();
                if (circleBounds == null) {
                    return false;
                }

                if (pitPoly.getBounds().intersects(circleBounds)) {
                    log(ButterMoonlightAntelope.class, "Pit is occupied by respawn circle!");
                    return true;
                }

                return false;
            });
        }
        log(ButterMoonlightAntelope.class, "No occupied pit traps found!");
        return false;
    }

    private List<RSObject> unusedTraps() {
        List<RSObject> availableTraps = new ArrayList<>();

        List<RSObject> pitsFound = getObjectManager().getObjects(pitObjQuery);
        if (pitsFound == null || pitsFound.isEmpty()) {
            log(ButterMoonlightAntelope.class, "pitsFound is null OR empty");
            return availableTraps;
        }

        pitsFound.forEach(pit -> {
            if (!pit.isInteractableOnScreen()) {
                log(ButterMoonlightAntelope.class, "Pit found is not interactable on screen!");
                return;
            }

            if (!MOONLIGHT_HUNT_AREA.contains(pit.getWorldPosition())) {
                return;
            }

            if (isRespawnCircleInTrapPit(pit)) {
                return;
            }

            log(ButterMoonlightAntelope.class, "Unused pit trap found!");
            availableTraps.add(pit);
        });

        return availableTraps;
    }

    // ensure invy is closed here? maybe just use findrespawn circle size?
    private int numberOfActiveTraps() {
        Map<RSObject, PixelAnalyzer.RespawnCircle> activePits = respawnCircleExists();
        if (activePits == null || activePits.isEmpty()) {
            return 0;
        }

        return activePits.size();
    }

    private boolean setTrap(List<RSObject> availablePits) {
        randLogsNeeded = RandomUtils.uniformRandom(1, 3);

        // find way to randomized which trap to set?
        for (RSObject pit : availablePits) {
            if (currNumLogs < 1) {
                log(ButterMoonlightAntelope.class, "No logs left to set trap!");
                return false;
            }

            int prevNumberOfActiveTraps = numberOfActiveTraps();

            dismantleForRespawnCircle = false;
            if (!pit.interact(setOrDismantleHook)) {
                log(ButterMoonlightAntelope.class, "Failed to set trap!");
                continue;
            }

            // should we continue to set next trap, or break out and restart the search?
            if (dismantleForRespawnCircle) {
                log(ButterMoonlightAntelope.class, "Dismantled existing trap instead of setting new one!");
                pollFramesHuman(() -> {
                    ensureChatboxVisible();
                    return false;
                }, RandomUtils.uniformRandom(4000, 7000));
                continue;
            }

            // Wait for respawn circle to show
            boolean trapSuccessfullySet = pollFramesHuman(() -> {
                ensureChatboxVisible();
                if (currNumLogs < 1) {
                    log(ButterMoonlightAntelope.class, "No logs left to set trap!");
                    return true;
                }

                int currentNumberOfActiveTraps = numberOfActiveTraps();
                if (currentNumberOfActiveTraps == prevNumberOfActiveTraps) {
                    return false;
                }

                return currentNumberOfActiveTraps > prevNumberOfActiveTraps;
            }, RandomUtils.uniformRandom(7000, 12000));

            if (trapSuccessfullySet) {
                if (currNumLogs < 1) {
                    return false;
                }
                currNumLogs--;
                log(ButterMoonlightAntelope.class, "Logs remaining after setting trap: " + currNumLogs);
                // 50% chance to set next trap
                int setNextTrap = RandomUtils.uniformRandom(2);
                if (setNextTrap == 0) {
                    continue;
                }
                return true;
            }
        }

        log(ButterMoonlightAntelope.class, "No trap was set!");
        return false;
    }

    private List<Rectangle> getAntelopePositions() {
        List<Rectangle> antelopePositions = new ArrayList<>();
        UIResultList<WorldPosition> npcPositions = getWidgetManager().getMinimap().getNPCPositions();

        if (npcPositions == null || npcPositions.isNotVisible()) {
            log(ButterMoonlightAntelope.class, "Minimap is not visible!");
            return antelopePositions;
        }

        if (npcPositions.isNotFound()) {
            log(ButterMoonlightAntelope.class, "No NPC positions found on minimap!");
            return antelopePositions;
        }

        drawSelectedAntelope(npcPositions);
        npcPositions.forEach((npcPos) -> {
            if (!VALID_MOONLIGHT_NPC_AREA.contains(npcPos)) {
                return;
            }

            LocalPosition localNpcPos = npcPos.toLocalPosition(this);
            Polygon npcTileCube = getSceneProjector().getTileCube(localNpcPos.getPreciseX(), localNpcPos.getPreciseY(), localNpcPos.getPlane(), 0, 0, 2, 2, true);
            if (npcTileCube == null) {
                log(ButterMoonlightAntelope.class, "npcTileCube is null!");
                return;
            }

            Rectangle highlightedBounds = getPixelAnalyzer().getHighlightBounds(npcTileCube.getResized(0.8), MOONLIGHT_ANTELOPE_CLUSTER);
            if (highlightedBounds == null) {
                log(ButterMoonlightAntelope.class, "highlightedBounds is null!");
                return;
            }

            antelopePositions.add(highlightedBounds);
        });

        return antelopePositions;
    }

    private boolean teaseAntelope(List<Rectangle> antelopePos) {
        enableRunEnergy(this);
        // get random antelope
        int randomIndex = RandomUtils.uniformRandom(0, antelopePos.size() - 1);
        log(ButterMoonlightAntelope.class, "Teasing antelope at index: " + randomIndex);

        if (!getFinger().tapGameScreen(antelopePos.get(randomIndex), "tease")) {
            log(ButterMoonlightAntelope.class, "Failed to tease antelope!");
            return false;
        }

        Timer animationTimer = new Timer();
        int randomTeaseDelay = RandomUtils.uniformRandom(250, 300);
        return pollFramesUntil(() -> {
            log(ButterMoonlightAntelope.class, "Animation timer: " + animationTimer.timeElapsed());
            if (animationTimer.timeElapsed() < randomTeaseDelay) {
                return false;
            }

            if (getPixelAnalyzer().isPlayerAnimating(0.15)) {
                animationTimer.reset();
                log(ButterMoonlightAntelope.class, "Player is animating...");
                return false;
            }

            log(ButterMoonlightAntelope.class, "Last Player Position: " + getLastPositionChangeMillis());
            return getLastPositionChangeMillis() > 100;
        }, RandomUtils.uniformRandom(6000, 9000));
    }

    private boolean trapAntelope(RSObject pit) {
        if (pit == null) {
            log(ButterMoonlightAntelope.class, "Pit is null!");
            return false;
        }

        log(ButterMoonlightAntelope.class, "Jumping over pit...");
        int randomTapOrInteract = RandomUtils.uniformRandom(2);
        if (randomTapOrInteract == 0) {
            if (!pit.interact(spikedPitHook)) {
                log(ButterMoonlightAntelope.class, "Failed to jump over pit!");
                return false;
            }
        } else {
            Polygon pitPoly = pit.getConvexHull();
            if (pitPoly == null) {
                log(ButterMoonlightAntelope.class, "Pit polygon is null!");
                return false;
            }

            MenuEntry response = getFinger().tapGetResponse(true, pitPoly);
            if (response == null) {
                log(ButterMoonlightAntelope.class, "Failed to get menu entry response when jumping pit!");
                return false;
            }

            if (response.getAction() == null || !response.getAction().equalsIgnoreCase("jump")) {
                log(ButterMoonlightAntelope.class, "Menu entry action is not jump-over!");
                return false;
            }
        }

        boolean inSafeAreaAfterJump = pollFramesHuman(() -> {
            WorldPosition currPos = getWorldPosition();
            if (currPos == null) {
                log(ButterMoonlightAntelope.class, "Current position is null after jumping pit!");
                return false;
            }

            return TRAP_SAFE_AREA.stream().anyMatch(area -> area.contains(currPos));
        }, RandomUtils.uniformRandom(5000, 8000));

        if (!inSafeAreaAfterJump) {
            log(ButterMoonlightAntelope.class, "Not in safe area after jumping pit! Retrying...");
            if (!pit.interact(spikedPitHook)) {
                log(ButterMoonlightAntelope.class, "Failed to jump over pit again!");
                return false;
            }

            inSafeAreaAfterJump = pollFramesHuman(() -> {
                WorldPosition currPos = getWorldPosition();
                if (currPos == null) {
                    log(ButterMoonlightAntelope.class, "Current position is null after jumping pit!");
                    return false;
                }

                return TRAP_SAFE_AREA.stream().anyMatch(area -> area.contains(currPos));
            }, RandomUtils.uniformRandom(3000, 5000));

            if (!inSafeAreaAfterJump) {
                log(ButterMoonlightAntelope.class, "Still not in safe area after second jump attempt!");
                return false;
            }
        }

        int initialNumActiveTraps = numberOfActiveTraps();
        return pollFramesHuman(() -> {
            Map<RSObject, PixelAnalyzer.RespawnCircle> respawnCircles = respawnCircleExists();
            if (respawnCircles == null) {
                return false;
            }

            return initialNumActiveTraps != respawnCircles.size();
        }, RandomUtils.uniformRandom(6000, 9000));
    }

    private boolean anyTrapsToDismantle(Map<RSObject, PixelAnalyzer.RespawnCircle> activePits) {
        RSObject trapToDismantle = null;

        // should we use a stream here?
        for (Map.Entry<RSObject, PixelAnalyzer.RespawnCircle> entry : activePits.entrySet()) {
            if (entry.getValue() == null || entry.getValue().getType() != PixelAnalyzer.RespawnCircle.Type.GREEN) {
                continue;
            }

            trapToDismantle = entry.getKey();
            break;
        }

        if (trapToDismantle == null) {
            log(ButterMoonlightAntelope.class, "No traps to dismantle!");
            return false;
        }

        if (!trapToDismantle.isInteractableOnScreen()) {
            log(ButterMoonlightAntelope.class, "Trap to dismantle is not interactable on screen! Walking to trap...");
            walkToObject(this, trapToDismantle);
        }

        isAntelopeCaught = false;
        log(ButterMoonlightAntelope.class, "Dismantling trap...");

        int randomTapOrInteract = RandomUtils.uniformRandom(2);
        if (randomTapOrInteract == 0) {
            if (!trapToDismantle.interact(dismantleHook)) {
                log(ButterMoonlightAntelope.class, "Failed to interact and dismantle trap!");
                return false;
            }
        } else {
            Polygon trapPoly = trapToDismantle.getConvexHull();
            if (trapPoly == null) {
                log(ButterMoonlightAntelope.class, "Trap polygon is null!");
                return false;
            }

            MenuEntry response = getFinger().tapGetResponse(true, trapPoly);
            if (response == null) {
                log(ButterMoonlightAntelope.class, "Failed to get menu entry response when dismantling trap!");
                return false;
            }

            if (response.getAction() == null || !response.getAction().equalsIgnoreCase("dismantle")) {
                log(ButterMoonlightAntelope.class, "Menu entry action is not dismantle!");
                return false;
            }
            log(ButterMoonlightAntelope.class, "Successfully tapped dismantle on trap!");
        }

        boolean dismantledTrap = pollFramesHuman(() -> {
            ensureChatboxVisible();
            return isAntelopeCaught || isInvyFull;
        }, RandomUtils.uniformRandom(4000, 7000));

        if (dismantledTrap) {
            log(ButterMoonlightAntelope.class, "Successfully dismantled trap!");
            return true;
        }

        log(ButterMoonlightAntelope.class, "Failed to dismantle trap");
        return false;
    }

    private void drawAvailablePits(List<RSObject> pits) {
        if (pits == null || pits.isEmpty()) {
            return;
        }

        getScreen().queueCanvasDrawable("available_pits", (canvas) -> {
            for (RSObject pit : pits) {
                Polygon pitPoly = pit.getConvexHull();
                if (pitPoly != null) {
                    canvas.fillPolygon(pitPoly, Color.CYAN.getRGB(), 0.1);
                    canvas.drawPolygon(pitPoly, Color.BLUE.getRGB(), 1);
                }
            }
        });
    }

    private void drawSpikedPit(Set<RSObject> activePits) {
        if (activePits == null || activePits.isEmpty()) {
            return;
        }

        getScreen().queueCanvasDrawable("spiked_pits", (canvas) -> {
           for (RSObject pit : activePits) {
               Polygon pitPoly = pit.getConvexHull();
               if (pitPoly != null) {
                   canvas.fillPolygon(pitPoly, Color.MAGENTA.getRGB(), 0.2);
                   canvas.drawPolygon(pitPoly, Color.RED.getRGB(), 1);
               }
           }
        });
    }

    private void drawSelectedAntelope(UIResultList<WorldPosition> npcPos) {
        if (npcPos == null) {
            return;
        }

        getScreen().queueCanvasDrawable("selected_antelope", (canvas) -> {
            npcPos.forEach((npc) -> {
                LocalPosition localNpcPos = npc.toLocalPosition(this);
                Polygon npcTileCube = getSceneProjector().getTileCube(localNpcPos.getPreciseX(), localNpcPos.getPreciseY(), localNpcPos.getPlane(), 0, 100, 2, 2, true);
                if (npcTileCube != null) {
                    npcTileCube = npcTileCube.getResized(0.8);
                    if (npcTileCube == null) {
                        return;
                    }
                    canvas.fillPolygon(npcTileCube, Color.GREEN.getRGB(), 0.1);
                    canvas.drawPolygon(npcTileCube, Color.BLUE.getRGB(), 1);
                }
            });
        });
    }

    private void eatFood() {
        log(ButterMoonlightAntelope.class, "Eating food...");
        List<ItemSearchResult> foodInInventory = inventorySnapshot.getAllOfItem(selectedFoodItemID);
        if (foodInInventory == null || foodInInventory.isEmpty()) {
            log(ButterMoonlightAntelope.class, "Food not found in inventory!");
            return;
        }

        UIResult<Boolean> tapToDropEnabled = getWidgetManager().getHotkeys().isTapToDropEnabled();

        if (tapToDropEnabled.isNotVisible()) {
            log(ButterMoonlightAntelope.class, "Tap to drop component not visible");
            return;
        }

        // now that we know component is visible from previous check, we check to see if we can read the value
        if (tapToDropEnabled.isNotFound()) {
            log(ButterMoonlightAntelope.class, "Tap to drop value is null!");
            return;
        }

        if (tapToDropEnabled.getIfFound()) {
            log(ButterMoonlightAntelope.class, "Tap to drop is enabled, disabling it first...");
            if (!getWidgetManager().getHotkeys().setTapToDropEnabled(false)) {
                log(ButterMoonlightAntelope.class, "Failed to disable tap to drop!");
                return;
            }
        }

        for (ItemSearchResult food : foodInInventory) {
            if (!isFoodNeeded()) {
                break;
            }
            int foodRemaining = inventorySnapshot.getAmount(selectedFoodItemID);
            getFinger().tap(true, food);
            pollFramesHuman(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(Set.of(selectedFoodItemID));
                if (inventorySnapshot == null) {
                    log(ButterMoonlightAntelope.class, "Inventory is null after eating food!");
                    return false;
                }

                return inventorySnapshot.getAmount(selectedFoodItemID) < foodRemaining;
            }, RandomUtils.uniformRandom(2000, 4000));
        }
        log(ButterMoonlightAntelope.class, "Finished eating.");
    }

    private void ensureChatboxVisible() {
        if (getWidgetManager().getDialogue().getDialogueType() == null && getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME);
        }
    }

    private void updateChatBoxLines() {
        if (getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            return;
        }
        Rectangle chatboxBounds = getWidgetManager().getChatbox().getBounds();
        if (chatboxBounds == null) {
            return;
        }
        // check if minimenu overlaps chatbox
        CachedObject<Rectangle> minimenuBounds = getWidgetManager().getMiniMenu().getMenuBounds();
        if (minimenuBounds != null && minimenuBounds.getScreenUUID() != null && minimenuBounds.getScreenUUID().equals(getScreen().getUUID())) {
            if (minimenuBounds.getObject().intersects(chatboxBounds)) {
                log("Minimenu intersects chatbox");
                activateChatboxReadDelay(1500);
                return;
            }
        }
        if (minimenuBounds != null && minimenuBounds.getObject() != null) {
            getScreen().getDrawableCanvas().drawRect(minimenuBounds.getObject(), Color.GREEN.getRGB());
        }

        // check is we recently tapped over the chatbox (this causes issues with text reading and gives false positives for new lines)
        Rectangle chatboxBounds2 = chatboxBounds.getPadding(0, 0, 12, 0);
        long lastTapMillis = getFinger().getLastTapMillis();
        if (chatboxBounds2.contains(getFinger().getLastTapX(), getFinger().getLastTapY()) && System.currentTimeMillis() - lastTapMillis < 1500) {
            activateChatboxReadDelay(1500);
            return;
        }
        if (isChatboxReadDelayActive()) {
            return;
        }
        UIResultList<String> currentChatboxLines = getWidgetManager().getChatbox().getText();
        if (currentChatboxLines.isNotVisible()) {
            log(ButterMoonlightAntelope.class, "Chatbox not visible");
            return;
        }
        List<String> currentLines = currentChatboxLines.asList();
        if (currentLines.isEmpty()) {
            return;
        }
        // Initialize first 7 lines on script start - From Davvy
        if (!chatboxInitialized) {
            log(getClass(), "Initializing chatbox reader… ignoring first 7 lines.");
            log(getClass(), "Raw current lines count: " + currentLines.size());

            // Debug-log the lines we are skipping
            for (int i = 0; i < currentLines.size(); i++) {
                log(getClass(), "[INIT] Line " + i + ": " + currentLines.get(i));
            }

            // Store only the most recent 7 lines (or all, if fewer)
            if (currentLines.size() > 7) {
                PREVIOUS_CHATBOX_LINES.clear();
                PREVIOUS_CHATBOX_LINES.addAll(currentLines.subList(
                        currentLines.size() - 7, currentLines.size()
                ));
            } else {
                PREVIOUS_CHATBOX_LINES.clear();
                PREVIOUS_CHATBOX_LINES.addAll(currentLines);
            }

            chatboxInitialized = true;
            return; // DO NOT trigger onNewChatBoxMessage()
        }

        List<String> newLines = getNewLines(currentLines, PREVIOUS_CHATBOX_LINES);
        PREVIOUS_CHATBOX_LINES.clear();
        PREVIOUS_CHATBOX_LINES.addAll(currentLines);
        onNewChatBoxMessage(newLines);
    }

    private void activateChatboxReadDelay(long durationMillis) {
        chatboxReadDelay = System.currentTimeMillis() + durationMillis;
    }

    private boolean isChatboxReadDelayActive() {
        return System.currentTimeMillis() < chatboxReadDelay;
    }

    private static List<String> getNewLines(List<String> currentLines, List<String> previousLines) {
//        lastChatBoxRead = System.currentTimeMillis();
        if (currentLines.isEmpty()) {
            return Collections.emptyList();
        }
        int firstDifference = 0;
        if (!previousLines.isEmpty()) {
            if (currentLines.equals(previousLines)) {
                return Collections.emptyList();
            }
            int currSize = currentLines.size();
            int prevSize = previousLines.size();
            for (int i = 0; i < currSize; i++) {
                int suffixLen = currSize - i;
                if (suffixLen > prevSize) continue;
                boolean match = true;
                for (int j = 0; j < suffixLen; j++) {
                    if (!currentLines.get(i + j).equals(previousLines.get(j))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    firstDifference = i;
                    break;
                }
            }
        } else {
            previousLines.addAll(currentLines);
        }
        List<String> newLines = firstDifference == 0 ? List.copyOf(currentLines) : currentLines.subList(0, firstDifference);
//        lastChatBoxChange = System.currentTimeMillis();
        return newLines;
    }

    private void onNewChatBoxMessage(List<String> newLines) {
        for (String line : newLines) {
            line = line.toLowerCase();
            log("Chatbox listener", "New line: " + line);
            if (line.contains("you've caught a moonlight antelope!")) {
                isAntelopeCaught = true;
                numAntelolpesCaught++;
                log(ButterMoonlightAntelope.class, "Caught a moonlight antelope!");
                log(ButterMoonlightAntelope.class, "Total moonlight antelopes caught: " + numAntelolpesCaught);
            }

            if (line.contains("you dismantle the trap.")) {
                dismantleForRespawnCircle = true;
                log(ButterMoonlightAntelope.class, "Dismantled trap for respawn circle!");
            }

            if (line.contains("you manage to ease some logs")) {
                currNumLogs++;
                log(ButterMoonlightAntelope.class, "Current number of logs: " + currNumLogs);
            }

            if (line.contains("you need some logs and a knife to set a pitfall trap")) {
                currNumLogs = 0;
                log(ButterMoonlightAntelope.class, "Out of logs!");
            }

            if (line.contains("your inventory is too full") || line.contains("you don't have enough inventory space")) {
                isInvyFull = true;
                log(ButterMoonlightAntelope.class, "Inventory full!");
            }

            if (line.contains("out of the pouch") || line.contains("pouch is empty")) {
                pouchEmptied = true;
                log(ButterMoonlightAntelope.class, "Emptied pouch!");
            }
        }
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/ButterB21/Butter-Scripts/main/ButterMoonlightAntelope/src/com/butter/script/hunter/buttermoonlightantelope/ButterMoonlightAntelope.java");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return false;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "❌ New version v" + latest + " found! Please update your script from github.");
            return false;
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
