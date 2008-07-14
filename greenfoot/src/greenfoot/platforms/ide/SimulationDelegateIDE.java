package greenfoot.platforms.ide;

import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.platforms.SimulationDelegate;

/**
 * IDE version of the Simulation delegates.
 * This class implements the setSpeed method to store the speed setting to the project properties.
 * 
 * @author Poul Henriksen
 */
public class SimulationDelegateIDE
    implements SimulationDelegate
{

    public void setSpeed(int speed)
    {
        GProject project = GreenfootMain.getInstance().getProject();
        if(project != null) {
            ProjectProperties props = project.getProjectProperties();
            props.setInt("simulation.speed", Simulation.getInstance().getSpeed());
        }
    }

}
