package com.butter.script.hunter.buttermoonlightantelope.handler;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.chatbox.dialogue.Dialogue;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;

import static com.butter.script.hunter.buttermoonlightantelope.Constants.*;
import static com.butter.script.hunter.buttermoonlightantelope.ButterMoonlightAntelope.ITEM_IDS_TO_RECOGNIZE;

public class InventoryHandler {
    private final ScriptCore core;
    private ItemGroupResult inventorySnapshot;

    public InventoryHandler(ScriptCore core) {
        this.core = core;
    }

    public boolean checkInvySpace() {
        inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
        if (inventorySnapshot == null) {
            core.log(InventoryHandler.class, "Inventory is null!");
            return false;
        }

        int inventorySpace = inventorySnapshot.getFreeSlots();
        return inventorySpace < 4;
    }

    public void handleInventory() {
        inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
        if (inventorySnapshot == null) {
            return;
        }

        // check if inventory contains food, and if so eat it?

        // ensure in safe zone!!
//        if (shouldChiselAntlers && inventorySnapshot.contains(ItemID.MOONLIGHT_ANTELOPE_ANTLER)) {
//            if (!interactAndWaitForDialogue()) {
//                core.log(InventoryHandler.class, "Failed to chisel antlers!");
//                return;
//            }
//            return;
//        }

        // if inventory is still full after chiseling and dropping items, go bank
        if (!inventorySnapshot.containsAny(ITEM_IDS_TO_DROP) && inventorySnapshot.getFreeSlots() <= 4) {
            // check if anything to chisel if no droppable items?
            climbUpStairs();
            return;
        }

        // check why it is dropping my chisel and antlers too?
        if (core.getWidgetManager().getInventory().dropItems(ITEM_IDS_TO_DROP)) {
            core.log(InventoryHandler.class, "Dropped item(s)!");
        }

        inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
        int inventorySpace = inventorySnapshot.getFreeSlots();
//        if (inventorySpace < 4 && !inventorySnapshot.contains(ItemID.MOONLIGHT_ANTELOPE_ANTLER)) {
//            climbUpStairs();
//        }

        // Use this until Moonlight antlers are chiseled properly
        if (inventorySpace < 4) {
            climbUpStairs();
        }
    }

    public boolean interactAndWaitForDialogue() {
        core.log(InventoryHandler.class, "Chiseling antlers before dropping items...");

        if (!inventorySnapshot.contains(ItemID.CHISEL)) {
            core.log(InventoryHandler.class, "No chisel in inventory to chisel antlers!");
            core.stop();
            return false;
        }

        ItemSearchResult chisel = inventorySnapshot.getItem(ItemID.CHISEL);
        ItemSearchResult randomAntler = inventorySnapshot.getRandomItem(ItemID.MOONLIGHT_ANTELOPE_ANTLER);
        if (!chisel.isSelected()) {
            if (!chisel.interact()) {
                core.log(InventoryHandler.class, "Failed to interact with chisel!");
                return false;
            }
        }

        if (randomAntler.interact()) {
            return core.pollFramesHuman(() -> {
                Dialogue dialogue = core.getWidgetManager().getDialogue();
                if (dialogue == null) {
                    return false;
                }
                return dialogue.getDialogueType() == DialogueType.ITEM_OPTION;
            }, RandomUtils.uniformRandom(2500, 5000));
        }
        return false;
    }


    public void climbUpStairs() {
        WorldPosition playerPos = core.getWorldPosition();
        if (playerPos == null) {
            core.log(InventoryHandler.class, "Player pos is null!");
            return;
        }
        RSObject stairs = core.getObjectManager().getClosestObject(playerPos,"Stairs");
        if (stairs == null) {
            return;
        }

        if (!stairs.isInteractableOnScreen()) {
            core.log(InventoryHandler.class, "Stairs are not interactable on screen! Walking to stairs...");
            WalkConfig.Builder walkConfig = new WalkConfig.Builder().tileRandomisationRadius(3);

            walkConfig.breakCondition(() -> {
                WorldPosition currPos = core.getWorldPosition();
                return currPos != null && stairs.isInteractableOnScreen();
            });

            core.getWalker().walkTo(stairs.getObjectArea().getRandomPosition(), walkConfig.build());
            core.log(InventoryHandler.class, "Walked to stairs!");
        }

        if (!stairs.interact("Climb-up")) {
            return;
        }

        boolean climbingStairs = core.pollFramesHuman(() -> {
            WorldPosition pos = core.getWorldPosition();

            return pos != null && pos.getRegionID() == BANK_REGION;
        }, RandomUtils.uniformRandom(16000, 24000));

        if (!climbingStairs) {
            return;
        }
    }
}
