package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import javax.swing.AbstractAction;


/**
 * Superclass for actions that depends on the selected class.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassAction.java 5154 2007-08-10 07:02:51Z davmac $
 */
public abstract class ClassAction extends AbstractAction
{
    private GreenfootFrame gfFrame;

    public ClassAction(String name, GreenfootFrame gfFrame)
    {
        super(name);
        this.gfFrame = gfFrame;
    }
    
    protected ClassView getSelectedClassView()
    {
    	ClassBrowser classBrowser = gfFrame.getClassBrowser();
    	Object selected = classBrowser.getSelectionManager().getSelected();
    	if (selected instanceof ClassView) {
    		return (ClassView) selected;
    	}
    	else {
    		return null;
    	}
    }
}
