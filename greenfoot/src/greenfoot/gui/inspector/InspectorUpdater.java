package greenfoot.gui.inspector;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Timer;

import bluej.debugmgr.inspector.Inspector;

/**
 * Updater that will call update on an inspector with fixed time intervals. <br>
 * The updater will stop when the window is closed.
 * 
 * @author Poul Henriksen
 */
public class InspectorUpdater
    implements WindowListener
{
    private Inspector inspector;
    private Timer timer;
    private static final int PERIOD = 500;

    /**
     * Creates a new updater. It will start updating as soon as the inspector
     * becomes visible.
     * 
     * @param inspector
     */
    public InspectorUpdater(Inspector inspector)
    {
        this.inspector = inspector;
        inspector.addWindowListener(this);
        if (inspector.isVisible()) {
            start();
        }

    }

    /**
     * Starts the updater.
     * 
     */
    public void start()
    {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new UpdateTask(inspector), PERIOD, PERIOD);
    }

    /**
     * Stops the updater.
     * 
     */
    public void stop()
    {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void windowActivated(WindowEvent e)
    {
    // Nothing to do
    }

    public void windowDeactivated(WindowEvent e)
    {
    // Nothing to do
    }

    public void windowOpened(WindowEvent e)
    {
        start();
    }

    public void windowClosed(WindowEvent e)
    {
        stop();
    }

    public void windowClosing(WindowEvent e)
    {
    // Nothing to do
    }

    public void windowDeiconified(WindowEvent e)
    {
        start();
    }

    public void windowIconified(WindowEvent e)
    {
        stop();
    }

}
