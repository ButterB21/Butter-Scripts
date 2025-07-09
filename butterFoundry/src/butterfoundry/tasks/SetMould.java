package butterfoundry.tasks;

import butterfoundry.component.MouldInterface;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static butterfoundry.ButterFoundry.mouldSet;

public class SetMould extends Task{
public SetMould(Script script) {
        super(script);
    }

    private MouldInterface mouldInterface = new MouldInterface(script);
    private static final String[] SETUP_MOULD_NAMES = {
        "Mould jig (Empty)",
        "Mould jig (Setup)",
    };

    private static final String[] SETUP_MOULD_ACTIONS = {
//            "check mould jig (setup)",
//            "setup mould jig (empty)",
            "setup",
            "check"
    };

    // Predicate for filtering mould jig objects
    private final Predicate<RSObject> mouldJigQuery = obj -> {
        if (obj.getName() == null || obj.getActions() == null) return false;
        if (!Arrays.stream(SETUP_MOULD_NAMES).anyMatch(name -> name.equalsIgnoreCase(obj.getName()))) return false;
        if (!Arrays.stream(obj.getActions()).anyMatch(action ->
                Arrays.stream(SETUP_MOULD_ACTIONS).anyMatch(setupAction -> action.equalsIgnoreCase(action))
        )) return false;
        return obj.canReach();
    };

    MenuHook hook = menuEntries -> {
        for (MenuEntry menuEntry : menuEntries ) {
            if (!menuEntry.getEntityName().equalsIgnoreCase("Mould jig (Empty)") && !menuEntry.getEntityName().equalsIgnoreCase("Mould jig (Setup)")) {
                continue;
            }
            for (String action : SETUP_MOULD_ACTIONS) {
                if (menuEntry.getAction().equalsIgnoreCase(action)) {
                    return menuEntry;
                }
            }
        }
        return null;
    };

    @Override
    public boolean activate() {
        script.log(SetMould.class,"Mould has been set: " + mouldSet);
        return !mouldSet; // Placeholder, replace with actual condition
    }

    @Override
    public void execute() {
        script.log(SetMould.class, "Executing SetMould task...");

        List<RSObject> mouldJigs = script.getObjectManager().getObjects(mouldJigQuery);
        if (mouldJigs.isEmpty()) {
            script.log(SetMould.class, "Mould jig not found!");
            return;
        }

        RSObject mouldJigObj = (RSObject) script.getUtils().getClosest(mouldJigs);
        if (!mouldJigObj.interact(hook)) {
           script.log(SetMould.class, "Failed to interact with mould jig!");
           return;
        }

        script.log(SetMould.class, "Interacting with mould jig...");
        // wait for the dialogue to appear
        script.submitHumanTask(() -> script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.CHAT_DIALOGUE || mouldInterface.isVisible(), script.random(4000, 10000)); //check the first conditional

    }

}
