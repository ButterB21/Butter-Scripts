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

public enum MothData {
    MOONLIGHT_MOTH(
            new PolyArea(List.of(
                    new WorldPosition(1573, 9451, 0),
                    new WorldPosition(1577, 9446, 0),
                    new WorldPosition(1575, 9442, 0),
                    new WorldPosition(1573, 9437, 0),
                    new WorldPosition(1557, 9433, 0),
                    new WorldPosition(1553, 9437, 0))),
            new SearchablePixel[]{
                    new SearchablePixel(-14221313, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-11035192, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-11974309, new SingleThresholdComparator(2), ColorModel.HSL) },
            6291,
            6191,
            new RectangleArea(1541, 3038, 9, 5, 0)),

    BLACK_WARLOCK(
            new PolyArea(List.of(
                    new WorldPosition(1239, 3749, 0),
                    new WorldPosition(1243, 3744, 0),
                    new WorldPosition(1242, 3740, 0),
                    new WorldPosition(1225, 3740, 0),
                    new WorldPosition(1225, 3743, 0))),
            new SearchablePixel[] {
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-7332332, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-15330284, new SingleThresholdComparator(1), ColorModel.HSL)},
            4922,
            4922,
            new RectangleArea(1247, 3739, 7, 4, 0)),

    RUBY_HARVEST(
            new PolyArea(List.of(
                    new WorldPosition(1239, 3749, 0),
                    new WorldPosition(1243, 3744, 0),
                    new WorldPosition(1242, 3740, 0),
                    new WorldPosition(1225, 3740, 0),
                    new WorldPosition(1225, 3743, 0))),
            new SearchablePixel[] {
                    new SearchablePixel(-7332332, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-11584468, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL),},
            4922,
            4922,
            new RectangleArea(1247, 3739, 7, 4, 0)),

    SUNLIGHT_MOTH(
            new RectangleArea(1570, 3016, 18, 15, 0),
            new SearchablePixel[] {
                    new SearchablePixel(-6532555, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-4874966, new SingleThresholdComparator(1), ColorModel.HSL),},
            6191,
            6191,
            new RectangleArea(1541, 3038, 9, 5, 0)),

    RUBY_HARVEST_KOUREND(
            new RectangleArea(1499, 3471, 25, 16, 0),
            new SearchablePixel[] {
                    new SearchablePixel(-7332332, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-11584468, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL),},
            5942,
            5941,
            new RectangleArea(1508, 3419, 5, 3, 0)),

    RUBY_HARVEST_ALDARIN(
            new PolyArea(List.of(
                    new WorldPosition(1332, 2945, 0),
                    new WorldPosition(1338, 2946, 0),
                    new WorldPosition(1349, 2937, 0),
                    new WorldPosition(1349, 2934, 0),
                    new WorldPosition(1345, 2929, 0),
                    new WorldPosition(1344, 2928, 0),
                    new WorldPosition(1330, 2926, 0))),
            new SearchablePixel[]{
                    new SearchablePixel(-7332332, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-11584468, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL)},
            5165,
            new int[]{5421, 5166},
            5421,
            new RectangleArea(1396, 2923, 5, 6, 0));

    private final Area mothArea;
    private final SearchablePixel[] mothClusterPixels;
    private final int mothRegion;
    private final int[] additionalMothRegions;
    private final int bankRegion;
    private final Area bankArea;

    // Original single-region constructor delegates to multi-region
    MothData(Area mothArea,
             SearchablePixel[] mothClusterPixels,
             int mothRegion,
             int bankRegion,
             Area bankArea) {
        this(mothArea, mothClusterPixels, mothRegion, null, bankRegion, bankArea);
    }

    // Multi-region constructor
    MothData(Area mothArea,
             SearchablePixel[] mothClusterPixels,
             int mothRegion,
             int[] additionalMothRegions,
             int bankRegion,
             Area bankArea) {
        this.mothArea = mothArea;
        this.mothClusterPixels = mothClusterPixels;
        this.mothRegion = mothRegion;
        this.additionalMothRegions = additionalMothRegions;
        this.bankRegion = bankRegion;
        this.bankArea = bankArea;
    }

    public Area getMothArea() { return mothArea; }
    public SearchablePixel[] getMothClusterPixels() { return mothClusterPixels; }
    public int getMothRegion() { return mothRegion; }
    public int getBankRegion() { return bankRegion; }
    public Area getBankArea() { return bankArea; }

    // Multi-region helpers
    public boolean isMultiRegion() {
        return additionalMothRegions != null && additionalMothRegions.length > 0;
    }

    public boolean isInMothRegion(WorldPosition pos) {
        if (pos == null) return false;
        int r = pos.getRegionID();
        if (r == mothRegion) return true;
        if (additionalMothRegions != null) {
            for (int extra : additionalMothRegions) {
                if (extra == r) return true;
            }
        }
        return false;
    }

    public int[] getAllMothRegions() {
        if (!isMultiRegion()) return new int[]{mothRegion};
        int[] all = new int[1 + additionalMothRegions.length];
        all[0] = mothRegion;
        System.arraycopy(additionalMothRegions, 0, all, 1, additionalMothRegions.length);
        return all;
    }

    // Aldarin routing when UI selects Ruby Harvest + Aldarin location
    public static MothData fromUI(UI ui) {
        if (ui.isCatchingMoonlightMoth()) return MOONLIGHT_MOTH;
        if (ui.isCatchingBlackWarlock()) return BLACK_WARLOCK;
        if (ui.isCatchingRubyHarvest()) {
            if (ui.isRubyHarvestAtLandsEnd()) return RUBY_HARVEST_KOUREND;
            if (ui.isRubyHarvestAtAldarin()) return RUBY_HARVEST_ALDARIN; // ADDED
            return RUBY_HARVEST;
        }
        if (ui.isCatchingBothWarlockAndHarvest()) return BLACK_WARLOCK;
        if (ui.isCatchingSunlightMoth()) return SUNLIGHT_MOTH;
        return MOONLIGHT_MOTH;
    }
}