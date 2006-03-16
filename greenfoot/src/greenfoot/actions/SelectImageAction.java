package greenfoot.actions;

import greenfoot.gui.ImageLibFrame;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;

import java.awt.event.ActionEvent;

/**
 * Action to select an image for a class.
 * 
 * @author Davin McCall
 * @version $Id: SelectImageAction.java 3830 2006-03-16 05:36:04Z davmac $
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
        ImageLibFrame imageLibFrame = new ImageLibFrame(classView, gclassRole);
    }

}
