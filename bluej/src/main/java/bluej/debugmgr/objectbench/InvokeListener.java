/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * Listener interface for some object to be notified when a method is to be
 * interactively invoked.
 * 
 * @author Davin McCall
 */
public interface InvokeListener
{
    /**
     * Execute a method. The listener must prompt for parameters, if appropriate,
     * and then actually execute the method.
     */
    void executeMethod(MethodView mv);
    
    /**
     * Execute a constructor. The listener must prompt for parameters, if appropriate,
     * and the actually execute the constructor.
     * @param cv
     */
    void callConstructor(ConstructorView cv);
}
