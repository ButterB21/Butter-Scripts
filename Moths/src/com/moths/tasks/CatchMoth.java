package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.profile.WorldProvider;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.world.World;
import moths.data.MothData;
import moths.ui.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static moths.Moths.*;


public class CatchMoth extends Task {

    public CatchMoth(Script script, UI ui) {
        super(script);
        this.ui = ui;
    }
    private final UI ui;
    private static final SearchablePixel HIGHLIGHT_PIXEL = new SearchablePixel(-14155777, new SingleThresholdComparator(2), ColorModel.HSL);
    private final PolyArea insideGuildArea = new PolyArea(List.of(new WorldPosition(1246, 3723, 0),new WorldPosition(1245, 3743, 0),new WorldPosition(1253, 3742, 0),new WorldPosition(1254, 3741, 0),new WorldPosition(1251, 3737, 0),new WorldPosition(1251, 3723, 0)));

    private boolean isCatchOnlyMode() {
        return "Only Catch (No bank)".equalsIgnoreCase(ui.getSelectedMethod());
    }
    private boolean isCatchEnabledMode() {
        String m = ui.getSelectedMethod();
        return "Catch & Bank".equalsIgnoreCase(m) || isCatchOnlyMode();
    }
    @Override
    public boolean activate() {
        if (!catchMothTask) {
            script.log(CatchMoth.class, "catchMothTask is false - not activating.");
            return false;
        }

        if (!isCatchEnabledMode()) {
            script.log(CatchMoth.class, "Not in catch moths mode");
            catchMothTask = false;
            return false;
        }

        if (!hasJarsAvailable() && !isCatchOnlyMode()) {
            script.log(CatchMoth.class, "No jars available - switching to banking mode");
            catchMothTask = false;
            bankTask = true;
            return false;
        }

        script.log(CatchMoth.class, "All conditions met - activating CatchMoth task");
        return true;
    }

