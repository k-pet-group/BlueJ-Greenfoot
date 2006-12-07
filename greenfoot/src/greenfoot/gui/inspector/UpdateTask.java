package greenfoot.gui.inspector;

import java.util.TimerTask;

import bluej.debugmgr.inspector.Inspector;

/**
 * Timer task that calls update on an inspector.
 * 
 * @author Poul Henriksen
 */
public class UpdateTask extends TimerTask
{
    private Inspector inspector;

    public UpdateTask(Inspector insp)
    {
        this.inspector = insp;
    }

    @Override
    public void run()
    {
        inspector.update();
    }
}
