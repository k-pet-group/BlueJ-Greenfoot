package greenfoot.actions;

import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.Selectable;
import greenfoot.gui.classbrowser.SelectionListener;

import javax.swing.AbstractAction;

import rmiextension.wrappers.RClass;

/**
 * Superclass for actions that depends on the selected class.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassAction.java 3124 2004-11-18 16:08:48Z polle $
 */
public abstract class ClassAction extends AbstractAction
    implements SelectionListener
{
    protected RClass selectedClass;

    public ClassAction(String name)
    {
        super(name);
    }

    public void selectionChange(Selectable source)
    {
        if (source == null) {
            selectedClass = null;
        }
        else if (source instanceof ClassView) {
            ClassView classLabel = (ClassView) source;
            RClass rClass = classLabel.getRClass();
            if (classLabel.isSelected()) {
                selectedClass = rClass;
            }
            else {
                selectedClass = null;
            }
        }

        if (selectedClass == null) {
            setEnabled(false);
        }
        else {
            setEnabled(true);
        }
    }
}