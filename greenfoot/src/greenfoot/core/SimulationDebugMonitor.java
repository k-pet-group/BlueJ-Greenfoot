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
package greenfoot.core;

/**
 * A dummy class that adjusts the details of the Run/Act/Pause actions when constructed.
 * It is solely designed for use by the GreenfootDebugHandler class,
 * which will construct an instance from the BlueJ VM when it wants
 * to enable/disable the actions.  Hence all the state is static, because a new
 * instance will be constructed each time.
 *
 */
public class SimulationDebugMonitor
{
    public final static Object RUNNING = new Object();
    public final static Object NOT_RUNNING = new Object();
    
    private static boolean isRunning = true;

    public SimulationDebugMonitor(Object running)
    {
        synchronized (RUNNING) {
            if (running == RUNNING && !isRunning) {
                Simulation.getInstance().notifyThreadStatus(false);
                isRunning = true;
            } else if (running == NOT_RUNNING && isRunning) {
                Simulation.getInstance().notifyThreadStatus(true);
                isRunning = false;
            }
        }
    }
}
