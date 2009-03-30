/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugger.gentype;


import java.util.*;


public class IntersectionType extends GenTypeSolid
{
    private GenTypeSolid [] intersectTypes;
    
    private IntersectionType(GenTypeSolid [] types)
    {
        if (types.length == 0) {
            throw new IllegalArgumentException();
        }
        
        intersectTypes = types;
    }
    
    /**
     * Factory method. Avoids creation of an intersection to hold only one type.
     * 
     * @param types   The types to create an intersection of
     * @return        The intersection of the given types
     */
    public static GenTypeSolid getIntersection(GenTypeSolid [] types)
    {
        // A quick optimization for a common case.
        if (types.length == 1)
            return types[0];
        
        // First remove cruft. If there are two classes (as opposed to interfaces),
        // combine them.
        
        ArrayList nonclasstypes = new ArrayList();
        GenTypeClass classtype = null;
        
        typeLoop:
            for (int i = 0; i < types.length; i++) {
                GenTypeClass tclass = types[i].asClass();
                if (tclass != null && ! tclass.isInterface()) {
                    if (classtype == null)
                        classtype = tclass;
                    else {
                        classtype = combineClasses(tclass, classtype);
                    }
                }
                else {
                    nonclasstypes.add(types[i]);
                }
            }
        
        // If there is a class, insert it at the head of the list.
        if (classtype != null)
            nonclasstypes.listIterator().add(classtype);
        
        // If there's only type left, return it.
        if (nonclasstypes.size() == 1)
            return (GenTypeSolid) nonclasstypes.get(0);
        
        return new IntersectionType((GenTypeSolid []) nonclasstypes.toArray(new GenTypeSolid[nonclasstypes.size()]));
    }
    
    /**
     * Convenience method to get the intersection of two types.
     * @param a  The first type
     * @param b  The second type
     * @return  The intersection of the two types
     */
    public static GenTypeSolid getIntersection(GenTypeSolid a, GenTypeSolid b)
    {
        return getIntersection(new GenTypeSolid [] {a, b});
    }
    
    /**
     * Combine two classes, to yield one class which is the intersection of both.
     * @param a  The first class
     * @param b  The second class
     * @return The intersection (as a single class)
     */
    public static GenTypeClass combineClasses(GenTypeClass a, GenTypeClass b)
    {
        // One class must be derived from the other
        //GenTypeParameterizable gtp = classtype.precisify(tclass);
        GenTypeClass aE = (GenTypeClass) a.getErasedType();
        GenTypeClass bE = (GenTypeClass) b.getErasedType();
        if (! aE.equals(bE)) {
            if (aE.isAssignableFrom(bE)) {
                a = (GenTypeClass) a.mapToDerived(bE.reflective);
            }
            else {
                b = (GenTypeClass) b.mapToDerived(aE.reflective);
            }
        }
        
        if (a.isRaw())
            return b;
        
        if (b.isRaw())
            return a;
        
        // Handle outer class recursively
        GenTypeClass outer = null;
        if (a.outer != null) {
            outer = combineClasses(a.outer, b.outer);
        }
        
        // Precisify type arguments
        List newParams = null;
        if (a.params != null) {
            newParams = new ArrayList();
            Iterator ia = a.params.iterator();
            Iterator ib = b.params.iterator();
            while (ia.hasNext()) {
                GenTypeParameterizable tpa = (GenTypeParameterizable) ia.next();
                GenTypeParameterizable tpb = (GenTypeParameterizable) ib.next();
                newParams.add(tpa.precisify(tpb));
            }
        }
        
        return new GenTypeClass(a.reflective, newParams, outer);
    }
    
    public String toString(NameTransform nt)
    {
        // This must return a valid java type. We can throw away all but one of
        // the intersection types, and it will be ok. So let's not use
        // java.lang.Object if we have any other choice.
        
        String xx = intersectTypes[0].toString();
        if (intersectTypes.length > 1 && xx.equals("java.lang.Object")) {
            return intersectTypes[1].toString(nt);
        }
        else {
            return intersectTypes[0].toString(nt);
        }
    }
    
    public String toTypeArgString(NameTransform nt)
    {
        // As a type argument, we can only go to a wildcard
        
        return "? extends " + toString(nt);
    }

    public boolean isInterface()
    {
        return false;
    }

    public GenTypeSolid[] getUpperBounds()
    {
        ArrayList ubounds = new ArrayList();
        for (int i = 0; i < intersectTypes.length; i++) {
            GenTypeSolid [] itUbounds = intersectTypes[i].getUpperBounds();
            ubounds.addAll(Arrays.asList(itUbounds));
        }
        return (GenTypeSolid []) ubounds.toArray(new GenTypeSolid[ubounds.size()]);
    }

    public GenTypeSolid[] getLowerBounds()
    {
        return new GenTypeSolid[] {this};
    }
    
    public JavaType mapTparsToTypes(Map tparams)
    {
        GenTypeSolid [] newIsect = new GenTypeSolid[intersectTypes.length];
        for (int i = 0; i < intersectTypes.length; i++) {
            newIsect[i] = (GenTypeSolid) intersectTypes[i].mapTparsToTypes(tparams);
        }
        return new IntersectionType(newIsect);
    }

    public boolean equals(GenTypeParameterizable other)
    {
        if (other == null)
            return false;
        
        return isAssignableFrom(other) && other.isAssignableFrom(this);
    }

    public void getParamsFromTemplate(Map map, GenTypeParameterizable template)
    {
        // This won't be needed
        return;
    }

    public GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // This won't be needed, I think
        throw new UnsupportedOperationException();
    }

    public String arrayComponentName()
    {
        return getErasedType().arrayComponentName();
    }

    public JavaType getErasedType()
    {
        return intersectTypes[0].getErasedType();
    }

    public boolean isAssignableFrom(JavaType t)
    {
        for (int i = 0; i < intersectTypes.length; i++) {
            if (intersectTypes[i].isAssignableFrom(t))
                return true;
        }
        return false;
    }

    public boolean isAssignableFromRaw(JavaType t)
    {
        for (int i = 0; i < intersectTypes.length; i++) {
            if (intersectTypes[i].isAssignableFromRaw(t))
                return true;
        }
        return false;
    }
    
    public void erasedSuperTypes(Set s)
    {
        for (int i = 0; i < intersectTypes.length; i++) {
            intersectTypes[i].erasedSuperTypes(s);
        }
    }
    
    public GenTypeClass [] getReferenceSupertypes()
    {
        ArrayList rsupTypes = new ArrayList();
        for (int i = 0; i < intersectTypes.length; i++) {
            GenTypeClass [] isTypes = intersectTypes[i].getReferenceSupertypes();
            for (int j = 0; j < isTypes.length; j++) {
                rsupTypes.add(isTypes[j]);
            }
        }
        return (GenTypeClass[]) rsupTypes.toArray(new GenTypeClass[rsupTypes.size()]);
    }
    
    public boolean contains(GenTypeParameterizable other)
    {
        return this.equals(other);
    }
}
