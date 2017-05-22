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
package bluej.debugmgr.objectbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.views.MethodView;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Simple action representing an interactive method invocation.
 *  
 * @author Davin McCall
 * @version $Id: InvokeAction.java 6215 2009-03-30 13:28:25Z polle $
 */
@OnThread(Tag.Swing)
public class InvokeAction extends AbstractAction
{
    @OnThread(Tag.Any)
    private final MethodView methodView;
    @OnThread(Tag.Any)
    private final InvokeListener invokeListener;
    
    /**
     * Constructor for an InvokeAction.
     * 
     * @param methodView   The method to be invoked
     * @param il           The listener to be notified
     * @param desc         The method description (as appearing on menu)
     */
    public InvokeAction(MethodView methodView, InvokeListener il, String desc)
    {
        super(desc);
        this.methodView = methodView;
        this.invokeListener = il;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
    {
        Platform.runLater(() -> invokeListener.executeMethod(methodView));
    }
}
