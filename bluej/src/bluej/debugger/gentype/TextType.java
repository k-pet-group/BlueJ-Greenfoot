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
 * @version $Id: TextType.java 4066 2006-05-02 11:10:55Z davmac $
 */
public class TextType extends GenTypeParameterizable
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
    
    // methods from GenTypeParameterizable
    
    public void getParamsFromTemplate(Map map, GenTypeParameterizable template)
    {
        throw new UnsupportedOperationException();
    }

    public boolean contains(GenTypeParameterizable other)
    {
        throw new UnsupportedOperationException();
    }
    
    public GenTypeSolid getLowerBound()
    {
        throw new UnsupportedOperationException();
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        if (other == null) {
            return false;
        }
        
        if (other == this) {
            return true;
        }
        
        throw new UnsupportedOperationException();
    }

    public String toTypeArgString(NameTransform nt)
    {
        // throw new UnsupportedOperationException();
    	
    	// Text types are generally typed in by the user, and require
    	// no transformation.
    	return text;
    }
    
    public GenTypeSolid [] getUpperBounds()
    {
        throw new UnsupportedOperationException();
    }
    
    public GenTypeSolid getUpperBound()
    {
        throw new UnsupportedOperationException();
    }
}
