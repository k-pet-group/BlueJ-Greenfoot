package bluej.runtime;

import java.util.Map;

/**
 * Interface implemented by all "shell" classes.
 *
 * The src for each "shell" class is constructed automatically,
 * compiled and executed. They are used for method invocation
 * and object creation.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Shell.java 2489 2004-04-08 08:58:58Z polle $
 */
public abstract class Shell
{
    /**
     * A dummy method called by class loader to prepare the class
     * after loading.
     */
    public static void prepare()
    {
    }

    /**
     * Provide the shell class with static access to the object
     * bench scopes.
     */
    protected static Map getScope(String scopeId)
    {
        return ExecServer.getScope();
    }

    /**
     * Put an object into the scope of one of the object benches.
     */
    protected static void putObject(String scopeId, String instanceName, Object value)
    {
        ExecServer.addObject(instanceName, value);
    }

    /**
     * Construct an object that allows the result to be plucked out by
     * the debugger.
     *
     * Object results are different from all the primitive types such as
     * boolean, byte etc.. don't exactly know why but it is..
     */
    public static ObjectResultWrapper makeObj(Object o) {
        return new ObjectResultWrapper(o);
    }

    /**
     * Construct an object that allows the result to be plucked out by
     * the debugger.
     */
    protected static Object makeObj(final String s) {
        return new Object() {
           public String result = s;
        };
    }

    protected static Object makeObj(final boolean b) {
        return new Object() {
           public boolean result = b;
        };
    }

    protected static Object makeObj(final byte b) {
        return new Object() {
           public byte result = b;
        };
    }

    protected static Object makeObj(final char c) {
        return new Object() {
           public char result = c;
        };
    }

    protected static Object makeObj(final double d) {
        return new Object() {
           public double result = d;
        };
    }

    protected static Object makeObj(final float f) {
        return new Object() {
           public float result = f;
        };
    }

    protected static Object makeObj(final int i) {
        return new Object() {
            public int result = i;
        };
    }

    protected static Object makeObj(final long l) {
        return new Object() {
           public long result = l;
        };
    }

    protected static Object makeObj(final short s) {
        return new Object() {
           public short result = s;
        };
    }
}
