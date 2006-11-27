package bluej.groupwork.ui;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.OverlayLayout;

public class ActivityIndicator extends JComponent implements Runnable
{
    private JProgressBar progressBar;
    private boolean running;
    
    public ActivityIndicator()
    {
        setBorder(null);
        setLayout(new OverlayLayout(this));
        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        add(progressBar);
    }
    
    /**
     * Set the activity indicator's running state. This is safe to call
     * from any thread.
     * 
     * @param running  The new running state
     */
    public void setRunning(boolean running)
    {
        this.running = running;
        if (EventQueue.isDispatchThread()) {
            progressBar.setVisible(running);
        }
        else {
            EventQueue.invokeLater(this);
        }
    }
    
    /*
     * The run() method will only be called on the event dispatch thread, and
     * is used to update the current running state.
     */
    public void run()
    {
        progressBar.setVisible(running);
    }
    
    public Dimension getPreferredSize()
    {
        return progressBar.getPreferredSize();
    }
    
    public Dimension getMinimumSize()
    {
        return progressBar.getMinimumSize();
    }
    
    public Dimension getMaximumSize()
    {
        return progressBar.getMaximumSize();
    }
    
    public boolean isValidateRoot()
    {
        return true;
    }
}
