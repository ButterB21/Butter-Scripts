package com.butter.script.hunter.buttermoonlightantelope.util;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;

import java.util.List;

import static com.butter.script.hunter.buttermoonlightantelope.Constants.MOONLIGHT_REGION;
import static com.butter.script.hunter.buttermoonlightantelope.Constants.TRAP_SAFE_AREA;
import static com.butter.script.hunter.buttermoonlightantelope.ButterMoonlightAntelope.runEnergyThreshold;

public class MovementUtils {

    public static boolean walkToArea(ScriptCore core, Area area) {
        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(2);
        builder.breakCondition(() -> {
            WorldPosition currentPosition = core.getWorldPosition();
            if (currentPosition == null) {
                return false;
            }

            core.log(MovementUtils.class, "Walking to area...");
            return area.contains(currentPosition);
        });
        return core.getWalker().walkTo(area.getRandomPosition(), builder.build());
    }

    public static void walkToObject(ScriptCore core, RSObject object) {
        int breakDistance = RandomUtils.uniformRandom(2, 5);
        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(2).breakDistance(breakDistance);
        builder.breakCondition(() -> {
            WorldPosition playerPos = core.getWorldPosition();
            if (playerPos == null) {
                return false;
            }

            Polygon objPolygon = object.getConvexHull();
            if (objPolygon == null) {
                return false;
            }

            return object.distance(playerPos) < 3 || (core.getWidgetManager().insideGameScreenFactor(objPolygon, List.of(ChatboxComponent.class)) >= 0.7);
        });
        core.getWalker().walkTo(object, builder.build());
    }

    public static boolean playerInSafeArea(ScriptCore core) {
        WorldPosition playerPos = core.getWorldPosition();
        if (playerPos == null) {
            core.log(MovementUtils.class, "Player pos is null!");
            return false;
        }

        int randomSafeArea = RandomUtils.uniformRandom(TRAP_SAFE_AREA.size() - 1);
        if (TRAP_SAFE_AREA.stream().noneMatch(safeArea -> safeArea.contains(playerPos))) {
            core.log(MovementUtils.class, "Walking to safe area...");
            walkToArea(core, TRAP_SAFE_AREA.get(randomSafeArea));
            return false;
        }
        return true;
    }

    public static void climbDownStairs(ScriptCore core) {
        WorldPosition playerPos = core.getWorldPosition();
        if (playerPos == null) {
            core.log(MovementUtils.class, "Player pos is null!");
            return;
        }

        RSObject stairs = core.getObjectManager().getClosestObject(playerPos,"Stairs");
        if (stairs == null) {
            core.log(MovementUtils.class, "Cannot find stairs!");
            return;
        }

        if (stairs.interact("Climb-down")) {
            core.pollFramesHuman(() -> {
                WorldPosition pos = core.getWorldPosition();
                if (pos == null) {
                    return false;
                }

                return pos.getRegionID() == MOONLIGHT_REGION;
            }, RandomUtils.uniformRandom(10000, 15000));
        }
    }

    public static void enableRunEnergy(ScriptCore core) {
        if (!core.getWidgetManager().getMinimapOrbs().isRunEnabled()) {
            if (core.getWidgetManager().getMinimapOrbs().getRunEnergy() > runEnergyThreshold) {
                core.log(MovementUtils.class, "Enabling run energy...");
                runEnergyThreshold = RandomUtils.uniformRandom(30, 60);
                core.log(MovementUtils.class, "Next run energy threshold set to: " + runEnergyThreshold);
                core.getWidgetManager().getMinimapOrbs().setRun(true);
            }
        }
    }
}
