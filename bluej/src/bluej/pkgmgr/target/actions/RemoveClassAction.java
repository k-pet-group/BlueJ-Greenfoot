package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action to remove a classtarget from its package
 */
@OnThread(Tag.FXPlatform)
public class RemoveClassAction extends ClassTargetOperation
{
    public RemoveClassAction()
    {
        super("removeClass", Combine.ALL, null, EditableTarget.removeStr, MenuItemOrder.REMOVE, EditableTarget.MENU_STYLE_INBUILT);
    }

    @OnThread(Tag.FXPlatform)
    protected void execute(ClassTarget target)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(target.getPackage());
        if (pmf.askRemoveClass())
        {
            target.remove();
        }
    }
}
