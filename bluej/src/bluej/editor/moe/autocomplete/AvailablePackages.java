package bluej.editor.moe.autocomplete;

import bluej.classmgr.ClassMgr;
import bluej.editor.moe.autocomplete.parser.MoeClassLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


/**
 * This class is used to determine all the available packages and to separate
 * them out into separate collections using the root of each package. When
 * an AvailablePackages object is constructed it determines all the available
 * packages and creates a hashmap.  The hashmap has it's keys populated with
 * all the root package names and the corresponding values are ArrayLists
 * of packages that begin with the key.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class AvailablePackages {

    //This array stores all the available packages.
    private Package[] pkgs;

    //This hashmap stores all the available packages by the root package names.
    private HashMap packagesByRoot;

    //This constructor simply performs a scan for the available packages.
    public AvailablePackages( ClassLoader projectClassLoader ){
        scanForPackages(projectClassLoader );
    }



    /**
     * This method determines the available packages and puts them into groups
     * using the root of each package name.  The root of java.lang is java and
     * the root of javax.swing.event is javax.  After this method has completed
     * the packagesByRoot hashmap will contain a key for every different package
     * root.  The corresponding values in the hashmap will be ArrayLists of
     * packages that begin with the key.
     */
    private void scanForPackages( ClassLoader projectClassLoader){

    //ProjectClassLoader loader = ClassMgr.getProjectLoader(projRoot);
        MoeClassLoader loader = new MoeClassLoader(projectClassLoader);

        packagesByRoot = new HashMap();

        pkgs = loader.getPackages();

        for(int i=0; i<pkgs.length; i++){
            String pkgName = pkgs[i].getName();
            Debug.printAvailablePackagesMessage(pkgName);

            int dotPos = pkgName.indexOf(".");
            if (dotPos >= 1){
                String rootPackageName = pkgName.substring(0, dotPos);
                ArrayList pkgSubSet = (ArrayList) packagesByRoot.get(rootPackageName);
                if(pkgSubSet==null){
                    pkgSubSet = new ArrayList();
                    pkgSubSet.add(pkgs[i]);
                    packagesByRoot.put(rootPackageName, pkgSubSet);
                }
                else{
                    pkgSubSet.add(pkgs[i]);
                }

            }
        }

    }



    /**
     * This method returns an ArrayList of MoeDropDownPackages that begin with
     * the specified package root.  The returned MoeDropDownPackages can
     * then be used to populate a MoeDropDownList
     *
     * @param pkgRoot The returned ArrayList will contain packages that
     *                begin with this String.
     * @return An ArrayList of MoeDropDownPackages that begin with pkgRoot.
     */
    public ArrayList getMoeDropDownPackagesWithRoot(String pkgRoot){
        ArrayList mddPackages = new ArrayList();
        ArrayList pkgs = getPackagesWithRoot(pkgRoot);
        Iterator it = pkgs.iterator();
        while(it.hasNext()){
            Package p = (Package) it.next();
            try{
                MoeDropDownPackage mddPackage = new MoeDropDownPackage(p);
                mddPackages.add(mddPackage);
            }
            catch(IllegalArgumentException e){
                //Package doesn't contain sub packages
                //Just ignore it
            }

        }

        return mddPackages;
    }



    /**
     * This method returns an ArrayList of Packages that begin with
     * the specified package root.
     *
     * @param pkgRoot The returned ArrayList will contain Packages that
     *                begin with this String.
     * @return An ArrayList of Packages that begin with pkgRoot.
     */
    public ArrayList getPackagesWithRoot(String pkgRoot){
        ArrayList pkgSubSet = (ArrayList) packagesByRoot.get(pkgRoot);
        if(pkgSubSet==null) pkgSubSet = new ArrayList();
        return pkgSubSet;
    }



    /**
     * This method returns a String array containing all the
     * package roots.
     *
     * @return a String array containing all the package roots.
     */
    public String[] getPackageRoots(){
        Set roots = packagesByRoot.keySet();
        String[] s = {};
        String[] rootsArray = (String[]) roots.toArray(s);
        return rootsArray;
    }

}
