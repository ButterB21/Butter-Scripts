package moths.tasks;

import com.osmb.api.script.Script;
import moths.ui.UI;

public abstract class Task {
    protected Script script;
    public UI ui;
    public Task(Script script) {
        this.script = script;
    }

    public abstract boolean activate();
    public abstract void execute();
}
