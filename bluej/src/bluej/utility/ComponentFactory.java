package bluej.utility;
import java.awt.Component;
import javax.swing.JButton;


/**
 * Interface for a factory to create components for a GrowableBox.
 * 
 * @see bluej.utility.GrowableBox
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ComponentFactory.java 2585 2004-06-10 13:27:46Z polle $
 */
public interface ComponentFactory
{
    public Component createComponent(JButton addButton, JButton removeButton);
}
