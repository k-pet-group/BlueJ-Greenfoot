/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010 Poul Henriksen and Michael Kolling 
 
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

/**
 * A dummy class that adjusts the details of the Run/Act/Pause actions when constructed.
 * It is solely designed for use by the GreenfootDebugHandler class,
 * which will construct an instance from the BlueJ VM when it wants
 * to enable/disable the actions.  Hence all the state is static, because a new
 * instance will be constructed each time.
 *
 */
public class RunActionsAdjuster
{
    public final static Object RUNNING = new Object();
    public final static Object NOT_RUNNING = new Object();
    
    private static boolean runEnabled;
    private static boolean runOnceEnabled;
    private static boolean pauseEnabled;
    
    private static boolean isRunning = true; // Refers to thread, not simulation
    
    public RunActionsAdjuster(Object running)
    {
        synchronized (RUNNING) {
            if (NOT_RUNNING == running && isRunning) {
                runEnabled = RunSimulationAction.getInstance().isEnabled();
                runOnceEnabled = RunOnceSimulationAction.getInstance().isEnabled();
                pauseEnabled = PauseSimulationAction.getInstance().isEnabled();
                isRunning = false;
                
                RunSimulationAction.getInstance().setEnabled(false);
                RunOnceSimulationAction.getInstance().setEnabled(false);
                PauseSimulationAction.getInstance().setEnabled(false);
            } else if (RUNNING == running && !isRunning) {
                isRunning = true;
                RunSimulationAction.getInstance().setEnabled(runEnabled);
                RunOnceSimulationAction.getInstance().setEnabled(runOnceEnabled);
                PauseSimulationAction.getInstance().setEnabled(pauseEnabled);
            }
        }
    }
}
