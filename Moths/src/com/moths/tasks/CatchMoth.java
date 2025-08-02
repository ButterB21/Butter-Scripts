package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.tabs.Inventory;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CatchMoth extends com.moths.tasks.Task {

    public CatchMoth(Script script) {
        super(script);
    }

    private static final SearchablePixel[] MOONLIGHT_MOTH_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-14221313, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11035192, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11974309, new SingleThresholdComparator(2), ColorModel.HSL),
    };

    private static final SearchablePixel highlightPixels = new SearchablePixel(-14155777, new SingleThresholdComparator(2), ColorModel.HSL);
    private static final PolyArea MOONLIGHT_MOTH_WANDERAREA = new PolyArea(List.of(new WorldPosition(1573, 9451, 0),new WorldPosition(1577, 9446, 0),new WorldPosition(1575, 9442, 0),new WorldPosition(1573, 9437, 0),new WorldPosition(1557, 9433, 0),new WorldPosition(1553, 9437, 0)));


    @Override
    public boolean activate() {
        if (script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR)) != null) {
            script.log(CatchMoth.class, "Catch Moth task activated...");
            return true;
        }
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
            return;
        }

        script.log(CatchMoth.class, "inventory snapshot 2: " + inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR) + " jars");
        int currButterFlyJarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);

        WorldPosition myPosition = script.getWorldPosition();
        if (!MOONLIGHT_MOTH_WANDERAREA.contains(myPosition)){
            script.log(CatchMoth.class, "Player is not in the moth wander area! Walking to the area...");
            walkToArea(MOONLIGHT_MOTH_WANDERAREA);
            return;
        }

        List<WorldPosition> validPositions = butterFlyPositions();
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

        Rectangle mothBounds = script.getUtils().getHighlightBounds(poly, MOONLIGHT_MOTH_CLUSTER);
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
            if (postButterFlyJarCount < currButterFlyJarCount) {
                script.log(CatchMoth.class, "Successfully caught a moth! Current jar count: " + postButterFlyJarCount + " jars");
                return true;
            }
            script.log(CatchMoth.class, "Attempting to catch moth..." + postButterFlyJarCount + " jars");
            return false;
        }, Utils.random(7000, 12000));

    }

    private void walkToArea(PolyArea MOONLIGHT_MOTH_WANDERAREA) {
        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(3);
        builder.breakCondition(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return MOONLIGHT_MOTH_WANDERAREA.contains(myPosition);
        });

        script.getWalker().walkTo(MOONLIGHT_MOTH_WANDERAREA.getRandomPosition(), builder.build());
    }

    private List<WorldPosition> butterFlyPositions() {
        List<WorldPosition> searchArea = MOONLIGHT_MOTH_WANDERAREA.getAllWorldPositions();

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

            if (script.getUtils().getHighlightBounds(poly, highlightPixels) != null) {
                script.log(CatchMoth.class, "Highlighted Moth found.");
                validMothPositions.add(position);
            }
        });
        return validMothPositions;
    }
}
