package greenfoot.gui;

import greenfoot.Greenfoot;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * Listens for resize events on a frame and tells BlueJ to use the bounds when
 * placing dialog.
 * 
 * HACK for the mac, to be able to show dialogs on the right side of the main
 * greenfoot window. Otherwise the dialogs would be hidden behind the greenfoot
 * window.
 * 
 * @see bluej.utility.DialogManager
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: FrameBoundsManager.java 3124 2004-11-18 16:08:48Z polle $
 *  
 */
public class FrameBoundsManager
    implements ComponentListener
{

    public FrameBoundsManager(Component subject)
    {
        subject.addComponentListener(this);

    }

    public void componentHidden(ComponentEvent e)
    {
    // TODO Auto-generated method stub

    }

    public void componentMoved(ComponentEvent e)
    {
        Greenfoot.getInstance().frameResized(e.getComponent().getBounds());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    public void componentResized(ComponentEvent e)
    {
        Greenfoot.getInstance().frameResized(e.getComponent().getBounds());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    public void componentShown(ComponentEvent e)
    {
        Greenfoot.getInstance().frameResized(e.getComponent().getBounds());
    }

}