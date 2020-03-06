/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2016,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
import java.util.function.Supplier;

import bluej.Config;
import bluej.debugger.*;
import bluej.debugger.gentype.JavaType;
import bluej.utility.Debug;

import bluej.utility.javafx.FXPlatformSupplier;
import com.sun.jdi.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static List<String> excludes;

    @OnThread(Tag.Any)
    private static synchronized List<String> getExcludes()
    {
        if (excludes == null) {
            setExcludes("java.*, javax.*, sun.*, com.sun.*");
        }
        return excludes;
    }

    @OnThread(Tag.Any)
    private static synchronized void setExcludes(String excludeString)
    {
        StringTokenizer t = new StringTokenizer(excludeString, " ,;");
        List<String> list = new ArrayList<String>();
        while (t.hasMoreTokens()) {
            list.add(t.nextToken());
        }
        excludes = list;
    }

    @OnThread(Tag.Any)
    static void addExcludesToRequest(StepRequest request)
    {
        Iterator<String> iter = getExcludes().iterator();
        while (iter.hasNext()) {
            String pattern = iter.next();
            request.addClassExclusionFilter(pattern);
        }
    }

    /** the reference to the remote thread */
    @OnThread(Tag.VMEventHandler)
    private final ThreadReference rt;
    
    /** We track suspension status internally */
    @OnThread(Tag.VMEventHandler)
    private boolean isSuspended;
    
    /** Any active step request */
    @OnThread(Tag.VMEventHandler)
    StepRequest stepRequest;
    
    /*
     * Note that we have to track suspension status internally, because JDI will happily
     * tell us that a thread is suspended when, in fact, it is suspended only because of some
     * VM event which suspends every thread (ThreadStart / ThreadDeath) or because of application
     * suspension - see https://bugs.openjdk.java.net/browse/JDK-4257690
     */
    
    // stores a stack frame that was selected for this
    // thread (selection is done for debugging)
    private int selectedFrame;

    @OnThread(Tag.VMEventHandler)
    private EventRequestManager eventReqMgr;
    
    @OnThread(Tag.VMEventHandler)
    private final JdiDebugger debugger;

    // ---- instance: ----

    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker") // Name is fairly safe to access from any thread
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
     * Return the current status of this thread.
     */
    @OnThread(Tag.VMEventHandler)
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
     * Return true if this thread is currently suspended.
     */
    @OnThread(Tag.VMEventHandler)
    public synchronized boolean isSuspended()
    {
        return isSuspended;
    }

    /** 
     * Return true if this thread is currently at a breakpoint.
     */
    @OnThread(Tag.VMEventHandler)
    public boolean isAtBreakpoint()
    {
        return rt.isAtBreakpoint();
    }

    /** 
     * Return the class this thread was executing in when the
     * specified stack frame was active.
     */
    @OnThread(Tag.VMEventHandler)
    @Override
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
    @OnThread(Tag.VMEventHandler)
    @Override
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
    @OnThread(Tag.VMEventHandler)
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

    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker") // It's only checking names, so pragmatically it's thread safe.
    public boolean isKnownSystemThread()
    {
        // A finished thread will have a null thread group.
        try {
            ThreadGroupReference tgr = rt.threadGroup();
            if(tgr == null || ! tgr.name().equals(MAIN_THREADGROUP)) {
                return true;
            }

            String name = rt.name();

            // Don't count the AWT and FX event threads as system threads, since user code
            // often runs on them:
            if (name.startsWith("AWT-Event")
                || name.equals("JavaFX Application Thread")
                // Sometimes on Windows, it seems the FX application thread can get a different name
                // WindowsNativeRunLoop, but this seems transient.
                || name.startsWith("WindowsNative"))
            {
                return false;
            }
            
            if (name.startsWith("AWT-") ||
                    name.equals("DestroyJavaVM") ||
                    name.equals("BlueJ worker thread") ||
                    name.equals("Timer Queue") ||
                    name.equals("Screen Updater") ||
                    name.startsWith("SunToolkit.") ||
                    name.startsWith("Native Carbon") ||
                    name.equals("JavaFX-Launcher") ||
                    name.startsWith("QuantumRenderer") ||
                    name.equals("JavaFX BlueJ Helper") ||
                    name.equals("Java2D Disposer") ||
                    name.equals("InvokeLaterDispatcher"))
            {
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
    @OnThread(Tag.VMEventHandler)
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
    @OnThread(Tag.VMEventHandler)
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
    @OnThread(Tag.VMEventHandler)
    @Override
    public List<FXPlatformSupplier<VarDisplayInfo>> getLocalVariables(int frameNo)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                List<FXPlatformSupplier<VarDisplayInfo>> localVars = new ArrayList<>();
                
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

                    final int iFinal = i;
                    FXPlatformSupplier<DebuggerObject> getObjectToInspect = varIsObject(frameNo, i) ?
                            getStackObject(frameNo, iFinal)
                            : null;
                    localVars.add(() -> {
                        JavaType vartype = JdiReflective.fromLocalVar(localTypes.get(iFinal), genericSigs.get(iFinal),
                            typeNames.get(iFinal), declaringType);
                        return new VarDisplayInfo(vartype, var, localVals.get(iFinal), getObjectToInspect);
                    });
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
        return new ArrayList<>();
    }

    /**
     * Return true if the identified slot on the stack contains an object.
     */
    @OnThread(Tag.VMEventHandler)
    public boolean varIsObject(int frameNo, int index)
    {
        try
        {
            if (rt.isSuspended())
            {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                if (index >= vars.size())
                {
                    return false;
                }
                LocalVariable var = vars.get(index);
                Value val = frame.getValue(var);
                return (val instanceof ObjectReference);
            }
            else
            {
                return false;
            }
        }
        catch (IncompatibleThreadStateException | InvalidStackFrameException itse)
        {
            // Don't need to report this; thread must have been resumed already.
        }
        catch(Exception e)
        {
            // nothing can be done...
            Debug.reportError("could not get local variable info: " + e);
            e.printStackTrace(System.out);
        }
        return false;
    }

    /**
     * Return an object from this thread's stack. The variable must contain
     * an object.
     */
    @OnThread(Tag.VMEventHandler)
    // @SuppressWarnings("threadchecker")
    public FXPlatformSupplier<DebuggerObject> getStackObject(int frameNo, int index)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                LocalVariable var = vars.get(index);
                FXPlatformSupplier<JavaType> vartype = JdiReflective.fromLocalVar(frame, var);
                ObjectReference val = (ObjectReference)frame.getValue(var);
                return () -> JdiObject.getDebuggerObject(val, vartype.get());
            }
            else
                return null;
        }
        catch(Exception e) {
            // nothing can be done...
            Debug.reportError("could not get local variable info: " + e);
            e.printStackTrace(System.out);
        }
        return null;
    }

    /**
     * Gets the stack object, but without fetching its type, allowing it to be done from the VMEventHandler thread.
     */
    @OnThread(Tag.VMEventHandler)
    public DebuggerObject getStackObjectUntyped(int frameNo, int index)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List<LocalVariable> vars = frame.visibleVariables();
                LocalVariable var = vars.get(index);
                ObjectReference val = (ObjectReference)frame.getValue(var);
                return JdiObject.getDebuggerObject(val);
            }
            else
                return null;
        }
        catch(Exception e) {
            // nothing can be done...
            Debug.reportError("could not get local variable info: " + e);
            e.printStackTrace(System.out);
        }
        return null;
    }

    @Override
    @OnThread(Tag.VMEventHandler)
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
    @OnThread(Tag.VMEventHandler)
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
    @OnThread(Tag.VMEventHandler)
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
    @OnThread(Tag.VMEventHandler)
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
    @OnThread(Tag.VMEventHandler)
    public void stopped()
    {
        synchronized (this)
        {
            isSuspended = true;
        }
        clearPreviousStep(rt);
    }
    
    /**
     * Make this thread step a single line.
     */
    @OnThread(Tag.VMEventHandler)
    public void step()
    {
        boolean doStepOver = true;
        try {
            // We've seen a JVM bug where a step request is ignored (or severely delayed) when
            // a native method is being executed (codeIndex() == -1). So, we use STEP_OUT instead
            // of STEP_OVER in that case.
            // Possibly related to: https://bugs.openjdk.java.net/browse/JDK-6980202
            
            Location loc = rt.frame(0).location();
            doStepOver = (loc.codeIndex() != -1);
        }
        catch (IncompatibleThreadStateException itse) { }
        doStep(doStepOver ? StepRequest.STEP_OVER : StepRequest.STEP_OUT);
    }

    @OnThread(Tag.VMEventHandler)
    public void stepInto()
    {
        doStep(StepRequest.STEP_INTO);
    }
    
    @OnThread(Tag.VMEventHandler)
    private void doStep(int depth)
    {
        clearPreviousStep(rt);
        stepRequest = eventReqMgr.createStepRequest(rt,
                                             StepRequest.STEP_LINE, depth);
        addExcludesToRequest(stepRequest);

        // Make sure the step event is done only once
        stepRequest.addCountFilter(1);
        stepRequest.putProperty(VMEventHandler.DONT_RESUME, "yes");
        stepRequest.enable();

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
    @OnThread(Tag.VMEventHandler)
    private void clearPreviousStep(ThreadReference thread)
    {
        if (eventReqMgr == null)
            getEventRequestManager();

        if (stepRequest != null) {
            eventReqMgr.deleteEventRequest(stepRequest);
            stepRequest = null;
        }
    }

    @OnThread(Tag.VMEventHandler)
    private void getEventRequestManager()
    {
        eventReqMgr = rt.virtualMachine().eventRequestManager();
    }
    
    @OnThread(value = Tag.VMEventHandler, ignoreParent = true)
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
    
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker") // Unique IDs are thread-safe
    public boolean sameThread(DebuggerThread dt)
    {
        if (dt != null && dt instanceof JdiThread) {
            return rt.uniqueID() == ((JdiThread)dt).rt.uniqueID();
        } else {
            return false;
        }
    }

    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker") // Unique IDs are thread-safe
    public boolean sameThread(ThreadReference threadReference)
    {
        if (threadReference != null) {
            return rt.uniqueID() == threadReference.uniqueID();
        } else {
            return false;
        }
    }

    /**
     * Resume, but we are the server thread: updated our internal isSuspended status,
     * but no need to fire listeners.  Also, can be called from any thread.
     */
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker") // The server thread is special, and can be resumed from another thread.
    public synchronized void contServerThread()
    {
        rt.resume();
        isSuspended = false;
    }
}
