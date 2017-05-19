/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
import javafx.application.Platform;

/**
 * User chooses "close project". Save & close the current project. If the command
 * was issued from a menu, always keep the last window open, otherwise close
 * the window regardless.
 * 
 * @author Davin McCall
 * @version $Id: CloseProjectAction.java 16695 2016-10-07 11:34:53Z nccb $
 */
final public class CloseProjectAction extends PkgMgrAction
{
    public CloseProjectAction(PkgMgrFrame pmf)
    {
        super(pmf, "menu.package.close");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.doClose(true, true);
    }
}
