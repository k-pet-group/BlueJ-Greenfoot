package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.util.GreenfootUtil;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import bluej.extensions.ProjectNotOpenException;

/**
 * An action for creating a new (non-Actor, non-World) class.
 * 
 * @author dam
 * @version $Id: NewClassAction.java 5159 2007-08-17 03:27:38Z davmac $
 */
public class NewClassAction extends AbstractAction
{
    //private ClassBrowser classBrowser;
    private GreenfootFrame gfFrame;

    public NewClassAction(GreenfootFrame gfFrame)
    {
        super("New Class...");
        setEnabled(false);
        this.gfFrame = gfFrame;
    }
	
    public void actionPerformed(ActionEvent arg0)
    {
        JFrame f = gfFrame;
        NewClassDialog dialog = new NewClassDialog(f);
        dialog.setVisible(true);
        if (!dialog.okPressed()) {
            return;
        }
        
        String className = dialog.getClassName();
        //GClass gClass = superclass.createSubclass(className);        
        
        try {
        	ClassBrowser classBrowser = gfFrame.getClassBrowser();
            GPackage pkg = classBrowser.getProject().getDefaultPackage();
            
            File dir = pkg.getProject().getDir();
            File newJavaFile = new File(dir, className + ".java");
            GreenfootUtil.createSkeleton(className, null, newJavaFile, NormalClassRole.getInstance().getTemplateFileName());

            GClass newClass = pkg.newClass(className);

            ClassView classView = new ClassView(classBrowser, newClass);
            classBrowser.addClass(classView);
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (IOException ioe) {
            // TODO definitely should report an error condition via dialog
            ioe.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
    }
}
