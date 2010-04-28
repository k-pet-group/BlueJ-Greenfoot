/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.Window;

/**
 * A focus policy to override the initially focused component.
 * All other operations are delegated to the original policy.
 * 
 * @author Davin McCall
 * @version $Id: TeamPanelFocusPolicy.java 7421 2010-04-28 04:20:46Z davmac $
 */
public class TeamPanelFocusPolicy extends FocusTraversalPolicy
{
    private Component defaultComponent;
    private FocusTraversalPolicy delegate;
    
    public TeamPanelFocusPolicy(Component defaultComponent, FocusTraversalPolicy delegate)
    {
        super();
        this.defaultComponent = defaultComponent;
        this.delegate = delegate;
    }
    
    public Component getDefaultComponent(Container aContainer)
    {
        return delegate.getDefaultComponent(aContainer);
    }
    
    public Component getComponentAfter(Container aContainer, Component aComponent)
    {
        aContainer.setFocusTraversalPolicy(delegate);
        Component r = delegate.getComponentAfter(aContainer, aComponent);
        aContainer.setFocusTraversalPolicy(this);
        return r;
    }
    
    public Component getComponentBefore(Container aContainer, Component aComponent)
    {
        aContainer.setFocusTraversalPolicy(delegate);
        Component r = delegate.getComponentBefore(aContainer, aComponent);
        aContainer.setFocusTraversalPolicy(this);
        return r;
    }
        
    public Component getFirstComponent(Container aContainer)
    {
        aContainer.setFocusTraversalPolicy(delegate);
        Component r = delegate.getFirstComponent(aContainer);
        aContainer.setFocusTraversalPolicy(this);
        return r;
    }
    
    public Component getLastComponent(Container aContainer)
    {
        aContainer.setFocusTraversalPolicy(delegate);
        Component r = delegate.getLastComponent(aContainer);
        aContainer.setFocusTraversalPolicy(this);
        return r;
    }
    
    public Component getInitialComponent(Window window)
    {
        return defaultComponent;
    }
}