    @Override
    public void execute() {
        // Minimal change: Resolve Ruby Harvest location from UI (Farming Guild vs Land's End)
        MothData mothType = resolveMothTypeFromUI();

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (inventorySnapshot == null) {
            script.log(CatchMoth.class, "Cannot get inventory snapshot");
            return;
        }

        script.log(CatchMoth.class, "inventory snapshot: " + inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR) + " jars remaining");
        int currButterFlyJarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);

        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(CatchMoth.class, "Position is null!");
            return;
        }

        if (mothType.getMothRegion() != myPosition.getRegionID()) {
            returnToMothRegion(mothType);
            return;
        }

        if (!mothType.getMothArea().contains(myPosition)) {
            script.log(CatchMoth.class, "Player is not in the moth wander area!");
            if (mothType == MothData.BLACK_WARLOCK || mothType == MothData.RUBY_HARVEST || mothType == MothData.RUBY_HARVEST_KOUREND) {
                script.log(CatchMoth.class, "Returning to area for Black Warlock/Ruby Harvest");
                returnToMothRegion(mothType);
                return;
            }
            walkToArea(mothType.getMothArea());
            return;
        }

        List<WorldPosition> validPositions = butterFlyPositions(mothType.getMothArea());
        if (validPositions == null || validPositions.isEmpty()) {
            script.log(CatchMoth.class, "no valid positions found for moths!");
            return;
        }

        script.log(CatchMoth.class, "Finding closest moth to catch...");

        WorldPosition closestMoth = script.getWorldPosition().getClosest(validPositions);
        Polygon poly = script.getSceneProjector().getTileCube(closestMoth, 120);
        if (poly == null) {
            script.log(CatchMoth.class, "Polygon for closest moth is null at position: " + closestMoth);
            return;
        }

        Rectangle mothBounds = script.getPixelAnalyzer().getHighlightBounds(poly, mothType.getMothClusterPixels());
        if (mothBounds == null) {
            script.log(CatchMoth.class, "No highlight bounds found for moth at position: " + closestMoth);
            return;
        }

        if (!script.getFinger().tap(mothBounds.getCenter(), "catch") ) {
            script.log(CatchMoth.class, "Failed to tap on moth at position: " + closestMoth);
            return;
        }

        AtomicInteger animatingTimeout = new AtomicInteger(script.random(500));
        script.log(CatchMoth.class, "Initial Animating timeout: " + animatingTimeout.get());
        Timer animatingTimer = new Timer();
        script.submitHumanTask(() -> {
            if (isCatchOnlyMode()) {
                if (!script.getPixelAnalyzer().isPlayerAnimating(0.5)) {
                    if (animatingTimer.timeElapsed() > animatingTimeout.get()) {
                        script.log(CatchMoth.class, "Animating timeout hit: " + animatingTimeout.get());
                        animatingTimeout.set(script.random(500));
                        mothsCaught++;
                        return true;
                    }
                    return false;
                }
                animatingTimer.reset();
            } else {
                ItemGroupResult postInvSnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
                if (postInvSnapshot == null) {
                    script.log(CatchMoth.class, "Current inventory snapshot is null!");
                    return false;
                }

                int postButterFlyJarCount = postInvSnapshot.getAmount(ItemID.BUTTERFLY_JAR);
                if (postButterFlyJarCount < currButterFlyJarCount) {
                    script.log(CatchMoth.class, "Successfully caught a moth! Current jar count: " + postButterFlyJarCount + " jars");
                    mothsCaught++;
                    return true;
                }
                script.log(CatchMoth.class, "Attempting to catch moth..." + postButterFlyJarCount + " jars");
            }

            return false;
        }, script.random(9000, 13000));

    }

    // Map UI selection to MothData, only branching for Ruby Harvest (Land's End vs Farming Guild)
    private MothData resolveMothTypeFromUI() {
        if (ui.getSelectedMothItemId() == ItemID.RUBY_HARVEST) {
            if (ui.isRubyHarvestAtLandsEnd()) {
                script.log(CatchMoth.class, "UI selected Ruby Harvest at Land's End (Kourend).");
                return MothData.RUBY_HARVEST_KOUREND;
            } else {
                script.log(CatchMoth.class, "UI selected Ruby Harvest at Farming Guild.");
                return MothData.RUBY_HARVEST; // existing Farming Guild entry
            }
        }
        // Fall back to your existing mapping for other types/modes
        return MothData.fromUI(ui);
    }

    private boolean hasJarsAvailable() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));

        if (inventorySnapshot == null) {
            script.log(CatchMoth.class, "Cannot get inventory snapshot");
            return false;
        }

        int jarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);
        return jarCount > 0;
    }

    private void walkToArea(Area mothWanderArea) {
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return mothWanderArea.contains(myPosition);
        });

        script.getWalker().walkTo(mothWanderArea.getRandomPosition(), builder.build());
    }

    private List<WorldPosition> butterFlyPositions(Area mothWanderArea) {
        List<WorldPosition> searchArea = mothWanderArea.getAllWorldPositions();
        UIResultList<WorldPosition> playerPositions = script.getWidgetManager().getMinimap().getPlayerPositions();

        if (searchArea.isEmpty()) {
            script.log(CatchMoth.class, "No surrounding positions found in the search area.");
            return null;
        }

        if (playerPositions.isFound()) {
            script.log(CatchMoth.class, "Player position found!");

            if (playerPositions.asList().stream().anyMatch(playerPos -> searchArea.contains(playerPos))) {
                script.log(CatchMoth.class, "Player found in moth area! Hopping worlds...");
                script.getProfileManager().forceHop();
                return null;
            }
            script.log(CatchMoth.class, "Player(s) found, but none in moth area.");
        }

        List<WorldPosition> validMothPositions = new ArrayList<>();
        searchArea.forEach(position -> {
            Polygon poly = script.getSceneProjector().getTilePoly(position);
            if (poly == null) {
                script.log(CatchMoth.class, "Polygon inside the search area is null.");
                return;
            }


            if (script.getWorldPosition().distanceTo(position) > 20) {
                script.log(CatchMoth.class, "Moth is too far away, looking for a closer option...");
                return;
            }

            if (script.getPixelAnalyzer().getHighlightBounds(poly, HIGHLIGHT_PIXEL) != null) {
                script.log(CatchMoth.class, "Highlighted Moth found.");
                validMothPositions.add(position);
            }
        });
        return validMothPositions;
    }

    private boolean returnToMothRegion(MothData moth){
        script.log(CatchMoth.class, "Returning to moth area/region...");
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(CatchMoth.class, "Player position is null!");
            return false;
        }

        if (moth == MothData.MOONLIGHT_MOTH) {
            RSObject stairObj = script.getObjectManager().getClosestObject("Stairs");
            if (stairObj == null) {
                script.log(CatchMoth.class, "No stairs found to return to moth area!");
                return false;
            }

            if (stairObj.interact("Climb-down")) {
                script.submitHumanTask(() -> {
                    WorldPosition position = script.getWorldPosition();
                    if (position == null) {
                        script.log(CatchMoth.class, "Position is null.");
                        return false;
                    }
                    return moth.getMothRegion() == position.getRegionID();
                }, Utils.random(10000, 15000));
            }
        } else if (moth == MothData.BLACK_WARLOCK || moth == MothData.RUBY_HARVEST || moth == MothData.RUBY_HARVEST_KOUREND) {
            if (insideGuildArea.contains(myPosition)) {
                script.log(CatchMoth.class, "Player is inside guild, exiting...");
                RSObject guildDoor = script.getObjectManager().getClosestObject("Door");
                if (guildDoor == null) {
                    script.log(CatchMoth.class, "Guild door object is null!");
                    return false;
                }

                if (!guildDoor.interact("Open")) {
                    script.log(CatchMoth.class, "Failed to interact with guild door!");
                    return false;
                }

                // Wait for player to move through the door
                boolean exitedGuild = script.submitHumanTask(() -> {
                    WorldPosition pos = script.getWorldPosition();
                    return pos != null && !insideGuildArea.contains(pos);
                }, Utils.random(15000, 20000));
                if (!exitedGuild) {
                    script.log(CatchMoth.class, "Failed to move through door, timing out!");
                    return false;
                }
            }

            script.log(CatchMoth.class, "Exiting guild area...");
            if (!insideGuildArea.contains(myPosition)) {
                script.log(CatchMoth.class, "Not inside guild area. Walking to moth area...");
                walkToArea(moth.getMothArea());
            }
        } else {
            // For Sunlight Moths
            walkToArea(moth.getMothArea());
        }
        return true;
    }
}