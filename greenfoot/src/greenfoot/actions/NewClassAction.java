package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.Greenfoot;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * An action for creating a new (non-Actor, non-World) class.
 * 
 * @author dam
 * @version $Id: NewClassAction.java 4013 2006-04-25 15:08:57Z davmac $
 */
public class NewClassAction extends AbstractAction {

    private ClassBrowser classBrowser;
    
	public NewClassAction(ClassBrowser classBrowser)
	{
		super("New class");
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
        GPackage pkg = Greenfoot.getInstance().getPackage();

        try {
            File dir = pkg.getProject().getDir();
            File newJavaFile = new File(dir, className + ".java");
            Writer writer = new FileWriter(newJavaFile);
            // DAV write the file from skeleton
        
            writer.close();
            GClass newClass = pkg.newClass(className);
        
            ClassView classView = classBrowser.addClass(newClass);
            classView.select();
            classBrowser.revalidate();
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (IOException ioe) {
            // TODO definitely should report an error condition via dialog
            ioe.printStackTrace();
        }
        catch (PackageNotFoundException pnfe) {
            pnfe.printStackTrace();
        }
        catch (MissingJavaFileException mjfe) {
            mjfe.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
	}
}
