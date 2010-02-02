/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

/**
 * A cache for class comments (javadoc/parameter names). Adding new entries to the cache
 * will purge old entries, if there are too many.
 * 
 * @author Davin McCall
 */
public class CommentCache
{
    private Map<String,Properties> cache = new HashMap<String,Properties>();
    private List<String> targets = new LinkedList<String>();
    
    public void put(String target, Properties comments)
    {
        cache.put(target, comments);
        targets.add(target);
        if (targets.size() > 20) {
            String removed = targets.remove(0);
            cache.remove(removed);
        }
    }
    
    public Properties get(String target)
    {
        Properties props = cache.get(target);
        if (props != null) {
            // Move the entry up in the list
            for (ListIterator<String> i = targets.listIterator(); ; ) {
                String tcheck = i.next();
                if (tcheck.equals(target)) {
                    i.remove();
                    if (i.hasPrevious())
                        i.previous();
                    if (i.hasPrevious())
                        i.previous();
                    i.add(tcheck);
                    break;
                }
            }
        }
        return props;
    }
}
