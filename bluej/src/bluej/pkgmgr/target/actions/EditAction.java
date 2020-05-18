package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.EditableTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * Action to open the editor for a classtarget
 */
@OnThread(Tag.FXPlatform)
public class EditAction extends EditableTargetOperation
{
    public EditAction()
    {
        super("edit", Combine.ANY, null, EditableTarget.editStr, MenuItemOrder.EDIT, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void executeEditable(EditableTarget target)
    {
        target.open();
    }
}
