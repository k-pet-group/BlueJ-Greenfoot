package bluej.debugger;

import bluej.utility.Debug;

import java.util.Vector;
import sun.tools.debug.*;

/**
 ** @version $Id: SunThread.java 63 1999-05-04 00:03:10Z mik $
 ** @author Michael Cahill
 ** A class implementing the debugger primitives needed by BlueJ
 ** Implemented in a remote VM (via sun.tools.debug)
 **/

public class SunThread extends DebuggerThread
{
    RemoteThread rt;
    boolean halted;	// true if explicitely suspended
    RemoteStackFrame[] frames = null;

    public SunThread(RemoteThread rt)
    {
	this.rt = rt;
	halted = false;
    }
	
    public String getName()
    {
	String name = null;
		
	try {
	    name = rt.getName();
	} catch(Exception e) {
	    // ignore it
	}
		
	return name;
    }

    public RemoteThread getRemoteThread()
    {
	return rt;
    }
	
    public String getStatus()
    {
	String status = null;
		
	try {
	    status = rt.getStatus();
	} catch(Exception e) {
	    // ignore it
	}
		
	return status;
    }

    public void setHalted(boolean halted)
    {
	this.halted = halted;
    }

    public boolean isHalted()
    {
	return halted;
    }

    public String getClassSourceName(int frameNo)
    {
	try {
	    readStackFrames();
	    return frames[frameNo].getRemoteClass().getSourceFileName();
	} catch(Exception e) {
	    return "";
	}
    }

    public int getLineNumber(int frameNo)
    {
	try {
	    readStackFrames();
	    return frames[frameNo].getLineNumber();
	} catch(Exception e) {
	    return 1;
	}
    }

    /**
     * Get strings showing the current stack frames. Ignore everything
     * including the __SHELL clas and below.
     *
     * The thread must be suspended to do this. Otherwise an empty vector
     * is returned.
     */
    public Vector getStack()
    {
	try {
	    if(rt.isSuspended()) {
		readStackFrames();
		Vector stack = new Vector();
		
		boolean shellFound = false;
		for(int i = 0; i < frames.length; i++) {
		    RemoteStackFrame f = frames[i];
		    RemoteClass cl = frames[i].getRemoteClass();
		    if(cl.getName().startsWith("__SHELL")) {
			shellFound = true;
			break;
		    }
		    stack.addElement(cl.getName() + "." + f.getMethodName());
		    // cl.getSourceFileName()  f.getLineNumber();
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
     *
     * The thread must be suspended to do this. Otherwise an empty string
     * array is returned.
     */
    public Vector getInstanceVariables(int frameNo)
    {
	try {
	    if(rt.isSuspended()) {
		readStackFrames();
		RemoteStackVariable thisVar = 
		    frames[frameNo].getLocalVariable("this");
		if(thisVar == null)
		    return new Vector();
		RemoteValue value = thisVar.getValue();
		if(value == null)
		    return new Vector();
		SunObject obj = new SunObject((RemoteObject)value);

		String[] staticVar = obj.getStaticFields(false);
		String[] instVar = obj.getFields(false);

		Vector allVars = new Vector(staticVar.length+instVar.length);

		for(int i=0; i < staticVar.length; i++)
		    allVars.addElement(staticVar[i]);
		for(int i=0; i < instVar.length; i++)
		    allVars.addElement(instVar[i]);

		return allVars;
	    }
	} catch(Exception e) {
	    // nothing can be done...
	    Debug.reportError("could not get instance variable info: " + e);
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
	try {
	    if(rt.isSuspended()) {
		readStackFrames();
		RemoteStackVariable[] allVars = 
		    frames[frameNo].getLocalVariables();
		Vector localVars = new Vector();

		for(int i = 0; i < allVars.length; i++) {
		    RemoteStackVariable var = allVars[i];
		    String name = var.getName();
		    if(! name.equals("this")) {
			localVars.addElement(var.getType() + " " + name +
					     " = " +
					     getValueString(var.getValue()));
		    }
		}
		return localVars;
	    }
	} catch(Exception e) {
	    // nothing can be done...
	    Debug.reportError("could not get local variable info: " + e);
	}
	return new Vector();
    }

    public void stop()
    {
	try {
	    rt.suspend();
	    halted = true;
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    public void step()
    {
	try {
	    rt.next();
//  	    if(halted) {
//  		rt.resume();
//  		halted = false;
//  	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    public void stepInto()
    {
	try {
	    rt.step(true);
//  	    if(halted) {
//  		rt.resume();
//  		halted = false;
//  	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    public void cont()
    {
	try {
	    rt.cont();
	    if(halted) {
		rt.resume();
		halted = false;
	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    public void terminate()
    {
	try {
	    //rt.stop();
	    Debug.message("terminate nyi");
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	}
    }

    private String getValueString(RemoteValue val)
    {
	String valString;
	
	if(val == null)
	    valString = "<null>";
	else if(val.isString()) {
	    // Horrible special case:
	    if("null".equals(val.toString()))
		valString = "\"\"";
	    else
		valString = "\"" + val.toString() + "\"";
	}
	else if(val instanceof RemoteObject)
	    valString = "<object reference>";
	else
	    valString = val.toString();

	return valString;
    }

    private void readStackFrames()
    {
	try {
	    if(frames == null)
		frames = rt.dumpStack();
	} catch(Exception e) {
	    Debug.reportError("could not read stack info: " + e);
	}
    }
}
