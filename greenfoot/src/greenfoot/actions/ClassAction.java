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
 * @version $Id: ClassAction.java 4017 2006-04-25 17:51:23Z davmac $
 */
public abstract class ClassAction extends AbstractAction
    implements SelectionListener
{
    protected GClass selectedClass;
    protected ClassView selectedClassView;

    public ClassAction(String name)
    {
        super(name);
    }

    public void selectionChange(Selectable source)
    {
        if (source == null) {
            selectedClassView = null;
            selectedClass = null;
        }
        else if (source instanceof ClassView) {
            ClassView classLabel = (ClassView) source;
            GClass gClass = classLabel.getGClass();
            if (classLabel.isSelected()) {
                selectedClassView = classLabel;
                selectedClass = gClass;
            }
            else {
                selectedClassView = null;
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