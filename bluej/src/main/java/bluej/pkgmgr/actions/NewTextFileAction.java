/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017,2025  Michael Kolling and John Rosenberg

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
 * "New Text File" command. Allows the user to create a new plain file with a
 * specified name and extension.
 * 
 * @author Davin McCall
 */
final public class NewTextFileAction extends PkgMgrAction
{
    public NewTextFileAction(PkgMgrFrame pmf)
    {
        super(pmf, "menu.edit.newText");
    }

    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doCreateNewTextFile(-1, -1);
    }
}
