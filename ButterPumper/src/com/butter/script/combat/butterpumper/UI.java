package com.butter.script.combat.butterpumper;

import com.osmb.api.ScriptCore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class UI extends VBox {

    private final RadioButton world302Radio;
    private final RadioButton world303Radio;
    private final ToggleGroup worldGroup;
    private final CheckBox hopBetweenWorldsCheckBox;
    private final Spinner<Integer> minAfkSpinner;
    private final Spinner<Integer> maxAfkSpinner;

    private static final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_PRIMARY_WORLD = "butter_pumper_primary_world";
    private static final String PREF_HOP_BETWEEN   = "butter_pumper_hop_between";

    public UI(ScriptCore core) {
        setStyle("-fx-spacing: 10; -fx-alignment: center; -fx-padding: 5; -fx-background-color: #636E72;");
        setAlignment(Pos.CENTER);

        Label worldLabel = new Label("Select your primary pumping world");
        getChildren().add(worldLabel);

        world302Radio = new RadioButton("World 302");
        world303Radio = new RadioButton("World 303 (recommended)");

        worldGroup = new ToggleGroup();
        world302Radio.setToggleGroup(worldGroup);
        world303Radio.setToggleGroup(worldGroup);

        // Default: 303
        world302Radio.setSelected(false);
        world303Radio.setSelected(true);

        getChildren().add(world302Radio);
        getChildren().add(world303Radio);

        // Hop checkbox
        hopBetweenWorldsCheckBox = new CheckBox("Occasionally hop between 302 and 303");
        hopBetweenWorldsCheckBox.setStyle("-fx-text-fill: white;");
        getChildren().add(hopBetweenWorldsCheckBox);

        Label afkLabel = new Label("Random tab AFK delay (minutes)");
        afkLabel.setStyle("-fx-text-fill: white;" + "-fx-font-weight: bold;" + "-fx-padding: 10 0 0 0;");
        getChildren().add(afkLabel);

        minAfkSpinner = new Spinner<>(1, 60, 1);
        minAfkSpinner.setEditable(true);
        styleSpinnerTextWhite(minAfkSpinner);
        getChildren().add(minAfkSpinner);

        // Max AFK spinner: 1–60, default 15
        maxAfkSpinner = new Spinner<>(1, 60, 5);
        maxAfkSpinner.setEditable(true);
        styleSpinnerTextWhite(maxAfkSpinner);
        updateMaxAfkColor(maxAfkSpinner.getValue());

        maxAfkSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal < minAfkSpinner.getValue()) {
                maxAfkSpinner.getValueFactory().setValue(minAfkSpinner.getValue());
                updateMaxAfkColor(minAfkSpinner.getValue());
            } else {
                updateMaxAfkColor(newVal);
            }
        });

        // --- Min/Max in one horizontal line ---
        Label minLabel = new Label("Min:");
        minLabel.setStyle("-fx-text-fill: white;");
        Label maxLabel = new Label("Max:");
        maxLabel.setStyle("-fx-text-fill: white;");

        HBox afkRow = new HBox(10); // spacing between controls
        afkRow.setAlignment(Pos.CENTER);
        afkRow.setPadding(new Insets(0, 0, 0, 0));
        afkRow.getChildren().addAll(
                minLabel,
                minAfkSpinner,
                maxLabel,
                maxAfkSpinner
        );

        getChildren().add(afkRow);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        getChildren().add(confirmButton);

        confirmButton.setOnAction(event -> {
            int selectedWorld = getSelectedPrimaryWorldNumber();

            // Close window
            ((Stage) confirmButton.getScene().getWindow()).close();
        });
    }
    private void styleSpinnerTextWhite(Spinner<Integer> spinner) {
        spinner.getEditor().setStyle(
                "-fx-text-fill: white; -fx-background-color: #2d3436;"
        );
    }

    private void updateMaxAfkColor(int minutes) {
        String color;
        if (minutes >= 35) {
            color = "#ff4d4d";
        } else if (minutes >= 30) {
            color = "#ffd700";
        } else {
            color = "#ffffff";
        }
        maxAfkSpinner.getEditor().setStyle(
                "-fx-text-fill: " + color + "; -fx-background-color: #2d3436;"
        );
    }
    public int getSelectedPrimaryWorldNumber() {
        Toggle selected = worldGroup.getSelectedToggle();
        if (selected == world302Radio) {
            return 302;
        }
        return 303;
    }

    public boolean isHopBetweenWorldsEnabled() {
        return hopBetweenWorldsCheckBox.isSelected();
    }

    public int getMinAfkMinutes() {
        return minAfkSpinner.getValue();
    }

    public int getMaxAfkMinutes() {
        return maxAfkSpinner.getValue();
    }
}