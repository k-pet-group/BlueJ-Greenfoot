package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import bluej.utility.javafx.JavaFXUtil;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public class ConvertToStrideAction extends ClassTargetOperation
{
    private final Window parentWindow;
    public ConvertToStrideAction(Window parentWindow)
    {
        super("convertToStride", Combine.ONE, null, ClassTarget.convertToStrideStr, MenuItemOrder.CONVERT_TO_STRIDE, EditableTarget.MENU_STYLE_INBUILT);
        this.parentWindow = parentWindow;
    }

    @Override
    protected void execute(ClassTarget target)
    {
        target.promptAndConvertJavaToStride(parentWindow);
    }
}
