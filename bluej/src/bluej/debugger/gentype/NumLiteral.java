package bluej.debugger.gentype;

/**
 * A type to represent numberic literals, both integer and floating point.
 * 
 * @author Davin McCall
 * @version $Id: NumLiteral.java 3102 2004-11-18 01:39:18Z davmac $
 */
public class NumLiteral extends GenTypePrimitive
{
    long value;
    double fvalue;
    
    private boolean isFloatingPoint;  // is a float/double value?
    
    // constructors.
    
    public NumLiteral(int type, long value)
    {
        super(type);
        this.value = value;
    }
    
    public NumLiteral(int type, double fvalue)
    {
        super(type);
        this.fvalue = fvalue;
    }
   
    /*
     * Being a constant, a literal cannot be assigned anything.
     */
    public boolean isAssignableFrom(GenType other)
    {
        return false;
    }    
    
    public boolean fitsType(int type)
    {
        if (! isFloatingPoint) {
            // Crazy java. A literal can be cast to anything - except long -
            // and still assigned to anything that it will fit into it. If you
            // cast to long, however, it can only be assigned to a long (or
            // a floating point type)
            if (! typeIs(GT_LONG)) {
                if (type == GT_CHAR)
                    return value >= '\u0000' && value <= '\uFFFF';
                else if (type == GT_BYTE)
                    return value >= -128 && value <= 127;
                else if (type == GT_SHORT)
                    return value >= -32768 && value < 32767;
                else
                    return true;
            }
            else {
                // The literal type is long.  The only thing it will fit
                // into is a long or a floating point.
                return type >= GT_LONG;
            }
        }
        else {
            // floating point value.
            return type >= getMyIndex();
        }
    }
}
