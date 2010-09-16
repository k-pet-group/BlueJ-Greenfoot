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
package greenfoot.gui;

import greenfoot.platforms.ide.WorldHandlerDelegateIDE;

import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This is a subclass of CenterLayout but which works for the IDE, not the
 * stand alone. It'll allow right clicking of the border/background
 * around the World Canvas to enable right clicking to bring up world
 * methods, inspection and "Save the world" options.
 *
 * @author Philip Stevens
 * @version 0.1
 */
public class IDECenterLayout extends CenterLayout
{
	private WorldHandlerDelegateIDE worldHandlerDelegate;
	
    /**
     * Creates a new instance of IDECenterLayout.
     */
    public IDECenterLayout(WorldHandlerDelegateIDE worldHandlerDelegate)
    {
        this.worldHandlerDelegate = worldHandlerDelegate;
    }
    
    /**
     * Layout the target container.
     */
    public void layoutContainer(Container target)
    {
        super.layoutContainer(target);
        if (worldHandlerDelegate != null) {
        	target.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						if (e.isPopupTrigger()) {
							worldHandlerDelegate.showWorldPopupMenu(e);
						}
						
					}
					@Override
					public void mouseReleased(MouseEvent e) {
						if (e.isPopupTrigger()) {
							worldHandlerDelegate.showWorldPopupMenu(e);
						}
					}
	        });
        }
    }
}
