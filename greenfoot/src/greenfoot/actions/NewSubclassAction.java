package greenfoot.actions;

import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import rmiextension.wrappers.RClass;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewSubclassAction.java 3124 2004-11-18 16:08:48Z polle $
 */
public class NewSubclassAction extends AbstractAction
{

    private ClassView superclass;
    private ClassBrowser classBrowser;

    /**
     * Creates a new subclass of the class represented by the view
     * 
     * @param view
     *            The class that is to be the superclass
     * @param name
     *            Name of the action that appears in the menu
     */
    public NewSubclassAction(String name, ClassView view, ClassBrowser classBrowser)
    {
        super(name);
        this.superclass = view;
        this.classBrowser = classBrowser;
    }

    public void actionPerformed(ActionEvent e)
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        NewClassDialog dialog = new NewClassDialog(f, superclass.getQualifiedClassName());
        dialog.show();
        if (!dialog.okPressed()) {
            return;
        }

        String className = dialog.getClassName();
        RClass rClass = superclass.createSubclass(className);

        ClassView classView = classBrowser.addClass(rClass);
        classView.select();
        classBrowser.revalidate();
    }

}