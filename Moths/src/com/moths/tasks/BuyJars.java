package moths.tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import moths.components.ShopInterface;
import moths.ui.UI;

import java.util.Set;

import static moths.Moths.*;

public class BuyJars extends Task {

    private static final SearchablePixel[] NPC_PIXEL_CLUSTER = new SearchablePixel[]{
            new SearchablePixel(-12566996, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13884650, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-10202045, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-14286849, new SingleThresholdComparator(2), ColorModel.HSL),
    };
    private final UI ui;
    private final static RectangleArea NPC_AREA = new RectangleArea(1559, 3055, 6, 7, 0);
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
//            bankTask = true;
            catchMothTask = true;
            return;
        }

        if (shopInterface.isVisible()) {
            handleShop(inventorySnapshot);
            return;
        } else {
            WorldPosition position = script.getWorldPosition();
            if (position == null) {
                return;
            }

            if (NPC_AREA.contains(position)) {
                if (!openShop()) {
                    script.log(BuyJars.class, "Shop still not visible!");
                    return;
                }
            } else {
                walkToArea(NPC_AREA);
            }
        }
        script.log(BuyJars.class, "Jars bought so far: " + jarsBought);
        script.log(BuyJars.class, "End of BuyJars task.");
    }

    private boolean buyJars(int currentFreeSlots) {
        return script.pollFramesHuman(() -> {
            ItemGroupResult currInventorySnapShot = script.getWidgetManager().getInventory().search(Set.of());
            if (currInventorySnapShot == null) {
                return false;
            }
            int postFreeSlots = currInventorySnapShot.getFreeSlots();
            return postFreeSlots < currentFreeSlots;
        }, 5000);
    }

    private boolean openShop() {
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

            Rectangle validBounds = script.getPixelAnalyzer().getHighlightBounds(poly, NPC_PIXEL_CLUSTER);
            if (validBounds == null) {
                script.log(BuyJars.class, "npc position is null, checking next position...");
                continue;
            }

            script.log(BuyJars.class, "NPC found!");
            if (script.getFinger().tap(validBounds, "Trade")) {
                script.log(BuyJars.class, "Trade interaction successful!");
                return script.pollFramesHuman(() -> shopInterface.isVisible(), RandomUtils.uniformRandom(4000, 8000));
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
        int jarsInShop = itemInShop.getStackAmount();

        if (jarsInShop < 10) {
            script.log(BuyJars.class, "Shop stock amount is low. Hopping worlds..." + jarsInShop);
            shopInterface.close();
            script.getProfileManager().forceHop();
            return false;
        }

        if (!shopInterface.setSelectedAmount(50)) {
            script.log(BuyJars.class, "Failed to set selected amount.");
            return false;
        }

        if (itemInShop.interact()) {
            if (buyJars(inventoryFreeSlots)) {
                int after = script.getWidgetManager().getInventory().search(Set.of()).getFreeSlots();
                jarsBought += inventoryFreeSlots - after;
            }
            shopInterface.close();
            return true;
        }
        script.log(BuyJars.class, "interact failed!");
        return false;
    }

    private boolean walkToArea(RectangleArea area) {
        script.log(BuyJars.class, "Walking to area...");

        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(3);
        builder.breakCondition(() -> {
            WorldPosition currentPosition = script.getWorldPosition();
            if (currentPosition == null) {
                return false;
            }
            return area.contains(currentPosition);
        });
        return script.getWalker().walkTo(area.getRandomPosition(), builder.build());
    }
}
