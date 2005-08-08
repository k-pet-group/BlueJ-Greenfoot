package bluej.debugger.gentype;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class GenTypeArray extends GenTypeClass
{
    JavaType baseType;
    
    public GenTypeArray(JavaType baseType, Reflective r)
    {
        super(r);
        this.baseType = baseType;
    }

    public String toString(boolean stripPrefix)
    {
        return baseType.toString(stripPrefix) + "[]";
    }
    
    public String toString(NameTransform nt)
    {
        if(baseType instanceof GenTypeParameterizable)
            return ((GenTypeParameterizable)baseType).toString(nt) + "[]";
        else
            return baseType.toString() + "[]";
    }
    
    public String arrayComponentName()
    {
        return "[" + baseType.arrayComponentName();
    }
    
    public JavaType getArrayComponent()
    {
        return baseType;
    }
    
    public JavaType mapTparsToTypes(Map tparams)
    {
        JavaType newBase = baseType.mapTparsToTypes(tparams);
        if( newBase == baseType )
            return this;
        else
            return new GenTypeArray(newBase, reflective);
    }

    public GenTypeSolid getLowerBound()
    {
        if (baseType.isPrimitive())
            return this;
        else {
            GenTypeSolid Lbounds = ((GenTypeParameterizable) baseType).getLowerBound();
            Reflective newR = Lbounds.getErasedType().asClass().reflective.getArrayOf();
            return new GenTypeArray(Lbounds, newR);
        }
    }
    
    public JavaType getErasedType()
    {
        if (baseType instanceof GenTypeParameterizable) {
            GenTypeParameterizable pbtype = (GenTypeParameterizable) baseType;
            GenTypeClass pbErased = (GenTypeClass) pbtype.getErasedType();
            return new GenTypeArray(pbErased, pbErased.reflective.getArrayOf());
        }
        else
            return this;
    }

    public void erasedSuperTypes(Set s)
    {
        Stack refs = new Stack();
        if (baseType instanceof GenTypeSolid) {
            GenTypeSolid sbaseType = (GenTypeSolid) baseType;
            Set baseEST = new HashSet();
            sbaseType.erasedSuperTypes(baseEST);
            Iterator i = baseEST.iterator();
            while (i.hasNext()) {
                refs.push(((Reflective) i.next()).getArrayOf());
            }
        }
        else
            refs.push(reflective);
        
        while(! refs.empty()) {
            Reflective r = (Reflective) refs.pop();
            if (! s.contains(r)) {
                // The reflective is not already in the set, so
                // add it and queue its supertypes
                s.add(r);
                refs.addAll(r.getSuperTypesR());
            }
        }
    }

}
