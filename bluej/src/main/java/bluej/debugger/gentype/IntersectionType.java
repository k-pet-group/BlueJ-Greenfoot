/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2020  Michael Kolling and John Rosenberg
 
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


import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.*;

/**
 * Represents an intersection type, eg. I1&I2&I3 as specified in the Java Language
 * Specification. 
 * 
 * @author Davin McCall
 */
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
    @OnThread(Tag.FXPlatform)
    public static GenTypeSolid getIntersection(GenTypeSolid [] types)
    {
        // A quick optimization for a common case.
        if (types.length == 1)
            return types[0];
        
        // Get the real list of types, in case some of the intersecting types
        // are already intersections:
        List<GenTypeSolid> allTypes = new ArrayList<GenTypeSolid>(types.length);
        for (GenTypeSolid type : types) {
            for (GenTypeSolid itype : type.getIntersectionTypes()) {
                allTypes.add(itype);
            }
        }
        
        // Remove cruft. If there are two classes (as opposed to interfaces),
        // combine them.
        
        ArrayList<GenTypeSolid> nonclasstypes = new ArrayList<GenTypeSolid>();
        GenTypeClass classtype = null;
        
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
            return nonclasstypes.get(0);
        
        return new IntersectionType(nonclasstypes.toArray(new GenTypeSolid[nonclasstypes.size()]));
    }
    
    /**
     * Convenience method to get the intersection of two types.
     * @param a  The first type
     * @param b  The second type
     * @return  The intersection of the two types
     */
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
        List<GenTypeParameter> newParams = null;
        if (a.params != null) {
            newParams = new ArrayList<GenTypeParameter>();
            Iterator<? extends GenTypeParameter> ia = a.params.iterator();
            Iterator<? extends GenTypeParameter> ib = b.params.iterator();
            while (ia.hasNext()) {
                GenTypeParameter tpa = ia.next();
                GenTypeParameter tpb = ib.next();
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

    public GenTypeSolid[] getLowerBounds()
    {
        return new GenTypeSolid[] {this};
    }

    @OnThread(Tag.FXPlatform)
    public GenTypeSolid mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        GenTypeSolid [] newIsect = new GenTypeSolid[intersectTypes.length];
        for (int i = 0; i < intersectTypes.length; i++) {
            newIsect[i] = (GenTypeSolid) intersectTypes[i].mapTparsToTypes(tparams);
        }
        return new IntersectionType(newIsect);
    }

    @OnThread(Tag.FXPlatform)
    public boolean equals(JavaType other)
    {
        if (other == null)
            return false;
        
        if (other instanceof JavaType) {
            JavaType otherJT = (JavaType) other;
            return isAssignableFrom(otherJT) && otherJT.isAssignableFrom(this);
        }
        
        return false;
    }

    public void getParamsFromTemplate(Map<String,GenTypeParameter> map, GenTypeParameter template)
    {
        // This won't be needed
        return;
    }

    public GenTypeParameter precisify(GenTypeParameter other)
    {
        // This won't be needed, I think
        throw new UnsupportedOperationException();
    }

    @OnThread(Tag.FXPlatform)
    public String arrayComponentName()
    {
        return getErasedType().arrayComponentName();
    }

    @OnThread(Tag.FXPlatform)
    public JavaType getErasedType()
    {
        return intersectTypes[0].getErasedType();
    }

    @OnThread(Tag.FXPlatform)
    public boolean isAssignableFrom(JavaType t)
    {
        for (int i = 0; i < intersectTypes.length; i++) {
            if (intersectTypes[i].isAssignableFrom(t))
                return true;
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isAssignableFromRaw(JavaType t)
    {
        for (int i = 0; i < intersectTypes.length; i++) {
            if (intersectTypes[i].isAssignableFromRaw(t))
                return true;
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public void erasedSuperTypes(Set<Reflective> s)
    {
        for (int i = 0; i < intersectTypes.length; i++) {
            intersectTypes[i].erasedSuperTypes(s);
        }
    }

    @OnThread(Tag.FXPlatform)
    public GenTypeClass [] getReferenceSupertypes()
    {
        ArrayList<GenTypeClass> rsupTypes = new ArrayList<GenTypeClass>();
        for (int i = 0; i < intersectTypes.length; i++) {
            GenTypeClass [] isTypes = intersectTypes[i].getReferenceSupertypes();
            for (int j = 0; j < isTypes.length; j++) {
                rsupTypes.add(isTypes[j]);
            }
        }
        return rsupTypes.toArray(new GenTypeClass[rsupTypes.size()]);
    }
    
    @Override
    public GenTypeArray getArray()
    {
        return new GenTypeArray(this);
    }
    
    @Override
    public GenTypeSolid[] getIntersectionTypes()
    {
        return intersectTypes;
    }
}
