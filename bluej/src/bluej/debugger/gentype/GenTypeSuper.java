package bluej.debugger.gentype;

import java.util.Map;

/*
 * "? super ..." type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeSuper.java 2818 2004-07-26 03:42:35Z davmac $
 */
public class GenTypeSuper extends GenTypeWildcard
{
    public GenTypeSuper(GenTypeSolid baseType) {
        super(null, baseType);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "? super " + lowerBound.toString(stripPrefix);
    }
    
    public String toString(NameTransform nt)
    {
        return "? super " + lowerBound.toString(nt);
    }
    
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // If other is a "extends", we have two bounds on the parameter, upper
        // and lower.
        if( other instanceof GenTypeExtends)
            return new GenTypeWildcard(((GenTypeExtends)other).upperBound, lowerBound);
        
        if( ! (other instanceof GenTypeSuper) )
            return (GenTypeClass)other;

        GenTypeClass baseClass = (GenTypeClass)lowerBound;
        GenTypeClass otherBaseClass = (GenTypeClass)((GenTypeExtends)other).upperBound;

        // if the baseType classes are the same, one could still be
        // more specific, for instance ? super List<? extends Runnable>
        // compared to ? super List<? extends Thread>.
        if( otherBaseClass.rawName().equals(baseClass.rawName()) ) {
            GenTypeClass specializedBase = (GenTypeClass)lowerBound.precisify(otherBaseClass);
            if( specializedBase == lowerBound )
                return this;
            else
                return new GenTypeSuper(specializedBase);
        }
        
        // Here is an interesting situation. One of the baseTypes extends the
        // other. However the parameters for the less specific baseType may
        // actually be more specific. Compare:
        //       List<? extends Thread>      LinkedList<? extends Runnable>
        // First the less specific type's parameters must be mapped to the
        // more specific type, then the parameters must be precisified.

        // Try mapping other -> this
        GenTypeClass superClass = baseClass;
        Reflective superReflective = baseClass.reflective;
        Map mapping = otherBaseClass.mapToSuper(superReflective.getName());
        
        // If that failed, map this -> other
        if( mapping == null ) {
            superClass = otherBaseClass;
            superReflective = otherBaseClass.reflective;
            mapping = baseClass.mapToSuper(superReflective.getName());
        }
        
        GenTypeClass newType = new GenTypeClass(superReflective, mapping);
        return new GenTypeSuper((GenTypeSolid)newType.precisify(superClass));
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        GenType n = lowerBound.mapTparsToTypes(tparams);

        if(n instanceof GenTypeSolid) {
            GenTypeSolid m = (GenTypeSolid)n;
            return new GenTypeSuper(m);
        }
        
        if(n instanceof GenTypeSuper) {
            GenTypeSuper s = (GenTypeSuper)n;
            GenTypeSolid bound = s.lowerBound;
            return new GenTypeSuper(bound);
        }
        
        // otherwise assume n is GenTypeExtends or GenTypeUnbounded.
        return new GenTypeUnbounded();
    }
}
