package greenfoot.actions;

import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;


/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class RemoveClassAction extends ClassAction
{

    private ClassView cls;

    
    public RemoveClassAction(ClassView view)
    {
        super("Remove");
        this.cls = view;
    }

    public void actionPerformed(ActionEvent e)
    {
        cls.remove();
    }

}
