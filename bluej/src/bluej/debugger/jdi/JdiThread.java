package bluej.debugger.jdi;

import bluej.debugger.DebuggerThread;
import bluej.debugger.DebuggerObject;
import bluej.utility.Debug;

import java.util.Vector;
import java.util.List;

import com.sun.jdi.*;

/**
 ** This class represents a thread running on the remote virtual machine.
 **
 ** @author Michael Kolling
 **/

public final class JdiThread extends DebuggerThread
{
    ThreadReference rt; // the reference to the remote thread
    //boolean halted;	// true if explicitely suspended
    Object userParam;	// an optional user parameter associated with this
			// thread
    
    public JdiThread(ThreadReference rt)
    {
	this.rt = rt;
	this.userParam = null;
	//halted = false;
    }
	
    public JdiThread(ThreadReference rt, Object userParam)
    {
	this.rt = rt;
	this.userParam = userParam;
	//halted = false;
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
	    if(rt.isAtBreakpoint())
		return "at breakpoint";

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

    public void setHalted(boolean halted)
    {
	Debug.message("[JdiThread] setHalted - NYI");
	//this.halted = halted;
    }

    public boolean isHalted()
    {
	Debug.message("[JdiThread] isHalted - NYI");
	return false;
    }

    public String getClassSourceName(int frameNo)
    {
	try {
	    return rt.frame(frameNo).location().sourceName();
	} catch(Exception e) {
	    return "";
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
	Debug.message("[JdiThread] getStack");
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
	Debug.message("[JdiThread] getLocalVariables");
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


    public void stop()
    {
	Debug.message("[JdiThread] stop - NYI");
//  	try {
//  	    rt.suspend();
//  	    halted = true;
//  	} catch(Exception e) {
//  	    e.printStackTrace(System.err);
//  	}
    }

    public void step()
    {
	Debug.message("[JdiThread] step - NYI");
//  	try {
//  	    rt.next();
//  //  	    if(halted) {
//  //  		rt.resume();
//  //  		halted = false;
//  //  	    }
//  	} catch(Exception e) {
//  	    e.printStackTrace(System.err);
//  	}
    }

    public void stepInto()
    {
	Debug.message("[JdiThread] stepInto - NYI");
//  	try {
//  	    rt.step(true);
//  //  	    if(halted) {
//  //  		rt.resume();
//  //  		halted = false;
//  //  	    }
//  	} catch(Exception e) {
//  	    e.printStackTrace(System.err);
//  	}
    }

    public void cont()
    {
	Debug.message("[JdiThread] cont");
  	try {
  	    rt.resume();
//  	    if(halted) {
//  		rt.resume();
//  		halted = false;
//  	    }
  	} catch(Exception e) {
  	    e.printStackTrace(System.err);
  	}
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

}
