package bluej.pkgmgr;

import java.util.Hashtable;

import bluej.utility.Utility;

/**
 * A simple hashtable-based cache for storing Package objects with
 * their location on disk as keys.
 * 
 * @author $Author: mik $
 * @version $Id: PackageCacheMgr.java 36 1999-04-27 04:04:54Z mik $
 */
public class PackageCacheMgr {
    // cache of previously viewed Package objects
    // keys = pathname to package, objects = package itself
    private Hashtable cache = new Hashtable(); 

    /**
     * Look in the package cache for the package with the specified name.
     * 
     * @param key
     * @return true if the package is currently in the cache
     */
    public boolean isPackageInCache(String key) {
	    return (cache.get(key) != null);
    }
   
    /**
     * Return the Package stored with this key.
     * 
     * @param key the location on disk where the package is stored
     * @return the Package object stored with this key, or null if none found.
     */
    public Package getPackageFromCache(String key) {
	    Package pkg = null;
	    
	    try {
		pkg = (Package)cache.get(key);
	    } catch (ClassCastException cce) {
		    Utility.reportError(cce.getMessage());
	    }
	    
	    return pkg;
    }
    
    /**
     * Add the specified package object to the cache, using the package
     * name as the key to retrieve.
     * 
     * @param key the directory of the package 
     * @param pkg the package to add
     * @return true if packageName did not already exist in cache
     */
    public boolean addPackageToCache(String key, Package pkg) {
	    return cache.put(key, pkg) == null;
    }
    
}
