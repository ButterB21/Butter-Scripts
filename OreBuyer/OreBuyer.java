import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import javafx.scene.Scene;
import com.osmb.api.item.ItemID;

import java.awt.*;
import java.awt.Color;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import com.osmb.api.utils.timing.Timer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;



@ScriptDefinition(name = "OreBuyer", author = "Butter", version = 1.5, description = "Buy Ores and Bank", skillCategory = SkillCategory.OTHER)
public class OreBuyer extends Script {

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};

    private final Set<Integer> oreIDs = new HashSet<>(Set.of(ItemID.COPPER_ORE, ItemID.TIN_ORE, ItemID.IRON_ORE, ItemID.MITHRIL_ORE, ItemID.SILVER_ORE, ItemID.COAL, ItemID.GOLD_ORE));
    private Shop selectedShop;
    private ShopInterface shopInterface;
    private int selectedItemID;
    private String itemName = null;
    private boolean hopFlag = false;
    private boolean useCoalBag = false;
    private ItemGroupResult inventorySnapshot;
    private long startTime = 0;
    private int oresBought = 0;
    private static final Font ARIEL = new Font("Arial", Font.PLAIN, 14);

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

        useCoalBag = ui.useCoalBag;
        log(OreBuyer.class, "useCoalBag value: " + useCoalBag + " From ScriptOptions: " + ui.useCoalBag);

        selectedShop = ui.getSelectedShop();
        log("Selected shop value: " + selectedShop);

        if (itemName == null) {
            log(OreBuyer.class, "Item not found. Logging out!");
            getWidgetManager().getLogoutTab().logout();
            stop();
        }
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onPaint(Canvas c) {
        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        f.setDecimalFormatSymbols(s);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        String runtime = formatTime(elapsed);

        int oresPerHour = elapsed > 0 ? (int) ((oresBought * 3600000L) / elapsed) : 0;
        int y = 40;

        c.fillRect(5, y, 220, 110, Color.BLACK.getRGB(), 1);
        c.drawRect(5, y, 220, 110, Color.WHITE.getRGB());

        c.drawText("Ores bought: " + f.format(oresBought), 10, y += 25, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Ores/hr: " + f.format(oresPerHour), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Runtime: " + runtime, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Script version: 1.5", 10, y += 20, Color.WHITE.getRGB(), ARIEL);
    }


    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank interface...");
            handleBank();
            return 0;
        }

        if (shopInterface.isVisible()) {
            handleShopInterface();
            return 0;
        }

        if (hopFlag) {
            hopWorlds();
            return 0;
        }

        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.COINS_995));
        if (inventorySnapshot == null) {
            // Inventory is not visible
            return 0;
        }

        int amountOfCoins = inventorySnapshot.getAmount(ItemID.COINS_995);
        log(OreBuyer.class, "Coins: " + amountOfCoins);

        if (amountOfCoins < 5000) {
            log(OreBuyer.class, "You don't have enough coins! Logging out...");
            getWidgetManager().getLogoutTab().logout();
            stop();
        }

        if (inventorySnapshot.getFreeSlots() <= 1) {
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
        Set<Integer> itemsToIgnore = Set.of(ItemID.COINS_995, ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        if (!getWidgetManager().getBank().depositAll(itemsToIgnore)) {
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

            Polygon npcLocation = polygon.getResized(0.5);
            getFinger().tap(npcLocation, "Trade Ordan");
            submitHumanTask(() -> shopInterface.isVisible(), random(6000, 9000));
        }
    }
    private void handleShopInterface () {
        log("HandleShopInterface selectedOreID: " + selectedItemID);
        inventorySnapshot = getWidgetManager().getInventory().search(Set.of());
        ItemGroupResult shopSnapShot = shopInterface.search(oreIDs);

        if (inventorySnapshot == null || shopSnapShot == null) {
            return;
        }

        int freeSlots = inventorySnapshot.getFreeSlots();
        log(OreBuyer.class, "handleShopInterface freeSlots value: " + freeSlots);

        if (inventorySnapshot.isFull()) {
            log(OreBuyer.class, "Inventory Full. Closing shop interface.");
            shopInterface.close();

            if (useCoalBag) {
                fillCoalBag(freeSlots);
            }
            return;
        }
        log(OreBuyer.class, "oreIDs: " + oreIDs);

        ItemSearchResult itemInShop = getItemManager().scanItemGroup(shopInterface, oreIDs).getItem(selectedItemID);
//        Set<ItemSearchResult> recognisedItems = shopSnapShot.getRecognisedItems();

        if (itemInShop == null) {
            log(OreBuyer.class, "Could not find selected item in the shop. Logging out...");
            getWidgetManager().getLogoutTab().logout();
            stop();
        }

        int itemInStock = itemInShop.getStackAmount();
        log(OreBuyer.class, "item in shop: " + itemInShop);
        log(OreBuyer.class, "Item remaining in shop: " + itemInStock);

        if (itemInStock > 0) {
            handleShopBuying(itemInShop);
        } else {
            log(OreBuyer.class, "Shop is out of stock. Hopping worlds...");
            shopInterface.close();
            hopFlag = true;
        }
    }

    private void handleShopBuying(ItemSearchResult itemToBuy) {
        int amountToBuy = 50;
        int freeSlots = getWidgetManager().getInventory().search(Set.of()).getFreeSlots();
        log(OreBuyer.class, "Initial freeSlots value: " + freeSlots);

        int before = getWidgetManager().getInventory().search(Set.of(selectedItemID)).getAmount(selectedItemID);
        if(buyItem(amountToBuy, itemToBuy, freeSlots)) {
            int after = getWidgetManager().getInventory().search(Set.of(selectedItemID)).getAmount(selectedItemID);
            int bought = Math.max(0, after - before);
            oresBought += bought;
        }
    }

    private boolean buyItem (int amount, ItemSearchResult item, int freeSlots) {
        log(OreBuyer.class, "Buying item: " + itemName);
        shopInterface.setSelectedAmount(amount);
        log(OreBuyer.class, "setSelectedAmount executed with a value of: " + amount);

        if (item.interact()) {
            log(OreBuyer.class, "Buying " + item);
            return submitTask(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(Set.of());
                if (inventorySnapshot == null) {
                    log(OreBuyer.class, "Inventory is not visible");
                    return false;
                }
                log(OreBuyer.class, "buyItem free slots: " + inventorySnapshot);
                if (inventorySnapshot.isEmpty()) {
                    return false;
                }
                log(OreBuyer.class, "buyItem func: freeSlots_ value: " + inventorySnapshot.getFreeSlots() + " vs freeSlots value: " + freeSlots);
                return inventorySnapshot.getFreeSlots() < freeSlots;
            }, 5000);
        }
        return false;
    }

    private boolean fillCoalBag (int freeSlots) {
        Set<Integer> coalBagIDs = Set.of(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(coalBagIDs);

        if (inventorySnapshot == null) {
            log(OreBuyer.class, "Inventory not visible.");
        }
        if (!inventorySnapshot.containsAny(coalBagIDs)) {
            log(OreBuyer.class, "Coal Bag not found in the inventory");
            return false;
        }

        log(OreBuyer.class, "coalBag result: " + inventorySnapshot);
        //Add check for if bank is visible?
        if (inventorySnapshot.getRandomItem().interact("Fill")) {
            return submitTask(() -> {
                int freeSlots_ = getWidgetManager().getInventory().search(Set.of()).getFreeSlots(); //check
                if (freeSlots_ == 0) {
                    return false;
                }
                log(OreBuyer.class, "fillCoalBag func: freeSlots_ value: " + freeSlots_ + " vs freeSlots value: " + freeSlots);
                return freeSlots_ < freeSlots;
            }, this.random(1600,5000));
        }
        return false;
    }

    private boolean emptyCoalBag() {
        Set<Integer> coalBagIDs = Set.of(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(coalBagIDs);

        if (inventorySnapshot == null) {
            log(OreBuyer.class, "Inventory not visible.");
            return false;
        }

        if (!inventorySnapshot.containsAny(coalBagIDs)) {
            log(OreBuyer.class, "Coal Bag not found in the inventory");
            return false;
        }

        if (inventorySnapshot.getRandomItem().interact("Empty")) {
            return submitTask(() -> false, this.random(1600, 5000));
        }
        return false;
    }

    private void walkToNPC (WorldPosition selectedNPCLocation) {
        log(OreBuyer.class.getSimpleName(), "NPC not on screen. Find NPC...");
        getWalker().walkTo(selectedNPCLocation, new WalkConfig.Builder().tileRandomisationRadius(2).breakDistance(2).build());
    }

    private void hopWorlds() {
        getProfileManager().forceHop();
        hopFlag = false;
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }


    private void checkForUpdates() {
        String latest = getLatestVersion("https://github.com/ButterB21/Butter-Scripts/blob/1403d61b31476384836f5442302e80d02a81cd76/Moths/jar/Moths.jar");

        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }

        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("dCamTorumMiner.jar") || n.startsWith("dCamTorumMiner-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dCamTorumMiner-" + latest + ".jar");
                URL url = new URL("https://github.com/ButterB21/Butter-Scripts/tree/main/OreBuyer/jar");
                try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }

                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();

            } catch (Exception e) {
                log("UPDATE", "❌ Error downloading new version: " + e.getMessage());
            }
        } else {
            log("SCRIPTVERSION", "✅ You are running the latest version (v" + scriptVersion + ").");
        }
    }


    private String getLatestVersion(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            if (c.getResponseCode() != 200) return null;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    if (l.trim().startsWith("version")) {
                        return l.split("=")[1].replace(",", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }


    public static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }
}
