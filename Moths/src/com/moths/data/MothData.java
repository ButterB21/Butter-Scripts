package moths.data;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

import java.util.List;

public enum MothData {
    MOONLIGHT_MOTH(
            new PolyArea(List.of(
                    new WorldPosition(1573, 9451, 0),new WorldPosition(1577, 9446, 0),new WorldPosition(1575, 9442, 0),new WorldPosition(1573, 9437, 0),new WorldPosition(1557, 9433, 0),new WorldPosition(1553, 9437, 0))),
            new SearchablePixel[]{
                    new SearchablePixel(-14221313, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-11035192, new SingleThresholdComparator(2), ColorModel.HSL),
                    new SearchablePixel(-11974309, new SingleThresholdComparator(2), ColorModel.HSL) },
            6291, 6191, new RectangleArea(1541, 3038, 9, 5, 0));
//    RUBY_HARVEST(
//            new PolyArea(List.of(new WorldPosition(1239, 3748, 0),new WorldPosition(1237, 3747, 0),new WorldPosition(1232, 3746, 0),new WorldPosition(1232, 3740, 0),new WorldPosition(1237, 3739, 0),new WorldPosition(1241, 3739, 0),new WorldPosition(1241, 3746, 0),new WorldPosition(1243, 3746, 0),new WorldPosition(1243, 3744, 0),));
//    );

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
}
