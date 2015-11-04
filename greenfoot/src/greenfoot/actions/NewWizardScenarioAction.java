/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2014,2015  Poul Henriksen and Michael Kolling
 
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

import bluej.Config;
import bluej.extensions.SourceType;
import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Creates a new Scenario add subclasses of the World and Actor classes
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class NewWizardScenarioAction extends AbstractAction
{
    SourceType sourceType;
    public NewWizardScenarioAction(SourceType sourceType)
    {
        super(Config.getString(sourceType.name().toLowerCase() + ".new.project"));
        this.sourceType = sourceType;
    }
   
    @Override
    public void actionPerformed(ActionEvent e)
    {
       GreenfootMain.getInstance().newProject(true, sourceType);
    }
}