package greenfoot.gui;

import greenfoot.Simulation;
import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;

import java.awt.FlowLayout;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * Panel that holds the buttons that controls the simulation.
 * 
 * @author Poul Henriksen
 * @version $Id: ControlPanel.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ControlPanel extends JPanel
    implements ChangeListener
{
    private RunSimulationAction runSimulationAction;
    private PauseSimulationAction pauseSimulationAction;
    private JSlider speedSlider;

    protected EventListenerList listenerList = new EventListenerList();
    private ChangeEvent changeEvent;

    public ControlPanel(Simulation simulation)
    {
        setLayout(new FlowLayout());
        URL stepIconFile = this.getClass().getClassLoader().getResource("step.gif");
        Icon stepIcon = new ImageIcon(stepIconFile);
        RunOnceSimulationAction stepSimulationAction = new RunOnceSimulationAction("Act", stepIcon, simulation);
        stepSimulationAction.putValue(Action.LONG_DESCRIPTION, "Makes one run of the simulation loop.");
        stepSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Makes one run of the simulation loop.");

        AbstractButton stepButton = new JButton(stepSimulationAction);

        add(stepButton);

        URL runIconFile = this.getClass().getClassLoader().getResource("run.gif");
        Icon runIcon = new ImageIcon(runIconFile);
        runSimulationAction = new RunSimulationAction("Run", runIcon, simulation);
        runSimulationAction.putValue(Action.LONG_DESCRIPTION, "Runs the simulation until stopped.");
        runSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Runs the simulation.");

        URL pauseIconFile = this.getClass().getClassLoader().getResource("pause.gif");
        Icon pauseIcon = new ImageIcon(pauseIconFile);
        pauseSimulationAction = new PauseSimulationAction("Pause", pauseIcon, simulation);
        pauseSimulationAction.putValue(Action.LONG_DESCRIPTION,
                "Pauses the simulation, leaving it in the current state.");
        pauseSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Pauses the simulation.");

        AbstractButton runButton = new ToggleActionButton(runSimulationAction, pauseSimulationAction);

        add(runButton);

        speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 400, 200);
        speedSlider.addChangeListener(this);
        add(speedSlider);
    }

    public int getDelay()
    {
        int value = speedSlider.getValue();
        //invert
        value = speedSlider.getMaximum() - value;
        int delay = value;
        return delay;
    }

    public void addChangeListener(ChangeListener l)
    {
        listenerList.add(ChangeListener.class, l);
    }

    public void stateChanged(ChangeEvent e)
    {
        fireStateChanged();
    }

    protected void fireStateChanged()
    {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

}