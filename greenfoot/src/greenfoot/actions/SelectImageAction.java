package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.core.GreenfootMain;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.ImageClassRole;

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
 * @version $Id: SelectImageAction.java 5139 2007-08-02 06:37:21Z davmac $
 */
public class SelectImageAction extends ClassAction
{
    private ClassView classView;
    private ImageClassRole gclassRole;
    
    public SelectImageAction(ClassView classView, ImageClassRole gcr)
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

    public static void setClassImage(ClassView classView, ImageClassRole gclassRole, File imageFile)
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
