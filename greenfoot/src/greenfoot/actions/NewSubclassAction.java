package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;


/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewSubclassAction.java 4672 2006-10-30 10:58:35Z polle $
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
    public NewSubclassAction(ClassView view, ClassBrowser classBrowser)
    {
        super("New subclass...");
        this.superclass = view;
        this.classBrowser = classBrowser;
    }
    public void actionPerformed(ActionEvent e)
    {
        String actor = "Actor";
        GClass superG = superclass.getGClass();
        if(superG.isSubclassOf(actor) || superG.getName().equals(actor)) {
            createActorClass();
        } else {
            createNonActorClass();
        }
    }
    
    public void createActorClass()
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        
        ImageLibFrame dialog = new ImageLibFrame(f, superclass.getGClass());
        if (! (dialog.getResult() == ImageLibFrame.OK)) {
            return;
        }
        
        String className = dialog.getClassName();
        GClass gClass = superclass.createSubclass(className);
       
        ClassView classView = new ClassView(gClass);
        classBrowser.addClass(classView);
        
        SelectImageAction.setClassImage(classView,
                (GreenfootClassRole) classView.getRole(),
                dialog.getSelectedImageFile());
        
        classView.select();
        classBrowser.revalidate();
    }
    
    public void createNonActorClass()
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        NewClassDialog dialog = new NewClassDialog(f);
        dialog.setVisible(true);
        if (!dialog.okPressed()) {
            return;
        }

        String className = dialog.getClassName();
        GClass gClass = superclass.createSubclass(className);   
        
        ClassView classView = new ClassView(gClass);
        classBrowser.addClass(classView);
        
        classView.select();
        classBrowser.revalidate();
    }

}