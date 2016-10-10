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
package bluej.groupwork.actions;

import javafx.application.Platform;

import bluej.Config;
import bluej.groupwork.ui.HistoryFrame;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * An action to show the repository history.
 * 
 * @author Davin McCall
 * @version $Id: ShowLogAction.java 16722 2016-10-10 16:33:55Z nccb $
 */
public class ShowLogAction extends TeamAction
{
    public ShowLogAction(PkgMgrFrame pmf)
    {
        super(pmf, Config.getString("team.history"), false);
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        HistoryFrame hd = new HistoryFrame(pmf);
        Platform.runLater(() -> hd.setLocationRelativeTo(pmf.getFXWindow()));
        hd.setVisible(true);
    }

}
