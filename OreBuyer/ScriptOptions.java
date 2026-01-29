import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;

import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ScriptOptions {

    protected static final int[] ORES = new int[] {
            ItemID.COPPER_ORE, ItemID.TIN_ORE, ItemID.IRON_ORE, ItemID.MITHRIL_ORE, ItemID.SILVER_ORE, ItemID.COAL, ItemID.GOLD_ORE
    };

    //Dropdown menu for the ores
    private ComboBox<Integer> oreComboBox;
    private ComboBox<Shop> shopComboBox = new ComboBox<>();
    private CheckBox coalBag = new CheckBox("Use Coal Bag");
    protected boolean useCoalBag = false;

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        Label shopLabel = new Label("Select Shop");
        root.getChildren().add(shopLabel);
        shopComboBox.getItems().addAll(Shop.values());
        shopComboBox.getSelectionModel().select(0);
        root.getChildren().add(shopComboBox);

        Label oreLabel = new Label("Select Ore to buy");
        oreComboBox = JavaFXUtils.createItemCombobox(core, ORES);
        root.getChildren().addAll(oreLabel, oreComboBox);

        coalBag.setStyle("-fx-text-fill: white");
        coalBag.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            useCoalBag = newVal;
        });
        VBox coalBagSelectionBox = new VBox(coalBag);
        root.getChildren().add(coalBagSelectionBox);

        Button confirmButton = new Button("Confirm");
        root.getChildren().add(confirmButton);

        Scene scene = new Scene(root);
        confirmButton.setOnAction(actionEvent -> {
            if (oreComboBox.getSelectionModel().getSelectedIndex() >= 0) {
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        scene.getStylesheets().add("style.css");
        return scene;
    }

    public int getSelectedOre() {
        return oreComboBox.getSelectionModel().getSelectedItem();
    }

    public Shop getSelectedShop () {
        return shopComboBox.getSelectionModel().getSelectedItem();
    }
}
