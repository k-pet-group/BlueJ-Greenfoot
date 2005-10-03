package greenfoot.gui;

import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;
import greenfoot.core.Simulation;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
 * @version $Id: ControlPanel.java 3634 2005-10-03 10:34:22Z polle $
 */
public class ControlPanel extends JPanel
    implements ChangeListener, SimulationListener
{
    private RunSimulationAction runSimulationAction;
    private PauseSimulationAction pauseSimulationAction;
    private JSlider speedSlider;

    protected EventListenerList listenerList = new EventListenerList();
    private ChangeEvent changeEvent;
    
    private CardLayout runpauseLayout;
    private JPanel runpauseContainer;

    public ControlPanel(Simulation simulation)
    {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10 ,10));
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

        runpauseLayout = new CardLayout();
        runpauseContainer = new JPanel(runpauseLayout);
        runpauseContainer.add(new JButton(runSimulationAction), "run");
        runpauseContainer.add(new JButton(pauseSimulationAction), "pause");
        add(runpauseContainer);

        createSpeedSlider();
        
        simulation.addSimulationListener(this);
    }

    private void createSpeedSlider()
    {
        int min = 0;
        int max = 400;
        speedSlider = new JSlider(JSlider.HORIZONTAL, min, max, 200);
        speedSlider.setPaintLabels(true);
        speedSlider.setMajorTickSpacing( (min+max) /2);
        speedSlider.setMinorTickSpacing( (min+max) /4);
        speedSlider.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(this);
        Dictionary labelTable = new Hashtable();
        JLabel startLabel = new JLabel("Slow");
        JLabel endLabel = new JLabel("Fast");
        labelTable.put(new Integer(min), startLabel);
        labelTable.put(new Integer(max), endLabel);
        speedSlider.setLabelTable(labelTable );
        add(speedSlider);
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

}