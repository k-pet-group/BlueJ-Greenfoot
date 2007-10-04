package greenfoot.actions;

import bluej.Config;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;


/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class RemoveClassAction extends AbstractAction
{
    private ClassView cls;
    
    public RemoveClassAction(ClassView view)
    {
        super(Config.getString("remove.class"));
        this.cls = view;
    }

    public void actionPerformed(ActionEvent e)
    {
        cls.remove();
    }
}
