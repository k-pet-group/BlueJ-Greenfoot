/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2017  Michael Kolling and John Rosenberg 
 
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
import bluej.parser.lexer.LocatableToken;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Maintain and manage a collection of import statements.
 * 
 * @author Davin McCall
 */
public class ImportsCollection
{
    public static class LocatableImport
    {
        private final JavaEntity javaEntity;
        // Start and end may be -1 if non-applicable
        private final int start; // Position of start of import statement (inclusive)
        private final int end; // Position of end of import statement (exclusive)

        public LocatableImport(JavaEntity javaEntity, int start, int end)
        {
            this.javaEntity = javaEntity;
            this.start = start;
            this.end = end;
        }

        public int getStart()
        {
            return start;
        }

        public int getLength()
        {
            return end - start;
        }
    }
    
    /** non-wildcard non-static type imports. the entities should resolve to types. */
    private Map<String, LocatableImport> normalImports;
    /** non-static wildcard imports. The entities should resolve to PackageOrClass */
    private List<LocatableImport> wildcardImports;
    /** static wildcard imports. The entities should resolve to types. */
    private List<LocatableImport> staticWildcardImports; // list of TypeEntity
    private Map<String,List<LocatableImport>> staticImports; // The String gives
                                // the name of the imported static member(s) from the given
                                // class(es).
    
