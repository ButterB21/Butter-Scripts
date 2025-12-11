package com.butter.script.hunter.buttermoonlightantelope.ui;

import com.butter.script.hunter.buttermoonlightantelope.data.Food;
import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.prefs.Preferences;

public class UI extends VBox {
    private final Preferences prefs = Preferences.userRoot().node("Butter-Scripts/MoonlightAntelope");
    private final String PREF_FOOD_ID = "food_id";
    private final String PREF_FOOD_EAT_PCT = "food_eat_pct";
    private final String PREF_CHISEL_ANTLERS = "chisel_antlers";

    private ComboBox<Integer> foodComboBox;
    private final CheckBox chiselAntlers = new CheckBox("Chisel Antlers (coming soon)");
    private Spinner<Integer> foodEatPctSpinner;

    public Scene buildScene(ScriptCore core) {
        VBox mainVBox = new VBox();
        mainVBox.setSpacing(20);
        mainVBox.setStyle("-fx-background-color: #636E72; -fx-padding: 10");

        Label foodLabel = new Label("Select Food");
        Label foodEatPctLabel = new Label("% to eat or bank at:");

        chiselAntlers.setDisable(true);

        int[] foodItemIDs = Arrays.stream(Food.values()).mapToInt(Food::getItemID).toArray();
        foodComboBox = JavaFXUtils.createItemCombobox(core, foodItemIDs);
        foodComboBox.setPrefWidth(200);

        // Load preferences
        chiselAntlers.setSelected(prefs.getBoolean(PREF_CHISEL_ANTLERS, false));
        foodComboBox.setValue(prefs.getInt(PREF_FOOD_ID, Food.JUG_OF_WINE.getItemID()));
        foodEatPctSpinner = new Spinner<>(1, 100, prefs.getInt(PREF_FOOD_EAT_PCT, 40));

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> {
            if (foodComboBox.getValue() != null && foodEatPctSpinner.getValue() != null) {
                savePrefSettings();
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });


        VBox foodEatPctBox = new VBox(foodEatPctLabel, foodEatPctSpinner);
        foodEatPctBox.setPrefWidth(100);
        VBox foodBox = new VBox(foodLabel, foodComboBox);

        mainVBox.getChildren().addAll(chiselAntlers, foodEatPctBox, foodBox, confirmButton);

        Scene scene = new Scene(mainVBox, 300, 280);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public int getSelectedFoodID() {
        return foodComboBox.getValue();
    }

    public int getFoodEatPct() {
        return foodEatPctSpinner.getValue();
    }

    public boolean chisselAntlers() {
        return chiselAntlers.isSelected();
    }

    private void savePrefSettings() {
        prefs.putInt(PREF_FOOD_ID, getSelectedFoodID());
        prefs.putInt(PREF_FOOD_EAT_PCT, getFoodEatPct());
        prefs.putBoolean(PREF_CHISEL_ANTLERS, chisselAntlers());
    }

}