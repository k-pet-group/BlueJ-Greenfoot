package bluej.runtime;

import java.util.Hashtable;

/**
 ** Interface implemented by all "shell" classes - used for method invocation
 ** and object creation.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: Shell.java 93 1999-05-28 00:54:37Z mik $
 **/

public abstract class Shell
{
    protected static Object makeObj(String s) { return new StringResultWrapper(s); }
    protected static Object makeObj(boolean b) { return new BooleanResultWrapper(b); }
    protected static Object makeObj(byte b) { return new ByteResultWrapper(b); }
    protected static Object makeObj(char c) { return new CharResultWrapper(c); }
    protected static Object makeObj(double d) { return new DoubleResultWrapper(d); }
    protected static Object makeObj(float f) { return new FloatResultWrapper(f); }
    protected static Object makeObj(int i) { return new IntResultWrapper(i); }
    protected static Object makeObj(long l) { return new LongResultWrapper(l); }
    protected static Object makeObj(short s) { return new ShortResultWrapper(s); }
    protected static ObjectResultWrapper makeObj(Object obj) { return new ObjectResultWrapper(obj); }
	
    protected static Hashtable getScope(String scopeId)
    {
	return BlueJRuntime.getScope(scopeId);
    }
	
    protected static void putObject(String scopeId, String instanceName, Object value)
    {
	BlueJRuntime.putObject(scopeId, instanceName, value);
    }


    //  	protected static Object getObject(String scopeId, String instanceName)
    //  	{
    //  		Hashtable scope = getScope(scopeId);
    //  		return scope.get(instanceName);
    //  	}
}
