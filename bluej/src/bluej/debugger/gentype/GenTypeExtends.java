package bluej.debugger.gentype;

import java.util.Map;

/* "? extends ..." type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeExtends.java 2639 2004-06-21 02:09:00Z davmac $
 */
public class GenTypeExtends extends GenTypeWildcard
{
    // GenTypeSolid baseType;
    
    public GenTypeExtends(GenTypeSolid baseType)
    {
        super(baseType, null);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "? extends " + upperBound.toString(stripPrefix);
    }
    
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // If other is a "super", we have two bounds on the parameter, upper
        // and lower.
        if( other instanceof GenTypeSuper)
            return new GenTypeWildcard(upperBound, ((GenTypeSuper)other).lowerBound);
        
        if( ! (other instanceof GenTypeExtends) )
            return other;

        GenTypeClass baseClass = (GenTypeClass)upperBound;
        GenTypeClass otherBaseClass = (GenTypeClass)((GenTypeExtends)other).upperBound;

        // if the baseType classes are the same, one could still be
        // more specific, for instance ? extends List<? extends Runnable>
        // compared to ? extends List<? extends Thread>.
        if( ((GenTypeExtends)other).upperBound.equals(upperBound) ) {
            GenTypeParameterizable specializedBase = baseClass.precisify(otherBaseClass);
            if( specializedBase == upperBound )
                return this;
            else
                return new GenTypeExtends((GenTypeSolid)specializedBase);
        }
        
        // Here is an interesting situation. One of the baseTypes extends the
        // other. However the parameters for the less specific baseType may
        // actually be more specific. Compare:
        //       List<? extends Thread>      LinkedList<? extends Runnable>
        // First the less specific type's parameters must be mapped to the
        // more specific type, then the parameters must be precisified.
        
        // Try mapping this -> other
        GenTypeClass subClass = otherBaseClass;
        Reflective subReflective = subClass.reflective;
        Map mapping = baseClass.mapToDerived(subReflective);
        
        // If that failed, map other -> this
        if( mapping == null ) {
            subClass = baseClass;
            subReflective = baseClass.reflective;
            mapping = otherBaseClass.mapToDerived(subReflective);
        }
        
        GenTypeClass newType = new GenTypeClass(subReflective, mapping);
        return new GenTypeExtends((GenTypeSolid)newType.precisify(subClass));
    }

    public GenTypeParameterizable mapTparsToTypes(Map tparams)
    {
        GenType n = upperBound.mapTparsToTypes(tparams);

        if(n instanceof GenTypeSolid) {
            GenTypeSolid m = (GenTypeSolid)n;
            return new GenTypeExtends(m);
        }
        
        if(n instanceof GenTypeExtends) {
            GenTypeExtends e = (GenTypeExtends)n;
            GenTypeSolid bound = e.upperBound;
            return new GenTypeExtends(bound);
        }
        
        // otherwise assume n is GenTypeSuper or GenTypeUnbounded.
        return new GenTypeUnbounded();
    }
}
