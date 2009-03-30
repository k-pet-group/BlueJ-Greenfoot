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
package bluej.pkgmgr.actions;

import bluej.pkgmgr.PkgMgrFrame;

/**
 * Java ME Project action. User chooses "create new ME project". This prompts for a
 * project name, creates the directory, and displays the new project in a new window.
 * 
 * @author Cecilia Vargas (based on Davin McCall's NewProjectAction)
 * @version $Id: NewProjectAction.java 2505 2004-04-21 01:50:28Z davmac $
 */

final public class NewMEprojectAction extends PkgMgrAction {
    
    static private NewMEprojectAction instance = null;
    
    private NewMEprojectAction()  { super("menu.mepackage.new"); }
    
    /**
     * Factory method to retrieve an instance of the class as constructor is private.
     * @return an instance of the class.
     */
    static public NewMEprojectAction getInstance()
    {
        if(instance == null)
            instance = new NewMEprojectAction();
        return instance;
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doNewProject( true );  //pass true because we are creating an ME project
    }                        
}