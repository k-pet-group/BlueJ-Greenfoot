package greenfoot.actions;

import greenfoot.core.GClass;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.Selectable;
import greenfoot.gui.classbrowser.SelectionListener;

import javax.swing.AbstractAction;


/**
 * Superclass for actions that depends on the selected class.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassAction.java 3552 2005-09-06 15:53:28Z polle $
 */
public abstract class ClassAction extends AbstractAction
    implements SelectionListener
{
    protected GClass selectedClass;

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
            GClass gClass = classLabel.getGClass();
            if (classLabel.isSelected()) {
                selectedClass = gClass;
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