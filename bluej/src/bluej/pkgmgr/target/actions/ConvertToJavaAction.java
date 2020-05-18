package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import bluej.utility.javafx.JavaFXUtil;
import javafx.event.ActionEvent;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(Tag.FXPlatform)
public class ConvertToJavaAction extends ClassTargetOperation
{
    private final Window parentWindow;
    
    public ConvertToJavaAction(Window parentWindow)
    {
        super("convertToJava", Combine.ONE, null, ClassTarget.convertToJavaStr, MenuItemOrder.CONVERT_TO_JAVA, EditableTarget.MENU_STYLE_INBUILT);
        this.parentWindow = parentWindow;
    }

    @Override
    protected void execute(ClassTarget target)
    {
        if (JavaFXUtil.confirmDialog("convert.to.java.title", "convert.to.java.message", parentWindow, true))
        {
            target.convertStrideToJava();
        }
    }
}
