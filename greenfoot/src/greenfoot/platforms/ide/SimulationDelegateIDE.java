/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
