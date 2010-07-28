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
package bluej.runtime;

/**
 * Interface implemented by all "shell" classes.
 *
 * The src for each "shell" class is constructed automatically,
 * compiled and executed. They are used for method invocation
 * and object creation.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 */
public abstract class Shell
{
    /**
     * A dummy method called by class loader to prepare the class
     * after loading.
     */
    public static void prepare()
    {
    }

    /**
     * Provide the shell class with static access to the object
     * bench scopes.
     */
    protected static BJMap<String,Object> getScope(String scopeId)
    {
        return ExecServer.getScope(scopeId);
    }

    /**
     * Put an object into the scope of one of the object benches.
     */
    protected static void putObject(String scopeId, String instanceName, Object value)
    {
        ExecServer.addObject(scopeId, instanceName, value);
    }

    /**
     * Construct an object that allows the result to be plucked out by
     * the debugger. We do this so that primitive types can be encapsulated
     * properly; if we boxed them, the debugger wouldn't know if the result
     * was really primitive or not.
     */
    public static Object makeObj(final Object o)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public Object result = o;
        };
    }

    protected static Object makeObj(final String s)
    {
        return new Object() {
           @SuppressWarnings("unused")
           public String result = s;
        };
    }

    protected static Object makeObj(final boolean b)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public boolean result = b;
        };
    }

    protected static Object makeObj(final byte b)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public byte result = b;
        };
    }

    protected static Object makeObj(final char c)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public char result = c;
        };
    }

    protected static Object makeObj(final double d) {
        return new Object() {
            @SuppressWarnings("unused")
            public double result = d;
        };
    }

    protected static Object makeObj(final float f)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public float result = f;
        };
    }

    protected static Object makeObj(final int i)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public int result = i;
        };
    }

    protected static Object makeObj(final long l)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public long result = l;
        };
    }

    protected static Object makeObj(final short s)
    {
        return new Object() {
            @SuppressWarnings("unused")
            public short result = s;
        };
    }
}
