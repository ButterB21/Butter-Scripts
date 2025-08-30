package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
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
        script.log(HandleBank.class, "Handle Bank task check...");

        // Never run HandleBank in Only Catch mode
        if ("Only Catch (No bank)".equalsIgnoreCase(ui.getSelectedMethod())) {
            bankTask = false;
            script.log(HandleBank.class, "Only Catch mode - banking disabled.");
            return false;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            return true;
        }

        if (!bankTask) {
            script.log(HandleBank.class, "Bank task is already set to false.");
            return false;
        }

        if (activateRestocking) {
            return true;
        }

        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(HandleBank.class, "Cannot get player position!");
            return false;
        }

        if (jarsAvailable() > 0) {
            script.log(HandleBank.class, "Jars available - switching back to catch mode");
            catchMothTask = true;
            bankTask = false;
            return false;
        }

        MothData mothType = MothData.fromUI(ui);
        boolean inMothRegion = mothType.getMothRegion() == myPosition.getRegionID();
        boolean inBankRegion = mothType.getBankRegion() == myPosition.getRegionID();

        if (inMothRegion || inBankRegion) {
            script.log(HandleBank.class, "Need banking and in appropriate region - activating HandleBank");
            return true;
        }

        script.log(HandleBank.class, "Not in appropriate region for banking");
        return false;
    }

    @Override
    public void execute() {
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(HandleBank.class, "Cannot get player position!");
            return;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            handleBank();
            return;
        }

        mothType = MothData.fromUI(ui);
        JarShopData shop = JarShopData.fromUI(ui);

        if (jarsAvailable() <= 0) {
            if (activateRestocking) {
                //check for coins
                script.log(HandleBank.class, "No jars in inventory, ending HandleBank task...");
                bankTask = false;
                return;
            }
            if (mothType == MothData.MOONLIGHT_MOTH) {
                WorldPosition myPos = script.getWorldPosition();
                if (myPos == null) {
                    return;
                }

                if (mothType.getBankRegion() != myPos.getRegionID()) {
                    RSObject stairs = script.getObjectManager().getClosestObject("Stairs");
                    if (stairs == null) {
                        script.log(HandleBank.class, "Stairs obj is null!");
                        return;
                    }

                    if (!stairs.interact("Climb-up")) {
                        script.log(HandleBank.class, "Failed to interact with stairs to go up!");
                        return;
                    }

                    boolean climbedStairs = script.submitHumanTask(() -> {
                        WorldPosition pos = script.getWorldPosition();
                        if (pos == null) {
                            script.log(HandleBank.class, "Position is null!");
                            return false;
                        }
                        if (mothType.getBankRegion() != pos.getRegionID()) {
                            script.log(HandleBank.class, "Player not in bank region!");
                            return false;
                        }
                        return true;
                    }, script.random(16000, 22000));

                    if (!climbedStairs) {
                        script.log(HandleBank.class, "Failed to climb stairs.");
                        return;
                    }
                }
            } else if (mothType == MothData.BLACK_WARLOCK || mothType == MothData.RUBY_HARVEST) {
                if (!insideGuildArea.contains(myPosition)) {
                    script.log(HandleBank.class, "Player not in guild area. Walking to guild area...");

                    RectangleArea guildDoorArea = new RectangleArea(1246, 3715, 5, 7, 0);
                    if (!walkToArea(guildDoorArea)) {
                        script.log(HandleBank.class, "Not in guild door area");
                        return;
                    }

                    script.log(HandleBank.class, "Getting door...");
                    RSObject guildDoor = script.getObjectManager().getClosestObject("Door");
                    if (guildDoor == null) {
                        script.log(HandleBank.class, "Guild door object is null!");
                        return;
                    }

                    if (!guildDoor.interact("Open")) {
                        script.log(HandleBank.class, "Failed to interact with guild door!");
                        return;
                    }

                    // Wait for player to move through the door
                    boolean movedThroughDoor = script.submitHumanTask(() -> {
                        WorldPosition pos = script.getWorldPosition();
                        return insideGuildArea.contains(pos);
                    }, script.random(15000, 20000));
                    if (!movedThroughDoor) {
                        script.log(HandleBank.class, "Failed to move through door, timing out!");
                        return;
                    }
                }
            }
        }

        if (ui.getSelectedMethod().equalsIgnoreCase("Only Buy & Bank Jars")) {
            if (!walkToArea(shop.getbankArea())) {
                return;
            }

        } else if (!walkToArea(mothType.getBankArea())) {
            script.log(HandleBank.class, "Not in bank area.");
            return;
        }

        if (!openBank()) {
            script.log(HandleBank.class, "Failed to open bank!");
            return;
        }

        if (!handleBank()) {
            script.log(HandleBank.class, "Failed to handle bank!");
            return;
        }

        script.log(HandleBank.class, "Handle Bank task done...");
    }

    private int jarsAvailable() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BUTTERFLY_JAR));

        if (inventorySnapshot == null) {
            script.log(HandleBank.class, "Inventory is null!");
            return -1;
        }
        return inventorySnapshot.getAmount(ItemID.BUTTERFLY_JAR);
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

        return script.submitHumanTask(() -> script.getWidgetManager().getBank().isVisible(), script.random(10000, 15000));
    }

    private boolean handleBank() {
        if (!script.getWidgetManager().getBank().isVisible()) {
            script.log(HandleBank.class, "Bank is not visible!");
            return false;
        }

        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.COINS_995))) {
            script.log(HandleBank.class, "Failed to deposit");
            return false;
        }

        ItemGroupResult bankSnapShot = script.getWidgetManager().getBank().search(Set.of(ItemID.BUTTERFLY_JAR));
        if (bankSnapShot == null) {
            script.log(HandleBank.class, "Cannot get bank snapshot!");
            return false;
        }

        if (ui.getSelectedMethod().equalsIgnoreCase("Only Buy & Bank Jars")) {
            if (jarsBought >= ui.restockAmount) {
                script.log(HandleBank.class, "Finished restocking! Stopping script.");
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
            }
            bankTask = false;
            script.getWidgetManager().getBank().close();
            return true;
        }

        if (activateRestocking) {
            if (bankSnapShot.getAmount(ItemID.BUTTERFLY_JAR) >= ui.getRestockAmount()) {
                script.log(HandleBank.class, "Finished restocking!");
                activateRestocking = false;
            }
            script.log(HandleBank.class, "Current jars left to buy: " + (ui.getRestockAmount() - bankSnapShot.getAmount(ItemID.BUTTERFLY_JAR)));
            script.getWidgetManager().getBank().close();
            return true;
        }

        if (bankSnapShot.getAmount(ItemID.BUTTERFLY_JAR) <= 27) {
            // Check if user has restocking enabled
            if (!ui.isRestocking()) {
                script.log(HandleBank.class, "No butterfly jars in the bank! Stopping script.");
                script.getWidgetManager().getBank().close();
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return false;
            }
            script.log(HandleBank.class, "Restocking is enabled!");
            activateRestocking = true;
            script.getWidgetManager().getBank().close();
            return true;
        }

        script.log(HandleBank.class, "Withdrawing butterfly jars from bank...");
        script.getWidgetManager().getBank().withdraw(ItemID.BUTTERFLY_JAR, Integer.MAX_VALUE);
        script.getWidgetManager().getBank().close();
        return true;
    }

    private boolean walkToArea(Area bankArea) {
        script.log(HandleBank.class, "Walking to area...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(BANK_QUERY);
        if (banksFound.isEmpty()) {
            script.log(HandleBank.class, "No bank found nearby!");
            return false;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);

        WorldPosition worldPosition = script.getWorldPosition();
        if (worldPosition == null) {
            script.log(HandleBank.class, "World position is null, cannot fill watering can.");
            return false;
        }

        boolean walk = script.random(3) == 1 && bankArea.distanceTo(worldPosition) > 7;
        int breakDistance = script.random(2, 5);
        if (walk || !bank.isInteractableOnScreen()) {
            WalkConfig.Builder builder = new WalkConfig.Builder().breakDistance(breakDistance);
            builder.breakCondition(() -> {
                WorldPosition currentPosition = script.getWorldPosition();
                if (currentPosition == null) {
                    return false;
                }
                script.log(HandleBank.class, "Walking to bank...");
                return bank.isInteractableOnScreen();
            });
            return script.getWalker().walkTo(bankArea.getRandomPosition(), builder.build());
        }
        return true;
    }
}
