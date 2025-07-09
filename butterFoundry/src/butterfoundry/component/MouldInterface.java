package butterfoundry.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class MouldInterface extends ComponentCentered {
    protected static final Rectangle TITLE_BOUNDS = new Rectangle(10, 8, 426, 21);
    protected static final Rectangle CLOSE_BUTTON_BOUNDS = new Rectangle(452, 7, 21, 21);
    private static final int NW_SELECTED_MOULD_TYPE = 1004;
    private Map<MouldTypes, Rectangle> mouldTypeButtons;
    //Mould type selection (left side)
//    protected static final Rectangle FORTE_TAB = new Rectangle(10, 119, 113, 36);

    public MouldInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(580, 310, ColorUtils.TRANSPARENT_PIXEL);
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        return new ComponentImage<>(canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB), -1, 1); //check tolerancecomparator (aio furnace)
    }

    @Override
    public boolean isVisible() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }

        Rectangle titleBounds = bounds.getSubRectangle(TITLE_BOUNDS);
        String title = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleBounds, ColorUtils.ORANGE_UI_TEXT);
        return title.equalsIgnoreCase("Giants' Foundry Mould Setup");
    }

    @Override
    public Rectangle getBounds() { //Allows us to get the bounds of the buttons everytime we work with the interface
        Rectangle bounds = super.getBounds();
        if (bounds == null) {
            return null;
        }

        this.mouldTypeButtons = findMouldButtons(bounds);
        return bounds;
    }

    private Map<MouldTypes, Rectangle> findMouldButtons(Rectangle bounds) {
        List<Rectangle> mouldButtons = core.getImageAnalyzer().findContainers(bounds, 3540, 3544, 3542, 3545);
        List<Rectangle> selectedMouldButtons = core.getImageAnalyzer().findContainers(bounds, NW_SELECTED_MOULD_TYPE, 1008, 1006, 1009);
        List<Rectangle> allMouldButtons = new ArrayList<>(mouldButtons);
        allMouldButtons.addAll(selectedMouldButtons);

        Map<MouldTypes, Rectangle> mouldTypeButtons = new HashMap<>();
        for (Rectangle button : allMouldButtons) {
            String buttonText = core.getOCR().getText(Font.STANDARD_FONT, button, ColorUtils.ORANGE_UI_TEXT);
            if (buttonText.isEmpty()) {
                core.log(MouldInterface.class, "Failed to read button text for: " + button);
            }

            MouldTypes mouldType = MouldTypes.fromText(buttonText);
            if (mouldType != null) {
                Rectangle interfaceBounds = new Rectangle(button.x - bounds.x, button.y - bounds.y, button.width, button.height); //This gets the bounds relative to the interface
                mouldTypeButtons.put(mouldType, interfaceBounds);
                core.log(MouldInterface.class, "Found mould button: " + mouldType.getText() + " at " + interfaceBounds);
            } else {
                core.log(MouldInterface.class, "Unknown mould type button: " + buttonText + " at " + button);
            }
        }
        return mouldTypeButtons;
    }

    public MouldTypes getMouldTypeButton() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }

        SearchableImage buttonCornerImage = new SearchableImage(NW_SELECTED_MOULD_TYPE, core, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
        for (Map.Entry<MouldTypes, Rectangle> entry : mouldTypeButtons.entrySet()) {
            Rectangle buttonScreenBounds = bounds.getSubRectangle(entry.getValue());
            if (core.getImageAnalyzer().isSubImageAt(buttonScreenBounds.x,buttonScreenBounds.y,buttonCornerImage) != null) {
                return entry.getKey();
            }
        }

        core.log(MouldInterface.class, "No mould type button found in the interface bounds.");
        return null;
    }

    public boolean selectMouldTypeButton(MouldTypes mouldType) {
        if (mouldType == null) {
            throw new IllegalArgumentException("Mould type cannot be null");
        }

        MouldTypes currSelectedMouldType = getMouldTypeButton();
        if (currSelectedMouldType == mouldType) {
            core.log(MouldInterface.class, "Mould type selected: " + mouldType.getText());
            return true; // Already selected
        }

        Rectangle buttonBounds = mouldTypeButtons.get(mouldType);
        if (buttonBounds == null) {
            core.log(MouldInterface.class, "Mould type button not found: " + mouldType.getText());
            return false;
        }

        Rectangle bounds = getBounds();
        if (bounds == null) {
            core.log(MouldInterface.class, "Mould interface bounds not found.");
            return false;
        }

        Rectangle screenBounds = bounds.getSubRectangle(buttonBounds);
        Point point = RandomUtils.generateRandomPoint(screenBounds, 6.5);
        core.getFinger().tap(point);
        return core.submitHumanTask(() -> getMouldTypeButton() == mouldType, 3000);
    }

    public enum MouldTypes {
        FORTE("FORTE"),
        TIPS("TIPS"),
        BLADES("BLADES");

        public final String text;

        MouldTypes(String text) {
            this.text = text;
        }

        public static MouldTypes fromText(String text) {
            for (MouldTypes type : MouldTypes.values()) {
                if (type.getText().equalsIgnoreCase(text)) {
                    return type;
                }
            }
            return null;
        }

        public String getText() {
            return text;
        }
    }
}
