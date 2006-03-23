package greenfoot.actions;

import greenfoot.core.Greenfoot;
import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

/**
 * Action to select an image for a class.
 * 
 * @author Davin McCall
 * @version $Id: SelectImageAction.java 3862 2006-03-23 03:05:51Z davmac $
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
        ImageLibFrame imageLibFrame = new ImageLibFrame(gfFrame, classView, gclassRole);
    }

}
