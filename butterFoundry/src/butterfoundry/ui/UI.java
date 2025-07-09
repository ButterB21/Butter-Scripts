package butterfoundry.ui;

import butterfoundry.data.Alloys;
import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UI extends VBox {
    private final ScriptCore core; //check
    private static final int[] alloyTypeIDs = new int[] {
            -1, ItemID.BRONZE_BAR, ItemID.IRON_BAR, ItemID.STEEL_BAR, ItemID.MITHRIL_BAR, ItemID.ADAMANTITE_BAR, ItemID.RUNITE_BAR
    };

    // Alloy selection and quantities
    protected ComboBox<Integer> alloyTypeComboBox1;
    private VBox itemsVBox1;
    private final Map<Integer, Spinner<Integer>> itemSpinners1 = new HashMap<>();

    private ComboBox<Alloys> alloyItemComboBox1;
    private Spinner<Integer> quantitySpinner1;

    protected ComboBox<Integer> alloyTypeComboBox2;
    private ComboBox<Alloys> alloyItemComboBox2;
    private VBox itemsVBox2;
    private final Map<Integer, Spinner<Integer>> itemSpinners2 = new HashMap<>();

    private Spinner<Integer> quantitySpinner2;

    // For tracking the total bars
    private Label totalBarsLabel;
    private Button confirmButton;


    // Alloy type options
    private static final String[] ALLOY_TYPES = {"Bronze", "Iron", "Steel", "Mithril", "Adamantite", "Runite"};

    public UI(ScriptCore core) {
        this.core = core;
        setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-spacing: 15; -fx-alignment: center");
        setPrefWidth(800);
        setMaxHeight(600); // Prevent excessive height

        // Title
        Label titleLabel = new Label("Giants' Foundry Alloy Setup");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");

        // Instructions
        Label instructionsLabel = new Label("Select alloys and items for smelting. Total bars must be at least 28.");
        instructionsLabel.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");

        // Create sections side by side to save vertical space
        HBox sectionsContainer = new HBox(20);
        sectionsContainer.setAlignment(Pos.TOP_CENTER);

        // Setup each alloy section
        VBox alloy1Section = createAlloySection("Alloy 1", 1);
        VBox alloy2Section = createAlloySection("Alloy 2", 2);

        sectionsContainer.getChildren().addAll(alloy1Section, alloy2Section);

        // Total bars indicator
        totalBarsLabel = new Label("Total Bars: 0");
        totalBarsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-alignment: center;");


        // Confirm button
        confirmButton = new Button("Confirm");
        confirmButton.setDisable(true);
        confirmButton.setPrefWidth(120);
        confirmButton.setOnAction(e -> {
            if (getTotalBars() >= 28) {
                ((Stage) getScene().getWindow()).close();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "You need at least 28 bars total to start the foundry!", ButtonType.OK);
                alert.show();
            }
        });

        // Add all to main layout
        getChildren().addAll(titleLabel, instructionsLabel, sectionsContainer, totalBarsLabel, confirmButton);

        // Initial update
        updateTotalBars();

    }

    private VBox createAlloySection(String title, int sectionNum) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: #4c5459; -fx-padding: 10; -fx-background-radius: 5;");
        section.setPrefWidth(400);
        section.setMaxHeight(400); // Limit section height

        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");

        // Alloy type selection
        Label typeLabel = new Label("Alloy Type:");
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        ComboBox<Integer> typeComboBox = JavaFXUtils.createItemCombobox(core, alloyTypeIDs);
        typeComboBox.setPromptText("Select Type");
        typeComboBox.setPrefWidth(200);

        // Items container with scroll
        Label itemsLabel = new Label("Items & Quantities:");
        itemsLabel.setStyle("-fx-font-size: 12;");

        VBox itemsVBox = new VBox(5);
        itemsVBox.setPadding(new Insets(5));

        // Scroll pane for items (in case there are many)
        ScrollPane scrollPane = new ScrollPane(itemsVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(150);
        scrollPane.setMaxHeight(200);

        scrollPane.setStyle(
                "-fx-background: #4c5459; " +
                        "-fx-background-color: #4c5459; " +
                        "-fx-control-inner-background: #4c5459; " +
                        "-fx-border-color: #636E72; " +
                        "-fx-border-width: 1px;"
        );

        // Set up listener for type selection
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateItemRowsForAlloyType(newVal, itemsVBox, sectionNum);
            updateTotalBars();
        });

        // Store references
        if (sectionNum == 1) {
            alloyTypeComboBox1 = typeComboBox;
            itemsVBox1 = itemsVBox;
        } else {
            alloyTypeComboBox2 = typeComboBox;
            itemsVBox2 = itemsVBox;
        }

        section.getChildren().addAll(sectionTitle, typeLabel, typeComboBox, itemsLabel, scrollPane);

        return section;
    }

    private void updateItemRowsForAlloyType(Integer alloyTypeId, VBox itemsVBox, int sectionNum) {
        // Clear existing items
        itemsVBox.getChildren().clear();
        Map<Integer, Spinner<Integer>> spinnerMap = (sectionNum == 1) ? itemSpinners1 : itemSpinners2;
        spinnerMap.clear();

        if (alloyTypeId == null || alloyTypeId == -1) {
            // Show bank filler image when "None" is selected
            if (alloyTypeId == -1) {
                HBox noneRow = new HBox(8);
                noneRow.setAlignment(Pos.CENTER);
                noneRow.setPadding(new Insets(10));

                Label noneLabel = new Label("No alloy selected");
                noneLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-style: italic; -fx-font-size: 12;");

                ImageView bankFillerIcon = JavaFXUtils.getItemImageView(core, ItemID.BANK_FILLER);
                noneRow.getChildren().addAll(bankFillerIcon, noneLabel);
                itemsVBox.getChildren().add(noneRow);
            }
            return;
        }

        // Get alloy type name from the selected item ID
        String alloyTypeName = getAlloyTypeStringFromId(alloyTypeId);
        if (alloyTypeName == null) return;

        // Filter alloys by type
        List<Alloys> relevantItems = Arrays.stream(Alloys.values())
                .filter(alloy -> alloy.getAlloyType().equals(alloyTypeName))
                .collect(Collectors.toList());

        // Create a row for each item
        for (Alloys alloy : relevantItems) {
            HBox row = createItemRow(alloy, spinnerMap);
            itemsVBox.getChildren().add(row);
        }
    }

    private HBox createItemRow(Alloys alloy, Map<Integer, Spinner<Integer>> spinnerMap) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2));

        // Add hover effect styling
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 3px;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 3px;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 3px;"));

        // Item icon
        ImageView icon = JavaFXUtils.getItemImageView(core, alloy.getItemID());

        // Item name
        String itemName = core.getItemManager().getItemName(alloy.getItemID());
        Label nameLabel = new Label(itemName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10;");
        nameLabel.setPrefWidth(120);
        nameLabel.setWrapText(true);

        // Bar value info
        Label barLabel = new Label("(" + alloy.getBarValue() + " bars)");
        barLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 9;");

        // Quantity spinner
        Spinner<Integer> spinner = new Spinner<>(0, 50, 0);
        spinner.setPrefWidth(60);
        spinner.setEditable(true);

        // Add listener to update total bars when quantity changes
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalBars());

        // Store spinner reference
        spinnerMap.put(alloy.getItemID(), spinner);

        row.getChildren().addAll(icon, nameLabel, barLabel, spinner);
        return row;
    }

    private String getAlloyTypeStringFromId(int itemId) {
        if (itemId == -1) return "None";

        // Map item IDs back to alloy type strings
        if (itemId == ItemID.BRONZE_BAR) return "Bronze";
        if (itemId == ItemID.IRON_BAR) return "Iron";
        if (itemId == ItemID.STEEL_BAR) return "Steel";
        if (itemId == ItemID.MITHRIL_BAR) return "Mithril";
        if (itemId == ItemID.ADAMANTITE_BAR) return "Adamantite";
        if (itemId == ItemID.RUNITE_BAR) return "Runite";
        return null;
    }

    private void updateTotalBars() {
        int totalBars = getTotalBars();
        totalBarsLabel.setText("Total Bars: " + totalBars);

        if (totalBars >= 28) {
            totalBarsLabel.setStyle("-fx-text-fill: #7CFC00; -fx-font-size: 14; -fx-font-weight: bold;");
            confirmButton.setDisable(false);
        } else {
            totalBarsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
            confirmButton.setDisable(true);
        }
    }

    private int getTotalBars() {
        int total = 0;

        // Calculate from alloy 1 spinners
        for (Map.Entry<Integer, Spinner<Integer>> entry : itemSpinners1.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue().getValue();
            int barValue = getBarValueForItemId(itemId);
            total += barValue * quantity;
        }

        // Calculate from alloy 2 spinners
        for (Map.Entry<Integer, Spinner<Integer>> entry : itemSpinners2.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue().getValue();
            int barValue = getBarValueForItemId(itemId);
            total += barValue * quantity;
        }

        return total;
    }

    private int getBarValueForItemId(int itemId) {
        for (Alloys alloy : Alloys.values()) {
            if (alloy.getItemID() == itemId) {
                return alloy.getBarValue();
            }
        }
        return 0;
    }

    // Public methods for the main script to get user selections

    public Map<Integer, Integer> getAlloy1ItemsAndQuantities() {
        Map<Integer, Integer> result = new HashMap<>();
        for (Map.Entry<Integer, Spinner<Integer>> entry : itemSpinners1.entrySet()) {
            int quantity = entry.getValue().getValue();
            if (quantity > 0) {
                result.put(entry.getKey(), quantity);
            }
        }
        return result;
    }

    public Map<Integer, Integer> getAlloy2ItemsAndQuantities() {
        Map<Integer, Integer> result = new HashMap<>();
        for (Map.Entry<Integer, Spinner<Integer>> entry : itemSpinners2.entrySet()) {
            int quantity = entry.getValue().getValue();
            if (quantity > 0) {
                result.put(entry.getKey(), quantity);
            }
        }
        return result;
    }

    public Integer getSelectedAlloyType1() {
        return alloyTypeComboBox1.getValue();
    }

    public Integer getSelectedAlloyType2() {
        return alloyTypeComboBox2.getValue();
    }

    public int getTotalBarsPublic() {
        return getTotalBars();
    }
}