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
package bluej.debugmgr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.debugmgr.objectbench.InvokeListener;
import bluej.views.ConstructorView;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Simple action to construct an object.
 * 
 * @author Davin McCall
 * @version $Id$
 */
@OnThread(Tag.Swing)
public class ConstructAction extends AbstractAction
{
    @OnThread(Tag.Any)
    private final ConstructorView constructor;
    @OnThread(Tag.Any)
    private final InvokeListener invokeListener;
    
    public ConstructAction(ConstructorView cv, InvokeListener il, String desc)
    {
        super(desc);
        constructor = cv;
        invokeListener = il;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        Platform.runLater(() -> invokeListener.callConstructor(constructor));
    }

}
