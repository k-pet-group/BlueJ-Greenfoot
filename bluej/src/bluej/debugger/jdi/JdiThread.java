/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import java.util.*;

import bluej.Config;
import bluej.debugger.*;
import bluej.debugger.gentype.JavaType;
import bluej.utility.Debug;

import com.sun.jdi.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

/**
 * This class represents a thread running on the remote virtual machine.
 *
 * @author  Michael Kolling
 */
class JdiThread extends DebuggerThread
{
    static final String statusFinished = Config.getString("debugger.threadstatus.finished");
    static final String statusBreakpoint = Config.getString("debugger.threadstatus.breakpoint");
    static final String statusStopped = Config.getString("debugger.threadstatus.stopped");
    static final String statusMonitor = Config.getString("debugger.threadstatus.monitor");
    static final String statusNotStarted = Config.getString("debugger.threadstatus.notstarted");
    static final String statusRunning = Config.getString("debugger.threadstatus.running");
    static final String statusSleeping = Config.getString("debugger.threadstatus.sleeping");
    static final String statusUnknown = Config.getString("debugger.threadstatus.unknown");
    static final String statusWaiting = Config.getString("debugger.threadstatus.waiting");
    static final String statusZombie = Config.getString("debugger.threadstatus.zombie");

    /** a list of classes to exclude from source display */
    private static List<String> excludes;

    private static List<String> getExcludes()
    {
        if (excludes == null) {
            setExcludes("java.*, javax.*, sun.*, com.sun.*");
        }
        return excludes;
    }

    private static void setExcludes(String excludeString)
    {
        StringTokenizer t = new StringTokenizer(excludeString, " ,;");
        List<String> list = new ArrayList<String>();
        while (t.hasMoreTokens()) {
            list.add(t.nextToken());
        }
        excludes = list;
    }

    static void addExcludesToRequest(StepRequest request)
    {
        Iterator<String> iter = getExcludes().iterator();
        while (iter.hasNext()) {
            String pattern = iter.next();
            request.addClassExclusionFilter(pattern);
        }
    }

    /** the reference to the remote thread */
    private ThreadReference rt;
    
    /** We track suspension status internally */
    private boolean isSuspended;
    
    /*
     * Note that we have to track suspension status internally, because JDI will happily
     * tell us that a thread is suspended when, in fact, it is suspended only because of some
     * VM event which suspends every thread (ThreadStart / ThreadDeath).
     */
    
    // stores a stack frame that was selected for this
    // thread (selection is done for debugging)
    private int selectedFrame;
   
    private EventRequestManager eventReqMgr;
    
    private JdiDebugger debugger;

    // ---- instance: ----

    public JdiThread(JdiDebugger debugger, ThreadReference rt)
    {
        this.rt = rt;
        this.debugger = debugger;

        selectedFrame = 0;      // unless specified otherwise, assume we want
                                //  to see the top level frame
    }

    /** 
     * Return the name of this thread.
     */
    public String getName()
    {
        String name = null;
        try {
            name = rt.name();
        }
        catch(Exception e) {
            // ignore it
        }
        return name;
    }

    /** 
     * Return the reference to the thread object in the remote machine.
     */
    ThreadReference getRemoteThread()
    {
        return rt;
    }

    /** 
     * Return the current status of this thread.
     */
    public String getStatus()
    {
        try {
            if (rt.isAtBreakpoint()) {
                if(VMReference.isAtMainBreakpoint(rt)) {
                    return statusFinished;
                }
                else {
                    return statusBreakpoint;
                }
            }
                        
            if(rt.isSuspended()) {
                return statusStopped;
            }

            int status = rt.status();
            switch(status) {
             case ThreadReference.THREAD_STATUS_MONITOR:
                return statusMonitor;
             case ThreadReference.THREAD_STATUS_NOT_STARTED:
                return statusNotStarted;
             case ThreadReference.THREAD_STATUS_RUNNING:
                return statusRunning;
             case ThreadReference.THREAD_STATUS_SLEEPING:
                return statusSleeping;
             case ThreadReference.THREAD_STATUS_UNKNOWN:
                return statusUnknown;
             case ThreadReference.THREAD_STATUS_WAIT:
                return statusWaiting;
             case ThreadReference.THREAD_STATUS_ZOMBIE:
                return statusZombie;
            }
        }
        catch(Exception e) {
            return "???";
        }
        return null;
    }

