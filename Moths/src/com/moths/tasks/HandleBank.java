package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import moths.data.MothData;
import moths.ui.UI;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class HandleBank extends Task {

    public HandleBank(Script script, UI ui) {
        super(script);
        this.ui = ui;
    }

    private UI ui;
    private MothData moth;
    private static final SearchablePixel HIGHLIGHT_PIXEL = new SearchablePixel(-14221313, new SingleThresholdComparator(2), ColorModel.HSL);
    private static final boolean buyJars = false;
    private static final int amountToBuy = 100;

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table", "Banker"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        // if object has no name
        if (gameObject.getName() == null) {
            return false;
        }
        // has no interact options (eg. bank, open etc.)
        if (gameObject.getActions() == null) {
            return false;
        }

        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
            return false;
        }

        // if no actions contain bank or open
        if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
            return false;
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };

    @Override
    public boolean activate() {
        moth = MothData.MOONLIGHT_MOTH; // Example moth, replace with actual logic to determine the moth
        return true;
    }

    @Override
    public void execute() {
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(HandleBank.class, "Position is null!");
            return;
        }

        if (myPosition.getRegionID() != moth.getBankRegion()) {
            script.log(HandleBank.class, "Not in the bank region!");
            return;
        }

        walkToBank(moth.getBankArea());

        if (!openBank()) {
            script.log(HandleBank.class, "Failed to open bank!");
            return;
        }

        if (!handleBank()) {
            script.log(HandleBank.class, "Failed to handle bank!");
            return;
        }

        returnToMothRegion(moth);

    }

    private boolean openBank() {
        script.log(HandleBank.class, "Opening bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(BANK_QUERY);
        if (banksFound.isEmpty()) {
            script.log(HandleBank.class, "No bank found nearby!");
            return false;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
            script.log(HandleBank.class, "Failed to interact with bank object: " + bank.getName());
            return false;
        }

        return script.submitHumanTask(() -> script.getWidgetManager().getBank().isVisible(), Utils.random(10000, 15000));
    }

    private boolean handleBank() {
        if (!script.getWidgetManager().getBank().isVisible()) {
            script.log(HandleBank.class, "Bank is not visible!");
            return false;
        }

        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.COINS_995))){
            script.log(HandleBank.class, "Failed to deposit");
            return false;
        }

        ItemGroupResult bankSnapShot = script.getWidgetManager().getBank().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (bankSnapShot == null) {
            script.log(HandleBank.class, "Cannot get bank snapshot!");
            return false;
        }

        if (bankSnapShot.getAmount(ItemID.BUTTERFLY_JAR) <= 0) {
            //handle no jars in bank when region id is fixed...
            if (buyJars){
                script.log(HandleBank.class, "Purchasing jars...");
            }
            script.log(HandleBank.class, "No butterfly jars in the bank! Stopping script.");
            script.getWidgetManager().getBank().close();
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return false;
        }

        script.log(HandleBank.class, "Withdrawing butterfly jars from bank...");
        script.getWidgetManager().getBank().withdraw(ItemID.BUTTERFLY_JAR, Integer.MAX_VALUE);
        return true;
    }


    private void walkToBank(Area bankArea) {
        script.log(HandleBank.class, "Walking to the bank...");

        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            WorldPosition currentPosition = script.getWorldPosition();
            if (currentPosition == null) {
                return false;
            }
            return bankArea.contains(currentPosition);
        });
        script.getWalker().walkTo(bankArea.getRandomPosition(), builder.build());

    }

    private void returnToMothRegion(MothData moth){
        script.log(HandleBank.class, "Returning to moth area...");

        if (moth == MothData.MOONLIGHT_MOTH) {
            script.log(HandleBank.class, "Returning to Moonlight Moth area...");

            RSObject stairObj = script.getObjectManager().getClosestObject("Stairs");
            if (stairObj == null) {
                script.log(HandleBank.class, "No stairs found to return to moth area!");
                return;
            }

            if (stairObj.interact("Climb-down")) {
                script.submitHumanTask(() -> {
                    WorldPosition position = script.getWorldPosition();
                    if (position == null) {
                        script.log(HandleBank.class, "Position is null.");
                        return true;
                    }
                    return moth.getMothRegion() == position.getRegionID();
                }, Utils.random(10000, 15000));
            }
        }

    }

    private void buyJars() {
        RectangleArea npcPos = new RectangleArea(1560, 3058, 4, 4, 0);

        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(HandleBank.class, "Cannot get player position!");
            return;
        }

        if (!npcPos.contains(myPosition)) {
            script.log(HandleBank.class, "Walking to NPC to buy jars...");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> {
                WorldPosition currentPosition = script.getWorldPosition();
                if (currentPosition == null) {
                    return false;
                }
                return npcPos.contains(currentPosition);
            });

            script.getWalker().walkTo(npcPos.getRandomPosition(), new WalkConfig.Builder().tileRandomisationRadius(2).breakDistance(2).build());
        }

        UIResultList<WorldPosition> validNPCPositions = script.getWidgetManager().getMinimap().getNPCPositions();
        if (validNPCPositions == null || validNPCPositions.isEmpty()) {
            script.log(HandleBank.class, "No valid NPC positions found to buy jars!");
            return;
        }




    }
}
