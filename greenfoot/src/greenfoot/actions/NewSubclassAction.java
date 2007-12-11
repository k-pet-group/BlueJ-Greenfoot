package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ImageClassRole;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.Config;


/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: NewSubclassAction.java 5415 2007-12-11 04:05:11Z davmac $
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
        super(Config.getString("new.subclass"));
        this.superclass = view;
        this.classBrowser = classBrowser;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        GClass superG = superclass.getGClass();
        
        boolean imageClass = superG.isActorClass() || superG.isActorSubclass();
        imageClass |= superG.isWorldClass() || superG.isWorldSubclass();
            
        if (imageClass) {
            createImageClass();
        } else {
            createNonActorClass();
        }
    }
    
    public void createImageClass()
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        
        ImageLibFrame dialog = new ImageLibFrame(f, superclass.getGClass());
        if (! (dialog.getResult() == ImageLibFrame.OK)) {
            return;
        }
        
        String className = dialog.getClassName();
        GClass gClass = superclass.createSubclass(className);
       
        ClassView classView = new ClassView(classBrowser, gClass);
        
        SelectImageAction.setClassImage(classView,
                (ImageClassRole) classView.getRole(),
                dialog.getSelectedImageFile());

        classBrowser.addClass(classView);
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
        
        ClassView classView = new ClassView(classBrowser, gClass);
        classBrowser.addClass(classView);        
    }

}