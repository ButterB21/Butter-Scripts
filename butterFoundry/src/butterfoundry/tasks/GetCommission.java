package butterfoundry.tasks;

import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.utils.UIResult;
import com.osmb.api.walker.WalkConfig;

import java.util.List;

import static butterfoundry.ButterFoundry.commissionObtained;


public class GetCommission extends Task {

    public GetCommission(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        script.log(GetCommission.class,"Commision has been otained: " + commissionObtained);
        return !commissionObtained;
    }

    @Override
    public void execute() {
        // Logic to execute the commission task
        script.log(GetCommission.class, "Executing GetComission task...");
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(GetCommission.class, "Position is null!");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return;
        }

        // Get the interaction area for Kovac
        Polygon kovacPoly = getKovacInteractArea();

        if (kovacPoly == null || script.getWidgetManager().insideGameScreenFactor(kovacPoly, List.of(ChatboxComponent.class)) < 0.4) { // this would mean 60% of the polygon on the game screen
            script.log(GetCommission.class, "Kovac interaction area not found!");
            walkToKovac();
            return;
        }

        Polygon resizeKovacPoly = kovacPoly.getResized(0.5);
        script.getFinger().tap(resizeKovacPoly, "commission kovac");

        if (script.submitHumanTask(() -> isKovacDialogueVisible(), script.random(6000, 9000))) {
            script.log(GetCommission.class, "Kovac dialogue is visible");
            UIResult<String> swordType = script.getWidgetManager().getDialogue().getText();

            if (swordType != null) {
                String swordName = swordType.get().toLowerCase();
                script.log(GetCommission.class, "Selected sword type: " + swordName);
                commissionObtained = true;
            } else {
                script.log(GetCommission.class, "Could not read sword type from dialogue");
                script.stop();
                return;
            }
        } else {
            script.log(GetCommission.class, "Kovac dialogue not visible after timeout, retrying...");
            walkToKovac(); // Retry walking to Kovac if dialogue is not visible
            return;
        }
    }

    private Polygon getKovacInteractArea() {
        // Static world position for Kovac's SW tile
        WorldPosition kovacWorldPos = new WorldPosition(3369, 11486, 0);

        // Convert to current scene's local coordinates
        LocalPosition kovacLocalPos = kovacWorldPos.toLocalPosition(script);

        if (kovacLocalPos == null) {
            return null; // Position not in current scene
        }

        return script.getSceneProjector().getTileCube(
                kovacLocalPos.getX(),
                kovacLocalPos.getY(),
                kovacLocalPos.getPlane(),
                165,  // baseHeight - lifts cube above ground
                105,  // cubeHeight - extends upward to cover NPC
                2, 2,
                false
        );
    }

    private void walkToKovac() {
        script.log(GetCommission.class, "Walking to Kovac...");
        WorldPosition kovacWorldPos = new WorldPosition(3369, 11486, 0);
        script.getWalker().walkTo(kovacWorldPos, new WalkConfig.Builder().tileRandomisationRadius(2).breakDistance(2).build());
    }

    private boolean isKovacDialogueVisible() {
        DialogueType kovacDialogue = script.getWidgetManager().getDialogue().getDialogueType();

        if (kovacDialogue != DialogueType.CHAT_DIALOGUE) {
            script.log(GetCommission.class, "Dialogue type is not visible or valid!");
            return false;
        }

        UIResult<String> swordType = script.getWidgetManager().getDialogue().getText();
        return swordType != null && swordType.get().toLowerCase().contains("go make me");
    }
}
