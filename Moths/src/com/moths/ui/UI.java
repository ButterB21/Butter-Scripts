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
    private static final double LABEL_COL_WIDTH = 140;

    private final ScriptCore core;
    private ComboBox<String> methodComboBox;
    private ComboBox<Integer> mothTypeComboBox;
    private CheckBox combinedMothsCheckBox;
    private CheckBox restockCheckBox;
    private Spinner<Integer> restockAmountSpinner;
    private HBox restockContainer; // Container for restock controls

    // Row containers for consistent alignment
    private HBox methodRow;
    private HBox mothTypeRow;

    // Minimal addition: Location selection (only for Ruby Harvest catching)
    private ComboBox<String> locationComboBox;
    private HBox locationContainer;

    // Buy & Bank location (only for "Only Buy & Bank Jars")
    private ComboBox<String> buyBankLocationCombo;
    private HBox buyBankLocationRow;

    // Labels we reuse in rows
    private Label mothTypeLabel;

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

    // Buy & Bank selectable locations (labels only)
    private static final String BUYBANK_LOC_HUNTERS_GUILD = "Hunter's Guild";
    private static final String BUYBANK_LOC_NEW = "Nardah"; // your other shop label
    public String selectedBuyBankLocation = BUYBANK_LOC_HUNTERS_GUILD;

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
    private static final String PREF_LOCATION = "selectedLocation"; // Ruby catch location
    private static final String PREF_BUYBANK_LOCATION = "buyBankLocation"; // Buy&Bank location

    // Labels for methods to avoid typos
    private static final String MODE_CATCH_MOTHS = "Catch & Bank";
    private static final String MODE_ONLY_BUY_AND_BANK = "Only Buy & Bank Jars";
    private static final String MODE_ONLY_CATCH = "Only Catch (No bank)";

    // Selected location (persisted) for Ruby Harvest catching
    public String selectedLocation = LOCATION_FARMING_GUILD;

    public UI(ScriptCore core) {
        this.core = core;
        setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-spacing: 15; -fx-alignment: center");
        setPrefWidth(400);
        setMaxHeight(400); // keep original

        Label titleLabel = new Label("Moth Catcher Setup");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");

        // Method row (label + combo inline)
        Label methodLabel = new Label("Select Method:");
        methodLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        methodLabel.setMinWidth(LABEL_COL_WIDTH);
        methodLabel.setPrefWidth(LABEL_COL_WIDTH);
        methodLabel.setAlignment(Pos.CENTER_RIGHT);

        methodComboBox = new ComboBox<>();
        methodComboBox.getItems().addAll(MODE_CATCH_MOTHS, MODE_ONLY_BUY_AND_BANK, MODE_ONLY_CATCH);
        methodComboBox.setPrefWidth(250);
        methodComboBox.setPromptText("Select Method");

        methodRow = new HBox(10);
        methodRow.setAlignment(Pos.CENTER_LEFT);
        methodRow.getChildren().addAll(methodLabel, methodComboBox);
        methodRow.managedProperty().bind(methodRow.visibleProperty());

        // Moth type row (label + combo inline)
        mothTypeLabel = new Label("Select Moth Type:");
        mothTypeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        mothTypeLabel.setMinWidth(LABEL_COL_WIDTH);
        mothTypeLabel.setPrefWidth(LABEL_COL_WIDTH);
        mothTypeLabel.setAlignment(Pos.CENTER_RIGHT);

        mothTypeComboBox = JavaFXUtils.createItemCombobox(core, MOTH_TYPE_IDS);
        mothTypeComboBox.setPrefWidth(250);
        mothTypeComboBox.setPromptText("Select Moth Type");

        mothTypeRow = new HBox(10);
        mothTypeRow.setAlignment(Pos.CENTER_LEFT);
        mothTypeRow.getChildren().addAll(mothTypeLabel, mothTypeComboBox);
        mothTypeRow.managedProperty().bind(mothTypeRow.visibleProperty());

        // Catching location row (only for Ruby Harvest)
        Label locationLabel = new Label("Location:");
        locationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        locationLabel.setMinWidth(LABEL_COL_WIDTH);
        locationLabel.setPrefWidth(LABEL_COL_WIDTH);
        locationLabel.setAlignment(Pos.CENTER_RIGHT);

        locationComboBox = new ComboBox<>();
        locationComboBox.getItems().addAll(LOCATION_FARMING_GUILD, LOCATION_LANDS_END);
        locationComboBox.setPrefWidth(250);
        locationComboBox.setPromptText("Select Location");
        locationComboBox.getSelectionModel().select(LOCATION_FARMING_GUILD);

        locationContainer = new HBox(10);
        locationContainer.setAlignment(Pos.CENTER_LEFT);
        locationContainer.getChildren().addAll(locationLabel, locationComboBox);
        locationContainer.setVisible(false);
        locationContainer.managedProperty().bind(locationContainer.visibleProperty());

        // Buy & Bank location row (only in that mode)
        Label buyBankLocationLabel = new Label("Buy & Bank Location:");
        buyBankLocationLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        buyBankLocationLabel.setMinWidth(LABEL_COL_WIDTH);
        buyBankLocationLabel.setPrefWidth(LABEL_COL_WIDTH);
        buyBankLocationLabel.setAlignment(Pos.CENTER_RIGHT);

        buyBankLocationCombo = new ComboBox<>();
        buyBankLocationCombo.getItems().addAll(BUYBANK_LOC_HUNTERS_GUILD, BUYBANK_LOC_NEW);
        buyBankLocationCombo.setPrefWidth(250);
        buyBankLocationCombo.setPromptText("Select Location");
        buyBankLocationCombo.getSelectionModel().select(BUYBANK_LOC_HUNTERS_GUILD);

        buyBankLocationRow = new HBox(10);
        buyBankLocationRow.setAlignment(Pos.CENTER_LEFT);
        buyBankLocationRow.getChildren().addAll(buyBankLocationLabel, buyBankLocationCombo);
        buyBankLocationRow.setVisible(false);
        buyBankLocationRow.managedProperty().bind(buyBankLocationRow.visibleProperty());

        // Combined moths checkbox (initially hidden)
        combinedMothsCheckBox = new CheckBox("Catch Both Black Warlock & Ruby Harvest");
        combinedMothsCheckBox.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
        combinedMothsCheckBox.setVisible(false);
        combinedMothsCheckBox.managedProperty().bind(combinedMothsCheckBox.visibleProperty());

        // Restock container
        restockContainer = new HBox(10);
        restockContainer.setAlignment(Pos.CENTER_LEFT);

        restockCheckBox = new CheckBox("Enable Jar Restocking");
        restockCheckBox.setStyle("-fx-text-fill: white; -fx-font-size: 12;");

        restockAmountSpinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(50, 10000, 50, 50);
        restockAmountSpinner.setValueFactory(valueFactory);
        restockAmountSpinner.setPrefWidth(80);
        restockAmountSpinner.setEditable(true);
        restockAmountSpinner.setDisable(true);

        restockContainer.getChildren().addAll(restockCheckBox, restockAmountSpinner);
        restockContainer.managedProperty().bind(restockContainer.visibleProperty());

        // Method selection listener
        methodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (MODE_CATCH_MOTHS.equals(newVal)) {
                mothTypeRow.setVisible(true);
                mothTypeComboBox.setDisable(false);
                buyBankLocationRow.setVisible(false);
                updateOptionsVisibility();
            } else if (MODE_ONLY_CATCH.equals(newVal)) {
                mothTypeRow.setVisible(true);
                mothTypeComboBox.setDisable(false);
                combinedMothsCheckBox.setVisible(false);
                restockContainer.setVisible(false);
                restockCheckBox.setVisible(false);
                restockAmountSpinner.setDisable(true);
                buyBankLocationRow.setVisible(false);
                updateOptionsVisibility();
            } else { // MODE_ONLY_BUY_AND_BANK
                // Hide moth type row entirely in Buy&Bank mode (removes the gap)
                mothTypeRow.setVisible(false);

                // Clear combined selection state to avoid stale flags influencing logic
                isCombinedSelected = false;
                combinedMothsCheckBox.setSelected(false);
                combinedMothsCheckBox.setVisible(false);
                catchBothWarlockAndHarvest = false;

                locationContainer.setVisible(false); // hide catching location

                // Show Buy&Bank row
                buyBankLocationRow.setVisible(true);

                restockContainer.setVisible(true);
                restockCheckBox.setVisible(false);
                restockAmountSpinner.setDisable(false);
            }
            applyBankPolicyForCurrentMethod();
            updateRestockVisibility();
        });

        // Moth type selection listener
        mothTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateOptionsVisibility();
            updateRestockVisibility();
        });

        // Catching location selection listener
        locationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            selectedLocation = newVal != null ? newVal : LOCATION_FARMING_GUILD;
        });

        // Buy&Bank location selection listener
        buyBankLocationCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            selectedBuyBankLocation = newVal != null ? newVal : BUYBANK_LOC_HUNTERS_GUILD;
        });

        // Combined checkbox
        combinedMothsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isCombinedSelected = newVal;
            mothTypeComboBox.setDisable(newVal);
            updateRestockVisibility();
        });

        // Restock checkbox
        restockCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isRestocking = newVal;
            restockAmountSpinner.setDisable(!newVal);
            if (newVal) {
                restockAmount = restockAmountSpinner.getValue();
            }
        });

        // Restock spinner
        restockAmountSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isRestocking || MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                restockAmount = newVal;
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
                Preferences prefs = Preferences.userNodeForPackage(UI.class);
                prefs.put(PREF_BANK_LOCATION, preferredBank);
                prefs.remove("lastBank");
                prefs.remove("selectedBank");
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

            if (!isCombinedSelected || MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                catchBothWarlockAndHarvest = false;
            }

            selectedLocation = locationComboBox.getValue() != null ? locationComboBox.getValue() : LOCATION_FARMING_GUILD;

            if (MODE_ONLY_BUY_AND_BANK.equals(selectedMethod)) {
                isRestocking = true;
                restockAmount = restockAmountSpinner.getValue();
            } else if (MODE_ONLY_CATCH.equals(selectedMethod)) {
                isRestocking = false;
            } else {
                isRestocking = restockCheckBox.isSelected() && isRestockingAllowed();
                if (isRestocking) {
                    restockAmount = restockAmountSpinner.getValue();
                }
            }

            savePreferences();
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
            restockAmountSpinner.getValueFactory().setValue(50);

            selectedMethod = MODE_CATCH_MOTHS;
            selectedMothItemId = ItemID.MOONLIGHT_MOTH;
            isRestocking = false;
            restockAmount = 50;
            catchBothWarlockAndHarvest = false;
            isCombinedSelected = false;
            preferredBank = BANK_NEAREST;

            selectedLocation = LOCATION_FARMING_GUILD;
            locationComboBox.getSelectionModel().select(LOCATION_FARMING_GUILD);

            selectedBuyBankLocation = BUYBANK_LOC_HUNTERS_GUILD;
            buyBankLocationCombo.getSelectionModel().select(BUYBANK_LOC_HUNTERS_GUILD);

            savePreferences();
        });

        HBox buttonContainer = new HBox(20);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10));
        buttonContainer.getChildren().addAll(confirmButton, resetButton);

        getChildren().addAll(
                titleLabel,
                methodRow,
                mothTypeRow,
                locationContainer,
                buyBankLocationRow,
                combinedMothsCheckBox,
                restockContainer,
                buttonContainer
        );

        // Load saved preferences
        loadPreferences();

        // Enforce bank policy once after loading
        applyBankPolicyForCurrentMethod();
    }

    private void updateOptionsVisibility() {
        String method = methodComboBox.getValue();
        Integer mothType = mothTypeComboBox.getValue();

        // Show/hide catching location picker: only when catching and Ruby Harvest is selected
        boolean showLocation = (MODE_CATCH_MOTHS.equals(method) || MODE_ONLY_CATCH.equals(method))
                && mothType != null && mothType == ItemID.RUBY_HARVEST;
        locationContainer.setVisible(showLocation);

        // Show/hide Buy&Bank location row (only in that mode)
        buyBankLocationRow.setVisible(MODE_ONLY_BUY_AND_BANK.equals(method));

        if (MODE_CATCH_MOTHS.equals(method) || MODE_ONLY_CATCH.equals(method)) {
            // Show combined option only when Black Warlock is selected
            boolean showCombined = (mothType != null) && (mothType == ItemID.BLACK_WARLOCK);
            combinedMothsCheckBox.setVisible(showCombined);

            if (!showCombined) {
                combinedMothsCheckBox.setSelected(false);
                isCombinedSelected = false;
                mothTypeComboBox.setDisable(false);
            }

            mothTypeRow.setVisible(true);
        } else {
            mothTypeRow.setVisible(false);
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
            restockCheckBox.setVisible(false);
            restockAmountSpinner.setDisable(false);
        } else {
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

        String savedMethod = prefs.get(PREF_METHOD, MODE_CATCH_MOTHS);
        methodComboBox.getSelectionModel().select(savedMethod);
        selectedMethod = savedMethod;

        if (MODE_CATCH_MOTHS.equals(savedMethod) || MODE_ONLY_CATCH.equals(savedMethod)) {
            mothTypeRow.setVisible(true);
        } else {
            mothTypeRow.setVisible(false);
            combinedMothsCheckBox.setVisible(false);
            locationContainer.setVisible(false);
            restockContainer.setVisible(true);
            buyBankLocationRow.setVisible(true);
        }

        int savedMothType = prefs.getInt(PREF_MOTH_TYPE, ItemID.MOONLIGHT_MOTH);
        mothTypeComboBox.getSelectionModel().select((Integer) savedMothType);
        selectedMothItemId = savedMothType;

        selectedLocation = prefs.get(PREF_LOCATION, LOCATION_FARMING_GUILD);
        locationComboBox.getSelectionModel().select(selectedLocation);

        selectedBuyBankLocation = prefs.get(PREF_BUYBANK_LOCATION, BUYBANK_LOC_HUNTERS_GUILD);
        buyBankLocationCombo.getSelectionModel().select(selectedBuyBankLocation);

        catchBothWarlockAndHarvest = prefs.getBoolean(PREF_COMBINED, false);
        isCombinedSelected = catchBothWarlockAndHarvest;
        combinedMothsCheckBox.setSelected(isCombinedSelected);
        mothTypeComboBox.setDisable(isCombinedSelected);

        isRestocking = prefs.getBoolean(PREF_RESTOCK, false);
        restockCheckBox.setSelected(isRestocking);

        restockAmount = prefs.getInt(PREF_RESTOCK_AMOUNT, 50);
        restockAmountSpinner.getValueFactory().setValue(restockAmount);

        preferredBank = prefs.get(PREF_BANK_LOCATION, BANK_NEAREST);
        if (MODE_ONLY_BUY_AND_BANK.equals(savedMethod)) {
            preferredBank = BANK_HUNTERS_GUILD;
            prefs.put(PREF_BANK_LOCATION, preferredBank);
            prefs.remove("lastBank");
            prefs.remove("selectedBank");
        }

        updateOptionsVisibility();
    }

    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(UI.class);
        prefs.put(PREF_METHOD, selectedMethod);
        prefs.putInt(PREF_MOTH_TYPE, selectedMothItemId);
        prefs.putBoolean(PREF_RESTOCK, isRestocking);
        prefs.putInt(PREF_RESTOCK_AMOUNT, restockAmount);
        prefs.putBoolean(PREF_COMBINED, catchBothWarlockAndHarvest);
        prefs.put(PREF_LOCATION, selectedLocation);
        prefs.put(PREF_BUYBANK_LOCATION, selectedBuyBankLocation);
        prefs.put(PREF_BANK_LOCATION, getPreferredBank());
    }

    private void applyBankPolicyForCurrentMethod() {
        Preferences prefs = Preferences.userNodeForPackage(UI.class);
        String method = methodComboBox.getValue();
        if (MODE_ONLY_BUY_AND_BANK.equals(method)) {
            preferredBank = BANK_HUNTERS_GUILD;
            prefs.put(PREF_BANK_LOCATION, preferredBank);
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

    // Minimal helpers for Ruby Harvest location (catching)
    public boolean isRubyHarvestAtLandsEnd() {
        return selectedMothItemId == ItemID.RUBY_HARVEST && LOCATION_LANDS_END.equals(selectedLocation);
    }
    public boolean isRubyHarvestAtFarmingGuild() {
        return selectedMothItemId == ItemID.RUBY_HARVEST && LOCATION_FARMING_GUILD.equals(selectedLocation);
    }
    public String getSelectedLocation() {
        return selectedLocation;
    }

    public String getBuyBankLocation() {
        return selectedBuyBankLocation;
    }
}