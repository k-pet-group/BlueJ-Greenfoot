package bluej.debugger.gentype;

import java.util.Map;

/**
 * A type for which we know the text representation, but not the structure. Ie.
 * a type that the user has supplied in a text box, and which we haven't yet
 * parsed or performed equivalent magic with.<p>
 * 
 * Most operations on this type fail with an UnsupportedOperationException.
 * 
 * @author Davin McCall
 * @version $Id: TextType.java 3463 2005-07-13 01:55:27Z davmac $
 */
public class TextType extends JavaType
{
    private String text;
    
    public TextType(String text)
    {
        this.text = text;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return text;
    }
    
    public String arrayComponentName()
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isPrimitive()
     */
    public boolean isPrimitive()
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#getErasedType()
     */
    public JavaType getErasedType()
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isAssignableFrom(bluej.debugger.gentype.GenType)
     */
    public boolean isAssignableFrom(JavaType t)
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isAssignableFromRaw(bluej.debugger.gentype.GenType)
     */
    public boolean isAssignableFromRaw(JavaType t)
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#mapTparsToTypes(java.util.Map)
     */
    public JavaType mapTparsToTypes(Map tparams)
    {
        throw new UnsupportedOperationException();
    }

    public boolean isNumeric()
    {
        throw new UnsupportedOperationException();
    }
    
    public boolean isIntegralType()
    {
        throw new UnsupportedOperationException();
    }
    
    public boolean couldHold(int n)
    {
        throw new UnsupportedOperationException();
    }
    
    public boolean typeIs(int v)
    {
        throw new UnsupportedOperationException();
    }
    
    public GenTypeClass asClass()
    {
        throw new UnsupportedOperationException();
    }

}
