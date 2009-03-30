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
package greenfoot.actions;

import bluej.Config;
import greenfoot.core.GreenfootMain;

import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

/**
 * @author Bruce Quig
 * @version $Id: OpenProjectAction.java 4062 2006-05-02 09:38:55Z mik $
 */
public class OpenRecentProjectAction extends AbstractAction
{
    private static OpenRecentProjectAction instance = new OpenRecentProjectAction();
    
    /**
     * Singleton factory method for action.
     * @return singleton instance of the action for this VM
     */
    public static OpenRecentProjectAction getInstance()
    {
        return instance;
    }
    
    
    private OpenRecentProjectAction()
    {
        super(Config.getString("open.recentProject"));
    }
    
    public void actionPerformed(final ActionEvent e)
    {
        Object obj = e.getSource();
        if(obj instanceof JMenuItem){
            final JMenuItem item = (JMenuItem)obj;
            Thread t = new Thread(){
                public void run() {
                    try {
                        GreenfootMain.getInstance().openProject(item.getText());
                    }
                    catch(RemoteException ex){
                        ex.printStackTrace();
                    }
                }
            };
            t.start();
        }
    }
}