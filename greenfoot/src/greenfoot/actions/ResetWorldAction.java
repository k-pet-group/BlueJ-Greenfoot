/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009, 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.actions;

import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import bluej.Config;
import bluej.utility.Debug;

public class ResetWorldAction extends AbstractAction implements SimulationListener
{

    private Simulation simulation;
    private static ResetWorldAction instance = new ResetWorldAction();

    private static final String iconFile = "reset.png";
    
    /**
     * Singleton factory method for action.
     */
    public static ResetWorldAction getInstance()
    {
        return instance;
    }
    
    private ResetWorldAction()
    {
        super(Config.getString("reset.world"), new ImageIcon(ResetWorldAction.class.getClassLoader().getResource(iconFile)));
    }

    /**
     * Attach this action to a simulation object that it controls.
     */
    public void attachSimulation(Simulation simulation)
    {
        this.simulation = simulation;
        simulation.addSimulationListener(this);
    }
    
    /**
     * If the simulation is not null, it disables the simulation, discards the world
     * and instantiates a new world
     */
    public void actionPerformed(ActionEvent e)
    {
        if(simulation == null) 
        {
            Debug.reportError("attempt to reset a simulation while none exists.");
        }
        else {
            resetWorld();
        }
    }
    
    // Identify this method so that the debugger can notice when the user has clicked reset:
    public static final String RESET_WORLD = "resetWorld";    
    private void resetWorld()
    {
        simulation.setEnabled(false);
        WorldHandler.getInstance().discardWorld();
        WorldHandler.getInstance().instantiateNewWorld();
    }

    /**
     * Observing for the simulation state so we can dis/en-able us appropiately
     */
    public void simulationChanged(final SimulationEvent e)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                int eventType = e.getType();
                if (eventType == SimulationEvent.STOPPED) {
                    setEnabled(true);
                }
                else if (eventType == SimulationEvent.STARTED) {
                    setEnabled(true);
                }
                else if (eventType == SimulationEvent.DISABLED) {
                    setEnabled(false);
                }
            }
        });
    }

}
