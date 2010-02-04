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
package bluej.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bluej.Config;
import bluej.debugger.gentype.GenTypeClass;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;

/**
 * Maintain and manage a collection of import statements.
 * 
 * @author Davin McCall
 */
public class ImportsCollection
{
    private Map<String,TypeEntity> normalImports; // non-wildcard non-static type imports
    private List<PackageOrClass> wildcardImports;  // wildcard imports
    private List<TypeEntity> staticWildcardImports; // list of TypeEntity
    private Map<String,List<TypeEntity>> staticImports; // The String gives
                                // the name of the imported static member(s) from the given
                                // class(es).
    
    public ImportsCollection()
    {
        normalImports = new HashMap<String,TypeEntity>();
        wildcardImports = new ArrayList<PackageOrClass>();
        staticWildcardImports = new ArrayList<TypeEntity>(); 
        staticImports = new HashMap<String,List<TypeEntity>>();
    }
    
    /**
     * Remove all imports from the collection.
     */
    public void clear()
    {
        normalImports.clear();
        wildcardImports.clear();
    }
    
    /**
     * Add a (non-wildcard) import to the collection.
     * @param name          The short name of the import
     * @param importEntity  The entity corresponding to the import
     */
    public void addNormalImport(String name, TypeEntity importEntity)
    {
        normalImports.put(name, importEntity);
    }
    
    /**
     * Add a wildcard import to the collection.
     * @param importEntity  The entity representing the import excluding the final '*' part.
     */
    public void addWildcardImport(PackageOrClass importEntity)
    {
        wildcardImports.add(importEntity);
    }
    
    /**
     * Add a static (non-wildcard) import to the collection.
     * @param name           The name of the imported member(s)
     * @param importEntity   The class from which members are imported
     */
    public void addStaticImport(String name, TypeEntity importEntity)
    {
        List<TypeEntity> l = staticImports.get(name);
        if (l == null) {
            l = new ArrayList<TypeEntity>();
            staticImports.put(name, l);
        }
        l.add(importEntity);
    }
    
    /**
     * Add a static wildcard import to the collection.
     * @param importEntity  The class from which members are imported
     */
    public void addStaticWildcardImport(TypeEntity importEntity)
    {
        staticWildcardImports.add(importEntity);
    }
    
    /**
     * Try to find an imported type. Does not check wildcard imports (see 
     * getTypeImportWC). Returns null if no imported type with the given
     * name exists.
     * @param name  The name of the imported type to retrieve
     * @return      A TypeEntity representing the type
     */
    public TypeEntity getTypeImport(String name)
    {
        // See if there is a normal import for the given name
        TypeEntity r = (TypeEntity) normalImports.get(name);
        if (r != null)
            return r;
        
        // There might be a suitable static import
        List<TypeEntity> l = staticImports.get(name);
        if (l != null) {
            Iterator<TypeEntity> i = l.iterator();
            while (i.hasNext()) {
                r = (TypeEntity) i.next();
                r = (TypeEntity) r.getPackageOrClassMember(name);
                if (r != null) {
                    return r;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Retrieve all the static import classes for a given import name. The
     * returned list must not be modified.
     * 
     * @param name  The name of the import to retrieve.
     */
    public List<TypeEntity> getStaticImports(String name)
    {
        List<TypeEntity> l = staticImports.get(name);
        if (l == null) {
            l = Collections.emptyList();
        }
        return l;
    }
    
    /**
     * Retrieve a list of all the static wildcard imports.
     * @return  A List of TypeEntity
     */
    public List<TypeEntity> getStaticWildcardImports()
    {
        return staticWildcardImports;
    }
    
    /**
     * Try to find a type accessible via a wildcard import.
     * Return null if no such type can be found.
     * @param name  The name of the imported type to find
     * @return      A TypeEntity, or null if not found
     */
    public TypeEntity getTypeImportWC(String name)
    {
        // Try non-static wildcard imports first
        Iterator<PackageOrClass> i = wildcardImports.iterator();
        
        while (i.hasNext()) {
            PackageOrClass importEntity = i.next();
            PackageOrClass member = importEntity.getPackageOrClassMember(name);
            if (member == null) {
                continue;
            }
            TypeEntity clMember = member.resolveAsType();
            if (clMember != null) {
                return clMember;
            }
        }
        
        // Now try static wildcard imports
        Iterator<TypeEntity> j = staticWildcardImports.iterator();
        while (j.hasNext()) {
            TypeEntity importEntity = j.next();
            GenTypeClass clType = importEntity.getClassType();
            List<GenTypeClass> inners = clType.getReflective().getInners();
            for (GenTypeClass inner : inners) {
                String innerName = inner.getReflective().getName();
                innerName = innerName.substring(innerName.lastIndexOf('$'));
                if (name.equals(innerName)) {
                    return new TypeEntity(inner);
                }
            }
        }
        
        return null;
    }
    
    /*
     * Convert the imports collection to a series of java "import" statements.
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String rr = "";
        
        // First process the normal (non-wildcard non-static) imports
        Iterator<? extends PackageOrClass> i = normalImports.values().iterator();
        while (i.hasNext()) {
            // String importName = ()
            JavaEntity importEntity = (JavaEntity) i.next();
            
            // build the statement string
            rr += "import ";
            rr += importEntity.getName() + ";" + Config.nl;
        }
        
        // Now do the (non-static) wildcard imports
        i = wildcardImports.iterator();
        while (i.hasNext()) {
            PackageOrClass importEntity = (PackageOrClass) i.next();
            rr += "import ";
            rr += importEntity.getName() + ".*;" + Config.nl;
        }
        
        // Now the static imports (non-wildcard)
        Iterator<String> ii = staticImports.keySet().iterator();
        while (ii.hasNext()) {
            String importName = ii.next();
            List<TypeEntity> l = staticImports.get(importName);
            Iterator<TypeEntity> j = l.iterator();
            while (j.hasNext()) {
                TypeEntity importEntity = (TypeEntity) j.next();
                rr += "import static " + importEntity.getName();
                rr += "." + importName + ";" + Config.nl;
            }
        }
        
        // Finally the wildcard static imports
        Iterator<TypeEntity> iii = staticWildcardImports.iterator();
        while (i.hasNext()) {
            TypeEntity importEntity = iii.next();
            rr += "import static " + importEntity.getName();
            rr += ".*;" + Config.nl;
        }
        
        return rr;
    }
}
