package bluej.utility;
import javax.swing.JButton;
import javax.swing.JComponent;


/**
 * Interface for a factory to create components for a GrowableBox.
 * 
 * @see bluej.utility.GrowableBox
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ComponentFactory.java 2638 2004-06-20 12:06:37Z polle $
 */
public interface ComponentFactory
{
    public JComponent createComponent(JButton addButton, JButton removeButton);
}
