/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.util.EventListener;

import bluej.testmgr.record.InvokerRecord;

/**
 * The listener for PackageEditor events.
 *
 * @author  Andrew Patterson
 */
public interface PackageEditorListener extends EventListener
{
    void targetEvent(PackageEditorEvent e);
    
    void recordInteraction(InvokerRecord ir);
    
    /**
     * The package editor received focus
     */
    void pkgEditorGotFocus();
    
    /**
     * The package editor lost permanent focus
     */
    void pkgEditorLostFocus();
}
