package greenfoot.gui.classbrowser;

/**
 * @author Poul Henriksen
 * @version $Id: Selectable.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface Selectable
{
    public boolean isSelected();

    public void select();

    public boolean deselect();
}