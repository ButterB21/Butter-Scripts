package com.butter.script.hunter.buttermoonlightantelope;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Constants {
    public static final SearchablePixel[] MOONLIGHT_ANTELOPE_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-14221313, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12107198, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-14469301, new SingleThresholdComparator(2), ColorModel.HSL),
    };

    public static final Set<Integer> LOG_IDS = new HashSet<>(Set.of(ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.MAPLE_LOGS));
    public static final Set<Integer> ITEM_IDS_TO_DROP = new HashSet<>(Set.of(ItemID.BIG_BONES, ItemID.MOONLIGHT_ANTELOPE_FUR, ItemID.JUG));
    public static final Set<Integer> ITEM_IDS_TO_KEEP = new HashSet<>(Set.of(
            ItemID.KNIFE, ItemID.CHISEL, ItemID.TEASING_STICK, ItemID.BONECRUSHER, ItemID.MOONLIGHT_ANTLER_BOLTS, ItemID.SMALL_MEAT_POUCH, ItemID.LARGE_MEAT_POUCH, ItemID.SMALL_FUR_POUCH, ItemID.LARGE_FUR_POUCH));
    public static final Set<Integer> ITEM_IDS_POUCHES = new HashSet<>(Set.of(
            ItemID.SMALL_MEAT_POUCH_OPEN, ItemID.LARGE_MEAT_POUCH_OPEN,
            ItemID.SMALL_FUR_POUCH_OPEN, ItemID.LARGE_FUR_POUCH_OPEN));
    public static final Set<Integer> ITEM_IDS_TO_BANK = new HashSet<>(Set.of(ItemID.RAW_MOONLIGHT_ANTELOPE, ItemID.MOONLIGHT_ANTELOPE_ANTLER));

    public static final int BANK_REGION = 6191;
    public static final int MOONLIGHT_REGION = 6291;

    public static final RectangleArea BANK_AREA = new RectangleArea(1541, 3038, 10, 4, 0);
    public static final RectangleArea MOONLIGHT_HUNT_AREA = new RectangleArea(1549, 9412, 19, 11, 0);

    public static final RectangleArea TRAP_SAFE_AREA_WEST = new RectangleArea(1551, 9417, 3, 5, 0);
    public static final RectangleArea TRAP_SAFE_AREA_EAST = new RectangleArea(1566, 9415, 2, 3, 0);
    public static final RectangleArea TRAP_SAFE_AREA_SOUTH = new RectangleArea(1559, 9412, 4, 2, 0);
    public static final List<RectangleArea> TRAP_SAFE_AREA = List.of(TRAP_SAFE_AREA_WEST, TRAP_SAFE_AREA_EAST, TRAP_SAFE_AREA_SOUTH);

    public static final PolyArea VALID_MOONLIGHT_NPC_AREA = new PolyArea(List.of(
            new WorldPosition(1557, 9422, 0),
            new WorldPosition(1557, 9417, 0),
            new WorldPosition(1563, 9417, 0),
            new WorldPosition(1567, 9423, 0),
            new WorldPosition(1561, 9424, 0)));

    public static final RectangleArea VALID_PLAYER_POS = new RectangleArea(1557, 9417, 6, 5, 0);

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Banker", "Bank chest"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
}
