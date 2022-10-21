/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2016  Michael Kolling and John Rosenberg
 
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
package bluej.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.debugmgr.codepad.DeclaredVar;

/**
 * An object bench implementation for test purposes.
 * 
 * @author Davin McCall
 */
public class TestObjectBench implements ValueCollection
{
    Map<String,NamedValue> varMap = new HashMap<String,NamedValue>();
    
    public void addDeclaredVars(List<DeclaredVar> vars)
    {
        for (final DeclaredVar var : vars) {
            varMap.put(var.getName(), var);
        }
    }
    
    @Override
    public Iterator<? extends NamedValue> getValueIterator()
    {
        return varMap.values().iterator();
    }

    @Override
    public NamedValue getNamedValue(String name)
    {
        return varMap.get(name);
    }

}
