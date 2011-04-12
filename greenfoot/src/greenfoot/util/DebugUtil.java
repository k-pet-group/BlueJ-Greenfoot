/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

import greenfoot.Actor;
import greenfoot.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class has some helper methods for the debugger in Greenfoot; 
 * currently, this is information on what fields to hide/show
 * when looking at objects (via the inspector or debugger).
 * 
 * This can be used to hide fields from Actor and World when looking at subclasses of them.
 *
 */
public class DebugUtil
{
    public static Map<Class<?>, List<String>> restrictedClasses()
    {
        return restricted;
    }
    
    /**
     * As restrictedClasses, but uses Strings (fully qualified class names)
     * as the keys rather than Class objects.
     */
    public static Map<String, Set<String>> restrictedClassesAsNames()
    {
        return restrictedAsNames;
    }
    
    private static final String[] actorIncludeFields = new String[]{"x", "y", "rotation", "image", "world"};
    private static final String[] worldIncludeFields = new String[]{"width", "height", "cellSize", "backgroundImage"};
    
    // Make these static to avoid reallocating the map every time it is asked for:
    private static final HashMap<Class<?>, List<String>> restricted;
    private static final HashMap<String, Set<String>> restrictedAsNames;
    static 
    {
        restricted = new HashMap<Class<?>, List<String>>();
        restricted.put(Actor.class, Arrays.asList(actorIncludeFields));
        restricted.put(World.class, Arrays.asList(worldIncludeFields));
        
        restrictedAsNames = new HashMap<String, Set<String>>();
        for (Entry<Class<?>, List<String>> e : restricted.entrySet()) {
            HashSet<String> values = new HashSet<String>();
            values.addAll(e.getValue());
            restrictedAsNames.put(e.getKey().getName(), values);
        }
    }
}