    public ImportsCollection()
    {
        normalImports = new HashMap<>();
        wildcardImports = new ArrayList<>();
        staticWildcardImports = new ArrayList<>(); 
        staticImports = new HashMap<>();
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
     * @param tokens        Either null if non-applicable, or the tokens making up the import 
     */
    public void addNormalImport(String name, JavaEntity importEntity, LocatableToken firstToken, LocatableToken lastToken)
    {
        normalImports.put(name, getLocatableImport(importEntity, firstToken, lastToken));
    }

    private static LocatableImport getLocatableImport(JavaEntity importEntity, LocatableToken firstToken, LocatableToken lastToken)
    {
        int start = -1, end = -1;
        if (firstToken != null && lastToken != null)
        {
            start = firstToken.getPosition();
            end = lastToken.getEndPosition();
        }
        return new LocatableImport(importEntity, start, end);
    }

    /**
     * Add a wildcard import to the collection.
     * @param importEntity  The entity representing the import excluding the final '*' part.
     * @param tokens        Either null if non-applicable, or the tokens making up the import
     */
    public void addWildcardImport(JavaEntity importEntity, LocatableToken firstToken, LocatableToken lastToken)
    {
        wildcardImports.add(getLocatableImport(importEntity, firstToken, lastToken));
    }
    
    /**
     * Add a static (non-wildcard) import to the collection.
     * @param name           The name of the imported member(s)
     * @param importEntity   The class from which members are imported
     * @param tokens        Either null if non-applicable, or the tokens making up the import
     */
    public void addStaticImport(String name, JavaEntity importEntity, LocatableToken firstToken, LocatableToken lastToken)
    {
        List<LocatableImport> l = staticImports.get(name);
        if (l == null) {
            l = new ArrayList<>();
            staticImports.put(name, l);
        }
        l.add(getLocatableImport(importEntity, firstToken, lastToken));
    }
    
    /**
     * Add a static wildcard import to the collection.
     * @param importEntity  The class from which members are imported
     * @param tokens        Either null if non-applicable, or the tokens making up the import
     */
    public void addStaticWildcardImport(TypeEntity importEntity, LocatableToken firstToken, LocatableToken lastToken)
    {
        staticWildcardImports.add(getLocatableImport(importEntity, firstToken, lastToken));
    }

    /**
     * Tries to get information about the import for the given type
     * (if it was imported as a single type, not via a wildcard).
     * @param fullTypeName The fully qualified type name of interest
     * @return Details if it was imported singly, or null (if it was not imported, or was imported via wildcard)
     */
    public LocatableImport getImportInfo(String fullTypeName)
    {
        for (Map.Entry<String, LocatableImport> entry : normalImports.entrySet())
        {
            // Entry key will be short name, like Color, not fully qualified name
            if (fullTypeName.endsWith(entry.getKey()))
            {
                if (entry.getValue().javaEntity.getName().equals(fullTypeName))
                {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Try to find an imported type. Does not check wildcard imports (see 
     * getTypeImportWC). Returns null if no imported type with the given
     * name exists.
     * @param name  The name of the imported type to retrieve
     * @return      A TypeEntity representing the type
     */
    @OnThread(Tag.FXPlatform)
    public TypeEntity getTypeImport(String name)
    {
        // See if there is a normal import for the given name
        LocatableImport r = normalImports.get(name);
        if (r != null)
            return r.javaEntity.resolveAsType();
        
        // There might be a suitable static import
        List<LocatableImport> l = staticImports.get(name);
        if (l != null) {
            for (LocatableImport locatableImport : l)
            {
                TypeEntity rt = locatableImport.javaEntity.resolveAsType();
                if (rt == null)
                {
                    continue;
                }
                rt = rt.getPackageOrClassMember(name);
                if (rt != null)
                {
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
        List<LocatableImport> l = staticImports.get(name);
        if (l == null) {
            l = Collections.emptyList();
        }
        return Utility.mapList(l, li -> li.javaEntity);
    }
    
    /**
     * Retrieve a list of all the static wildcard imports.
     * @return  A List of TypeEntity
     */
    public List<JavaEntity> getStaticWildcardImports()
    {
        return Utility.mapList(staticWildcardImports, li -> li.javaEntity);
    }
    
    /**
     * Try to find a type accessible via a wildcard import.
     * Return null if no such type can be found.
     * @param name  The name of the imported type to find
     * @return      A TypeEntity, or null if not found
     */
    @OnThread(Tag.FXPlatform)
    public TypeEntity getTypeImportWC(String name)
    {
        // Try non-static wildcard imports first

        for (LocatableImport entity : wildcardImports)
        {
            PackageOrClass importEntity = entity.javaEntity.resolveAsPackageOrClass();
            if (importEntity == null)
            {
                continue;
            }
            PackageOrClass member = importEntity.getPackageOrClassMember(name);
            if (member == null)
            {
                continue;
            }
            TypeEntity clMember = member.resolveAsType();
            if (clMember != null)
            {
                return clMember;
            }
        }
        
        // Now try static wildcard imports
        for (LocatableImport staticWildcardImport : staticWildcardImports)
        {
            TypeEntity importEntity = staticWildcardImport.javaEntity.resolveAsType();
            if (importEntity == null)
            {
                continue;
            }
            GenTypeClass clType = importEntity.getClassType();
            if (clType != null)
            {
                /*
                List<Reflective> inners = clType.getReflective().getInners();
                for (Reflective inner : inners) {
                    String innerName = inner.getName();
                    innerName = innerName.substring(innerName.lastIndexOf('$'));
                    if (name.equals(innerName)) {
                        return new TypeEntity(new GenTypeClass(inner));
                    }
                }
                */

                String innerName = clType.classloaderName() + "$" + name;
                Reflective inner = clType.getReflective().getRelativeClass(innerName);
                if (inner != null)
                {
                    return new TypeEntity(new GenTypeClass(inner));
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
        for (LocatableImport locatableImport : normalImports.values())
        {
            // String importName = ()
            JavaEntity importEntity = locatableImport.javaEntity;

            // build the statement string
            rr += "import ";
            rr += importEntity.getName() + ";" + Config.nl;
        }
        
        // Now do the (non-static) wildcard imports
        for (LocatableImport wildcardImport : wildcardImports)
        {
            PackageOrClass importEntity = (PackageOrClass) wildcardImport.javaEntity;
            rr += "import ";
            rr += importEntity.getName() + ".*;" + Config.nl;
        }
        
        // Now the static imports (non-wildcard)
        for (String importName : staticImports.keySet())
        {
            List<LocatableImport> l = staticImports.get(importName);
            for (LocatableImport locatableImport : l)
            {
                TypeEntity importEntity = locatableImport.javaEntity.resolveAsType();
                if (importEntity != null)
                {
                    rr += "import static " + importEntity.getName();
                    rr += "." + importName + ";" + Config.nl;
                }
            }
        }
        
        // Finally the wildcard static imports
        for (LocatableImport staticWildcardImport : staticWildcardImports)
        {
            JavaEntity importEntity = staticWildcardImport.javaEntity;
            rr += "import static " + importEntity.getName();
            rr += ".*;" + Config.nl;
        }
        
        return rr;
    }
}
