/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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
import java.util.Map;

import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;

/**
 * Implementation of ValueCollection for testing purposes.
 * 
 * @author Davin McCall
 */
public class TestValueCollection implements ValueCollection
{
    private Map<String,NamedValue> namedValues = new HashMap<String,NamedValue>();
    
    final class TestVar implements NamedValue
    {
        String name;
        boolean finalVar;
        boolean initialized = false;
        JavaType type;
        
        public TestVar(String name, JavaType type, boolean finalVar)
        {
            this.name = name;
            this.finalVar = finalVar;
            this.type = type;
        }
        
        public String getName()
        {
            return name;
        }
        
        public JavaType getGenType()
        {
            return type;
        }
        
        public boolean isFinal()
        {
            return finalVar;
        }
        
        public boolean isInitialized()
        {
            return initialized;
        }
        
        public void setInitialized()
        {
            initialized = true;
        }
    }
    
    public void addVariable(String name, JavaType type, boolean initialized, boolean isFinal)
    {
        TestVar nv = new TestVar(name, type, isFinal);
        if (initialized) {
            nv.setInitialized();
        }
        namedValues.put(name, nv);
    }
    
    @Override
    public Iterator<? extends NamedValue> getValueIterator()
    {
        return namedValues.values().iterator();
    }

    @Override
    public NamedValue getNamedValue(String name)
    {
        return namedValues.get(name);
    }
}
