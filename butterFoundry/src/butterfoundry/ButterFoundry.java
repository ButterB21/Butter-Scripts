package butterfoundry;

import butterfoundry.tasks.GetCommission;
import butterfoundry.tasks.SetMould;
import butterfoundry.tasks.Task;
import butterfoundry.ui.UI;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ScriptDefinition(name = "ButterFoundry",
                 description = "A script for managing butter production and distribution.",
                 version = 1.0,
                 author = "Butter Enthusiast",
                 skillCategory = SkillCategory.SMITHING)
public class ButterFoundry extends Script {

    private Integer selectedAlloyType1;
    private Integer selectedAlloyType2;
    private int totalBars;
    private List<Task> tasks = new ArrayList<>();
    public ButterFoundry(Object scriptCore) {
        super(scriptCore);
    }

    public static boolean commissionObtained = false;
    public static boolean mouldSet = false;

    @Override
    public void onStart() {
        //Create and display the UI
        UI ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Giants Foundry Setup", false);

        Map<Integer, Integer> alloy1Items = ui.getAlloy1ItemsAndQuantities();
        Map<Integer, Integer> alloy2Items = ui.getAlloy2ItemsAndQuantities();
        selectedAlloyType1 = ui.getSelectedAlloyType1();
        selectedAlloyType2 = ui.getSelectedAlloyType2();
        totalBars = ui.getTotalBarsPublic();

        log(GetCommission.class, "Alloy 1 items: " + alloy1Items);
        log(GetCommission.class, "Alloy 2 items: " + alloy2Items);

        tasks.add(new GetCommission(this));
        tasks.add(new SetMould(this));
    }

    @Override 
    public int[] regionsToPrioritise() {
            return new int[] {13491};
        }

        @Override
        public int poll () {
            for (Task task : tasks) {
                if (task.activate()) {
                    task.execute();
                    return 0;
                }
            }
            return 0;
        }
}
