package bluej.debugger.gentype;

import java.util.Map;

/*
 * A wildcard type with an upper and/or lower bound.
 * 
 * Note that both an upper and lower bound is allowed. This type doesn't occur
 * naturally- it can't be specified in the Java language. But in some cases we
 * can deduce the type of some object to be this.
 *
 * @author Davin McCall
 * @version $Id: GenTypeWildcard.java 2656 2004-06-25 01:44:18Z davmac $
 */
class GenTypeWildcard extends GenTypeParameterizable
{
    GenTypeSolid upperBound;  // ? extends upperBound
    GenTypeSolid lowerBound;  // ? super lowerBound
    
    public GenTypeWildcard(GenTypeSolid upper, GenTypeSolid lower)
    {
        upperBound = upper;
        lowerBound = lower;
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "? extends " + upperBound.toString(stripPrefix) + " super "
        + lowerBound.toString(stripPrefix);
    }
    
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        GenTypeExtends myUpper = new GenTypeExtends(upperBound);
        GenTypeSuper myLower = new GenTypeSuper(lowerBound);
        
        GenTypeExtends otherUpper = null;
        if( other instanceof GenTypeExtends )
            otherUpper = (GenTypeExtends)other;
        else if( other instanceof GenTypeWildcard )
            otherUpper = new GenTypeExtends(((GenTypeWildcard)other).upperBound);
        
        if( otherUpper != null )
            myUpper = (GenTypeExtends)myUpper.precisify(otherUpper);
        
        GenTypeSuper otherLower = null;
        if( other instanceof GenTypeSuper )
            otherLower = (GenTypeSuper)other;
        else if( other instanceof GenTypeWildcard )
            otherLower = new GenTypeSuper(((GenTypeWildcard)other).lowerBound);
        
        if( otherLower != null )
            myLower = (GenTypeSuper)myLower.precisify(otherLower);
        
        if( myUpper.upperBound == upperBound && myLower.lowerBound == lowerBound )
            return this;
        else {
            // Check if upper and lower bound match
            if( myUpper.upperBound.equals(myLower.lowerBound) )
                return myUpper.upperBound;
        }
        return new GenTypeWildcard(myUpper.upperBound, myLower.lowerBound);
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        GenTypeParameterizable newUpper = null;
        GenTypeParameterizable newLower = null;
        if( upperBound != null )
            newUpper = (GenTypeParameterizable)upperBound.mapTparsToTypes(tparams);
        if( lowerBound != null )
            newLower = (GenTypeParameterizable)lowerBound.mapTparsToTypes(tparams);
        
        return new GenTypeWildcard((GenTypeSolid)newUpper, (GenTypeSolid)newLower);
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        if( ! (other instanceof GenTypeWildcard) )
            return false;
        GenTypeWildcard bOther = (GenTypeWildcard)other;
        return upperBound.equals(bOther.upperBound) && lowerBound.equals(bOther.lowerBound);
    }
    
    protected void getParamsFromTemplate(Map map, GenTypeParameterizable template)
    {
        // TODO. Too complicated for now.
        return;
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