    /**
     * Return true if this is a user thread that is in idle state
     * (finished).
     */
    public boolean isFinished()
    {
        try {
            return  rt.isAtBreakpoint() && VMReference.isAtMainBreakpoint(rt);
        }
        catch (VMDisconnectedException vmde) {
            return true;
        }
    }

    /**
     * Return true if this thread is currently suspended.
     */
    public boolean isSuspended()
    {
        return isSuspended;
    }

    /** 
     * Return true if this thread is currently at a breakpoint.
     */
    public boolean isAtBreakpoint()
    {
        return rt.isAtBreakpoint();
    }

    /** 
     * Return the class this thread was executing in when the
     * specified stack frame was active.
     */
    public String getClass(int frameNo)
    {
        try {
            return rt.frame(frameNo).location().declaringType().name();
        }
        catch(Exception e) {
            return "<error finding type at frame " + frameNo +">";
        }
    }

    /** 
     * Return the source name of the class this thread was 
     * executing in when the specified stack frame was active.
     */
    public String getClassSourceName(int frameNo)
    {
        try {
            return rt.frame(frameNo).location().sourceName();
        }
        catch(Exception e) {
            return "<no source at frame no " + frameNo +">";
        }
    }

    /** 
     * Return the line number in the source where this thread was 
     * executing when the specified stack frame was active.
     */
    public int getLineNumber(int frameNo)
    {
        try {
            return rt.frame(frameNo).location().lineNumber();
        }
        catch(Exception e) {
            return 1;
        }
    }

    // name of the threadgroup that contains user threads
    static final String MAIN_THREADGROUP = "main";

    public boolean isKnownSystemThread()
    {
        // A finished thread will have a null thread group.
        try {
            ThreadGroupReference tgr = rt.threadGroup();
            if(tgr == null || ! tgr.name().equals(MAIN_THREADGROUP)) {
                return true;
            }

            String name = rt.name();
            if(name.startsWith("AWT-") ||
                    name.equals("DestroyJavaVM") ||
                    name.equals("BlueJ worker thread") ||
                    name.equals("Timer Queue") ||
                    name.equals("Screen Updater") ||
                    name.startsWith("SunToolkit.") ||
                    name.startsWith("Native Carbon") ||
                    name.equals("Java2D Disposer")) {
                return true;
            }

            return false;
        }
        catch (VMDisconnectedException vmde) {
            return false;
        }
        catch (ObjectCollectedException oce) {
            return true;
        }
    }

    /**
     * Get strings showing the current stack frames. Ignore everything
     * including the __SHELL class and below.
     *
     * <p>The thread must be suspended to do this. Otherwise an empty list
     * is returned.
     *
     * @return  A List of SourceLocations
     */
    public List<SourceLocation> getStack()
    {
        return getStack(rt);
    }

    /**
     * Get strings showing the current stack frames. Ignore everything
     * including the __SHELL class and below.
     *
     * <p>The thread must be suspended to do this. Otherwise an empty list
     * is returned.
     * 
     * @return  A List of SourceLocations
     */
    public static List<SourceLocation> getStack(ThreadReference thr)
    {
        try {
            if(thr.isSuspended()) {
                List<SourceLocation> stack = new ArrayList<SourceLocation>();
                List<StackFrame> frames = thr.frames();

                for(int i = 0; i < frames.size(); i++) {
                    StackFrame f = (StackFrame)frames.get(i);
                    Location loc = f.location();
                    String className = loc.declaringType().name();
                    
                    String fileName = null;
                    try {
                        fileName = loc.sourceName();
                    }
                    catch(AbsentInformationException e) { }
                    String methodName = loc.method().name();
                    int lineNumber = loc.lineNumber();

                    stack.add(new SourceLocation(className, fileName,
                                                 methodName, lineNumber));
                }
                return stack;
            }
        }
        catch (VMDisconnectedException vmde) {
            // Finished, no trace
        }
        catch(IncompatibleThreadStateException e) {
            // this is possible if the thread state changes after
            // our check for its suspended state
            // lets just return an empty List
        }
        catch(InvalidStackFrameException isfe) {
            // same here
        }
        return new ArrayList<SourceLocation>();
    }


