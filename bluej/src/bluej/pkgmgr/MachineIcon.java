package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import bluej.Config;
import bluej.utility.Debug;

public class MachineIcon extends JLabel
{
    private static final Icon workingIcon = Config.getImageAsIcon("image.working");
    private static final Icon notWorkingIcon = Config.getImageAsIcon("image.working.disab");
    private static final Icon stoppedIcon = Config.getImageAsIcon("image.working.stopped");

    private PkgMgrFrame frame;
    private JPopupMenu popupMenu;

    public MachineIcon(PkgMgrFrame frame)
    {
        this.frame = frame;
	 	setIcon(notWorkingIcon);
        setToolTipText(Config.getString("tooltip.progress"));
//            progressButton.setDisabledIcon(notWorkingIcon);
//            progressButton.setMargin(new Insets(0, 0, 0, 0));

//            progressButton.setEnabled(false);

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
     * Indicate that the machine is stopped.
     */
    private void showDebugger()
    {
        frame.getProject().getExecControls().showHide(true);
    }

    /**
     * Indicate that the machine is stopped.
     */
    private void resetMachine()
    {
        frame.restartDebugger();
    }

    
    /**
     * Process a mouse click into this object. If it was a popup event, show the object's
     * menu. If it was a double click, inspect the object. If it was a normal mouse click,
     * insert it into a parameter field (if any).
     */
    protected void processMouseEvent(MouseEvent evt)
    {
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
		
		item = new JMenuItem("Show Debugger");
		item.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									showDebugger();
								}
							 });
		menu.add(item);

		item = new JMenuItem("Reset Machine");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, InputEvent.SHIFT_MASK));
		item.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									resetMachine();
								}
							 });
		menu.add(item);
        
        return menu;
	}
}

