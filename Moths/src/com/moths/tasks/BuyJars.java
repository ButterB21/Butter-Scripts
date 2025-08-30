package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import moths.components.ShopInterface;
import moths.data.JarShopData;
import moths.ui.UI;

import java.util.List;
import java.util.Set;

import static moths.Moths.*;

public class BuyJars extends Task {
     private final UI ui;
    ShopInterface shopInterface = new ShopInterface(script);

    public BuyJars(Script script, UI ui) {
        super(script);
        this.ui = ui;
    }

    @Override
    public boolean activate() {
        script.log(BuyJars.class, "Checking BuyJars task...");
        return activateRestocking;
    }

    @Override
    public void execute() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.COINS_995));
        if (inventorySnapshot == null) {
            script.log(HandleBank.class, "Inventory is null!");
            return;
        }

        if(inventorySnapshot.isFull()) {
            script.log(BuyJars.class, "Invy full, ending BuyJars task...");
            bankTask = true;
            return;
        }

        JarShopData shop = JarShopData.fromUI(ui);
        Area npcArea = shop.getNpcArea();
        SearchablePixel[] NPC_CLUSTER = shop.getNpcPixels();

        if (shopInterface.isVisible()) {
            handleShop(inventorySnapshot);
            return;
        } else {
            WorldPosition position = script.getWorldPosition();
            if (position == null) {
                return;
            }

            int distance = script.random(1, 6);

            if (npcArea.contains(position) || npcArea.distanceTo(position) <= distance) {
                if (!openShop(NPC_CLUSTER)) {
                    script.log(BuyJars.class, "Shop still not visible!");
                    return;
                }
            } else {
                walkToArea(npcArea);
            }
        }
        script.log(BuyJars.class, "Jars bought so far: " + jarsBought);
        script.log(BuyJars.class, "End of BuyJars task.");
    }

    private boolean buyJars(int currentFreeSlots) {
        return script.submitTask(() -> {
            ItemGroupResult currInventorySnapShot = script.getWidgetManager().getInventory().search(Set.of());
            if (currInventorySnapShot == null) {
                return false;
            }
            int postFreeSlots = currInventorySnapShot.getFreeSlots();
            return postFreeSlots < currentFreeSlots;
        }, 5000);
    }

    private boolean openShop(SearchablePixel[] npcCluster) {
        UIResultList<WorldPosition> searchArea = script.getWidgetManager().getMinimap().getNPCPositions();

        if (searchArea.isNotVisible()) {
            script.log(BuyJars.class, "No positions found for npc!");
            return false;
        }

        for (WorldPosition position : searchArea) {
            Polygon poly = script.getSceneProjector().getTileCube(position, 130);
            if (poly == null) {
                script.log(BuyJars.class, "Poly is null, checking next...");
                continue;
            }

            Rectangle validBounds = script.getPixelAnalyzer().getHighlightBounds(poly, npcCluster);
            if (validBounds == null) {
                script.log(BuyJars.class, "npc position is null, checking next position...");
                continue;
            }

            script.log(BuyJars.class, "NPC found!");
            if (script.getFinger().tap(validBounds, "Trade")) {
                script.log(BuyJars.class, "Trade interaction successful!");
                return script.submitHumanTask(() -> shopInterface.isVisible(), script.random(4000, 8000));
            }

            script.log(BuyJars.class, "Failed to interact properly!");
        }

        script.log(BuyJars.class, "Failed to find npc to interact with!");
        return false;
    }

    private boolean handleShop(ItemGroupResult inventorySnapshot) {
        if (inventorySnapshot.getAmount(ItemID.COINS_995) < 1000) {
            ui.isRestocking = false;
            script.log(BuyJars.class, "Out of coins!");
            script.stop();
            return false;
        }
        ItemSearchResult itemInShop = script.getItemManager().scanItemGroup(shopInterface, Set.of(ItemID.BUTTERFLY_JAR)).getItem(ItemID.BUTTERFLY_JAR);

        if (itemInShop == null) {
            script.log(BuyJars.class, "itemInShop is null!");
            return false;
        }

        if (inventorySnapshot.isFull()) {
            script.log(BuyJars.class, "Inventory is full. Closing shop interface...");
            shopInterface.close();
            return false;
        }

        int inventoryFreeSlots = inventorySnapshot.getFreeSlots();
        int stock = itemInShop.getStackAmount();

        if (stock < 10) {
            script.log(BuyJars.class, "Shop stock amount is low. Hopping worlds..." + stock);
            shopInterface.close();
            script.getProfileManager().forceHop();
            return false;
        }

        if (!shopInterface.setSelectedAmount(50)) {
            script.log(BuyJars.class, "Failed to set selected amount.");
            return false;
        }

        if (!itemInShop.interact()) {
            script.log(BuyJars.class, "interact failed!");
            return false;
        }

        if (buyJars(inventoryFreeSlots)) {
            ItemGroupResult afterInv = script.getWidgetManager().getInventory().search(Set.of());
            if (afterInv != null) {
                int after = script.getWidgetManager().getInventory().search(Set.of()).getFreeSlots();
                jarsBought += inventoryFreeSlots - after;
            }
        }

        ItemSearchResult postItem = script.getItemManager().scanItemGroup(shopInterface, Set.of(ItemID.BUTTERFLY_JAR)).getItem(ItemID.BUTTERFLY_JAR);
        if (postItem != null) {
            int postStock = postItem.getStackAmount();
            if (postStock < 10) {
                script.log(BuyJars.class, "Shop stock amount is low after purchase. Hopping worlds..." + postStock);
                shopInterface.close();
                script.getProfileManager().forceHop();
                return false;
            }
        }
        if (shopInterface.isVisible()) {
            shopInterface.close();
        }
        return true;
    }

    private boolean walkToArea(Area area) {
        script.log(BuyJars.class, "Walking to npc...");
        int stopDistance = script.random(2, 5);
        script.log(BuyJars.class, "Stop distance: " + stopDistance);
        WalkConfig.Builder builder = new WalkConfig.Builder().breakDistance(stopDistance);
        builder.breakCondition(() -> {
            WorldPosition currentPosition = script.getWorldPosition();
            if (currentPosition == null) {
                return false;
            }
            return area.contains(currentPosition) || currentPosition.distanceTo(area.getRandomPosition()) <= stopDistance;
        });
        return script.getWalker().walkTo(area.getRandomPosition(), builder.build());
    }
}
