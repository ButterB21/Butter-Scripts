package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CatchMoth extends com.moths.tasks.Task {

    public CatchMoth(Script script) {
        super(script);
    }

    private static final SearchablePixel[] MOONLIGHT_MOTH_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-14221313, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
            new SearchablePixel(-11035192, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
            new SearchablePixel(-11974309, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
    };

    private static final SearchablePixel highlightPixels = new SearchablePixel(-14155777, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);

    private static final PolyArea MOONLIGHT_MOTH_WANDERAREA = new PolyArea(List.of(new WorldPosition(1573, 9450, 0),new WorldPosition(1577, 9447, 0),new WorldPosition(1575, 9442, 0),new WorldPosition(1573, 9437, 0),new WorldPosition(1563, 9433, 0),new WorldPosition(1556, 9433, 0),new WorldPosition(1555, 9439, 0),new WorldPosition(1561, 9443, 0)));



    @Override
    public boolean activate() {
        if (script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR)) != null) {
            script.log(CatchMoth.class, "Catch Moth task activated...");
            return true;
        }
        return false;
    }


    //ensure player position is in moths area
    //search tiles containing moth npcs
    //select moth closest to player?
    //if player animating, wait & have timeout
    //else (or after timeout) restart task execution.
    //if animation is done -> check inventory to see if amount of moonlight jar increased (or if # of butterfly jars decrease)
    //if yes, successful, re run task & update.
    // if no, re run task

    @Override
    public void execute() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));

        if (inventorySnapshot == null) {
            script.log(CatchMoth.class, "Cannot get inventory!");
            return;
        }

        if (inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR) == 0) {
            script.log(CatchMoth.class, "Inventory is full!");
            return;
        }

//        script.log(CatchMoth.class, "inventory snapshot 1 : " + script.getWidgetManager().getInventory().search(Set.of()).getAmount(ItemID.BUTTERFLY_JAR) + " jars");
        script.log(CatchMoth.class, "inventory snapshot 2: " + inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR) + " jars");

        int currButterFlyJarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);
        WorldPosition myPosition = script.getWorldPosition();
        //check to see if player is in moth wander area!...


//        List<WorldPosition> validPositions = getValidMothPositions();

        List<WorldPosition> validPositions = butterFlyPositions();

        if (validPositions == null || validPositions.isEmpty()) {
            //walk to moth area...
            script.log(CatchMoth.class, "no valid positions found for moths!");
            return;
        }

        script.log(CatchMoth.class, "Catching closest moth...");
        WorldPosition closestMoth = (WorldPosition) Utils.getClosestPosition(myPosition, validPositions.toArray(new WorldPosition[0]));

        Polygon poly = script.getSceneProjector().getTileCube(closestMoth.getX(),closestMoth.getY() , closestMoth.getPlane(),165, 30, true);
        poly = poly.getResized(0.55);

        Rectangle highlightArea = script.getUtils().getHighlightBounds(poly, MOONLIGHT_MOTH_CLUSTER);

        if (highlightArea == null) {
            script.log(CatchMoth.class, "Highlight Area is null!" + closestMoth);
            return;
        }

        if (!script.getFinger().tap(highlightArea)) {
            script.log(CatchMoth.class, "Failed to tap on moth at position: " + closestMoth);
            return;
        }

        script.submitHumanTask(() -> {
            int postButterFlyJarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);
            if (postButterFlyJarCount > currButterFlyJarCount) {
                script.log(CatchMoth.class, "Successfully caught a moth! Current jar count: " + postButterFlyJarCount + " jars");
                return true;
            }
            script.log(CatchMoth.class, "Failed to catch moth. Current jar count: " + postButterFlyJarCount + " jars");
            return false;

        }, script.random(3000, 6000));


    }

    private List<WorldPosition> butterFlyPositions() {
        RectangleArea testArea = new RectangleArea(1561, 9436, 15, 14, 0);
        List<WorldPosition> searchArea = testArea.getAllWorldPositions();

        if (searchArea.isEmpty()) {
            script.log(CatchMoth.class, "No surrounding positions found in the search area.");
            return null;
        }

        List<WorldPosition> validMothPositions = new ArrayList<>();
        searchArea.forEach(position -> {
            Polygon poly = script.getSceneProjector().getTilePoly(position);

            if (script.getUtils().getHighlightBounds(poly, highlightPixels) != null) {
                validMothPositions.add(position);
            }

        });

        return validMothPositions;
    }

    private List<WorldPosition> getValidMothPositions() {


        UIResultList<WorldPosition> npcPositions = script.getWidgetManager().getMinimap().getNPCPositions();

        if (npcPositions.isNotVisible()) {
            script.log(CatchMoth.class, "Minimap NPC positions are not visible!");
            return null;
        }

        if (npcPositions.isNotFound()) {
            script.log(CatchMoth.class, "No moths found in the current area.");
            return null;
        }

        List<WorldPosition> mothPositions = new ArrayList<>();
        npcPositions.forEach(npcPosition -> {

            RectangleArea tempArea = new RectangleArea(1561, 9436, 15, 14, 0);
            if (!tempArea.contains(npcPosition)) {
                script.log(CatchMoth.class, "NPC position " + npcPosition + " is not within the moth wander area.");
                return;
            }
            script.log(CatchMoth.class, "Checking NPC position: " + npcPosition);

            //check why we use localposition again
            LocalPosition localPosition = npcPosition.toLocalPosition(this.script);

//            Polygon poly = script.getSceneProjector().getTileCube(localPosition.getX(), localPosition.getY(), localPosition.getPlane(), 165, 30,true);

            Polygon poly = script.getSceneProjector().getTilePoly(localPosition);
            if (poly == null) {
                script.log(CatchMoth.class, "Polygon for NPC position " + npcPosition + " is null.");
                return;
            }

//            poly = poly.getResized(0.55);

            if (script.getPixelAnalyzer().findPixel(poly, highlightPixels) != null) {
                script.log(CatchMoth.class, "Found moth at position: " + npcPosition);
                mothPositions.add(npcPosition);

            }
        });

        return mothPositions;
    }
}
