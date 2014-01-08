/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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
import bluej.debugger.gentype.Reflective;
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
    /** non-wildcard non-static type imports. the entities should resolve to types. */
    private Map<String,JavaEntity> normalImports;
    /** non-static wildcard imports. The entities should resolve to PackageOrClass */
    private List<JavaEntity> wildcardImports;
    /** static wildcard imports. The entities should resolve to types. */
    private List<JavaEntity> staticWildcardImports; // list of TypeEntity
    private Map<String,List<JavaEntity>> staticImports; // The String gives
                                // the name of the imported static member(s) from the given
                                // class(es).
    
    public ImportsCollection()
    {
        normalImports = new HashMap<String,JavaEntity>();
        wildcardImports = new ArrayList<JavaEntity>();
        staticWildcardImports = new ArrayList<JavaEntity>(); 
        staticImports = new HashMap<String,List<JavaEntity>>();
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
     * @param importEntity  The entity corresponding to the imported type
     */
    public void addNormalImport(String name, JavaEntity importEntity)
    {
        normalImports.put(name, importEntity);
    }
    
    /**
     * Add a wildcard import to the collection.
     * @param importEntity  The entity representing the import excluding the final '*' part.
     */
    public void addWildcardImport(JavaEntity importEntity)
    {
        wildcardImports.add(importEntity);
    }
    
    /**
     * Add a static (non-wildcard) import to the collection.
     * @param name           The name of the imported member(s)
     * @param importEntity   The class from which members are imported
     */
    public void addStaticImport(String name, JavaEntity importEntity)
    {
        List<JavaEntity> l = staticImports.get(name);
        if (l == null) {
            l = new ArrayList<JavaEntity>();
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
        JavaEntity r = normalImports.get(name);
        if (r != null)
            return r.resolveAsType();
        
        // There might be a suitable static import
        List<JavaEntity> l = staticImports.get(name);
        if (l != null) {
            Iterator<JavaEntity> i = l.iterator();
            while (i.hasNext()) {
                TypeEntity rt = i.next().resolveAsType();
                if (rt == null) {
                    continue;
                }
                rt = rt.getPackageOrClassMember(name);
                if (rt != null) {
                    return rt;
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
    public List<JavaEntity> getStaticImports(String name)
    {
        List<JavaEntity> l = staticImports.get(name);
        if (l == null) {
            l = Collections.emptyList();
        }
        return l;
    }
    
    /**
     * Retrieve a list of all the static wildcard imports.
     * @return  A List of TypeEntity
     */
    public List<JavaEntity> getStaticWildcardImports()
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
        Iterator<JavaEntity> i = wildcardImports.iterator();
        
        while (i.hasNext()) {
            JavaEntity entity = i.next();
            PackageOrClass importEntity = entity.resolveAsPackageOrClass();
            if (importEntity == null) {
                continue;
            }
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
        Iterator<JavaEntity> j = staticWildcardImports.iterator();
        while (j.hasNext()) {
            TypeEntity importEntity = j.next().resolveAsType();
            if (importEntity == null) {
                continue;
            }
            GenTypeClass clType = importEntity.getClassType();
            if (clType != null) {
                List<Reflective> inners = clType.getReflective().getInners();
                for (Reflective inner : inners) {
                    String innerName = inner.getName();
                    innerName = innerName.substring(innerName.lastIndexOf('$'));
                    if (name.equals(innerName)) {
                        return new TypeEntity(new GenTypeClass(inner));
                    }
                }
            }
        }
        
        return null;
    }
    
    /*
     * Convert the imports collection to a series of java "import" statements.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        String rr = "";
        
        // First process the normal (non-wildcard non-static) imports
        Iterator<? extends JavaEntity> i = normalImports.values().iterator();
        while (i.hasNext()) {
            // String importName = ()
            JavaEntity importEntity = i.next();
            
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
            List<JavaEntity> l = staticImports.get(importName);
            Iterator<JavaEntity> j = l.iterator();
            while (j.hasNext()) {
                TypeEntity importEntity = j.next().resolveAsType();
                if (importEntity != null) {
                    rr += "import static " + importEntity.getName();
                    rr += "." + importName + ";" + Config.nl;
                }
            }
        }
        
        // Finally the wildcard static imports
        Iterator<JavaEntity> iii = staticWildcardImports.iterator();
        while (i.hasNext()) {
            JavaEntity importEntity = iii.next();
            rr += "import static " + importEntity.getName();
            rr += ".*;" + Config.nl;
        }
        
        return rr;
    }
}
