package moths.ui;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.prefs.Preferences;

public class UI extends VBox {
    private final ScriptCore core;
    private ComboBox<String> methodComboBox;
    private ComboBox<Integer> mothTypeComboBox;
    private CheckBox combinedMothsCheckBox;
    private CheckBox restockCheckBox;
    private Spinner<Integer> restockAmountSpinner;
    private HBox restockContainer; // Container for restock controls

    private boolean isCombinedSelected = false;

    // Variables for task usage
    public String selectedMethod = "Catch Moths";
    public int selectedMothItemId = ItemID.MOONLIGHT_MOTH;
    public boolean isRestocking = false; // Only used for "Catch Moths" with Moonlight/Sunlight
    public int restockAmount = 50; // Default and mandatory for "Only Buy & Bank Jars"
    public boolean catchBothWarlockAndHarvest = false;

    // Define your moth ItemIDs array
    private static final int[] MOTH_TYPE_IDS = new int[] {
            ItemID.MOONLIGHT_MOTH,
            ItemID.SUNLIGHT_MOTH,
            ItemID.BLACK_WARLOCK,
            ItemID.RUBY_HARVEST
    };

    // Preferences keys
    private static final String PREF_METHOD = "selectedMethod";
    private static final String PREF_MOTH_TYPE = "selectedMothType";
    private static final String PREF_RESTOCK = "isRestocking"; // Only for "Catch Moths"
    private static final String PREF_RESTOCK_AMOUNT = "restockAmount";
    private static final String PREF_COMBINED = "catchBothWarlockAndHarvest";

