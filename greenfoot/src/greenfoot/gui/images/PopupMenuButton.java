package greenfoot.gui.images;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Creates a button that can have a PopupMenu which will be
 * displayed when the button is clicked on.
 * 
 * It's appearance is completely dependent on the icon provided.
 * @author Philip Stevens
 */
public class PopupMenuButton extends JButton implements PopupMenuListener, MouseListener
{
    /** Holds the PopupMenu to be displayed on pressing this button. */
    private JPopupMenu popupMenu;
    
    /**
     * Initialises the button, setting up a mouse listener for when
     * pressed on, or released (mouse pressed on something else). Also
     * added a popup listener for the popup, to set display states.
     * @param icon      Icon to show as the button.
     * @param popupMenu PopupMenu to display when this button is clicked on.
     */
    public PopupMenuButton(Icon icon, JPopupMenu popupMenu)
    {
        super(icon);
        this.popupMenu = popupMenu;
        
        // removes the blue hint that could appear around the icon
        setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        
        // listeners for this, and the PopupMenu
        popupMenu.addPopupMenuListener(this);
        addMouseListener(this);
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.AbstractButton#fireActionPerformed(java.awt.event.ActionEvent)
     */
    protected void fireActionPerformed(ActionEvent actionEvent) 
    {
        super.fireActionPerformed(actionEvent);
    }
    
    /*-------------------[ PopupMenuListener ]------------------------------*/
    
    /**
     * Set the model's Armed and Pressed to true.
     * @param e is ignored
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) 
    {
        getModel().setArmed(true);
        getModel().setPressed(true);
    }
    
    /**
     * Set the model's Armed and Pressed to false.
     * @param e is ignored
     */
    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) 
    {
        getModel().setArmed(false);
        getModel().setPressed(false);
    }

    /**
     * Ignored
     */
    @Override
    public void popupMenuCanceled(PopupMenuEvent e) 
    {
    }
    
    /*-------------------[ MouseListener ]-----------------------------------*/
    
    /**
     * Show the PopupMenu at the current position of this class.
     * @param e is ignored
     */
    @Override
    public void mousePressed(MouseEvent e) 
    {
        popupMenu.show(PopupMenuButton.this, 0, getHeight());
    }

    /**
     * When mouse is released will close the PopupMenu.
     * @param e is ignored
     */
    @Override
    public void mouseReleased(MouseEvent e) 
    {
        fireActionPerformed(new ActionEvent(PopupMenuButton.this,
                ActionEvent.ACTION_PERFORMED, ""));
    }
    
    /**
     * Ignored
     */
    @Override
    public void mouseClicked(MouseEvent e) 
    {
    }
    
    /**
     * Ignored
     */
    @Override
    public void mouseEntered(MouseEvent e) 
    {
    }
    
    /**
     * Ignored
     */
    @Override
    public void mouseExited(MouseEvent e) 
    {
    }
}