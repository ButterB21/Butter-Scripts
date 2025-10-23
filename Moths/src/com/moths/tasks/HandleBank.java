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
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;
import moths.data.JarShopData;
import moths.data.MothData;
import moths.ui.UI;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static moths.Moths.*;
import static moths.Moths.jarsBought;

public class HandleBank extends Task {

    public HandleBank(Script script, UI ui) {
        super(script);
        this.ui = ui;
    }

    private UI ui;
    private MothData mothType;

    private final PolyArea insideGuildArea = new PolyArea(List.of(new WorldPosition(1244, 3742, 0), new WorldPosition(1244, 3729, 0), new WorldPosition(1245, 3723, 0), new WorldPosition(1250, 3723, 0), new WorldPosition(1251, 3739, 0), new WorldPosition(1254, 3739, 0), new WorldPosition(1254, 3742, 0)));
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Grand Exchange booth", "Bank counter", "Bank table", "Banker", "Bank chest"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        if (gameObject.getName() == null) return false;
        if (gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) return false;
        return gameObject.canReach();
    };

    @Override
    public boolean activate() {
        script.log(HandleBank.class, "banktask value: " + bankTask);
        if ("Only Catch (No bank)".equalsIgnoreCase(ui.getSelectedMethod())) {
            bankTask = false;
            return false;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            return true;
        }

        if (!bankTask) {
            return false;
        }

        if (activateRestocking) {
            return true;
        }

        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            return false;
        }

        if (jarsAvailable() > 0) {
            catchMothTask = true;
            bankTask = false;
            return false;
        }

        MothData mothType = MothData.fromUI(ui);
        // CHANGE: multi-region check
        boolean inMothRegion = mothType.isInMothRegion(myPosition);
        boolean inBankRegion = mothType.getBankRegion() == myPosition.getRegionID();

        if (inMothRegion || inBankRegion) {
            return true;
        }

        if (mothType == MothData.SUNLIGHT_MOTH) {
            walkToArea(mothType.getMothArea());
            return false;
        }
        return false;
    }

    @Override
    public void execute() {
        if ("Only Buy & Bank Jars".equalsIgnoreCase(ui.getSelectedMethod())) {
            handleBuyAndBankMode();
            return;
        }

        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            return;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            handleBank();
            return;
        }

        mothType = MothData.fromUI(ui);
        if (jarsAvailable() <= 0) {
            if (activateRestocking) {
                bankTask = false;
                return;
            }
            if (mothType == MothData.MOONLIGHT_MOTH) {
                if (mothType.getBankRegion() != myPosition.getRegionID()) {
                    RSObject stairs = script.getObjectManager().getClosestObject(myPosition, "Stairs");
                    if (stairs == null) return;
                    if (!stairs.interact("Climb-up")) return;
                    boolean climbed = script.pollFramesHuman(() -> {
                        WorldPosition pos = script.getWorldPosition();
                        return pos != null && mothType.getBankRegion() == pos.getRegionID();
                    }, RandomUtils.uniformRandom(16000, 22000));
                    if (!climbed) return;
                }
            } else if (mothType == MothData.BLACK_WARLOCK
                    || mothType == MothData.RUBY_HARVEST) {
                if (!insideGuildArea.contains(myPosition)) {
                    RectangleArea guildDoorArea = new RectangleArea(1246, 3715, 5, 7, 0);
                    if (!walkToArea(guildDoorArea)) return;
                    RSObject door = script.getObjectManager().getClosestObject(myPosition,"Door");
                    if (door == null) return;
                    if (!door.interact("Open")) return;
                    boolean moved = script.pollFramesHuman(() -> {
                        WorldPosition pos = script.getWorldPosition();
                        return insideGuildArea.contains(pos);
                    }, RandomUtils.uniformRandom(15000, 20000));
                    if (!moved) return;
                }
            }
        }

        if (!walkToArea(mothType.getBankArea())) {
            return;
        }

        if (!openBank()) {
            return;
        }
        if (!handleBank()) {
            return;
        }
    }

    private void handleBuyAndBankMode() {
        JarShopData shop = JarShopData.fromUI(ui);
        Area bankArea = shop.getbankArea();

        if (!walkToArea(bankArea)) {
            script.log(HandleBank.class, "Not at selected Buy&Bank location's bank area (" + shop.name() + ").");
            return;
        }

        if (!openBank()) {
            script.log(HandleBank.class, "Failed to open bank in Buy&Bank mode.");
            return;
        }

        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.COINS_995))) {
            script.log(HandleBank.class, "Failed to deposit butterfly jars.");
            return;
        }

        if (jarsBought >= ui.restockAmount) {
            script.log(HandleBank.class, "Finished restocking (" + jarsBought + "/" + ui.restockAmount + "). Stopping script.");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return;
        }

        // Continue the loop: close bank, keep restocking, ensure BuyJars remains active
        script.getWidgetManager().getBank().close();
        bankTask = false;
        script.log(HandleBank.class, "Deposited jars. Continuing Buy&Bank loop (" + jarsBought + "/" + ui.restockAmount + ").");
    }

    private int jarsAvailable() {
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (inv == null) return -1;
        return inv.getAmount(ItemID.BUTTERFLY_JAR);
    }

    private boolean openBank() {
        List<RSObject> banksFound = script.getObjectManager().getObjects(BANK_QUERY);
        if (banksFound.isEmpty()) {
            return false;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
            return false;
        }

        return script.pollFramesHuman(() -> script.getWidgetManager().getBank().isVisible(),
                RandomUtils.uniformRandom(10000, 15000));
    }

    private boolean handleBank() {
        if (!script.getWidgetManager().getBank().isVisible()) {
            return false;
        }

        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.COINS_995))) {
            return false;
        }

        ItemGroupResult bankSnapShot = script.getWidgetManager().getBank().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (bankSnapShot == null) {
            return false;
        }

//        if (ui.getSelectedMethod().equalsIgnoreCase("Only Buy & Bank Jars")) {
//            if (jarsBought >= ui.restockAmount) {
//                script.getWidgetManager().getLogoutTab().logout();
//                script.stop();
//            }
//            bankTask = false;
//            script.getWidgetManager().getBank().close();
//            return true;
//        }

        if (activateRestocking) {
            if (bankSnapShot.getAmount(ItemID.BUTTERFLY_JAR) >= ui.getRestockAmount()) {
                activateRestocking = false;
            }
            script.getWidgetManager().getBank().close();
            return true;
        }

        if (bankSnapShot.getAmount(ItemID.BUTTERFLY_JAR) <= 27) {
            if (!ui.isRestocking()) {
                script.getWidgetManager().getBank().close();
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return false;
            }
            activateRestocking = true;
            script.getWidgetManager().getBank().close();
            return true;
        }

        script.getWidgetManager().getBank().withdraw(ItemID.BUTTERFLY_JAR, Integer.MAX_VALUE);
        script.getWidgetManager().getBank().close();
        return true;
    }

    private boolean walkToArea(Area area) {
        script.log(HandleBank.class, "Walking to  area: " + area);
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            WorldPosition cur = script.getWorldPosition();
            return cur != null && (area.contains(cur) || area.distanceTo(cur) <= RandomUtils.uniformRandom(2,5));
        });
        return script.getWalker().walkTo(area.getRandomPosition(), builder.build());
    }
}