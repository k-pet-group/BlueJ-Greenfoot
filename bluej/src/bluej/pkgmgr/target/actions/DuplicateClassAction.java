package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A menu item to invoke a class duplication. This is valid for
 * Java and Stride classes.
 */
@OnThread(Tag.FXPlatform)
public class DuplicateClassAction extends ClassTargetOperation
{
    public DuplicateClassAction()
    {
        super("duplicate", Combine.ONE, null, ClassTarget.duplicateClassStr, MenuItemOrder.DUPLICATE, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void execute(ClassTarget target)
    {
        target.duplicate();
    }
}
