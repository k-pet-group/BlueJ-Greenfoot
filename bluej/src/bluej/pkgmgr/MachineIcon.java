package bluej.pkgmgr;

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.actions.RestartVMAction;
import bluej.pkgmgr.actions.ShowDebuggerAction;

public class MachineIcon extends JLabel
{
    private static final Icon workingIcon = Config.getImageAsIcon("image.working");
    private static final Icon notWorkingIcon = Config.getImageAsIcon("image.working.idle");
    private static final Icon workingIconDisabled = Config.getImageAsIcon("image.working.disab");
    private static final Icon stoppedIcon = Config.getImageAsIcon("image.working.stopped");

    private PkgMgrFrame frame;
    private JPopupMenu popupMenu;

    public MachineIcon(PkgMgrFrame frame)
    {
        this.frame = frame;
	 	setIcon(notWorkingIcon);
	 	setDisabledIcon(workingIconDisabled);
        setToolTipText(Config.getString("tooltip.progress"));

        popupMenu = createMachinePopup();
        enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    /**
     * Indicate that the machine is idle.
     */
    public void setIdle()
    {
	 	setIcon(notWorkingIcon);
    }
    
    /**
     * Indicate that the machine is running.
     */
    public void setRunning()
    {
        setIcon(workingIcon);
    }

    /**
     * Indicate that the machine is stopped.
     */
    public void setStopped()
    {
        setIcon(stoppedIcon);
    }
    
    /**
     * Process a mouse click into this object. If it was a popup event, show the object's
     * menu. If it was a double click, inspect the object. If it was a normal mouse click,
     * insert it into a parameter field (if any).
     */
    protected void processMouseEvent(MouseEvent evt)
    {
        if(!isEnabled())
            return;
        
        int menuOffset;
        super.processMouseEvent(evt);

        if(evt.isPopupTrigger()) {
            popupMenu.show(this, 10,10);
        }
//        else if(evt.getID() == MouseEvent.MOUSE_CLICKED) {
//            if(evt.getClickCount() > 1)  // double click
//                ;
//        }
    }

	private JPopupMenu createMachinePopup()
	{
		JPopupMenu menu = new JPopupMenu();
        JMenuItem item;
        
        item = new JMenuItem(ShowDebuggerAction.getInstance());
		menu.add(item);

		item = new JMenuItem(RestartVMAction.getInstance());
		menu.add(item);
        
        return menu;
	}
}

