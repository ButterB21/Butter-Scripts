package com.butter.script.hunter.buttermoonlightantelope.data;

import com.osmb.api.item.ItemID;

public enum Food {
    TUNA(ItemID.TUNA, "Tuna", 10),
    JUG_OF_WINE(ItemID.JUG_OF_WINE, "Jug of Wine", 11),
    LOBSTER(ItemID. LOBSTER, "Lobster", 12),
    BASS(ItemID.BASS, "Bass", 13),
    SWORDFISH(ItemID. SWORDFISH, "Swordfish", 14),
    POTATO_WITH_CHEESE(ItemID. POTATO_WITH_CHEESE, "Potato with Cheese", 16),
    MONKFISH(ItemID.MONKFISH, "Monkfish", 16),
    KARAMBWAN(ItemID. COOKED_KARAMBWAN, "Karambwan", 18),
    SHARK(ItemID.SHARK, "Shark", 20),
    MANTA_RAY(ItemID. MANTA_RAY, "Manta Ray", 22);

    private final int itemID;
    private final String name;
    private final int healAmount;

    Food (int itemID, String name, int healAmount) {
        this.itemID = itemID;
        this.name = name;
        this.healAmount = healAmount;
    }

    public int getItemID() {
        return itemID;
    }

    public String getName() {
        return name;
    }

    public static Food getFood(int itemID) {
        for (Food food : Food.values()) {
            if (food.getItemID() == itemID) {
                return food;
            }
        }
        return null;
    }

    public int getHealAmount() {
        return healAmount;
    }

}
