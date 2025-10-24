package com.butter.script.hunter.moonlightantelope;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Constants {
    public static final Set<Integer> LOG_IDS = new HashSet<>(Set.of(ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.MAPLE_LOGS));
    public static final Set<Integer> ITEM_IDS_TO_DROP = new HashSet<>(Set.of(ItemID.BIG_BONES, ItemID.MOONLIGHT_ANTELOPE_FUR));
    public static final Set<Integer> ITEM_IDS_TO_KEEP = new HashSet<>(Set.of(ItemID.RAW_MOONLIGHT_ANTELOPE, ItemID.MOONLIGHT_ANTELOPE_ANTLER));

    public static Set<Integer> ITEM_IDS_TO_RECOGNIZE = new HashSet<>();

    public static final int BANK_REGION = 6191;
    public static final int MOONLIGHT_REGION = 6291;

//    public static final int BANK_AREA =
    public static final PolyArea MOONLIGHT_HUNT_AREA = new PolyArea(List.of(
            new WorldPosition(1559, 9429, 0),
            new WorldPosition(1570, 9425, 0),
            new WorldPosition(1569, 9421, 0),
            new WorldPosition(1566, 9415, 0),
            new WorldPosition(1562, 9411, 0),
            new WorldPosition(1555, 9416, 0),
            new WorldPosition(1551, 9416, 0),
            new WorldPosition(1550, 9422, 0)));



}
