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

import javax.swing.SwingUtilities;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Cancel recording of a test method. Also removes from the bench objects which
 * were created since recording began.
 * 
 * @author Davin McCall
 * @version $Id: CancelTestRecordAction.java 16606 2016-09-27 12:30:05Z nccb $
 */
final public class CancelTestRecordAction extends PkgMgrAction
{
    @OnThread(Tag.Any)
    public CancelTestRecordAction(PkgMgrFrame pmf)
    {
        super(pmf, "menu.tools.cancel");
        shortDescription = Config.getString("tooltip.test.cancel");
    }
    
    public void actionPerformed(PkgMgrFrame pmf)
    {
        pmf.doCancelTest();
    }
}
