package moths;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import javafx.scene.Scene;
import moths.data.MothData;
import moths.tasks.BuyJars;
import moths.tasks.CatchMoth;
import moths.tasks.HandleBank;
import moths.tasks.Task;
import moths.ui.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
    name = "Moths",
    description = "A script for catching & banking moths.",
    version = 1.0,
    author = "Butter",
    skillCategory = SkillCategory.HUNTER)

public class Moths extends Script {
    public Moths(Object scriptCore) {
        super(scriptCore);
    }

    private UI ui;
    private final List<Task> tasks = new ArrayList<>();
    public static int mothsCaught = 0;
    public static int jarsBought = 0;
    public static boolean catchMothTask = true;
    public static boolean bankTask = true;
    public static boolean activateRestocking = false;

    @Override
    public void onStart() {
        ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Moth Catcher Setup", false);

        catchMothTask = ui.getSelectedMethod().equalsIgnoreCase("Catch & Bank");
        bankTask = true;
        activateRestocking = ui.getSelectedMethod().equalsIgnoreCase("Only Buy & Bank Jars");
        tasks.add(new CatchMoth(this, ui));
        tasks.add(new HandleBank(this, ui));
        tasks.add(new BuyJars(this, ui));
    }

    @Override
    public int poll() {
        for (Task task : tasks) {
            if (task.activate()) {
                task.execute();
                return 0;
            }
        }
        return 0;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{MothData.fromUI(ui).getMothRegion(), MothData.fromUI(ui).getBankRegion()};
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

}
