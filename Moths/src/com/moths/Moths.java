package moths;

import com.moths.tasks.Task;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import moths.data.MothData;
import moths.tasks.CatchMoth;
import moths.tasks.HandleBank;

import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(
    name = "Moths",
    description = "A script for catching moths in the game.",
    version = 1.0,
    author = "Butter",
    skillCategory = SkillCategory.HUNTER)

public class Moths extends Script {
    public Moths(Object scriptCore) {
        super(scriptCore);
    }

    private final List<Task> tasks = new ArrayList<>();

    @Override
    public void onStart() {
        tasks.add(new CatchMoth(this));
        tasks.add(new HandleBank(this));
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
        // Define regions to prioritise
        return new int[]{MothData.MOONLIGHT_MOTH.getMothRegion(), MothData.MOONLIGHT_MOTH.getBankRegion()}; // Example region ID
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }
}
