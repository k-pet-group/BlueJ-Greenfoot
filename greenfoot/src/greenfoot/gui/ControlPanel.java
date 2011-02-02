/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.gui;

import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.ResetWorldAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;
import greenfoot.core.Simulation;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.util.GreenfootUtil;

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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bluej.Config;

/**
 * Panel that holds the buttons that controls the simulation.
 * 
 * @author Poul Henriksen
 */
public class ControlPanel extends Box
    implements ChangeListener, SimulationListener
{
    private RunSimulationAction runSimulationAction;
    private PauseSimulationAction pauseSimulationAction;
    private RunOnceSimulationAction runOnceSimulationAction;

    private ResetWorldAction resetWorldAction ;
    private JSlider speedSlider;
 
    private Simulation simulation;
    private JPanel buttonPanel;
    private JButton pauseButton;
    private JButton runButton;
    
    /**
     * 
     * @param simulation
     * @param includeAllControls If false, the act-button and speedslider will be excluded.
     */
    public ControlPanel(Simulation simulation, boolean includeAllControls)
    {
        super(BoxLayout.X_AXIS);
        
        this.simulation = simulation;
        
        add(createButtonPanel(simulation, includeAllControls));

        if (includeAllControls) {
            add(createSpeedSlider());
        }
        simulation.addSimulationListener(this);

    }

    private JPanel createButtonPanel(Simulation simulation, boolean includeAllControls)
    {
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        if (includeAllControls) {
            runOnceSimulationAction = RunOnceSimulationAction.getInstance();
            runOnceSimulationAction.attachSimulation(simulation);
            runOnceSimulationAction.putValue(Action.LONG_DESCRIPTION, 
                Config.getString("controls.runonce.longDescription"));
            runOnceSimulationAction.putValue(Action.SHORT_DESCRIPTION, 
                Config.getString("controls.runonce.shortDescription"));
            runOnceSimulationAction.setEnabled(false);
            AbstractButton stepButton = GreenfootUtil.createButton(runOnceSimulationAction);

            buttonPanel.add(stepButton);
        }

        runSimulationAction = RunSimulationAction.getInstance();
        runSimulationAction.attachSimulation(simulation);
        runSimulationAction.putValue(Action.LONG_DESCRIPTION, 
            Config.getString("controls.run.longDescription"));
        runSimulationAction.putValue(Action.SHORT_DESCRIPTION, 
            Config.getString("controls.run.shortDescription"));
        runSimulationAction.setEnabled(false);
        runButton = GreenfootUtil.createButton(runSimulationAction);

        pauseSimulationAction = PauseSimulationAction.getInstance();
        pauseSimulationAction.attachSimulation(simulation);
        pauseSimulationAction.putValue(Action.LONG_DESCRIPTION, 
            Config.getString("controls.pause.longDescription"));
        pauseSimulationAction.putValue(Action.SHORT_DESCRIPTION, 
            Config.getString("controls.pause.shortDescription"));
        pauseSimulationAction.setEnabled(false);
        pauseButton = GreenfootUtil.createButton(pauseSimulationAction);
        
        // Make buttons the same size
        if(pauseButton.getPreferredSize().getWidth() > runButton.getPreferredSize().getWidth()) {
            runButton.setPreferredSize(pauseButton.getPreferredSize());
            runButton.setMaximumSize(pauseButton.getMaximumSize());
            runButton.setMinimumSize(pauseButton.getMinimumSize());
        } else  {
            pauseButton.setPreferredSize(runButton.getPreferredSize());
            pauseButton.setMaximumSize(runButton.getMaximumSize());
            pauseButton.setMinimumSize(runButton.getMinimumSize());
        }
        
        buttonPanel.add(runButton);
                
        resetWorldAction = ResetWorldAction.getInstance();
        resetWorldAction.putValue(Action.LONG_DESCRIPTION, Config.getString("controls.reset.longDescription"));
        resetWorldAction.putValue(Action.SHORT_DESCRIPTION, Config.getString("controls.reset.shortDescription"));
        resetWorldAction.attachSimulation(simulation);
        resetWorldAction.setEnabled(false);
        AbstractButton resetButton = GreenfootUtil.createButton(resetWorldAction);
        buttonPanel.add(resetButton);
        
        return buttonPanel;
    }

    private JComponent createSpeedSlider()
    {
        JPanel speedPanel = new JPanel(new FlowLayout());
        JLabel speedLabel = new JLabel(Config.getString("controls.speed.label"));
        speedPanel.add(speedLabel);
        
        int min = 0;
        int max = Simulation.MAX_SIMULATION_SPEED;
        speedSlider = new JSlider(JSlider.HORIZONTAL, min, max, simulation.getSpeed());
        speedSlider.setPaintLabels(false);
        speedSlider.setMajorTickSpacing( max / 2);
        speedSlider.setMinorTickSpacing( max / 4);
        speedSlider.setPaintTicks(true);
        speedSlider.setEnabled(false);
        speedSlider.addChangeListener(this);
        speedSlider.setToolTipText(Config.getString("controls.speedSlider.tooltip"));
        speedSlider.setFocusable(false);
        speedPanel.add(speedSlider);
        
        return speedPanel;
    }
    
    @Override
    public void simulationChanged(SimulationEvent e)
    {        
        final int etype = e.getType();
        if (etype == SimulationEvent.DEBUGGER_PAUSED
              || etype == SimulationEvent.DEBUGGER_RESUMED) {
            // we don't care about these events here so we want to avoid
            // creating a new thread below.
            return;
        }

        SwingUtilities.invokeLater(new Thread() {
            public void run()
            {
                if (etype == SimulationEvent.STARTED) {
                    if (speedSlider != null) {
                        speedSlider.setEnabled(true);
                    }
                    buttonPanel.remove(runButton);
                    buttonPanel.add(pauseButton, 1);
                    buttonPanel.validate();
                }
                else if (etype == SimulationEvent.STOPPED) {
                    if (speedSlider != null) {
                        speedSlider.setEnabled(true);
                    }
                    buttonPanel.remove(pauseButton);
                    buttonPanel.add(runButton, 1);
                    buttonPanel.validate();
                }
                else if (etype == SimulationEvent.CHANGED_SPEED) {
                    if (speedSlider != null) {
                        speedSlider.setEnabled(true);
                        int newSpeed = simulation.getSpeed();
                        if (newSpeed != speedSlider.getValue()) {
                            speedSlider.setValue(newSpeed);
                        }
                    }
                }
                else if (etype == SimulationEvent.DISABLED) {
                    if (speedSlider != null) {
                        speedSlider.setEnabled(false);
                    }
                }
            }
        });
    }
    

    // ---------- ChangeListener interface (for speed slider changes) -----------
    
    public void stateChanged(ChangeEvent e)
    {
        simulation.setSpeed(speedSlider.getValue());
    }
}
