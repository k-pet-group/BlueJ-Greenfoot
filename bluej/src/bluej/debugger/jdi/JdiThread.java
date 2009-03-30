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
package bluej.debugger.jdi;

import java.util.*;

import bluej.Config;
import bluej.debugger.*;
import bluej.debugger.gentype.JavaType;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

import com.sun.jdi.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;

/**
 * This class represents a thread running on the remote virtual machine.
 *
 * @author  Michael Kolling
 * @version $Id: JdiThread.java 6215 2009-03-30 13:28:25Z polle $
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
    private static List excludes;

    static private List getExcludes() {
        if (excludes == null) {
            setExcludes("java.*, javax.*, sun.*, com.sun.*");
        }
        return excludes;
    }

    static void setExcludes(String excludeString) {
        StringTokenizer t = new StringTokenizer(excludeString, " ,;");
        List list = new ArrayList();
        while (t.hasMoreTokens()) {
            list.add(t.nextToken());
        }
        excludes = list;
    }

    static void addExcludesToRequest(StepRequest request) {
        Iterator iter = getExcludes().iterator();
        while (iter.hasNext()) {
            String pattern = (String)iter.next();
            request.addClassExclusionFilter(pattern);
        }
    }

	// the reference to the remote thread
	private ThreadReference rt;
    
	// stores a stack frame that was selected for this
    // thread (selection is done for debugging)
    int selectedFrame;
   
    // the TreeModel that we are being stored in. This reference
    // lets us propogate changes to our state into the TreeModel.
	JdiThreadTreeModel jttm;
	
    EventRequestManager eventReqMgr;

    // ---- instance: ----

    public JdiThread(JdiThreadTreeModel jttm, ThreadReference rt)
    {
		this.jttm = jttm;
        this.rt = rt;

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
            	if(VMReference.isAtMainBreakpoint(rt))
                	return statusFinished;
            	else
               		return statusBreakpoint;
            }
                        
            if(rt.isSuspended())
                return statusStopped;

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
        return  rt.isAtBreakpoint() && VMReference.isAtMainBreakpoint(rt);
    }

    /**
     * Return true if this thread is currently suspended.
     */
    public boolean isSuspended()
    {
        return rt.isSuspended();
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
        ThreadGroupReference tgr = rt.threadGroup();
        if(tgr == null || ! tgr.name().equals(MAIN_THREADGROUP))
            return true;

        String name = rt.name();
        if(name.startsWith("AWT-") ||
           name.equals("DestroyJavaVM") ||
           name.equals("BlueJ worker thread") ||
           name.equals("Timer Queue") ||
           name.equals("Screen Updater") ||
           name.startsWith("SunToolkit.") ||
           name.startsWith("Native Carbon") ||
           name.equals("Java2D Disposer"))
            return true;

        return false;
    }

	/**
	 * Get strings showing the current stack frames. Ignore everything
	 * including the __SHELL class and below.
	 *
	 * The thread must be suspended to do this. Otherwise an empty list
	 * is returned.
	 *
	 * @return  A List of SourceLocations
	 */
	public List getStack()
	{
		return getStack(rt);
	}

    /**
     * Get strings showing the current stack frames. Ignore everything
     * including the __SHELL class and below.
     *
     * The thread must be suspended to do this. Otherwise an empty list
     * is returned.
     * 
     * If the fileName for a source location is unavailable, the location
     * will not be added to the stack.
     *
     * @return  A List of SourceLocations
     */
    public static List getStack(ThreadReference thr)
    {
        try {
            if(thr.isSuspended()) {
                List stack = new ArrayList();
                List frames = thr.frames();

                for(int i = 0; i < frames.size(); i++) {
                    StackFrame f = (StackFrame)frames.get(i);
                    Location loc = f.location();
                    String className = loc.declaringType().name();
                    
                    // ensure that the bluej.runtime.ExecServer frames are not shown
                    if(className.startsWith("bluej.runtime"))
                        break;
                    
                    // must getBase on classname so that we find __SHELL
                    // classes in other packages ie a.b.__SHELL
					// if it is a __SHELL class, stop processing the stack
                    if(JavaNames.getBase(className).startsWith("__SHELL"))
                        break;

					String fileName = null;
					try {
						fileName = loc.sourceName();
					}
					catch(AbsentInformationException e) {
						continue;
					}
                    String methodName = loc.method().name();
                    int lineNumber = loc.lineNumber();

                    stack.add(new SourceLocation(className, fileName,
                                                 methodName, lineNumber));
                }
                return stack;
            }
        }
        catch(IncompatibleThreadStateException e) {
			// this is possible if the thread state changes after
			// our check for its suspended state
			// lets just return an empty List
        }
        catch(InvalidStackFrameException isfe) {
        	// same here
        }
        return new ArrayList();
    }


    /**
     * Return strings listing the local variables.
     *
     * The thread must be suspended to do this. Otherwise an empty List
     * is returned.
     */
    public List getLocalVariables(int frameNo)
    {
        //Debug.message("[JdiThread] getLocalVariables");
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List vars = frame.visibleVariables();
                List localVars = new ArrayList();

                for(int i = 0; i < vars.size(); i++) {
                    LocalVariable var = (LocalVariable)vars.get(i);

                    // Add "type name = value" to the list
                    JavaType vartype = JdiReflective.fromLocalVar(frame, var);
                    String val = JdiUtils.getJdiUtils().getValueString(frame.getValue(var));
                    localVars.add(vartype.toString(true) + " " + var.name()
                            + " = " + val);
                }
                return localVars;
            }
        }
        catch(Exception e) {
            // nothing can be done...
        }
        return new ArrayList();
    }

    /**
     * Return true if the identified slot on the stack contains an object.
     */
    public boolean varIsObject(int frameNo, int index)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                List vars = frame.visibleVariables();
                LocalVariable var = (LocalVariable)vars.get(index);
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
                List vars = frame.visibleVariables();
                LocalVariable var = (LocalVariable)vars.get(index);
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

    /**
     * Return the current object of this thread. May be null (if, for
     * example, the thread executed only static methods).
     */
    public DebuggerObject getCurrentObject(int frameNo)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                return JdiObject.getDebuggerObject(frame.thisObject());
            }
        }
        catch(Exception e) {
            // nothing to do...
        }
        return null;
    }


    /**
     * Return the current class of this thread.
     */
    public DebuggerClass getCurrentClass(int frameNo)
    {
        try {
            if(rt.isSuspended()) {
                StackFrame frame = rt.frame(frameNo);
                return new JdiClass(frame.location().declaringType());
            }
        }
        catch(Exception e) {
            // nothing to do...
        }
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
	public void halt()
	{
		rt.suspend();
		
		JdiThreadNode jtn = jttm.findThreadNode(rt);
		
		if (jtn != null)
			jttm.nodeChanged(jtn);		
	}

	/**
	 * Continue a previously halted thread.
	 */
	public void cont()
	{
		rt.resume();		

		JdiThreadNode jtn = jttm.findThreadNode(rt);
		
		if (jtn != null)
			jttm.nodeChanged(jtn);		
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

		rt.resume();

		JdiThreadNode jtn = jttm.findThreadNode(rt);
		
		if (jtn != null)
			jttm.nodeChanged(jtn);		
    }

    /**
     * A previously set step may not have completed yet - find out and
     * if it is so, remove it.
     */
    private void clearPreviousStep(ThreadReference thread)
    {
        if(eventReqMgr == null)
            getEventRequestManager();

        List requests = eventReqMgr.stepRequests();
        Iterator iter = requests.iterator();

        while (iter.hasNext()) {
            StepRequest request = (StepRequest)iter.next();

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
}
