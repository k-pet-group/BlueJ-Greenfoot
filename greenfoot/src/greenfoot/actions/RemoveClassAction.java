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
