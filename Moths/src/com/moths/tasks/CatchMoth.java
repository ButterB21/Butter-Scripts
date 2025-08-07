package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import moths.data.MothData;
import moths.ui.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static moths.Moths.mothsCaught;

public class CatchMoth extends Task {

    public CatchMoth(Script script, UI ui) {
        super(script);
        this.ui = ui;
    }
    private final UI ui;
    private static final SearchablePixel HIGHLIGHT_PIXEL = new SearchablePixel(-14155777, new SingleThresholdComparator(2), ColorModel.HSL);
    private final MothData mothType = MothData.MOONLIGHT_MOTH;

    @Override
    public boolean activate() {
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(CatchMoth.class, "Cannot get player position! Task will not activate.");
            return false;
        }

        if (mothType.getMothRegion() == myPosition.getRegionID()) {
            script.log(CatchMoth.class, "Activaging Catch Moth Task...");
            return true;
        }

        script.log(CatchMoth.class, "Player not in moth region...");
        return false;
    }

    @Override
    public void execute() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (inventorySnapshot == null) {
            script.log(CatchMoth.class, "Cannot get inventory!");
            return;
        }

        if (inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR) <= 0) {
            script.log(CatchMoth.class, "Inventory is full!");

            if (mothType == MothData.MOONLIGHT_MOTH) {
                RSObject stairs = script.getObjectManager().getClosestObject("Stairs");
                if (stairs == null) {
                    script.log(CatchMoth.class, "Saris obj is null!");
                    return;
                }

                if (!stairs.interact("Climb-up")) {
                    script.log(CatchMoth.class, "Failed to interact with stairs to go up!");
                    return;
                }

                WorldPosition myPosition = script.getWorldPosition();
                if (myPosition == null) {
                    script.log(CatchMoth.class, "Player position is null after climbing stairs!");
                    return;
                }

                script.submitHumanTask(() -> mothType.getMothRegion() != myPosition.getRegionID(), Utils.random(15000, 20000));
                return;
            }
        }

        script.log(CatchMoth.class, "inventory snapshot: " + inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR) + " jars");
        int currButterFlyJarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);

        WorldPosition myPosition = script.getWorldPosition();
        if (!mothType.getMothArea().contains(myPosition)){
            script.log(CatchMoth.class, "Player is not in the moth wander area! Walking to the area...");
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
        Polygon poly = script.getSceneProjector().getTilePoly(closestMoth, true);
        if (poly == null) {
            script.log(CatchMoth.class, "Polygon for closest moth is null at position: " + closestMoth);
            return;
        }

        Rectangle mothBounds = script.getUtils().getHighlightBounds(poly, mothType.getMothClusterPixels());
        if (mothBounds == null) {
            script.log(CatchMoth.class, "No highlight bounds found for moth at position: " + closestMoth);
            return;
        }

        if (!script.getFinger().tap(mothBounds, "catch") ) {
            script.log(CatchMoth.class, "Failed to tap on moth at position: " + closestMoth);
            return;
        }

        script.submitHumanTask(() -> {
            ItemGroupResult currInvSnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
            if (currInvSnapshot == null) {
                script.log(CatchMoth.class, "Current inventory snapshot is null!");
                return false;
            }

            int postButterFlyJarCount = currInvSnapshot.getAmount(ItemID.BUTTERFLY_JAR);
            if (postButterFlyJarCount < currButterFlyJarCount && !script.getPixelAnalyzer().isPlayerAnimating(0.4)) {//check if player is no longer animating
                script.log(CatchMoth.class, "Successfully caught a moth! Current jar count: " + postButterFlyJarCount + " jars");
                mothsCaught++;
                return true;
            }
            script.log(CatchMoth.class, "Attempting to catch moth..." + postButterFlyJarCount + " jars");
            return false;
        }, Utils.random(7000, 12000));

    }

    private void walkToArea(PolyArea mothWanderArea) {
        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(3);
        builder.breakCondition(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return mothWanderArea.contains(myPosition);
        });

        script.getWalker().walkTo(mothWanderArea.getRandomPosition(), builder.build());
    }

    private List<WorldPosition> butterFlyPositions(PolyArea mothWanderArea) {
        List<WorldPosition> searchArea = mothWanderArea.getAllWorldPositions();

        if (searchArea.isEmpty()) {
            script.log(CatchMoth.class, "No surrounding positions found in the search area.");
            return null;
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

            if (script.getUtils().getHighlightBounds(poly, HIGHLIGHT_PIXEL) != null) {
                script.log(CatchMoth.class, "Highlighted Moth found.");
                validMothPositions.add(position);
            }
        });
        return validMothPositions;
    }
}
