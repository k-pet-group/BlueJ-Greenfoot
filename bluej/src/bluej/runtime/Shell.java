package bluej.runtime;

import java.util.Hashtable;

/**
 ** @version $Id: Shell.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface implemented by all "shell" classes - used for method invocation
 ** and object creation.
 **/

public class ObjectResultWrapper
{
    public Object result;
    ObjectResultWrapper(Object result) { this.result = result; }
}

public class StringResultWrapper
{
    public String result;
    StringResultWrapper(String result) { this.result = result; }
}

public class BooleanResultWrapper
{
    public boolean result;
    BooleanResultWrapper(boolean result) { this.result = result; }
}

public class ByteResultWrapper
{
    public byte result;
    ByteResultWrapper(byte result) { this.result = result; }
}

public class CharResultWrapper
{
    public char result;
    CharResultWrapper(char result) { this.result = result; }
}

public class DoubleResultWrapper
{
    public double result;
    DoubleResultWrapper(double result) { this.result = result; }
}

public class FloatResultWrapper
{
    public float result;
    FloatResultWrapper(float result) { this.result = result; }
}

public class IntResultWrapper
{
    public int result;
    IntResultWrapper(int result) { this.result = result; }
}

public class LongResultWrapper
{
    public long result;
    LongResultWrapper(long result) { this.result = result; }
}

public class ShortResultWrapper
{
    public short result;
    ShortResultWrapper(short result) { this.result = result; }
}
	
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
