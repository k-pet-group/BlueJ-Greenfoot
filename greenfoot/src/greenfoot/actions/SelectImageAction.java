package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.Greenfoot;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;

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
 * @version $Id: SelectImageAction.java 3866 2006-03-24 04:23:52Z davmac $
 */
public class SelectImageAction extends ClassAction
{
    private ClassView classView;
    private GreenfootClassRole gclassRole;
    
    public SelectImageAction(ClassView classView, GreenfootClassRole gcr)
    {
        super("Select image for class");
        this.classView = classView;
        this.gclassRole = gcr;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        JFrame gfFrame = Greenfoot.getInstance().getFrame();
        ImageLibFrame imageLibFrame = new ImageLibFrame(gfFrame, classView);
        
        File currentImageFile = imageLibFrame.getSelectedImageFile();
        
        setClassImage(classView, gclassRole, currentImageFile);
    }

    public static void setClassImage(ClassView classView, GreenfootClassRole gclassRole, File imageFile)
    {
        Greenfoot greenfootInstance = Greenfoot.getInstance();
        
        try {
            File projDir = greenfootInstance.getProject().getDir();
            File projImagesDir = new File(projDir, "images");
            
            if (imageFile != null) {
                if (! imageFile.getParent().equals(projImagesDir)) {
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
