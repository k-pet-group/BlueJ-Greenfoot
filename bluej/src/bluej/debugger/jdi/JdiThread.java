package bluej.debugger.jdi;

import bluej.debugger.DebuggerThread;
import bluej.debugger.DebuggerObject;
import bluej.utility.Debug;

import java.util.Vector;
import java.util.List;
import java.util.Iterator;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

/**
 ** This class represents a thread running on the remote virtual machine.
 **
 ** @author Michael Kolling
 **/

public final class JdiThread extends DebuggerThread
{
    ThreadReference rt; // the reference to the remote thread
    Object userParam;	// an optional user parameter associated with this
			// thread
    int selectedFrame;  // stores a stack frame that was selected for this
			// thread (selection is done for debugging)

    EventRequestManager eventReqMgr;

    public JdiThread(ThreadReference rt, Object userParam)
    {
	this.rt = rt;
	this.userParam = userParam;
	selectedFrame = 0;	// unless specified otherwise, assume we want
				//  to see the top level frame
    }
	
    public JdiThread(ThreadReference rt)
    {
	this(rt, null);
    }
	
    public String getName()
    {
	String name = null;
		
	try {
	    name = rt.name();
	} catch(Exception e) {
	    // ignore it
	}
	return name;
    }

    public void setParam(Object param)
    {
	userParam = param;
    }

    public Object getParam()
    {
	return userParam;
    }

    public ThreadReference getRemoteThread()
    {
	return rt;
    }
	
    public String getStatus()
    {
  	try {
	    if(rt.isAtBreakpoint()) {
		if(rt.frame(0).location().declaringType().name().equals(
						"bluej.runtime.ExecServer"))
		    return "finished";
		else
		    return "at breakpoint";
	    }

	    if(rt.isSuspended())
		return "stopped";

  	    int status = rt.status();
	    switch(status) {
		case ThreadReference.THREAD_STATUS_UNKNOWN: 
		    return "unknown status";
		case ThreadReference.THREAD_STATUS_ZOMBIE:
		    return "zombie";
		case ThreadReference.THREAD_STATUS_RUNNING:
		    return "running";
		case ThreadReference.THREAD_STATUS_SLEEPING:
		    return "sleeping";
		case ThreadReference.THREAD_STATUS_MONITOR: 
		    return "at monitor";
		case ThreadReference.THREAD_STATUS_WAIT:
		    return "waiting";
	    }
  	} catch(Exception e) {
  	    return "(???)";
  	}
	return null; // to shut up compiler
    }

    public boolean isSuspended()
    {
	return rt.isSuspended();
    }

    public String getClassSourceName(int frameNo)
    {
	try {
	    return rt.frame(frameNo).location().sourceName();
	} catch(Exception e) {
	    return "<no source at frame no " + frameNo +">";
	}
    }

    public int getLineNumber(int frameNo)
    {
	try {
	    return rt.frame(frameNo).location().lineNumber();
	} catch(Exception e) {
	    return 1;
	}
    }

    /**
     * Get strings showing the current stack frames. Ignore everything
     * including the __SHELL class and below.
     *
     * The thread must be suspended to do this. Otherwise an empty vector
     * is returned.
     *
     * @return  A Vector of Strings in the format "<class>.<method>"
     */
    public Vector getStack()
    {
	//Debug.message("[JdiThread] getStack");
	try {
	    if(rt.isSuspended()) {

		Vector stack = new Vector();
		List frames = rt.frames();

		boolean shellFound = false;
		for(int i = 0; i < frames.size(); i++) {
		    StackFrame f = (StackFrame)frames.get(i);
		    Location loc = f.location();
		    String classname = loc.declaringType().name();
		    if(classname.startsWith("__SHELL")) {
			shellFound = true;
			break;
		    }
		    stack.addElement(classname + "." + loc.method().name());
		}
		if(shellFound)
		    return stack;
		else
		    return new Vector();
	    }
	} catch(Exception e) {
	    Debug.reportError("error while getting stack info");
	}
	return new Vector();
    }

	
    /**
     * Return strings listing the local variables. Note that internally, the
     * local variables include "this" - we leave that out here (and leave it
     * up to the instance variable section to display those).
     *
     * The thread must be suspended to do this. Otherwise an empty string
     * array is returned.
     */
    public Vector getLocalVariables(int frameNo)
    {
	//Debug.message("[JdiThread] getLocalVariables");
	try {
	    if(rt.isSuspended()) {
		StackFrame frame = rt.frame(frameNo);
		List vars = frame.visibleVariables();
		Vector localVars = new Vector();

		for(int i = 0; i < vars.size(); i++) {
		    LocalVariable var = (LocalVariable)vars.get(i);
		    String val = JdiObject.getValueString(
						frame.getValue(var));
		    localVars.addElement(var.typeName() + " " + 
					 var.name() + " = " + val);
  					 
		}
		return localVars;
	    }
	} catch(Exception e) {
	    // nothing can be done...
	    Debug.reportError("could not get local variable info: " + e);
	}
	return new Vector();
    }

    public DebuggerObject getCurrentObject(int frameNo)
    {
	try {
	    if(rt.isSuspended()) {
		StackFrame frame = rt.frame(frameNo);
		return JdiObject.getDebuggerObject(frame.thisObject());
	    }
	} catch(Exception e) {
	    // nothing can be done...
	    Debug.reportError("could not get current object: " + e);
	}
	return null;
    }


    public void setSelectedFrame(int frame)
    {
	selectedFrame = frame;
    }

    public int getSelectedFrame()
    {
	return selectedFrame;
    }


    public void stop()
    {
	rt.suspend();
    }

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
        // Make sure the step event is done only once
        request.addCountFilter(1);
        request.enable();
	rt.resume();
    }

    public void cont()
    {
	rt.resume();
    }

    public void terminate()
    {
	Debug.message("[JdiThread] terminate - NYI");
//  	try {
//  	    //rt.stop();
//  	    Debug.message("terminate nyi");
//  	} catch(Exception e) {
//  	    e.printStackTrace(System.err);
//  	}
    }

    /**
     *  a previously set step may not have completed yet - find out and
     *  if it is so, remove it.
     */
    private void clearPreviousStep(ThreadReference thread) 
    {
	if(eventReqMgr == null)
	    getEventRequestManager();

         List requests = eventReqMgr.stepRequests();
         Iterator iter = requests.iterator();
         while (iter.hasNext()) {
             StepRequest request = (StepRequest)iter.next();
             if (request.thread().equals(thread)) {
                 eventReqMgr.deleteEventRequest(request);
                 break;
             }
         }
    }
    
    private void getEventRequestManager()
    {
	eventReqMgr = rt.virtualMachine().eventRequestManager();
    }

}
