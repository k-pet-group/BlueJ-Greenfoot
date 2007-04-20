package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ActorClassRole;

import java.awt.event.ActionEvent;
import java.io.File;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import bluej.extensions.ProjectNotOpenException;
import bluej.utility.FileUtility;

/**
 * Action to select an image for a class.
 * 
 * @author Davin McCall
 * @version $Id: SelectImageAction.java 4982 2007-04-20 05:45:52Z davmac $
 */
public class SelectImageAction extends ClassAction
{
    private ClassView classView;
    private ActorClassRole gclassRole;
    
    public SelectImageAction(ClassView classView, ActorClassRole gcr)
    {
        super("Set image...");
        this.classView = classView;
        this.gclassRole = gcr;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        JFrame gfFrame = GreenfootMain.getInstance().getFrame();
        ImageLibFrame imageLibFrame = new ImageLibFrame(gfFrame, classView);
        
        File currentImageFile = imageLibFrame.getSelectedImageFile();
        
        setClassImage(classView, gclassRole, currentImageFile);
    }

    public static void setClassImage(ClassView classView, ActorClassRole gclassRole, File imageFile)
    {
        GreenfootMain greenfootInstance = GreenfootMain.getInstance();
        
        try {
            File projDir = greenfootInstance.getProject().getDir().getAbsoluteFile();
            File projImagesDir = new File(projDir, "images");
            
            if (imageFile != null) {
                if (! imageFile.getParentFile().getAbsoluteFile().equals(projImagesDir)) {
                    // An image was selected from an external dir. We need
                    // to copy it into the project images directory first.
                    File destFile = new File(projImagesDir, imageFile.getName());
                    FileUtility.copyFile(imageFile, destFile);
                    imageFile = destFile;
                }
                
                GClass gclass = classView.getGClass();
                gclass.setClassProperty("image", imageFile.getName());
                gclassRole.changeImage();
            }
        }
        catch (RemoteException re) {}
        catch (ProjectNotOpenException pnoe) {}
    }
}
