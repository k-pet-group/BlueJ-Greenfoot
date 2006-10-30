package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.NormalClassRole;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * An action for creating a new (non-Actor, non-World) class.
 * 
 * @author dam
 * @version $Id: NewClassAction.java 4674 2006-10-30 11:38:31Z polle $
 */
public class NewClassAction extends AbstractAction {

    private static NewClassAction instance;
    
    /**
     * Singleton factory method for action.
     */
    public static synchronized NewClassAction getInstance(ClassBrowser classBrowser)
    {
        if(instance == null) {
            instance = new NewClassAction(classBrowser);
        }
        return instance;
    }


    /**
     * Singleton accessor method. The action must already be initialised before
     * using this method.
     */
    public static NewClassAction getInstance()
    {
        if(instance == null) {
            Debug.reportError("Attempt to access uninitialised NewClassAction");
        }
        return instance;
    }


    private ClassBrowser classBrowser;
    
    private NewClassAction(ClassBrowser classBrowser)
    {
        super("New Class...");
        setEnabled(false);
        this.classBrowser = classBrowser;
    }
	
    public void actionPerformed(ActionEvent arg0)
    {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(classBrowser);
        NewClassDialog dialog = new NewClassDialog(f);
        dialog.setVisible(true);
        if (!dialog.okPressed()) {
            return;
        }
        
        String className = dialog.getClassName();
        //GClass gClass = superclass.createSubclass(className);        
        
        try {
            GPackage pkg = GreenfootMain.getInstance().getProject().getDefaultPackage();
            
            File dir = pkg.getProject().getDir();
            File newJavaFile = new File(dir, className + ".java");
            FileWriter writer = new FileWriter(newJavaFile);
            
            NormalClassRole.getInstance().createSkeleton(className, null, writer);
            
            writer.close();
            GClass newClass = pkg.newClass(className);
            
            ClassView classView = new ClassView(newClass);
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
