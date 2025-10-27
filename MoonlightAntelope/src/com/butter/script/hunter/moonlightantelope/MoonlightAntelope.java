package com.butter.script.hunter.moonlightantelope;


import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.walker.WalkConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.butter.script.hunter.moonlightantelope.Constants.*;

@ScriptDefinition(
        name = "Moonlight Antelope",
        description =  "A script for catching & banking Moonlight Antelope bolts & meat.",
        version = 0.1,
        author = "Butter",
        skillCategory = SkillCategory.HUNTER)
public class MoonlightAntelope extends Script {
    public MoonlightAntelope(Object scriptCore) {
        super(scriptCore);
    }

    private final String scriptVersion = "0.1";

    private ItemGroupResult inventorySnapshot = null;
    private Integer logsInInventory = null;

    private final Predicate<RSObject> interactableRoots = roots -> {
        if (roots.getName() == null) {
            return false;
        }

        if (roots.getActions() == null) {
            return false;
        }

        if (Arrays.stream(roots.getActions()).noneMatch(action -> "Take-log".equalsIgnoreCase(action))) {
            return false;
        }

        return roots.canReach();
    };

    @Override
    public void onStart() {
        ITEM_IDS_TO_RECOGNIZE.addAll(LOG_IDS);
        ITEM_IDS_TO_RECOGNIZE.addAll(ITEM_IDS_TO_DROP);
        ITEM_IDS_TO_RECOGNIZE.addAll(ITEM_IDS_TO_KEEP);
    }

