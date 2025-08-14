import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import org.w3c.dom.css.Rect;

public enum Shop {

    BLAST_FURNACE("Blast Furnace", 7757, new WorldPosition(1936, 4966, 0), new RectangleArea(1946, 4958, 1949, 4956, 0), true);

    private final String name;
    private final int regionID;
    private final WorldPosition idleLocation;
    private final Area bankArea;
    private final boolean isIdleNPC;

    Shop(String name, int regionID, WorldPosition idleLocation, Area bankArea, boolean isIdleNPC) {
        this.bankArea = bankArea;
        this.idleLocation = idleLocation;
        this.regionID = regionID;
        this.name = name;
        this.isIdleNPC = isIdleNPC;
    }

    public Area getBankArea() {
        return bankArea;
    }

    public WorldPosition getIdleLocation() {
        return idleLocation;
    }

    public int getRegionID() {
        return regionID;
    }

    public boolean isIdleNPC() {
        return isIdleNPC;
    }

    @Override
    public String toString() {
        return name;
    }
}


