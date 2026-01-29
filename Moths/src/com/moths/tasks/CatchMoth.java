package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
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
    private final RectangleArea ABOVE_STAIRS_AREA = new RectangleArea(1553, 3042, 7, 7, 0);

    private boolean isCatchOnlyMode() {
        return "Only Catch (No bank)".equalsIgnoreCase(ui.getSelectedMethod());
    }
    private boolean isCatchEnabledMode() {
        String m = ui.getSelectedMethod();
        return "Catch & Bank".equalsIgnoreCase(m) || isCatchOnlyMode();
    }

    private int currButterFlyJarCount = 0;

    @Override
    public boolean activate() {
        if (!catchMothTask) {
            return false;
        }

        if (!isCatchEnabledMode()) {
            catchMothTask = false;
            return false;
        }

        if (isCatchOnlyMode()) {
            return true;
        }

        if (!hasJarsAvailable()) {
            catchMothTask = false;
            bankTask = true;
            return false;
        }
        return true;
    }

    @Override
    public void execute() {
        MothData mothType = resolveMothTypeFromUI();

        if (!isCatchOnlyMode()) {
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
            if (inventorySnapshot == null) return;
            currButterFlyJarCount = inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);
        } else {
            script.getWidgetManager().getTabManager().closeContainer();
        }

        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) return;

        // CHANGE: Multi-region support
        if (!mothType.isInMothRegion(myPosition)) {
            returnToMothRegion(mothType);
            return;
        }

        if (!mothType.getMothArea().contains(myPosition)) {
            if (mothType == MothData.BLACK_WARLOCK
                    || mothType == MothData.RUBY_HARVEST
                    || mothType == MothData.RUBY_HARVEST_KOUREND
                    || mothType == MothData.RUBY_HARVEST_ALDARIN) {
                returnToMothRegion(mothType);
                return;
            }
            script.log(CatchMoth.class, "Does nto contain my position!");
            walkToArea(mothType.getMothArea());
            return;
        }

        List<WorldPosition> validPositions = butterFlyPositions(mothType.getMothArea());
        if (validPositions == null || validPositions.isEmpty()) return;

        WorldPosition closestMoth = script.getWorldPosition().getClosest(validPositions);
        Polygon poly = script.getSceneProjector().getTileCube(closestMoth, 120);
        if (poly == null) return;

        Rectangle mothBounds = script.getPixelAnalyzer().getHighlightBounds(poly, mothType.getMothClusterPixels());
        if (mothBounds == null) return;

        if (!script.getFinger().tap(mothBounds.getCenter(), "catch")) return;

        AtomicInteger animatingTimeout = new AtomicInteger(RandomUtils.uniformRandom(500));
        Timer animatingTimer = new Timer();
        script.pollFramesHuman(() -> {
            if (isCatchOnlyMode()) {
                if (!script.getPixelAnalyzer().isPlayerAnimating(0.5)) {
                    if (animatingTimer.timeElapsed() > animatingTimeout.get()) {
                        animatingTimeout.set(RandomUtils.uniformRandom(500));
                        mothsCaught++;
                        return true;
                    }
                    return false;
                }
                animatingTimer.reset();
            } else {
                ItemGroupResult postInvSnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
                if (postInvSnapshot == null) return false;
                int postCount = postInvSnapshot.getAmount(ItemID.BUTTERFLY_JAR);
                if (postCount < currButterFlyJarCount) {
                    mothsCaught++;
                    return true;
                }
            }
            return false;
        }, RandomUtils.uniformRandom(9000, 13000));
    }

    // CHANGE: Added Aldarin resolution path
    private MothData resolveMothTypeFromUI() {
        if (ui.getSelectedMothItemId() == ItemID.RUBY_HARVEST) {
            if (ui.isRubyHarvestAtLandsEnd()) return MothData.RUBY_HARVEST_KOUREND;
            if (ui.isRubyHarvestAtAldarin()) return MothData.RUBY_HARVEST_ALDARIN;
            return MothData.RUBY_HARVEST;
        }
        return MothData.fromUI(ui);
    }

    private boolean hasJarsAvailable() {
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (inv == null) return false;
        return inv.getAmount(ItemID.BUTTERFLY_JAR) > 0;
    }

    private void walkToArea(Area area) {
        WalkConfig.Builder b = new WalkConfig.Builder();
        b.breakCondition(() -> {
            WorldPosition p = script.getWorldPosition();
            return p != null && area.contains(p);
        });
        script.getWalker().walkTo(area.getRandomPosition(), b.build());
    }

    private List<WorldPosition> butterFlyPositions(Area area) {
        List<WorldPosition> search = area.getAllWorldPositions();
        UIResultList<WorldPosition> players = script.getWidgetManager().getMinimap().getPlayerPositions();
        if (search.isEmpty()) return null;

        if (players.isFound()) {
            if (players.asList().stream().anyMatch(search::contains)) {
                script.getProfileManager().forceHop();
                return null;
            }
        }

        List<WorldPosition> valid = new ArrayList<>();
        search.forEach(pos -> {
            Polygon poly = script.getSceneProjector().getTilePoly(pos);
            if (poly == null) return;
            if (script.getWorldPosition().distanceTo(pos) > 20) return;
            if (script.getPixelAnalyzer().getHighlightBounds(poly, HIGHLIGHT_PIXEL) != null) {
                valid.add(pos);
            }
        });
        return valid;
    }

    private boolean returnToMothRegion(MothData moth){
        WorldPosition playerPos = script.getWorldPosition();
        if (playerPos == null) {
            return false;
        }

        if (moth.isMultiRegion()) {
            script.log(CatchMoth.class, "Multi region moth!");
            walkToArea(moth.getMothArea());
            return true;
        }

        if (moth == MothData.MOONLIGHT_MOTH) {
            RSObject stairs = script.getObjectManager().getClosestObject(playerPos,"Stairs");
            if (stairs == null) {
                return false;
            }

            Polygon stairsPoly = stairs.getConvexHull();
            if (stairsPoly == null || script.getWidgetManager().insideGameScreenFactor(stairsPoly, List.of(ChatboxComponent.class)) < 0.3) {
                script.log(CatchMoth.class, "stairsPoly is null!");
                walkToArea(ABOVE_STAIRS_AREA);
                return false;
            }

            if (stairs.interact("Climb-down")) {
                script.pollFramesHuman(() -> {
                    WorldPosition p = script.getWorldPosition();
                    return moth.isInMothRegion(p);
                }, RandomUtils.uniformRandom(10000, 15000));
            }
        } else if (moth == MothData.BLACK_WARLOCK
                || moth == MothData.RUBY_HARVEST
                || moth == MothData.RUBY_HARVEST_KOUREND
                || moth == MothData.RUBY_HARVEST_ALDARIN) { // ADDED
            if (insideGuildArea.contains(playerPos)) {
                RSObject door = script.getObjectManager().getClosestObject(playerPos,"Door");
                if (door == null) return false;
                if (!door.interact("Open")) return false;
                boolean exited = script.pollFramesHuman(() -> {
                    WorldPosition p = script.getWorldPosition();
                    return p != null && !insideGuildArea.contains(p);
                }, RandomUtils.uniformRandom(15000, 20000));
                if (!exited) return false;
            }
            if (!insideGuildArea.contains(playerPos)) {
                script.log(CatchMoth.class, "Not inside guild area!");

                walkToArea(moth.getMothArea());
            }
        } else {
            script.log(CatchMoth.class, "afeter guild area!");

            walkToArea(moth.getMothArea());
        }
        return true;
    }

}