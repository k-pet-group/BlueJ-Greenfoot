package bluej.runtime;

import java.util.Map;

/**
 * Interface implemented by all "shell" classes - used for method invocation
 * and object creation.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Shell.java 319 2000-01-02 13:03:50Z ajp $
 */
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

    // dummy method called by class loader to prepare the class
    // after loading
    public static void prepare()
    {
    }

    protected static Map getScope(String scopeId)
    {
        return ExecServer.getScope(scopeId);
    }

    protected static void putObject(String scopeId, String instanceName, Object value)
    {
        ExecServer.putObject(scopeId, instanceName, value);
    }
}
