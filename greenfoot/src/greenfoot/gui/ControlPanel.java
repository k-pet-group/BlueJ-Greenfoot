package greenfoot.gui;

import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;
import greenfoot.core.Simulation;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;

import java.awt.CardLayout;
import java.awt.FlowLayout;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * Panel that holds the buttons that controls the simulation.
 * 
 * @author Poul Henriksen
 * @version $Id: ControlPanel.java 3879 2006-03-25 20:40:14Z mik $
 */
public class ControlPanel extends Box
    implements ChangeListener, SimulationListener, WorldListener
{
    private RunSimulationAction runSimulationAction;
    private PauseSimulationAction pauseSimulationAction;
    private RunOnceSimulationAction runOnceSimulationAction;
    private JSlider speedSlider;

    protected EventListenerList listenerList = new EventListenerList();
    private ChangeEvent changeEvent;
    
    private CardLayout runpauseLayout;
    private JPanel runpauseContainer;

    public ControlPanel(Simulation simulation)
    {
        super(BoxLayout.X_AXIS);
        
        add(createButtonPanel(simulation));
        add(createSpeedSlider());

        simulation.setDelay(getDelay());
        addChangeListener(simulation);
        simulation.addSimulationListener(this);
    }

    private JPanel createButtonPanel(Simulation simulation)
    {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        runOnceSimulationAction = new RunOnceSimulationAction(simulation);
        runOnceSimulationAction.putValue(Action.LONG_DESCRIPTION, "Makes one run of the simulation loop.");
        runOnceSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Makes one run of the simulation loop.");
        runOnceSimulationAction.setEnabled(false);
        AbstractButton stepButton = new JButton(runOnceSimulationAction);

        buttonPanel.add(stepButton);

        runSimulationAction = new RunSimulationAction(simulation);
        runSimulationAction.putValue(Action.LONG_DESCRIPTION, "Runs the simulation until stopped.");
        runSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Runs the simulation.");
        runSimulationAction.setEnabled(false);

        pauseSimulationAction = new PauseSimulationAction(simulation);
        pauseSimulationAction.putValue(Action.LONG_DESCRIPTION,
                "Pauses the simulation, leaving it in the current state.");
        pauseSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Pauses the simulation.");
        pauseSimulationAction.setEnabled(false);
        
        runpauseLayout = new CardLayout();
        runpauseContainer = new JPanel(runpauseLayout);
        runpauseContainer.add(new JButton(runSimulationAction), "run");
        runpauseContainer.add(new JButton(pauseSimulationAction), "pause");
        buttonPanel.add(runpauseContainer);
        
        return buttonPanel;
    }

    private JComponent createSpeedSlider()
    {
        JPanel speedPanel = new JPanel(new FlowLayout());
        JLabel speedLabel = new JLabel("Speed:");
        speedPanel.add(speedLabel);
        
        int min = 0;
        int max = 400;
        speedSlider = new JSlider(JSlider.HORIZONTAL, min, max, 200);
        speedSlider.setPaintLabels(false);
        speedSlider.setMajorTickSpacing( (min+max) /2);
        speedSlider.setMinorTickSpacing( (min+max) /4);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(this);
        speedSlider.setToolTipText("Adjusts the execution speed");
        speedPanel.add(speedSlider);
        
        return speedPanel;
    }
    
//    /**
//     * Return the action object for the 'act' action.
//     */
//    public Action getActAction()
//    {
//        return runOnceSimulationAction;
//    }
//
//    /**
//     * Return the action object for the 'run' action.
//     */
//    public Action getRunAction()
//    {
//        return runSimulationAction;
//    }
//
//    /**
//     * Return the action object for the 'pause' action.
//     */
//    public Action getPauseAction()
//    {
//        return pauseSimulationAction;
//    }
//
    public void simulationChanged(SimulationEvent e)
    {
        int etype = e.getType();
        if (etype == SimulationEvent.STARTED) {
            runpauseLayout.show(runpauseContainer, "pause");
        }
        else if (etype == SimulationEvent.STOPPED) {
            runpauseLayout.show(runpauseContainer, "run");
        }
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

    public void worldCreated(WorldEvent e)
    {
        runSimulationAction.setEnabled(true);
        pauseSimulationAction.setEnabled(true);
        runOnceSimulationAction.setEnabled(true);
    }

    public void worldRemoved(WorldEvent e)
    {
        runSimulationAction.setEnabled(false);
        pauseSimulationAction.setEnabled(false);
        runOnceSimulationAction.setEnabled(false);
    }

}