    /**
     * Return strings listing the local variables.
     *
     * The thread must be suspended to do this. Otherwise an empty List
     * is returned.
     */
    public List<String> getLocalVariables(int frameNo)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                List<String> localVars = new ArrayList<String>();
                
                // To work around a JDI bug (probably related to the other one described
                // below) we collect information we need about the variables on the
                // stack frame before we do anything which might cause types to be
                // loaded:
                
                List<String> localVals = new ArrayList<String>();
                List<Type> localTypes = new ArrayList<Type>();
                List<String> genericSigs = new ArrayList<String>();
                List<String> typeNames = new ArrayList<String>();
                ReferenceType declaringType = frame.location().declaringType();
                
                for(int i = 0; i < vars.size(); i++) {
                    LocalVariable var = vars.get(i);
                    String val = JdiUtils.getJdiUtils().getValueString(frame.getValue(var));
                    localVals.add(val);
                    
                    try {
                        localTypes.add(var.type());
                    }
                    catch (ClassNotLoadedException cnle) {
                        localTypes.add(null);
                    }
                    
                    genericSigs.add(var.genericSignature());
                    typeNames.add(var.typeName());
                }
                
                for(int i = 0; i < vars.size(); i++) {
                    LocalVariable var = vars.get(i);

                    // Add "type name = value" to the list
                    JavaType vartype = JdiReflective.fromLocalVar(localTypes.get(i), genericSigs.get(i),
                            typeNames.get(i), declaringType);
                    localVars.add(vartype.toString(true) + " " + var.name()
                            + " = " + localVals.get(i));
                }
                return localVars;
            }
        }
        catch (IncompatibleThreadStateException itse) { }
        catch (AbsentInformationException ase) { }
        catch (VMDisconnectedException vmde) { }
        catch (InvalidStackFrameException e) {
            // This shouldn't happen, as we've checked the thread status, 
            // but it does, apparently; seems like a JDK bug.
            // Probably related to: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6644945
            // Occurs (at least) in JDK 1.6.0_25.
            try {
                Thread.sleep(100);
                return getLocalVariables(frameNo);
            }
            catch (InterruptedException ie) {}
        }
        return new ArrayList<String>();
    }

    /**
     * Return true if the identified slot on the stack contains an object.
     */
    public boolean varIsObject(int frameNo, int index)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                LocalVariable var = vars.get(index);
                Value val = frame.getValue(var);
                return (val instanceof ObjectReference);
            }
            else
                return false;
        }
        catch(Exception e) {
            // nothing can be done...
            Debug.reportError("could not get local variable info: " + e);
        }
        return false;
    }

    /**
     * Return an object from this thread's stack. The variable must contain
     * an object.
     */
    public DebuggerObject getStackObject(int frameNo, int index)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                LocalVariable var = vars.get(index);
                JavaType vartype = JdiReflective.fromLocalVar(frame, var);
                ObjectReference val = (ObjectReference)frame.getValue(var);
                return JdiObject.getDebuggerObject(val, vartype);
            }
            else
                return null;
        }
        catch(Exception e) {
            // nothing can be done...
            Debug.reportError("could not get local variable info: " + e);
        }
        return null;
    }

    @Override
    public DebuggerObject getCurrentObject(int frameNo)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                return JdiObject.getDebuggerObject(frame.thisObject());
            }
        }
        catch (IncompatibleThreadStateException e) { }
        catch (VMDisconnectedException vmde) { }
        catch (InvalidStackFrameException ise) { } // thread was resumed elsewhere
        return JdiObject.getDebuggerObject(null);
    }

    @Override
    public DebuggerClass getCurrentClass(int frameNo)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                return new JdiClass(frame.location().declaringType());
            }
        }
        catch (InvalidStackFrameException isfe) { }
        catch (IncompatibleThreadStateException e) { }
        catch (VMDisconnectedException vmde) { }
        return null;
    }

    /** 
     * Specify a frame as the currently selected frame in this thread.
     */
    public void setSelectedFrame(int frame)
    {
        selectedFrame = frame;
    }

    /** 
     * Return the selected frame in this thread.
     */
    public int getSelectedFrame()
    {
        return selectedFrame;
    }

    /**
     * Halt this thread.
     */
    public synchronized void halt()
    {
        try {
            if (! isSuspended) {
                rt.suspend();
                debugger.emitThreadHaltEvent(this);
                isSuspended = true;
            }
        }
        catch (VMDisconnectedException vmde) {}
    }

    /**
     * Continue a previously halted thread.
     */
    public synchronized void cont()
    {
        try {
            if (isSuspended) {
                debugger.emitThreadResumedEvent(this);
                rt.resume();
                isSuspended = false;
            }
        }
        catch (VMDisconnectedException vmde) {}
    }

    /**
     * Inform the JdiThread that the underlying thread has been suspended due to
     * (for example) hitting a breakpoint.
     */
    public void stopped()
    {
        isSuspended = true;
    }
    
    /**
     * Make this thread step a single line.
     */
    public void step()
    {
        doStep(StepRequest.STEP_OVER);
    }

    public void stepInto()
    {
        doStep(StepRequest.STEP_INTO);
    }

    /**
     * Return the JDI ThreadReference which this JdiThread wraps.
     */
    public ThreadReference getThreadReference()
    {
        return rt;
    }
    
    private void doStep(int depth)
    {
        clearPreviousStep(rt);
        StepRequest request = eventReqMgr.createStepRequest(rt,
                                             StepRequest.STEP_LINE, depth);
        addExcludesToRequest(request);

        // Make sure the step event is done only once
        request.addCountFilter(1);
        request.putProperty(VMEventHandler.DONT_RESUME, "yes");
        request.enable();

        synchronized (this) {
            if (isSuspended) {
                debugger.emitThreadResumedEvent(this);
                rt.resume();
                isSuspended = false;
            }
        }
    }

    /**
     * A previously set step may not have completed yet - find out and
     * if it is so, remove it.
     */
    private void clearPreviousStep(ThreadReference thread)
    {
        if(eventReqMgr == null)
            getEventRequestManager();

        List<StepRequest> requests = eventReqMgr.stepRequests();
        Iterator<StepRequest> iter = requests.iterator();

        while (iter.hasNext()) {
            StepRequest request = iter.next();

            if (request != null && request.thread() != null) {
                if (request.thread().equals(thread)) {
                    eventReqMgr.deleteEventRequest(request);
                    // we must break here because now we
                    // have deleted the step event, the
                    // list iterator is invalid
                    // our assumption is that we can have at
                    // most one step event for this thread
                    // in the system.
                    break;
                }
            }
        }
    }

    private void getEventRequestManager()
    {
        eventReqMgr = rt.virtualMachine().eventRequestManager();
    }
    
    public String toString()
    {
        try {
            return getName() + " (" + getStatus() + ")";
            //return getName() + " (" + getStatus() + ") " + rt.threadGroup().name();  // for debugging
        }
        catch (ObjectCollectedException oce)
        {
            return "collected";
        }
    }
    
    public boolean sameThread(DebuggerThread dt)
    {
        if (dt != null && dt instanceof JdiThread) {
            return getThreadReference().uniqueID() == ((JdiThread)dt).getThreadReference().uniqueID();
        } else {
            return false;
        }
    }

    /**
     * Called when we are the serverThread, to let us know we've been resumed
     * (and should update our internal status accordingly)
     */
    public void notifyResumed()
    {
        isSuspended = false;
    }
}
