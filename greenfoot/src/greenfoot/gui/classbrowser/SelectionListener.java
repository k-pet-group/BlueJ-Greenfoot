package greenfoot.gui.classbrowser;

import java.util.EventListener;

/**
 * @author Poul Henriksen
 * @version $Id: SelectionListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface SelectionListener
    extends EventListener
{
    public void selectionChange(Selectable source);
}