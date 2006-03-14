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
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
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
 * @version $Id: ControlPanel.java 3820 2006-03-14 21:03:48Z polle $
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
        
        URL stepIconFile = this.getClass().getClassLoader().getResource("step.gif");
        Icon stepIcon = new ImageIcon(stepIconFile);
        runOnceSimulationAction = new RunOnceSimulationAction("Act", stepIcon, simulation);
        runOnceSimulationAction.putValue(Action.LONG_DESCRIPTION, "Makes one run of the simulation loop.");
        runOnceSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Makes one run of the simulation loop.");
        runOnceSimulationAction.setEnabled(false);
        AbstractButton stepButton = new JButton(runOnceSimulationAction);

        buttonPanel.add(stepButton);

        URL runIconFile = this.getClass().getClassLoader().getResource("run.gif");
        Icon runIcon = new ImageIcon(runIconFile);
        runSimulationAction = new RunSimulationAction("Run", runIcon, simulation);
        runSimulationAction.putValue(Action.LONG_DESCRIPTION, "Runs the simulation until stopped.");
        runSimulationAction.putValue(Action.SHORT_DESCRIPTION, "Runs the simulation.");
        runSimulationAction.setEnabled(false);

        URL pauseIconFile = this.getClass().getClassLoader().getResource("pause.gif");
        Icon pauseIcon = new ImageIcon(pauseIconFile);
        pauseSimulationAction = new PauseSimulationAction("Pause", pauseIcon, simulation);
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