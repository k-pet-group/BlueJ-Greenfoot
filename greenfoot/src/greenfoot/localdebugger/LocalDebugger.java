/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.localdebugger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import bluej.classmgr.BPClassLoader;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.DebuggerThreadTreeModel;
import bluej.runtime.ExecServer;

// DAV comment
public class LocalDebugger extends Debugger
{

    @Override
    public void addDebuggerListener(DebuggerListener l)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addObject(String scopeId, String newInstanceName,
            DebuggerObject dob)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(boolean restart)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disposeWindows()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerClass getClass(String className)
            throws ClassNotFoundException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerObject getMirror(String value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, DebuggerObject> getObjects()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerObject getStaticValue(String className, String fieldName)
            throws ClassNotFoundException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerThreadTreeModel getThreadTreeModel()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String guessNewName(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String guessNewName(DebuggerObject obj)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void hideSystemThreads(boolean hide)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerResult instantiateClass(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerResult instantiateClass(String className,
            String[] paramTypes, DebuggerObject[] args)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void launch()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void newClassLoader(BPClassLoader bpClassLoader)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeBreakpointsForClass(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDebuggerListener(DebuggerListener l)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeObject(String scopeId, String instanceName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DebuggerResult runClassMain(String className)
            throws ClassNotFoundException
    {
        ClassLoader currentLoader = ExecServer.getCurrentClassLoader();
        Class<?> c = currentLoader.loadClass(className);
        try {
            Method m = c.getMethod("run", new Class[0]);
            Object result = m.invoke(null, new Object[0]);
            LocalObject resultObject = wrapResult(result, m.getReturnType());
            return new DebuggerResult(resultObject);
        }
        catch (NoSuchMethodException nsme) {
            return null; // DAV
        }
        catch (IllegalAccessException iae) {
            return null; // DAV
        }
        catch(InvocationTargetException ite) {
            //throw ite.getCause();
            return null; // DAV
        }
    }

    @Override
    public DebuggerTestResult runTestMethod(String className, String methodName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, DebuggerObject> runTestSetUp(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toggleBreakpoint(String className, int line, boolean set,
            Map<String, String> properties)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toggleBreakpoint(String className, String method,
            boolean set, Map<String, String> properties)
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Wrap a value, that is the result of a method call, in a form that the
     * ResultInspector can understand.<p>
     * 
     * Also ensure that if the result is a primitive type it is correctly
     * unwrapped.
     * 
     * @param r  The result value
     * @param c  The result type
     * @return   A DebuggerObject which wraps the result
     */
    private static LocalObject wrapResult(final Object r, Class<?> c)
    {
        Object wrapped;
        if (c == boolean.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public boolean result = ((Boolean) r).booleanValue();
            };
        }
        else if (c == byte.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public byte result = ((Byte) r).byteValue();
            };
        }
        else if (c == char.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public char result = ((Character) r).charValue();
            };
        }
        else if (c == short.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public short result = ((Short) r).shortValue();
            };
        }
        else if (c == int.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public int result = ((Integer) r).intValue();
            };
        }
        else if (c == long.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public long result = ((Long) r).longValue();
            };
        }
        else if (c == float.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public float result = ((Float) r).floatValue();
            };
        }
        else if (c == double.class) {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public double result = ((Double) r).doubleValue();
            };
        }
        else {
            wrapped = new Object() {
                @SuppressWarnings("unused")
                public Object result = r;
            };
        }
        return LocalObject.getLocalObject(wrapped);
    }
}
