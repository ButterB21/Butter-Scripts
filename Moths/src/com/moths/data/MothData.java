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
                    new WorldPosition(1573, 9451, 0),new WorldPosition(1577, 9446, 0),new WorldPosition(1575, 9442, 0),new WorldPosition(1573, 9437, 0),new WorldPosition(1557, 9433, 0),new WorldPosition(1553, 9437, 0))),
            new SearchablePixel[]{
                    new SearchablePixel(-14221313, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-11035192, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-11974309, new SingleThresholdComparator(2), ColorModel.HSL) },
            6291, 6191, new RectangleArea(1541, 3038, 9, 5, 0)),

    BLACK_WARLOCK(
            new PolyArea(List.of(
                    new WorldPosition(1239, 3748, 0),new WorldPosition(1243, 3745, 0),new WorldPosition(1243, 3744, 0),new WorldPosition(1241, 3740, 0),new WorldPosition(1224, 3740, 0),new WorldPosition(1224, 3743, 0),new WorldPosition(1232, 3744, 0),new WorldPosition(1232, 3746, 0))),
            new SearchablePixel[] {
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-7332332, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-15330284, new SingleThresholdComparator(1), ColorModel.HSL)},
            4922, 4922, new RectangleArea(1247, 3739, 7, 4, 0)),
    RUBY_HARVEST(
            new PolyArea(List.of(
                    new WorldPosition(1239, 3748, 0),new WorldPosition(1243, 3745, 0),new WorldPosition(1243, 3744, 0),new WorldPosition(1241, 3740, 0),new WorldPosition(1224, 3740, 0),new WorldPosition(1224, 3743, 0),new WorldPosition(1232, 3744, 0),new WorldPosition(1232, 3746, 0))),
            //      new WorldPosition(1239, 3748, 0),new WorldPosition(1237, 3747, 0),new WorldPosition(1232, 3746, 0),new WorldPosition(1232, 3740, 0),new WorldPosition(1237, 3739, 0),new WorldPosition(1241, 3739, 0),new WorldPosition(1241, 3746, 0),new WorldPosition(1243, 3746, 0),new WorldPosition(1243, 3744, 0))),
            new SearchablePixel[] {
                    new SearchablePixel(-7332332, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-11584468, new SingleThresholdComparator(1), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(1), ColorModel.HSL),},
            4922, 4922, new RectangleArea(1247, 3739, 7, 4, 0)
            ),
    SUNLIGHT_MOTH(
            new PolyArea(List.of(
                    new WorldPosition(1592, 3016, 0),new WorldPosition(1576, 3013, 0),new WorldPosition(1563, 3013, 0),new WorldPosition(1558, 3024, 0),new WorldPosition(1580, 3034, 0),new WorldPosition(1592, 3033, 0))),
            new SearchablePixel[] {
                    new SearchablePixel(-6532555, new SingleThresholdComparator(0), ColorModel.HSL),
                    new SearchablePixel(-14286849, new SingleThresholdComparator(0), ColorModel.HSL),
                    new SearchablePixel(-4874966, new SingleThresholdComparator(0), ColorModel.HSL),},
            6191, 6191, new RectangleArea(1541, 3038, 9, 5, 0)
                );


    private final PolyArea mothArea;
    private final SearchablePixel[] mothClusterPixels;
    private final int mothRegion;
    private final int bankRegion;
    private final Area bankArea;

    MothData(PolyArea mothArea,SearchablePixel[] mothClusterPixels, int mothRegion, int bankRegion, Area bankArea) {
        this.mothArea = mothArea;
        this.mothClusterPixels = mothClusterPixels;
        this.mothRegion = mothRegion;
        this.bankRegion = bankRegion;
        this.bankArea = bankArea;
    }

    public PolyArea getMothArea() {
        return mothArea;
    }
    public SearchablePixel[] getMothClusterPixels() {
        return mothClusterPixels;
    }
    public int getMothRegion() {
        return mothRegion;
    }
    public int getBankRegion() {
        return bankRegion;
    }

    public Area getBankArea() {
        return bankArea;
    }

    // Static helper method to get MothData from UI selection
    public static MothData fromUI(UI ui) {
        if (ui.isCatchingMoonlightMoth()) {
            return MOONLIGHT_MOTH;
        } else if (ui.isCatchingBlackWarlock()) {
            return BLACK_WARLOCK;
        } else if (ui.isCatchingRubyHarvest()) {
            return RUBY_HARVEST;
        } else if (ui.isCatchingBothWarlockAndHarvest()) {
            return BLACK_WARLOCK; //Black Warlock and Ruby Harvest are so similar in enum's, the user only needs to have highlights turned on for both to catch them.
        } else if (ui.isRestocking()) {
            return MOONLIGHT_MOTH;
        } else if (ui.isCatchingSunlightMoth()) {
            return SUNLIGHT_MOTH;
        }
        // Default fallback
        return MOONLIGHT_MOTH;
    }
}
