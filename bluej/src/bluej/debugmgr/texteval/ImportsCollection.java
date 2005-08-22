package bluej.debugmgr.texteval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bluej.Config;

/**
 * Maintain and manage a collection of import statements.
 * 
 * @author Davin McCall
 * @version $Id: ImportsCollection.java 3537 2005-08-22 07:12:11Z davmac $
 */
public class ImportsCollection
{
    private Map normalImports; // non-wildcard non-static type imports
                               // (map String -> PackageOrClass)
    private List wildcardImports;  // wildcard imports (list of PackageOrClass)
    private List staticWildcardImports; // list of ClassEntity
    private Map staticImports; // map String -> List of ClassEntity. The String gives
                                // the name of the imported static member(s) from the given
                                // class(es).
    
    public ImportsCollection()
    {
        normalImports = new HashMap();
        wildcardImports = new ArrayList();
        staticWildcardImports = new ArrayList(); 
        staticImports = new HashMap();
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
    public void addNormalImport(String name, JavaEntity importEntity)
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
    public void addStaticImport(String name, ClassEntity importEntity)
    {
        List l = (List) staticImports.get(name);
        if (l == null) {
            l = new ArrayList();
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
        List l = (List) staticImports.get(name);
        if (l != null) {
            Iterator i = l.iterator();
            while (i.hasNext()) {
                r = (ClassEntity) i.next();
                try {
                    r = (ClassEntity) r.getPackageOrClassMember(name);
                    return r;
                }
                catch (TextParser.SemanticException se) { }
            }
        }
        
        return null;
    }
    
    /**
     * Retrieve all the static import classes for a given import name.
     * May return null if there are no class for the given name.
     * @param name  The name of the import to retrieve.
     */
    public List getStaticImports(String name)
    {
        return (List) staticImports.get(name);
    }
    
    /**
     * Retrieve a list of all the static wildcard imports.
     * @return  A List of ClassEntity
     */
    public List getStaticWildcardImports()
    {
        return staticWildcardImports;
    }
    
    /**
     * Try to find a type accessible via a wildcard import.
     * Return null if no such type can be found.
     * @param name  The name of the imported type to find
     * @return      A ClassEntity, or null if not found
     */
    public ClassEntity getTypeImportWC(String name)
    {
        // Try non-static wildcard imports first
        Iterator i = wildcardImports.iterator();
        
        while (i.hasNext()) {
            PackageOrClass importEntity = (PackageOrClass) i.next();
            try {
                PackageOrClass member = importEntity.getPackageOrClassMember(name);
                if (member.isClass()) {
                    return (ClassEntity) member;
                }
            }
            catch (TextParser.SemanticException se) { }
        }
        
        // Now try static wildcard imports
        i = staticWildcardImports.iterator();
        while (i.hasNext()) {
            ClassEntity importEntity = (ClassEntity) i.next();
            try {
                ClassEntity member = importEntity.getStaticMemberClass(name);
                return member;
            }
            catch (TextParser.SemanticException se) { }
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
        Iterator i = normalImports.values().iterator();
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
        i = staticImports.keySet().iterator();
        while (i.hasNext()) {
            String importName = (String) i.next();
            List l = (List) staticImports.get(importName);
            Iterator j = l.iterator();
            while (j.hasNext()) {
                ClassEntity importEntity = (ClassEntity) j.next();
                rr += "import static " + importEntity.getName();
                rr += "." + importName + ";" + Config.nl;
            }
        }
        
        // Finally the wildcard static imports
        i = staticWildcardImports.iterator();
        while (i.hasNext()) {
            ClassEntity importEntity = (ClassEntity) i.next();
            rr += "import static " + importEntity.getName();
            rr += ".*;" + Config.nl;
        }
        
        return rr;
    }
}
