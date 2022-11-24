/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009, 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import bluej.utility.Debug;

import java.util.HashMap;
import java.util.Map;

/**
 * A class which resides on the debug VM.  Its job is to hold copies of the project
 * properties, mirrored across from the server VM.  It implements ReadOnlyProjectProperties
 * because it is passed to other classes as if it is read-only, with changes only being
 * possible via the propertyChangedOnServerVM method in this class; no debug VM code
 * other than this can change a property.
 */
public class ShadowProjectProperties implements ReadOnlyProjectProperties
{
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Called when a property has changed on the server VM, and the change needs
     * to be inserted into our shadow copy of the properties
     */
    public void propertyChangedOnServerVM(String key, String value)
    {
        if (value == null)
        {
            properties.remove(key);
        }
        else
        {
            properties.put(key, value);
        }
    }
    
    @Override
    public String getString(String key, String defaultValue)
    {
        return properties.getOrDefault(key, defaultValue);
    }
}
