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

import greenfoot.util.GreenfootUtil;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

/**
 * Action to open the Greenfoot API documentation in an external web browser.
 *
 * @author Poul Henriksen
 */
public class ShowApiDocAction extends AbstractAction {
    private String page = "index.html";
    
    /**
     * Will show the index.html of the API Documentation
     * @param name
     */
    public ShowApiDocAction(String name)
    {
        super(name);
    }
    

    /**
     * Opens the given page of the Greenfoot API documentation in a web browser.
     * @param page name of the page relative to the root of the API doc.
     * @throws IOException If the greenfoot directory can not be read
     */
    public ShowApiDocAction(String name, String page)
    {
        super(name);
        this.page = page;
    }

    public void actionPerformed(ActionEvent e)
    {
        try {
            GreenfootUtil.showApiDoc(page);
            // TODO: show status message: browser opened
        }
        catch (IOException e1) {
            // TODO: show status message: problem opening  
        }   
       
    }
}
