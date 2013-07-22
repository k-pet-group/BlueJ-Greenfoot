/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2013  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.Simulation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bluej.classmgr.BPClassLoader;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.DebuggerThreadTreeModel;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;

/**
 * A "local" debugger. This implements various parts of the Debugger interface, to allow
 * executing user code in the local VM. Some of the interface is not implemented however.
 * 
 * @author Davin McCall
 */
public class LocalDebugger extends Debugger
{
    @Override
    public void setUserLibraries(URL[] libraries)
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public int addDebuggerListener(DebuggerListener l)
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
    public DebuggerClass getClass(String className, boolean initialize)
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

    /**
     * A class to support running user code on the simulation thread.
     * 
     * @author Davin McCall
     */
    private static class QueuedInstantiation implements Runnable
    {
        private Class<?> c;
        private DebuggerResult result;
        
        public QueuedInstantiation(Class <?> c)
        {
            this.c = c;
        }
        
        public synchronized void run()
        {
            try {
                final Constructor<?> cons = c.getDeclaredConstructor(new Class[0]);
                cons.setAccessible(true);
                Object o = Simulation.newInstance(cons);
                result = new DebuggerResult(LocalObject.getLocalObject(o));
            }
            catch (InstantiationException ite) {
                Debug.reportError("LocalDebugger instantiateClass error", ite);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch (IllegalAccessException iae) {
                Debug.reportError("LocalDebugger instantiateClass error", iae);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch (NoSuchMethodException nsme) {
                Debug.reportError("LocalDebugger instantiateClass error", nsme);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch(InvocationTargetException ite) {
                ite.getCause().printStackTrace(System.err);
                ExceptionDescription exception = getExceptionDescription(ite.getCause());
                result = new DebuggerResult(exception);
            }
            catch (LinkageError le) {
                le.printStackTrace();
                result = new DebuggerResult(getExceptionDescription(le));
            }
            notify();
        }
        
        public synchronized DebuggerResult getResult()
        {
            while (result == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // should be safe to ignore
                }
            }
            return result;
        }
    }
    
    @Override
    public DebuggerResult instantiateClass(String className)
    {
        ClassLoader currentLoader = ExecServer.getCurrentClassLoader();
        
        try {
            Class<?> cl = currentLoader.loadClass(className);
            QueuedInstantiation q = new QueuedInstantiation(cl);
            Simulation.getInstance().runLater(q);
            return q.getResult();
        }
        catch (ClassNotFoundException cnfe) {
            Debug.reportError("Invoking constructor", cnfe);
        }
        catch (LinkageError le) {
            Debug.reportError("Invoking constructor", le);
        }
        return new DebuggerResult(new ExceptionDescription("Internal error"));
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

    /**
     * A class to support running user code on the simulation thread.
     * 
     * @author Davin McCall
     */
    private static class QueuedExecution implements Runnable
    {
        private Class<?> c;
        private DebuggerResult result;
        
        public QueuedExecution(Class <?> c)
        {
            this.c = c;
        }
        
        public synchronized void run()
        {
            try {
                Method m = c.getMethod("run", new Class[0]);
                Object result = m.invoke(null, new Object[0]);
                this.result = new DebuggerResult(LocalObject.getLocalObject(result));
            }
            catch (IllegalAccessException iae) {
                Debug.reportError("LocalDebugger runClassMain error", iae);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch (NoSuchMethodException nsme) {
                Debug.reportError("LocalDebugger runClassMain error", nsme);
                result = new DebuggerResult(new ExceptionDescription("Internal error"));
            }
            catch(InvocationTargetException ite) {
                ite.getCause().printStackTrace(System.err);
                ExceptionDescription exception = getExceptionDescription(ite.getCause());
                result = new DebuggerResult(exception);
            }
            notify();
        }
        
        public synchronized DebuggerResult getResult()
        {
            while (result == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // should be safe to ignore
                }
            }
            return result;
        }
    }
    
    @Override
    public DebuggerResult runClassMain(String className)
            throws ClassNotFoundException
    {
        ClassLoader currentLoader = ExecServer.getCurrentClassLoader();
        Class<?> c = currentLoader.loadClass(className);
        QueuedExecution qe = new QueuedExecution(c);
        Simulation.getInstance().runLater(qe);
        return qe.getResult();
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
    
    @Override
    public String toggleBreakpoint(DebuggerClass debuggerClass, String method,
            boolean set, Map<String, String> properties)
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Convert a Throwable into an ExceptionDescription.
     */
    private static ExceptionDescription getExceptionDescription(Throwable t)
    {
        List<SourceLocation> stack = new ArrayList<SourceLocation>();
        StackTraceElement [] stackTrace = t.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            stack.add(new SourceLocation(element.getClassName(), element.getFileName(),
                    element.getMethodName(), element.getLineNumber()));
        }
        new ExceptionDescription(t.getClass().getName(), t.getLocalizedMessage(), stack);
        return null;
    }
}
