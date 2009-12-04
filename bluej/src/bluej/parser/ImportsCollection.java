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

import java.util.*;

import bluej.Config;
import bluej.parser.entity.ClassEntity;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageOrClass;

/**
 * Maintain and manage a collection of import statements.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class ImportsCollection
{
    private Map<String,PackageOrClass> normalImports; // non-wildcard non-static type imports
    private List<JavaEntity> wildcardImports;  // wildcard imports
    private List<ClassEntity> staticWildcardImports; // list of ClassEntity
    private Map<String,List<ClassEntity>> staticImports; // The String gives
                                // the name of the imported static member(s) from the given
                                // class(es).
    
    public ImportsCollection()
    {
        normalImports = new HashMap<String,PackageOrClass>();
        wildcardImports = new ArrayList<JavaEntity>();
        staticWildcardImports = new ArrayList<ClassEntity>(); 
        staticImports = new HashMap<String,List<ClassEntity>>();
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
    public void addNormalImport(String name, PackageOrClass importEntity)
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
    public void addStaticImport(String name, ClassEntity importEntity)
    {
        List<ClassEntity> l = staticImports.get(name);
        if (l == null) {
            l = new ArrayList<ClassEntity>();
            staticImports.put(name, l);
        }
        l.add(importEntity);
    }
    
    /**
     * Add a static wildcard import to the collection.
     * @param importEntity  The class from which members are imported
     */
    public void addStaticWildcardImport(ClassEntity importEntity)
    {
        staticWildcardImports.add(importEntity);
    }
    
    /**
     * Try to find an imported type. Does not check wildcard imports (see 
     * getTypeImportWC). Returns null if no imported type with the given
     * name exists.
     * @param name  The name of the imported type to retrieve
     * @return      A ClassEntity representing the type
     */
    public ClassEntity getTypeImport(String name)
    {
        // See if there is a normal import for the given name
        ClassEntity r = (ClassEntity) normalImports.get(name);
        if (r != null)
            return r;
        
        // There might be a suitable static import
        List<ClassEntity> l = staticImports.get(name);
        if (l != null) {
            Iterator<ClassEntity> i = l.iterator();
            while (i.hasNext()) {
                r = (ClassEntity) i.next();
                r = (ClassEntity) r.getPackageOrClassMember(name);
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
    public List<ClassEntity> getStaticImports(String name)
    {
        List<ClassEntity> l = staticImports.get(name);
        if (l == null) {
            l = Collections.emptyList();
        }
        return l;
    }
    
    /**
     * Retrieve a list of all the static wildcard imports.
     * @return  A List of ClassEntity
     */
    public List<ClassEntity> getStaticWildcardImports()
    {
        return staticWildcardImports;
    }
    
    /**
     * Try to find a type accessible via a wildcard import.
     * Return null if no such type can be found.
     * @param name  The name of the imported type to find
     * @return      A ClassEntity, or null if not found
     */
//    public ClassEntity getTypeImportWC(String name)
//    {
//        // Try non-static wildcard imports first
//        Iterator<PackageOrClass> i = wildcardImports.iterator();
//        
//        while (i.hasNext()) {
//            PackageOrClass importEntity = i.next();
//            PackageOrClass member = importEntity.getPackageOrClassMember(name);
//            if (member != null && member.isClass()) {
//                return (ClassEntity) member;
//            }
//        }
//        
//        // Now try static wildcard imports
//        Iterator<ClassEntity> j = staticWildcardImports.iterator();
//        while (j.hasNext()) {
//            ClassEntity importEntity = j.next();
//            try {
//                ClassEntity member = importEntity.getStaticMemberClass(name);
//                return member;
//            }
//            catch (SemanticException se) { }
//        }
//        
//        return null;
//    }
    
    /*
     * Convert the imports collection to a series of java "import" statements.
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String rr = "";
        
        // First process the normal (non-wildcard non-static) imports
        Iterator<PackageOrClass> i = normalImports.values().iterator();
        while (i.hasNext()) {
            // String importName = ()
            JavaEntity importEntity = (JavaEntity) i.next();
            
            // build the statement string
            rr += "import ";
            rr += importEntity.getName() + ";" + Config.nl;
        }
        
        // Now do the (non-static) wildcard imports
//        i = wildcardImports.iterator();
//        while (i.hasNext()) {
//            PackageOrClass importEntity = (PackageOrClass) i.next();
//            rr += "import ";
//            rr += importEntity.getName() + ".*;" + Config.nl;
//        }
        
        // Now the static imports (non-wildcard)
        Iterator<String> ii = staticImports.keySet().iterator();
        while (ii.hasNext()) {
            String importName = ii.next();
            List<ClassEntity> l = staticImports.get(importName);
            Iterator<ClassEntity> j = l.iterator();
            while (j.hasNext()) {
                ClassEntity importEntity = (ClassEntity) j.next();
                rr += "import static " + importEntity.getName();
                rr += "." + importName + ";" + Config.nl;
            }
        }
        
        // Finally the wildcard static imports
        Iterator<ClassEntity> iii = staticWildcardImports.iterator();
        while (i.hasNext()) {
            ClassEntity importEntity = iii.next();
            rr += "import static " + importEntity.getName();
            rr += ".*;" + Config.nl;
        }
        
        return rr;
    }
}
