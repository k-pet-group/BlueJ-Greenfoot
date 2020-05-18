package bluej.pkgmgr.target.actions;

import bluej.extensions2.SourceType;
import bluej.pkgmgr.PackageEditor;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget;
import bluej.pkgmgr.target.EditableTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.javafx.JavaFXUtil;
import javafx.event.ActionEvent;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action which creates a test
 */
@OnThread(Tag.FXPlatform)
public class CreateTestAction extends ClassTargetOperation
{
    /**
     * Constructor for the CreateTestAction object
     */
    public CreateTestAction()
    {
        super("createTest", Combine.ONE, null, ClassTarget.createTestStr, MenuItemOrder.CREATE_TEST, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void execute(ClassTarget createTestFor)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(createTestFor.getPackage());

        if (pmf != null) {
            String testClassName = createTestFor.getIdentifierName() + "Test";
            pmf.createNewClass(testClassName, "unittest", SourceType.Java, true, -1, -1);
            // we want to check that the previous called actually
            // created a unit test class as a name clash with an existing
            // class would not. This prevents a non unit test becoming
            // associated with a class unintentionally
            Target target = createTestFor.getPackage().getTarget(testClassName);
            DependentTarget assoc = null;
            if (target instanceof ClassTarget) {
                ClassTarget ct = (ClassTarget) target;
                if (ct != null && ct.isUnitTest()) {
                    assoc = (DependentTarget) createTestFor.getPackage().getTarget(createTestFor.getIdentifierName() + "Test");
                }
            }
            DependentTarget assocFinal = assoc;
            PackageEditor pkgEd = createTestFor.getPackage().getEditor();
            if (assocFinal != null)
                createTestFor.setAssociation(assocFinal);
            createTestFor.updateAssociatePosition();
            pkgEd.repaint();
        }
    }
}
