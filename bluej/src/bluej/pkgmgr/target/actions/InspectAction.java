package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action to inspect the static members of a class
 */
@OnThread(Tag.FXPlatform)
public class InspectAction extends ClassTargetOperation
{
    private final Node animateFromCentreOverride;

    /**
     * Create an action to inspect a class (i.e. static members, not inspecting an instance)
     * 
     * @param animateFromCentreOverride If non-null, animate from centre of this node.  If null, use ClassTarget's GUI node
     */
    public InspectAction(Node animateFromCentreOverride)
    {
        super("inspectClass", Combine.ONE, null, ClassTarget.inspectStr, MenuItemOrder.INSPECT, EditableTarget.MENU_STYLE_INBUILT);
        this.animateFromCentreOverride = animateFromCentreOverride;
    }

    @Override
    protected void execute(ClassTarget target)
    {
        if (target.checkDebuggerState())
        {
            Window parent = target.getPackage().getUI().getStage();
            Node animateFromCentre = animateFromCentreOverride != null ? animateFromCentreOverride : target.getNode();

            target.inspect(parent, animateFromCentre);
        }
    }
}
