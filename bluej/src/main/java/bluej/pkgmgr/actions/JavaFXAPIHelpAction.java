/*
 This file is part of the BlueJ program. 
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * JavaFX API help. Attempt to show the JavaFX API help in a web browser. The
 * URL is taken from bluej.defs configuration file.
 */
final public class JavaFXAPIHelpAction extends PkgMgrAction
{
    public JavaFXAPIHelpAction(PkgMgrFrame pmf)
    {
        super(pmf, "menu.help.javafxApi");
    }

    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.menuCall();
        pmf.showWebPage(Config.getPropString("url.javaFXLib"));
    }
}
