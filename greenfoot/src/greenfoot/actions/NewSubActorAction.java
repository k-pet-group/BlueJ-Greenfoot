/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2014,2015,2016  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.gui.GreenfootFrame;
import greenfoot.record.InteractionListener;

import java.awt.event.ActionEvent;


/**
 * Action that creates a new class as a subclass of an Actor class
 */
public class NewSubActorAction extends NewSubclassAction
{
    private final boolean wizard;
    private GreenfootFrame gfFrame;


    /**
     * Creates a new subclass of the Actor class
     * 
     * @param gfFrame 
     *            Greenfoot main frame
     * 
     * @param interactionListener
     *            The listener to be notified of interactions (instance creation, method calls)
     *            which occur on the new class.
     */
    public NewSubActorAction(GreenfootFrame gfFrame, boolean wizard, InteractionListener interactionListener)
    {
        super();
        this.gfFrame = gfFrame;
        this.wizard = wizard;
        this.interactionListener = interactionListener;
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        this.classBrowser = gfFrame.getClassBrowser();
        this.superclass = classBrowser.getActorClassView();
        createImageClass((String)getValue(NAME), wizard ? "MyActor" : null, null);
    }
}