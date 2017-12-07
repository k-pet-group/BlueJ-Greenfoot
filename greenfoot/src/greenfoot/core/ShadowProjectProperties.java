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
 * properties, mirrored across from the server VM.
 * 
 * Note that at the moment, this appears to be a writeable item, with methods to set
 * properties.  This is only temporary, as it lets us keep in all the existing setString
 * etc methods to see where these setters are when we transition code across to the
 * server VM.  Once all the Greenfoot FX rewrite has been completed, there should be no
 * code left on the debug VM attempting to call the setters.  At that point, this class
 * can implement ReadOnlyProjectProperties and we can remove the ProjectProperties interface
 * entirely, and remove all the dummy setters from this class.
 */
public class ShadowProjectProperties implements ProjectProperties
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
    public void setString(String key, String value)
    {
        // This method is retained as a marker for code that needs changing.
        // All current debug VM uses of setString should end up on the server VM
        // At that point, this method should be removed.

        // Once removeProperty, setString and save have been removed,
        // ShadowProjectProperties should be changed to implement
        // ReadOnlyProjectProperties, and the ProjectProperties interface
        // should be deleted, too.
        Debug.printCallStack("WARNING: still using outdated method which is due for removal after finishing Greenfoot FX rewrite");
    }

    @Override
    public String removeProperty(String key)
    {
        // This method is retained as a marker for code that needs changing.
        // All current debug VM uses of setString should end up on the server VM
        // At that point, this method should be removed.

        // Once removeProperty, setString and save have been removed,
        // ShadowProjectProperties should be changed to implement
        // ReadOnlyProjectProperties, and the ProjectProperties interface
        // should be deleted, too.
        Debug.printCallStack("WARNING: still using outdated method which is due for removal after finishing Greenfoot FX rewrite");
        
        return null;
    }

    @Override
    public void save()
    {
        // This method is retained as a marker for code that needs changing.
        // All current debug VM uses of setString should end up on the server VM
        // At that point, this method should be removed.

        // Once removeProperty, setString and save have been removed,
        // ShadowProjectProperties should be changed to implement
        // ReadOnlyProjectProperties, and the ProjectProperties interface
        // should be deleted, too.
        Debug.printCallStack("WARNING: still using outdated method which is due for removal after finishing Greenfoot FX rewrite");
    }

    @Override
    public String getString(String key, String defaultValue)
    {
        return properties.getOrDefault(key, defaultValue);
    }
}