    @Override
    public int poll() {
        WorldPosition playerPosition = getWorldPosition();
        if (playerPosition == null) {
            log(MoonlightAntelope.class, "Player pos is null!");
            return 0;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
        if (inventorySnapshot == null) {
            log(MoonlightAntelope.class, "Inventory is null!");
            return 0;
        }

        // Ensure player has: Teasing Stick/Hunter's spear, knife & chisel (optional)
        if (playerPosition.getRegionID() == BANK_REGION) {
            if (getWidgetManager().getBank().isVisible()) {
                // handle banking
                return 0;
            }

            if (inventorySnapshot.containsAny(ITEM_IDS_TO_DROP)) {
                getWidgetManager().getInventory().dropItems(ITEM_IDS_TO_DROP);
                return 0;
            }

            // Ensure adequate inventory space
            if (inventorySnapshot.containsAny(ITEM_IDS_TO_KEEP)) {
                // open bank
                return 0;
            }
            climbDownStairs();
            return 0;
        } else if (playerPosition.getRegionID() == MOONLIGHT_REGION) {
            // ensure player is in the antelope area

            // If Moonlight area does NOT contain player - > walk to area...


            if(droppedRecognizedItems()) {
                return 0;
            }

            if (!hasLogs()) {
                log(MoonlightAntelope.class, "Getting logs...");
                getLogs();
                return 0;
            }

            if (!trapIsSet()) {
                setUpTrap();
                return 0;
            }


        }
        return 0;
    }

    private void climbDownStairs() {
        WorldPosition playerPos = getWorldPosition();
        if (playerPos == null) {
            log(MoonlightAntelope.class, "Player pos is null!");
            return;
        }

        RSObject stairs = getObjectManager().getClosestObject(playerPos,"Stairs");
        if (stairs == null) {
            log(MoonlightAntelope.class, "Cannot find stairs!");
            return;
        }

        if (stairs.interact("Climb-down")) {
            pollFramesHuman(() -> {
                WorldPosition pos = getWorldPosition();
                return pos.getRegionID() == MOONLIGHT_REGION;
            }, RandomUtils.uniformRandom(10000, 15000));
        }
    }

    private void climbUpStairs() {
        WorldPosition playerPos = getWorldPosition();
        if (playerPos == null) {
            log(MoonlightAntelope.class, "Player pos is null!");
            return;
        }
        RSObject stairs = getObjectManager().getClosestObject(playerPos,"Stairs");
        if (stairs == null) {
            return;
        }

        if (!stairs.isInteractableOnScreen()) {
            log(MoonlightAntelope.class, "Stairs are not interactable on screen! Walking to stairs...");
            WalkConfig.Builder walkConfig = new WalkConfig.Builder().tileRandomisationRadius(3);

            walkConfig.breakCondition(() -> {
                WorldPosition currPos = getWorldPosition();
                return currPos != null && stairs.isInteractableOnScreen();
            });

            getWalker().walkTo(stairs.getObjectArea().getRandomPosition(), walkConfig.build());
            log(MoonlightAntelope.class, "Walked to stairs!");
        }

        if (!stairs.interact("Climb-up")) {
            return;
        }

        boolean climbingStairs = pollFramesHuman(() -> {
            WorldPosition pos = getWorldPosition();

            return pos != null && pos.getRegionID() == BANK_REGION;
        }, RandomUtils.uniformRandom(16000, 24000));

        if (!climbingStairs) {
            return;
        }
    }

    private boolean droppedRecognizedItems() {
        if (inventorySnapshot == null) {
            return false;
        }
        int inventorySpace = inventorySnapshot.getFreeSlots();
        // ANTI-BAN: Choose random int between 4-6? before handling items
        if (inventorySpace < 4) {
            if (!inventorySnapshot.containsAny(ITEM_IDS_TO_DROP)) {
                climbUpStairs();
                return false;
            }

            if (getWidgetManager().getInventory().dropItems(ITEM_IDS_TO_DROP)) {
                log(MoonlightAntelope.class, "Dropped item(s)!");
                return true;
            }
        }
        return false;
    }

    private boolean hasLogs() {
        if (inventorySnapshot == null) {
            return false;
        }

        // ANTI-BAN: Sometimes Restock on 0 or 1 log remaining?
        if (!inventorySnapshot.containsAny(LOG_IDS) || inventorySnapshot.getAmount(LOG_IDS) < RandomUtils.uniformRandom(1, 2)) {
            log(MoonlightAntelope.class, "Insufficient logs remaining!");
            return false;
        }

        return true;
    }


    private boolean getLogs() {
        List<RSObject> roots = getObjectManager().getObjects(interactableRoots);
        if (roots == null || roots.isEmpty()) {
            log(MoonlightAntelope.class, "No roots found!");
            return false;
        }

        if (inventorySnapshot == null) {
            return false;
        }
        logsInInventory = inventorySnapshot.getAmount(LOG_IDS);

        RSObject root = (RSObject) getUtils().getClosest(roots);
        if (!root.interact("Take-log")) {
            log(MoonlightAntelope.class, "Failed to take logs!");
            return false;
        }

        return pollFramesHuman(() -> {
            log(MoonlightAntelope.class, "Taking logs...");
            WorldPosition currPos = getWorldPosition();
            if (currPos == null) {
                return false;
            }

            if (root.distance(currPos) > 1) {
                log(MoonlightAntelope.class, "root distance: " + root.distance(currPos));
                return false;
            }

            ItemGroupResult currInventorySnap = getWidgetManager().getInventory().search(LOG_IDS);
            if (currInventorySnap == null) {
                return false;
            }

            int currLogsInInventory = currInventorySnap.getAmount(LOG_IDS);

            return currLogsInInventory > logsInInventory;
        }, RandomUtils.uniformRandom(6000, 10000));
    }

    private boolean trapIsSet() {
        return true;
    }

    private void activeTraps() {
        // Ensure player has visible view of all traps?
        List<PixelAnalyzer.RespawnCircle> occupiedTraps = getPixelAnalyzer().findRespawnCircleTypes(MOONLIGHT_HUNT_AREA.getBounds());
        if (occupiedTraps == null) {
            log(MoonlightAntelope.class, "Occupied traps returned null!");
            return;
        }

        if (occupiedTraps.isEmpty()) {
            log(MoonlightAntelope.class, "No traps set up!");

            Predicate<RSObject> pitQuery = rsObject -> {
                if (rsObject.getName() == null || !rsObject.getName().equalsIgnoreCase("Pit")) {
                    return false;
                }
               return rsObject.canReach();
            };

            List<RSObject> pitsFound = getObjectManager().getObjects(pitQuery);
            if (pitsFound == null || pitsFound.isEmpty()) {
                log(MoonlightAntelope.class, "pitsFound is null OR empty");
                return;
            }

            pitsFound.forEach(pit -> {
               if (!pit.isInteractableOnScreen()) {
                   return;
               }


               if (!pit.interact("Set-trap")) {
                   log(MoonlightAntelope.class, "Failed to set trap!");
                   return;
               }


               pollFramesHuman(() -> {
                   List<PixelAnalyzer.RespawnCircle> respawnCircles = getPixelAnalyzer().findRespawnCircleTypes(pit.getConvexHull().getBounds());
                   if (respawnCircles == null || respawnCircles.isEmpty()) {
                       log(MoonlightAntelope.class, "Setting trap...");
                       return false;
                   }

                   log(MoonlightAntelope.class, "Trap set!");
                   return true;
               }, RandomUtils.uniformRandom(5000, 12000));
            });
        }

    }

    private void setUpTrap() {


    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[] {6291, 6191};
    }
}
