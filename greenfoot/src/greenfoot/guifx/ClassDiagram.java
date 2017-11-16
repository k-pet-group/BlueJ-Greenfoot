package greenfoot.guifx;

import bluej.debugmgr.objectbench.InvokeListener;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.Utility;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * The class diagram on the right-hand side of the Greenfoot window.
 *
 * For now, this is very primitive, but is useful for implementing other Greenfoot functionality.
 */
public class ClassDiagram extends VBox
{
    public ClassDiagram(Project project)
    {
        getChildren().setAll(Utility.mapList(
                project.getUnnamedPackage().getClassTargets(),
                this::makeClassItem));
    }

    /**
     * Make the graphical item for a ClassTarget.  Currently just a Label.
     */
    private Node makeClassItem(ClassTarget classTarget)
    {
        Label label = new Label(classTarget.getBaseName());
        label.setOnContextMenuRequested(e -> {
            Class<?> cl = classTarget.getPackage().loadClass(classTarget.getQualifiedName());
            if (cl != null)
            {
                ContextMenu contextMenu = new ContextMenu();
                classTarget.getRole().createClassConstructorMenu(contextMenu.getItems(), classTarget, cl);
                contextMenu.show(label, e.getScreenX(), e.getScreenY());
            }
        });
        return label;
    }
}
