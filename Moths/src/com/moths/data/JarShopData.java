package moths.data;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import moths.ui.UI;

import java.util.List;


public enum JarShopData {
    HUNTERS_GUILD(
            // NPC area
            new RectangleArea(1559, 3055, 6, 7, 0),
            // NPC highlight cluster
            new SearchablePixel[]{
                    new SearchablePixel(-12566996, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-13884650, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-10202045, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(2), ColorModel.HSL)},
            new RectangleArea(1541, 3038, 9, 5, 0),
            6191
            ),

    NARADH(
            new PolyArea(List.of(
                    new WorldPosition(3441, 2905, 0),
                    new WorldPosition(3444, 2905, 0),
                    new WorldPosition(3444, 2901, 0),
                    new WorldPosition(3442, 2901, 0),
                    new WorldPosition(3441, 2899, 0),
                    new WorldPosition(3438, 2899, 0),
                    new WorldPosition(3438, 2902, 0),
                    new WorldPosition(3441, 2902, 0))),
            new SearchablePixel[]{
            new SearchablePixel(-14155777, new SingleThresholdComparator(1), ColorModel.HSL),
            new SearchablePixel(-7570852, new SingleThresholdComparator(0), ColorModel.HSL),
            },
            new RectangleArea(3427, 2889, 3, 5, 0),
            13613
            );

    private final Area npcArea;
    private final SearchablePixel[] npcPixels;
    private final Area bankArea;
    private final int shopRegion;

    JarShopData(Area npcArea, SearchablePixel[] npcPixels, Area bankArea, int shopRegion) {
        this.npcArea = npcArea;
        this.npcPixels = npcPixels;
        this.bankArea = bankArea;
        this.shopRegion = shopRegion;
    }

    public Area getNpcArea() { return npcArea; }
    public SearchablePixel[] getNpcPixels() { return npcPixels; }
    public Area getbankArea() { return bankArea; }
    public int getShopRegion() { return shopRegion; }

    public static JarShopData fromUI(UI ui) {
        String label = ui.getBuyBankLocation();
        if ("Hunter's Guild".equalsIgnoreCase(label)) return HUNTERS_GUILD;
        return NARADH;
    }
}