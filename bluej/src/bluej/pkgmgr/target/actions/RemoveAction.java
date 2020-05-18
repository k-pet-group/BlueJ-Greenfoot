package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.EditableTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * Action to remove a target
 */
@OnThread(Tag.FXPlatform)
public class RemoveAction extends EditableTargetOperation
{
    public RemoveAction()
    {
        super("removeEditable", Combine.ANY, null, EditableTarget.removeStr, MenuItemOrder.EDIT, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void executeEditable(EditableTarget target)
    {
        target.remove();
    }
}
