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

    // Minimal addition: Location selection (only for Ruby Harvest)
    private ComboBox<String> locationComboBox;
    private HBox locationContainer;

    private boolean isCombinedSelected = false;

    // Variables for task usage
    public String selectedMethod = "Catch & Bank";
    public int selectedMothItemId = ItemID.MOONLIGHT_MOTH;
    public boolean isRestocking = false; // Only used for "Catch Moths" with Moonlight/Sunlight
    public int restockAmount = 50; // Default and mandatory for "Only Buy & Bank Jars"
    public boolean catchBothWarlockAndHarvest = false;

    // Bank preference (UI-only enforcement when "Only Buy & Bank Jars" is selected)
    private static final String PREF_BANK_LOCATION = "preferredBank";
    private static final String BANK_HUNTERS_GUILD = "HuntersGuild";
    private static final String BANK_FARMING_GUILD = "FarmingGuild";
    private static final String BANK_NEAREST = "Nearest";
    public String preferredBank = BANK_NEAREST;

    // Locations (strings only, used for Ruby Harvest)
    private static final String LOCATION_FARMING_GUILD = "Farming Guild";
    private static final String LOCATION_LANDS_END = "Land's End";

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
    private static final String PREF_LOCATION = "selectedLocation"; // NEW

    // Labels for methods to avoid typos
    private static final String MODE_CATCH_MOTHS = "Catch & Bank";
    private static final String MODE_ONLY_BUY_AND_BANK = "Only Buy & Bank Jars";
    private static final String MODE_ONLY_CATCH = "Only Catch (No bank)";

    // Selected location (persisted)
    public String selectedLocation = LOCATION_FARMING_GUILD;

    public UI(ScriptCore core) {
        this.core = core;
        setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-spacing: 15; -fx-alignment: center");
        setPrefWidth(400);
        setMaxHeight(400); // keep original

        Label titleLabel = new Label("Moth Catcher Setup");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");

        // Method selection
        Label methodLabel = new Label("Select Method:");
        methodLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        methodComboBox = new ComboBox<>();
        methodComboBox.getItems().addAll(MODE_CATCH_MOTHS, MODE_ONLY_BUY_AND_BANK, MODE_ONLY_CATCH);
        methodComboBox.setPrefWidth(250);
        methodComboBox.setPromptText("Select Method");

        // Moth type selection using JavaFXUtils
        Label mothTypeLabel = new Label("Select Moth Type:");
        mothTypeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        mothTypeComboBox = JavaFXUtils.createItemCombobox(core, MOTH_TYPE_IDS);
        mothTypeComboBox.setPrefWidth(250);
        mothTypeComboBox.setPromptText("Select Moth Type");

        // Minimal addition: Location selection (only for Ruby Harvest)
        Label locationLabel = new Label("Location:");
        locationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        locationComboBox = new ComboBox<>();
        locationComboBox.getItems().addAll(LOCATION_FARMING_GUILD, LOCATION_LANDS_END);
        locationComboBox.setPrefWidth(250);
        locationComboBox.setPromptText("Select Location");
        locationComboBox.getSelectionModel().select(LOCATION_FARMING_GUILD);

        locationContainer = new HBox(10);
        locationContainer.setAlignment(Pos.CENTER_LEFT);
        locationContainer.getChildren().addAll(locationLabel, locationComboBox);
        locationContainer.setVisible(false); // hidden unless Ruby Harvest is selected

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
            if (MODE_CATCH_MOTHS.equals(newVal)) {
                mothTypeComboBox.setVisible(true);
                mothTypeComboBox.setDisable(false);
                updateOptionsVisibility();
            } else if (MODE_ONLY_CATCH.equals(newVal)) {
                // Show moth selection, no restocking controls
                mothTypeComboBox.setVisible(true);
                mothTypeComboBox.setDisable(false);
                combinedMothsCheckBox.setVisible(false); // will be toggled by updateOptionsVisibility()
                restockContainer.setVisible(false);
                restockCheckBox.setVisible(false);
                restockAmountSpinner.setDisable(true);
                updateOptionsVisibility();
            } else { // MODE_ONLY_BUY_AND_BANK
                // Clear combined selection state to avoid stale flags influencing logic
                isCombinedSelected = false;
                combinedMothsCheckBox.setSelected(false);
                catchBothWarlockAndHarvest = false;

                mothTypeComboBox.setVisible(false);
                combinedMothsCheckBox.setVisible(false);
                locationContainer.setVisible(false); // hide location in this mode
                restockContainer.setVisible(true); // Show restock amount spinner for "Only Buy & Bank Jars"
                restockCheckBox.setVisible(false); // Hide checkbox
                restockAmountSpinner.setDisable(false); // Enable spinner directly
            }
            // Enforce bank policy on method change
            applyBankPolicyForCurrentMethod();
            updateRestockVisibility();
        });

        // Moth type selection listener
        mothTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateOptionsVisibility();
            updateRestockVisibility();
        });

        // Location selection listener (store selection; persist on Confirm)
        locationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            selectedLocation = newVal != null ? newVal : LOCATION_FARMING_GUILD;
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
            if (isRestocking || MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                restockAmount = newVal; // Update restock amount for both cases
                savePreferences();
            }
        });

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setPrefWidth(120);
        confirmButton.setStyle("-fx-font-size: 14; -fx-background-color: #4c5459; -fx-text-fill: white; -fx-background-radius: 5;");
        confirmButton.setOnAction(e -> {
            selectedMethod = methodComboBox.getValue() != null ? methodComboBox.getValue() : MODE_CATCH_MOTHS;

            if (MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                // Force Hunters Guild bank in this mode
                preferredBank = BANK_HUNTERS_GUILD;
                core.log("UI: Forcing bank to Hunter's Guild for 'Only Buy & Bank Jars'.");
                // Best-effort clearing of any stale cached bank preference keys some runtimes use
                Preferences prefs = Preferences.userNodeForPackage(UI.class);
                prefs.put(PREF_BANK_LOCATION, preferredBank);
                prefs.remove("lastBank");
                prefs.remove("selectedBank");
                // In buy & bank mode, combined selection should not influence behavior
                catchBothWarlockAndHarvest = false;
            } else if (isCombinedSelected) {
                catchBothWarlockAndHarvest = true;
                selectedMothItemId = ItemID.BLACK_WARLOCK; // Default to one of them
            } else if (mothTypeComboBox.getValue() != null && mothTypeComboBox.getValue() == ItemID.SUNLIGHT_MOTH) {
                core.log("UI: Sunlight moth selected");
                selectedMothItemId = ItemID.SUNLIGHT_MOTH;
            } else {
                core.log("UI: Moonlight/Ruby/Warlock selection");
                selectedMothItemId = mothTypeComboBox.getValue() != null ? mothTypeComboBox.getValue() : ItemID.MOONLIGHT_MOTH;
            }

            // If not combined, ensure flag is off
            if (!isCombinedSelected || MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                catchBothWarlockAndHarvest = false;
            }

            // Persist selected location (used only when Ruby Harvest is selected)
            selectedLocation = locationComboBox.getValue() != null ? locationComboBox.getValue() : LOCATION_FARMING_GUILD;

            // Set restocking based on method
            if (MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                isRestocking = true; // Mandatory for this method
                restockAmount = restockAmountSpinner.getValue(); // Use spinner value
            } else if (MODE_ONLY_CATCH.equals(selectedMethod)) {
                isRestocking = false; // No restocking in Only Catch
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
            selectedMethod = MODE_CATCH_MOTHS;
            selectedMothItemId = ItemID.MOONLIGHT_MOTH;
            isRestocking = false;
            restockAmount = 50;
            catchBothWarlockAndHarvest = false;
            isCombinedSelected = false;
            preferredBank = BANK_NEAREST; // Reset bank preference on full reset
            selectedLocation = LOCATION_FARMING_GUILD;
            locationComboBox.getSelectionModel().select(LOCATION_FARMING_GUILD);
            savePreferences();
        });

        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10));
        buttonContainer.getChildren().addAll(confirmButton, resetButton);

        getChildren().addAll(titleLabel, methodLabel, methodComboBox,
                mothTypeLabel, mothTypeComboBox,
                locationContainer,              // minimal addition (row)
                combinedMothsCheckBox, restockContainer, buttonContainer);

        // Load saved preferences
        loadPreferences();

        // Enforce bank policy once after loading persisted state
        applyBankPolicyForCurrentMethod();
    }

    private void updateOptionsVisibility() {
        String method = methodComboBox.getValue();
        Integer mothType = mothTypeComboBox.getValue();

        // Show/hide location picker: only when catching and Ruby Harvest is selected
        boolean showLocation = (MODE_CATCH_MOTHS.equals(method) || MODE_ONLY_CATCH.equals(method))
                && mothType != null && mothType == ItemID.RUBY_HARVEST;
        locationContainer.setVisible(showLocation);

        if (MODE_CATCH_MOTHS.equals(method) || MODE_ONLY_CATCH.equals(method)) {
            // Show combined option only when Black Warlock or Ruby Harvest is selected at Hunter's Guild.
            // Since Ruby Harvest at Farming Guild or Land's End isn't co-located with Warlock, hide combined when Ruby selected.
            boolean showCombined = (mothType != null) &&
                    (mothType == ItemID.BLACK_WARLOCK);
            combinedMothsCheckBox.setVisible(showCombined);

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
        boolean showRestock = (MODE_CATCH_MOTHS.equals(method) && isRestockingAllowed())
                || MODE_ONLY_BUY_AND_BANK.equals(method);
        restockContainer.setVisible(showRestock);

        if (MODE_CATCH_MOTHS.equals(method)) {
            restockCheckBox.setVisible(isRestockingAllowed());
            restockAmountSpinner.setDisable(!restockCheckBox.isSelected() || !isRestockingAllowed());
        } else if (MODE_ONLY_BUY_AND_BANK.equals(method)) {
            restockCheckBox.setVisible(false); // Hide checkbox for "Only Buy & Bank Jars"
            restockAmountSpinner.setDisable(false); // Enable spinner directly
        } else {
            // Only Catch (No bank)
            restockCheckBox.setVisible(false);
            restockAmountSpinner.setDisable(true);
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
        String savedMethod = prefs.get(PREF_METHOD, MODE_CATCH_MOTHS);
        methodComboBox.getSelectionModel().select(savedMethod);
        selectedMethod = savedMethod;

        if (MODE_CATCH_MOTHS.equals(savedMethod) || MODE_ONLY_CATCH.equals(savedMethod)) {
            mothTypeComboBox.setVisible(true);
        } else {
            mothTypeComboBox.setVisible(false);
            combinedMothsCheckBox.setVisible(false);
            locationContainer.setVisible(false);
            restockContainer.setVisible(true); // Show restock amount for "Only Buy & Bank Jars"
        }

        // Load moth type
        int savedMothType = prefs.getInt(PREF_MOTH_TYPE, ItemID.MOONLIGHT_MOTH);
        mothTypeComboBox.getSelectionModel().select((Integer) savedMothType);
        selectedMothItemId = savedMothType;

        // Load location (default Farming Guild)
        selectedLocation = prefs.get(PREF_LOCATION, LOCATION_FARMING_GUILD);
        locationComboBox.getSelectionModel().select(selectedLocation);

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

        // Load preferred bank (default NEAREST), but override to Hunters Guild if current mode requires it
        preferredBank = prefs.get(PREF_BANK_LOCATION, BANK_NEAREST);
        if (MODE_ONLY_BUY_AND_BANK.equals(savedMethod)) {
            preferredBank = BANK_HUNTERS_GUILD;
            prefs.put(PREF_BANK_LOCATION, preferredBank);
            // Clear potential stale cache keys some runtimes use
            prefs.remove("lastBank");
            prefs.remove("selectedBank");
        }

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
        prefs.put(PREF_LOCATION, selectedLocation); // NEW
        // Persist preferred bank; force Hunters Guild if the current mode requires it
        prefs.put(PREF_BANK_LOCATION, getPreferredBank());
    }

    // Enforce bank consistency whenever the mode changes or after loading prefs
    private void applyBankPolicyForCurrentMethod() {
        Preferences prefs = Preferences.userNodeForPackage(UI.class);
        String method = methodComboBox.getValue();
        if (MODE_ONLY_BUY_AND_BANK.equals(method)) {
            preferredBank = BANK_HUNTERS_GUILD;
            prefs.put(PREF_BANK_LOCATION, preferredBank);
            // Clear potential stale cache keys some runtimes use
            prefs.remove("lastBank");
            prefs.remove("selectedBank");
            core.log("UI: Bank locked to Hunter's Guild for 'Only Buy & Bank Jars'.");
        }
    }

    // Public helpers for runtime to query
    public boolean forceHuntersGuildBank() {
        return MODE_ONLY_BUY_AND_BANK.equals(selectedMethod);
    }

    public String getPreferredBank() {
        return forceHuntersGuildBank() ? BANK_HUNTERS_GUILD : preferredBank;
    }

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

    public boolean isOnlyCatchMode() {
        return MODE_ONLY_CATCH.equals(selectedMethod);
    }

    // Minimal helpers for Ruby Harvest location
    public boolean isRubyHarvestAtLandsEnd() {
        return selectedMothItemId == ItemID.RUBY_HARVEST && LOCATION_LANDS_END.equals(selectedLocation);
    }
    public boolean isRubyHarvestAtFarmingGuild() {
        return selectedMothItemId == ItemID.RUBY_HARVEST && LOCATION_FARMING_GUILD.equals(selectedLocation);
    }
    public String getSelectedLocation() {
        return selectedLocation;
    }
}