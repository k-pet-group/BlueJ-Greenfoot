package bluej.debugger.gentype;

import java.util.Map;

/**
 * Base class for primitive/built-in types.
 * 
 * @author Davin McCall
 */
public class GenTypePrimitive
    extends GenType
{
    private static GenTypePrimitive [] primitiveTypes = new GenTypePrimitive[GenType.GT_MAX+1];
    private static String [] typeNames = { "void", "java.lang.Object", "boolean", "char",
            "byte", "short", "int", "long", "float", "double" };
    // note, the types above should be valid java types. So the type of null
    // is java.lang.Object.
    
    // each element represents a primitive type, and contains an array of
    // other types that this type can be assigned from
    private static int assignableFrom [][] = new int [GT_MAX+1][];
    {
        assignableFrom[GT_VOID]    = new int[] {};
        assignableFrom[GT_NULL]    = new int[] {};
        assignableFrom[GT_BOOLEAN] = new int[] { GT_BOOLEAN };
        assignableFrom[GT_CHAR]    = new int[] { GT_CHAR };
        assignableFrom[GT_BYTE]    = new int[] { GT_BYTE };
        assignableFrom[GT_SHORT]   = new int[] { GT_SHORT, GT_BYTE };
        assignableFrom[GT_INT]     = new int[] { GT_INT, GT_BYTE, GT_SHORT, GT_CHAR };
        assignableFrom[GT_LONG]    = new int[] { GT_LONG, GT_BYTE, GT_SHORT, GT_CHAR, GT_INT };
        assignableFrom[GT_FLOAT]   = new int[] { GT_FLOAT, GT_LONG, GT_BYTE, GT_SHORT, GT_CHAR, GT_INT, GT_LONG };
        assignableFrom[GT_DOUBLE]  = new int[] { GT_DOUBLE, GT_LONG, GT_BYTE, GT_SHORT, GT_CHAR, GT_INT, GT_LONG, GT_FLOAT };
    }
    
    // instance fields
    
    private int myIndex;
    
    /*
     * Private constructor. Use "getXXX" methods to get a primitive instance. 
     */
    protected GenTypePrimitive(int index)
    {
        myIndex = index;
    }
    
    private static GenTypePrimitive getType(int v)
    {
        if (primitiveTypes[v] == null)
            primitiveTypes[v] = new GenTypePrimitive(v);
        
        return primitiveTypes[v];
    }
    
    /**
     * Obtain an instance of "void".
     */
    public static GenTypePrimitive getVoid()
    {
        return getType(GT_VOID);
    }
    
    public static GenTypePrimitive getNull()
    {
        return getType(GT_NULL);
    }
    
    public static GenTypePrimitive getBoolean()
    {
        return getType(GT_BOOLEAN);
    }
    
    public static GenTypePrimitive getByte()
    {
        return getType(GT_BYTE);
    }
    
    public static GenTypePrimitive getChar()
    {
        return getType(GT_CHAR);
    }
    
    public static GenTypePrimitive getShort()
    {
        return getType(GT_SHORT);
    }
    
    public static GenTypePrimitive getInt()
    {
        return getType(GT_INT);
    }
    
    public static GenTypePrimitive getLong()
    {
        return getType(GT_LONG);
    }
    
    public static GenTypePrimitive getFloat()
    {
        return getType(GT_FLOAT);
    }
    
    public static GenTypePrimitive getDouble()
    {
        return getType(GT_DOUBLE);
    }
    
    
    public String toString()
    {
        return typeNames[myIndex];
    }
    
    public String arrayComponentName()
    {
        // Simple lookup by index. It's not possible to have an array of
        // void or null types.
        return "!!ZCBSIJFD".substring(myIndex, myIndex + 1);
    }
    
    public boolean isAssignableFrom(GenType o)
    {
        int [] assignables = assignableFrom[myIndex];
        for (int i = 0; i < assignables.length; i++) {
            if (o.typeIs(assignables[i]))
                return true;
        }
        return false;
    }
    
    public boolean couldHold(int n)
    {
        if (myIndex >= GT_INT)
            return true;
        
        if (myIndex == GT_BYTE)
            return n >= -128 && n <= 127;
        else if (myIndex == GT_CHAR)
            return n >=0 && n <= 65535;
        else if (myIndex == GT_SHORT)
            return n >= -32768 && n <= 32767;

        return false;
    }
    
    public boolean fitsType(int gtype)
    {
        if (myIndex == GT_CHAR)
            return (gtype != GT_BYTE && gtype != GT_SHORT);
        else
            return gtype >= myIndex;
    }
    
    public GenType getErasedType()
    {
        return this;
    }

    /*
     * For primitive types, "isAssignableFromRaw" is equivalent to
     * "isAssignableFrom".
     */
    public boolean isAssignableFromRaw(GenType t)
    {
        return isAssignableFrom(t);
    }

    public boolean isPrimitive()
    {
        return true;
    }
    
    public boolean isNumeric()
    {
        return myIndex >= GT_LOWEST_NUMERIC;
    }
    
    public boolean isIntegralType()
    {
        return myIndex >= GT_CHAR && myIndex <= GT_LONG;
    }
    
    public boolean typeIs(int v)
    {
        return myIndex == v;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
    
    public boolean equals(Object other)
    {
        if (other instanceof GenType) {
            GenType gto = (GenType) other;
            return (gto.typeIs(myIndex));
        }
        else
            return false;
    }
    
    final protected int getMyIndex()
    {
        return myIndex;
    }
    
    public GenType opBNot()
    {
        // binary-not is defined for integer types
        if (myIndex >= GT_CHAR && myIndex <= GT_LONG)
            return this;
        
        return null;
    }
}
