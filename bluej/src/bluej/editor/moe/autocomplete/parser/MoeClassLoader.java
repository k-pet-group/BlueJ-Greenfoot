package bluej.editor.moe.autocomplete.parser;

import bluej.classmgr.*;
import bluej.editor.moe.autocomplete.*;
import java.io.*;

/**
 * This class extends the ProjectClassLoader in order
 * to expose the protected getPackages method.  This
 * is bad practive but we had no other way of
 * retrieving the packages.  Unfortuantely
 * getPackages() only retrieves the packages for
 * all the classes that have been loaded.  This means
 * that some packages are missing from the MoeDropDownList
 * when it displays packages.  How can this be fixed?
 *
 * THIS CLASS DOES NOT CONTAIN SPEED JAVA CODE
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class MoeClassLoader extends ClassLoader {

    /**
     * Constructs a new MoeClassLoader by passing the
     * arguments to the super class (ProjectClassLoader)
     */
    public MoeClassLoader(ClassLoader aLoader){
        super(aLoader);
    }

    /**
     * This exposes the protected getPackages() method
     */
    public Package[] getPackages(){
        return super.getPackages();
    }

}