    public UI(ScriptCore core) {
        this.core = core;
        setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-spacing: 15; -fx-alignment: center");
        setPrefWidth(400);
        setMaxHeight(400); // Increased height to accommodate spinner

        Label titleLabel = new Label("Moth Catcher Setup");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");

        // Method selection
        Label methodLabel = new Label("Select Method:");
        methodLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        methodComboBox = new ComboBox<>();
        methodComboBox.getItems().addAll("Catch Moths", "Only Buy & Bank Jars");
        methodComboBox.setPrefWidth(250);
        methodComboBox.setPromptText("Select Method");

        // Moth type selection using JavaFXUtils
        Label mothTypeLabel = new Label("Select Moth Type:");
        mothTypeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        mothTypeComboBox = JavaFXUtils.createItemCombobox(core, MOTH_TYPE_IDS);
        mothTypeComboBox.setPrefWidth(250);
        mothTypeComboBox.setPromptText("Select Moth Type");

        // Combined moths checkbox (initially hidden)
        combinedMothsCheckBox = new CheckBox("Catch Both Black Warlock & Ruby Harvest");
        combinedMothsCheckBox.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        combinedMothsCheckBox.setVisible(false);

        // Restock checkbox and spinner container
        restockContainer = new HBox(10);
        restockContainer.setAlignment(Pos.CENTER_LEFT);

        restockCheckBox = new CheckBox("Enable Jar Restocking");
        restockCheckBox.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        // Restock amount spinner
        restockAmountSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(50, 10000, 50, 50);
        restockAmountSpinner.setValueFactory(valueFactory);
        restockAmountSpinner.setPrefWidth(80);
        restockAmountSpinner.setEditable(true);
        restockAmountSpinner.setDisable(true); // Disabled until restocking is enabled for "Catch Moths"

        restockContainer.getChildren().addAll(restockCheckBox, restockAmountSpinner);

        // Method selection listener
        methodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Catch Moths".equals(newVal)) {
                mothTypeComboBox.setVisible(true);
                updateOptionsVisibility();
            } else {
                mothTypeComboBox.setVisible(false);
                combinedMothsCheckBox.setVisible(false);
                restockContainer.setVisible(true); // Show restock amount spinner for "Only Buy & Bank Jars"
                restockCheckBox.setVisible(false); // Hide checkbox
                restockAmountSpinner.setDisable(false); // Enable spinner directly
            }
            updateRestockVisibility();
        });

        // Moth type selection listener
        mothTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateOptionsVisibility();
            updateRestockVisibility();
        });

        // Combined moths checkbox listener
        combinedMothsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isCombinedSelected = newVal;
            if (newVal) {
                mothTypeComboBox.setDisable(true);
            } else {
                mothTypeComboBox.setDisable(false);
            }
            updateRestockVisibility();
        });

        // Restock checkbox listener
        restockCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isRestocking = newVal;
            restockAmountSpinner.setDisable(!newVal); // Enable spinner only if restocking is checked
            if (newVal) {
                restockAmount = restockAmountSpinner.getValue(); // Update restock amount when enabled
            }
        });

        // Restock amount spinner listener
        restockAmountSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isRestocking || "Only Buy & Bank Jars".equals(selectedMethod)) {
                restockAmount = newVal; // Update restock amount for both cases
                savePreferences();
            }
        });

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setPrefWidth(120);
        confirmButton.setStyle("-fx-font-size: 14; -fx-background-color: #4c5459; -fx-text-fill: white; -fx-background-radius: 5;");
        confirmButton.setOnAction(e -> {
            selectedMethod = methodComboBox.getValue() != null ? methodComboBox.getValue() : "Catch Moths";

            if (isCombinedSelected) {
                catchBothWarlockAndHarvest = true;
                selectedMothItemId = ItemID.BLACK_WARLOCK; // Default to one of them
            } else {
                catchBothWarlockAndHarvest = false;
                selectedMothItemId = mothTypeComboBox.getValue() != null ? mothTypeComboBox.getValue() : ItemID.MOONLIGHT_MOTH;
            }

            // Set restocking based on method
            if ("Only Buy & Bank Jars".equals(selectedMethod)) {
                isRestocking = true; // Mandatory for this method
                restockAmount = restockAmountSpinner.getValue(); // Use spinner value
            } else {
                isRestocking = restockCheckBox.isSelected() && isRestockingAllowed();
                if (isRestocking) {
                    restockAmount = restockAmountSpinner.getValue();
                }
            }

            // Save preferences
            savePreferences();

            // Close the window
            ((Stage) getScene().getWindow()).close();
        });

        // Reset button
        Button resetButton = new Button("Reset");
        resetButton.setPrefWidth(120);
        resetButton.setStyle("-fx-font-size: 14; -fx-background-color: #4c5459; -fx-text-fill: white; -fx-background-radius: 5;");
        resetButton.setOnAction(e -> {
            methodComboBox.getSelectionModel().clearSelection();
            mothTypeComboBox.getSelectionModel().clearSelection();
            mothTypeComboBox.setDisable(false);
            combinedMothsCheckBox.setSelected(false);
            combinedMothsCheckBox.setVisible(false);
            restockCheckBox.setSelected(false);
            restockCheckBox.setVisible(false);
            restockAmountSpinner.setDisable(true);
            restockAmountSpinner.getValueFactory().setValue(50); // Reset to default
            selectedMethod = "Catch Moths";
            selectedMothItemId = ItemID.MOONLIGHT_MOTH;
            isRestocking = false;
            restockAmount = 50;
            catchBothWarlockAndHarvest = false;
            isCombinedSelected = false;
            savePreferences();
        });

        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10));
        buttonContainer.getChildren().addAll(confirmButton, resetButton);

        getChildren().addAll(titleLabel, methodLabel, methodComboBox, mothTypeLabel,
                mothTypeComboBox, combinedMothsCheckBox, restockContainer, buttonContainer);

        // Load saved preferences
        loadPreferences();
    }

    private void updateOptionsVisibility() {
        String method = methodComboBox.getValue();
        Integer mothType = mothTypeComboBox.getValue();

        if ("Catch Moths".equals(method)) {
            // Show combined option only when Black Warlock or Ruby Harvest is selected
            boolean showCombined = (mothType != null) &&
                    (mothType == ItemID.BLACK_WARLOCK || mothType == ItemID.RUBY_HARVEST);
            combinedMothsCheckBox.setVisible(showCombined);

            // If combined option is hidden, uncheck it
            if (!showCombined) {
                combinedMothsCheckBox.setSelected(false);
                isCombinedSelected = false;
                mothTypeComboBox.setDisable(false);
            }
        } else {
            combinedMothsCheckBox.setVisible(false);
        }

        updateRestockVisibility();
    }

    private void updateRestockVisibility() {
        String method = methodComboBox.getValue();
        boolean showRestock = "Catch Moths".equals(method) && isRestockingAllowed() || "Only Buy & Bank Jars".equals(method);
        restockContainer.setVisible(showRestock);

        if ("Catch Moths".equals(method)) {
            restockCheckBox.setVisible(isRestockingAllowed());
            restockAmountSpinner.setDisable(!restockCheckBox.isSelected() || !isRestockingAllowed());
        } else {
            restockCheckBox.setVisible(false); // Hide checkbox for "Only Buy & Bank Jars"
            restockAmountSpinner.setDisable(false); // Enable spinner directly
        }
    }

    private boolean isRestockingAllowed() {
        if (isCombinedSelected) {
            return false; // No restocking for combined option
        }

        Integer mothType = mothTypeComboBox.getValue();
        return mothType != null &&
                (mothType == ItemID.MOONLIGHT_MOTH || mothType == ItemID.SUNLIGHT_MOTH);
    }

    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(UI.class);

        // Load method
        String savedMethod = prefs.get(PREF_METHOD, "Catch Moths");
        methodComboBox.getSelectionModel().select(savedMethod);
        selectedMethod = savedMethod;

        if ("Catch Moths".equals(savedMethod)) {
            mothTypeComboBox.setVisible(true);
        } else {
            mothTypeComboBox.setVisible(false);
            combinedMothsCheckBox.setVisible(false);
            restockContainer.setVisible(true); // Show restock amount for "Only Buy & Bank Jars"
        }

        // Load moth type
        int savedMothType = prefs.getInt(PREF_MOTH_TYPE, ItemID.MOONLIGHT_MOTH);
        mothTypeComboBox.getSelectionModel().select((Integer) savedMothType);
        selectedMothItemId = savedMothType;

        // Load combined preference
        catchBothWarlockAndHarvest = prefs.getBoolean(PREF_COMBINED, false);
        isCombinedSelected = catchBothWarlockAndHarvest;
        combinedMothsCheckBox.setSelected(isCombinedSelected);

        if (isCombinedSelected) {
            mothTypeComboBox.setDisable(true);
        }

        // Load restock preference
        isRestocking = prefs.getBoolean(PREF_RESTOCK, false);
        restockCheckBox.setSelected(isRestocking);

        // Load restock amount
        restockAmount = prefs.getInt(PREF_RESTOCK_AMOUNT, 50);
        restockAmountSpinner.getValueFactory().setValue(restockAmount);

        // Update visibility after loading
        updateOptionsVisibility();
    }

    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(UI.class);
        prefs.put(PREF_METHOD, selectedMethod);
        prefs.putInt(PREF_MOTH_TYPE, selectedMothItemId);
        prefs.putBoolean(PREF_RESTOCK, isRestocking); // Only saved for "Catch Moths"
        prefs.putInt(PREF_RESTOCK_AMOUNT, restockAmount);
        prefs.putBoolean(PREF_COMBINED, catchBothWarlockAndHarvest);
    }

    // Public getter methods for your main script
    public String getSelectedMethod() {
        return selectedMethod;
    }

    public int getSelectedMothItemId() {
        return selectedMothItemId;
    }

    public boolean isRestocking() {
        return isRestocking;
    }

    public int getRestockAmount() {
        return restockAmount;
    }

    public boolean isCatchingBothWarlockAndHarvest() {
        return catchBothWarlockAndHarvest;
    }

    public boolean isCatchingBlackWarlock() {
        return selectedMothItemId == ItemID.BLACK_WARLOCK || catchBothWarlockAndHarvest;
    }

    public boolean isCatchingRubyHarvest() {
        return selectedMothItemId == ItemID.RUBY_HARVEST || catchBothWarlockAndHarvest;
    }

    public boolean isCatchingMoonlightMoth() {
        return selectedMothItemId == ItemID.MOONLIGHT_MOTH;
    }

    public boolean isCatchingSunlightMoth() {
        return selectedMothItemId == ItemID.SUNLIGHT_MOTH;
    }

    // Get array of ItemIDs to catch
    public int[] getMothItemIdsToCatch() {
        if (catchBothWarlockAndHarvest) {
            return new int[]{ItemID.BLACK_WARLOCK, ItemID.RUBY_HARVEST};
        } else {
            return new int[]{selectedMothItemId};
        }
    }
}