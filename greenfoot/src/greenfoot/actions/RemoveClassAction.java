package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.gui.NewClassDialog;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class RemoveClassAction extends ClassAction
{

    private ClassView cls;

    
    public RemoveClassAction(String name, ClassView view)
    {
        super(name);
        this.cls = view;
    }

    public void actionPerformed(ActionEvent e)
    {
        cls.remove();
    }

}
