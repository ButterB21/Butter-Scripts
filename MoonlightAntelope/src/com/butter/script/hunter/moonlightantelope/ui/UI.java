package com.butter.script.hunter.moonlightantelope.ui;

import com.butter.script.hunter.moonlightantelope.data.Food;
import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;

public class UI extends VBox {

    private ComboBox<Integer> foodComboBox;
    private final CheckBox chiselAntlers = new CheckBox("Chisel Antlers (coming soon)");
    private Spinner<Integer> foodEatPctSpinner;

    public Scene buildScene(ScriptCore core) {
        VBox vBox = new VBox();
        vBox.setSpacing(20);
        vBox.setStyle("-fx-background-color: #636E72; -fx-padding: 10");

        Label foodLabel = new Label("Select Food");
        Label foodEatPctLabel = new Label("Eat Food at % (slightly randomized):");

        chiselAntlers.setDisable(true);

        int[] foodItemIDs = Arrays.stream(Food.values()).mapToInt(Food::getItemID).toArray();
        foodComboBox = JavaFXUtils.createItemCombobox(core, foodItemIDs);
        foodComboBox.setPrefWidth(200);

        foodEatPctSpinner = new Spinner<>(1, 100, 40);

        Button button = new Button("Confirm");
        button.setOnAction(event -> {
            if (foodComboBox.getValue() != null) {
                ((Stage) button.getScene().getWindow()).close();
            }
        });

        vBox.getChildren().addAll(chiselAntlers, foodEatPctLabel, foodEatPctSpinner, foodLabel, foodComboBox, button);

        Scene scene = new Scene(vBox, 300, 350);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public int getSelectedFood() {
        return foodComboBox.getValue();
    }

    public int getFoodEatPct() {
        return foodEatPctSpinner.getValue();
    }

    public boolean chisselAntlers() {
        return chiselAntlers.isSelected();
    }

}