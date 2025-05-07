import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.walker.WalkConfig;
import javafx.scene.Scene;
import com.osmb.api.item.ItemID;
import java.util.Arrays;
import java.util.List;
import com.osmb.api.utils.timing.Timer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;



@ScriptDefinition(name = "OreBuyer", author = "Butter", version = 1.0, description = "Buy Ores and Bank", skillCategory = SkillCategory.OTHER)
public class OreBuyer extends Script {

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};

    private final int[] oreIDs = new int[] {ItemID.COPPER_ORE, ItemID.TIN_ORE, ItemID.IRON_ORE, ItemID.MITHRIL_ORE, ItemID.SILVER_ORE, ItemID.COAL, ItemID.GOLD_ORE};
    private Shop selectedShop;
    private ShopInterface shopInterface;
    private int selectedItemID;
    private String itemName = null;
    private boolean hopFlag = false;
    private boolean useCoalBag = false;
    private final Predicate<RSObject> bankQuery = gameObject -> {
        // if object has no name
        if (gameObject.getName() == null) {
            return false;
        }
        // has no interact options (eg. bank, open etc.)
        if (gameObject.getActions() == null) {
            return false;
        }

        if (!Arrays.stream(BANK_NAMES).anyMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
            return false;
        }

        // if no actions contain bank or open
        if (!Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
            return false;
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };

    public OreBuyer(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{7757};
    }

    @Override
    public void onStart() {
        shopInterface = new ShopInterface(this);
        //inventory will be seen as visible when shop interface is visible?
        getWidgetManager().getInventory().registerInventoryComponent(shopInterface);
        ScriptOptions ui = new ScriptOptions();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Options", false);

        selectedItemID = ui.getSelectedOre();
        itemName = getItemManager().getItemName(selectedItemID);
        log(OreBuyer.class, "selectedItemID: " + selectedItemID);
//        if (selectedItemID == 453) {
//            useCoalBag = true;
//        }
        useCoalBag = ui.useCoalBag;
        log(OreBuyer.class, "useCoalBag value: " + useCoalBag + " From ScriptOptins: " + ui.useCoalBag);

        selectedShop = ui.getSelectedShop();
        log("Selected shop value: " + selectedShop);

        if (itemName == null) {
            log(OreBuyer.class, "Item not found. Logging out!");
            getWidgetManager().getLogoutTab().logout();
            stop();
        }
    }

    @Override
    public int poll() {
        if (getWidgetManager().getInventory().isVisible()) {
            UIResult<ItemSearchResult> coins = getItemManager().findItem(getWidgetManager().getInventory(), ItemID.COINS_995);
            int amountOfCoins = coins.get().getStackAmount();
            if (coins == null) {
                return 0;
            }
            if (amountOfCoins < 5000) {
                log(OreBuyer.class, "You don't have enough coins! Logging out...");
                getWidgetManager().getLogoutTab().logout();
                stop();
            }
        }

        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank interface...");
            handleBank();
            return 0;
        }

        if (shopInterface.isVisible()) {
            log(OreBuyer.class, "Ore shop is visible");
            handleShopInterface();
            return 0;
        }

        Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());

        log("freeSlots value isEmpty: " + freeSlots.isEmpty());
        log("freeSlots value get: " + freeSlots.get());

        //check for coal bag first to fill?
        if (freeSlots.get() == 0) {
            log(getClass().getSimpleName(), "Inventory full. Open bank...");
            openBank();
            return 0;
        } else {
            log(getClass().getSimpleName(), "Find Shop...");
            openShop();
        }
        return 0;
    }

    private void handleBank() {
        if (!getWidgetManager().getBank().depositAll(new int[]{ItemID.COINS_995, ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG})) {
            return;
        }

        if (useCoalBag) {
            emptyCoalBag();
        }
        getWidgetManager().getBank().close();
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");

        List<RSObject> banksFound = getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Cant find any banks matching criteria...");
            return;
        }

        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) return;
        log(OreBuyer.class, "Waiting for bank to open...");
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        submitTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                return false;
            }
            if (pos.get() == null || !position.equals(pos.get())) {
                positionChangeTimer.get().reset();
                pos.set(position);
            }

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 3000;
        }, 15000);
    }

    private void openShop () {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(OreBuyer.class, "Position is null!");
            return;
        }

        if (selectedShop.isIdleNPC()) {
            WorldPosition selectedNPCLocation = selectedShop.getIdleLocation();
            Polygon polygon = getSceneProjector().getTilePoly(selectedNPCLocation);

            if (polygon == null) {
                walkToNPC(selectedNPCLocation);
                return;
            }

            log("Poly value: " + polygon);
            Polygon npcLocation = polygon.getResized(0.5);
            getFinger().tap(npcLocation, "Trade Ordan");
            submitHumanTask(() -> shopInterface.isVisible(), random(6000, 9000));
        }
    }
    private void handleShopInterface () {
        log("HandleShopInterface selectedOreID: " + selectedItemID);
        Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
        if (freeSlots.get() == 0) {
            log(OreBuyer.class, "Inventory Full. Closing shop interface.");
            shopInterface.close();

            if (useCoalBag) {
                fillCoalBag(freeSlots.get());
            }
            return;
        }
        //find ore in shop
        //UIResult<ItemSearchResult> oreInShop = getItemManager().findItem(shopInterface, selectedOreID);

        //Workaround for finding the coal in the shop
        UIResultList<ItemSearchResult> oreInShop = getItemManager().findAllOfItem(shopInterface, oreIDs);
        ItemSearchResult itemInShop = getItemByID(oreInShop);

        if (itemInShop == null) {
            log(OreBuyer.class, "Could not find any items in the shop. Logging out...");
            getWidgetManager().getLogoutTab().logout();
            stop();
        }

        int itemInStock = itemInShop.getStackAmount();
        log(OreBuyer.class, "Item remaining in shop: " + itemInStock);

        if (itemInStock > 0) {
            handleShopBuying(itemInShop);
        } else {
            log(OreBuyer.class, "Shop is out of stock. Hopping worlds...");
            shopInterface.close();
            hopFlag = true;
            hopWorlds();
        }
    }

    private ItemSearchResult getItemByID (UIResultList<ItemSearchResult> itemsInShop) {
        for (int i = 0; i < itemsInShop.size(); i++) {
            if (itemsInShop.get(i).getId() == selectedItemID) {
                return itemsInShop.get(i);
            }
        }
        return null;
    }

    private void handleShopBuying(ItemSearchResult itemToBuy) {
        int amountToBuy = 50;
        Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
        log(OreBuyer.class, "Initial freeSlots value: " + freeSlots.get());
        //UIResult<ItemSearchResult> itemToBuy = getItemManager().findItem(shopInterface, selectedOreID); //check selectedOreID

        buyItem(amountToBuy, itemToBuy, freeSlots.get());
    }

    private boolean buyItem (int amount, ItemSearchResult item, int freeSlots) {
        log(OreBuyer.class, "Buying item: " + itemName);
        shopInterface.setSelectedAmount(amount); //Taps the selected amount to buy on the shop interface
        log(OreBuyer.class, "setSelectedAmount executed with a value of: " + amount);

        //Click on the selected item to buy
        if (item.interact()) {
            log(OreBuyer.class, "Buying " + item.getId());
            return submitTask(() -> {
                Optional<Integer> freeSlots_ = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                if (!freeSlots_.isPresent()) {
                    return false;
                }
                log(OreBuyer.class, "buyItem func: freeSlots_ value: " + freeSlots_.get() + " vs freeSlots value: " + freeSlots);
                return freeSlots_.get() < freeSlots;
            }, 5000);
        }
        return false;
    }

    private boolean fillCoalBag (int freeSlots) {
        //check if coal bag exists in inventory
        if (!getWidgetManager().getInventory().isVisible()) {
            log(OreBuyer.class, "Inventory not visible.");
            return false;
        }

        UIResultList<ItemSearchResult> coalBag = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        if (coalBag.isNotFound()) {
            log(OreBuyer.class, "Coal Bag not found in the inventory");
            return false;
        }
        log(OreBuyer.class, "coalBag result: " + coalBag);
        //Add check for if bank is visible
        if (coalBag.get(0).interact("Fill")) {
            return submitTask(() -> {
                Optional<Integer> freeSlots_ = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                if (!freeSlots_.isPresent()) {
                    return false;
                }
                log(OreBuyer.class, "fillCoalBag func: freeSlots_ value: " + freeSlots_.get() + " vs freeSlots value: " + freeSlots);
                return freeSlots_.get() < freeSlots;
            }, this.random(1600,5000));
        }
        return false;
    }

    private boolean emptyCoalBag() {
        if (!getWidgetManager().getInventory().isVisible()) {
            log(OreBuyer.class, "Inventory not visible.");
            return false;
        }
        UIResultList<ItemSearchResult> coalBag = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        if (coalBag.isNotFound()) {
            log(OreBuyer.class, "Coal Bag not found in the inventory");
            return false;
        }
        log(OreBuyer.class, "coalBag value at bank: " + coalBag);
        if (coalBag.get(0).interact("Empty")) {
            return submitTask(() -> {
                Optional<Integer> freeSlots_ = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                log(OreBuyer.class, "emptyCoalBag func: freeSlots_ value: " + freeSlots_.get());
                return true;
            }, this.random(1600, 5000));
        }
        return false;
    }

    private void walkToNPC (WorldPosition selectedNPCLocation) {
        log(OreBuyer.class.getSimpleName(), "NPC not on screen. Find NPC...");
        getWalker().walkTo(selectedNPCLocation, new WalkConfig.Builder().tileRandomisationRadius(2).breakDistance(2).build());
    }

    private void hopWorlds() {
        forceHop();
        hopFlag = false;
    }
}
