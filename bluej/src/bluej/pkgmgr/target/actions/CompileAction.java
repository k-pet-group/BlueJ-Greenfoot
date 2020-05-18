package bluej.pkgmgr.target.actions;

import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action to compile a classtarget
 */
@OnThread(Tag.FXPlatform)
public class CompileAction extends ClassTargetOperation
{
    public CompileAction()
    {
        super("compile", Combine.ANY, null, ClassTarget.compileStr, MenuItemOrder.COMPILE, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void execute(ClassTarget target)
    {
        target.getPackage().compile(target, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);
    }
}
