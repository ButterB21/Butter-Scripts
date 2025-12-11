package com.butter.script.hunter.moonlightantelope.handler;

import com.butter.script.hunter.moonlightantelope.MoonlightAntelope;
import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.butter.script.hunter.moonlightantelope.Constants.*;
import static com.butter.script.hunter.moonlightantelope.MoonlightAntelope.*;

public class BankHandler {
    private final ScriptCore core;
    private final Bank bank;

    public  BankHandler(ScriptCore core) {
        this.core = core;
        this.bank = core.getWidgetManager().getBank();
    }

    private static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) {
            return false;
        }

        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
            return false;
        }

        if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
            return false;
        }
        return gameObject.canReach();
    };


    public void openBank() {
        core.log("Opening bank...");
        WorldPosition myPosition = core.getWorldPosition();
        if (myPosition == null) {
            return;
        }

        if (!walkToBank()) {
            core.log(BankHandler.class, "Failed to walk to bank area.");
            return;
        }

        if (BANK_AREA.contains(myPosition)) {
            List<RSObject> banksFound = core.getObjectManager().getObjects(BANK_QUERY);
            if (banksFound.isEmpty()) {
                core.log(BankHandler.class, "Can't find any banks matching criteria...");
                return;
            }

            RSObject bankObject = (RSObject) core.getUtils().getClosest(banksFound);
            if (!bankObject.isInteractableOnScreen() || !bankObject.interact(BANK_ACTIONS)) {
                return;
            }

            core.pollFramesUntil(() -> bank.isVisible(), RandomUtils.uniformRandom(4000,8000));
        }
    }

    public void handleBank() {
        if (!bank.depositAll(ITEM_IDS_TO_KEEP)) {
            core.log("Failed to deposit items to bank.");
            return;
        }

        withdrawFood();
        bank.close();
        core.pollFramesHuman(() -> !bank.isVisible(), RandomUtils.uniformRandom(3000, 6000));
    }

    private void withdrawFood() {
        // Read player HP info
        Integer hpPct = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        Integer hpCurrent = core.getWidgetManager().getMinimapOrbs().getHitpoints();
        if (hpPct == null || hpCurrent == null) {
            core.log(BankHandler.class, "Could not get player hitpoints info!");
            return;
        }

        int deviation = RandomUtils.uniformRandom(-15, 15);
        randomizedHPPctToEatAt = deviation + hpPctToEatAt;
        core.log(BankHandler.class, "HP % to eat at for next run randomized to: " + randomizedHPPctToEatAt + "%");
        core.log(BankHandler.class, "Player HP: " + hpCurrent + "/" + userHPLevel + " (" + hpPct + "%), Eating at " + randomizedHPPctToEatAt + "%");

        ItemGroupResult foodInBank = core.getWidgetManager().getBank().search(Set.of(selectedFoodItemID));
        if (foodInBank == null) {
            core.log(MoonlightAntelope.class, "Food item not found in bank!");
            core.stop();
            return;
        }

        ItemSearchResult foodItem = foodInBank.getItem(selectedFoodItemID);
        if (foodItem == null || foodItem.getStackAmount() <= 0) {
            core.log(MoonlightAntelope.class, "Out of " + selectedFoodItemID + " in bank!");
            core.stop();
            return;
        }


        ItemGroupResult inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNIZE);
        if (inventorySnapshot == null) {
            core.log(BankHandler.class, "Failed to get inventory snapshot!");
            return;
        }

        int healPerFood = selectedFood.getHealAmount();
        int minFoodNeeded = (userHPLevel - hpCurrent) / healPerFood;
        int availableFoodInInventory = inventorySnapshot.getAmount(selectedFoodItemID);

        if (availableFoodInInventory >= minFoodNeeded) {
            core.log(BankHandler.class, "Already have enough food in inventory, skipping withdraw.");
            return;
        }

        minFoodNeeded -= availableFoodInInventory;
        int randomAmountToWithdraw = RandomUtils.uniformRandom(minFoodNeeded, minFoodNeeded + 3);
        core.log(BankHandler.class, "Withdrawing " + randomAmountToWithdraw + " of food item...");
        if (!bank.withdraw(selectedFoodItemID, randomAmountToWithdraw)) {
            core.log(BankHandler.class, "Failed to withdraw food item!");
            return;
        }
    }

    private boolean walkToBank() {
        core.log(BankHandler.class, "Walking to bank...");
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition playerPosition = core.getWorldPosition();
            if (playerPosition == null) {
                return false;
            }
            return BANK_AREA.contains(playerPosition);
        });

        return core.getWalker().walkTo(BANK_AREA.getRandomPosition(), walkConfig.build());
    }
}
