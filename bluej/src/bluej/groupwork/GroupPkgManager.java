package bluej.groupwork;

import java.io.*;

import bluej.pkgmgr.*;

import com.ice.cvsc.*;

/**
** @version $Id: GroupPkgManager.java 504 2000-05-24 04:44:14Z markus $
** @author Markus Ostman
** Interface between the group support and the rest of BlueJ
**/
public interface GroupPkgManager
{
    //Variabels for the different commands
    static final int REMOVE = 1000;
    static final int ADD    = REMOVE+1;
    static final int UPDATE = ADD+1;
    static final int COMMIT = UPDATE+1;
    static final int STATUS = COMMIT+1;
    static final int CHECKOUT = STATUS+1;
    static final int RELEASE = CHECKOUT+1;
    static final int LOG = RELEASE+1;

    /**
     * Method that opens a Group package in Bluej 
     * ./temp this method is not being used at all 
     * @param
     * @param frame  The current Frame
     * @returns   A Group package or null if someting went wrong
     */
    void openGrpPackage(PkgMgrFrame frame);

    /**
     * Checkout a local copy from the Master source
     *  
     * @param frame  The current Frame   
     * @returns void
     */
    void checkoutPackage(PkgMgrFrame frame);

    /**
     * Creates a new module in the Repository using a specified local package
     *  
     * @param frame  The current Frame   
     * @returns void
     */
    void importPackage(PkgMgrFrame frame);

    /**
     * Performs the various group commands 
     *  
     * @param frame      The current Frame 
     * @param command    The command 
     * @param entries    The entries to act upon
     * @param implicit   Is it an implicit call to the method
     * @returns True if successful, false if not
     */
    boolean performCommand(PkgMgrFrame frame, int command,
                           CVSEntryVector entries, boolean implicit );

    /**
     * Performs an update from the repository 
     *  
     * @param frame  The current Frame
     * @param implicit   was this an implicit call or not 
     *
     * @returns True if successful, false if not
     */
    boolean updatePkg(PkgMgrFrame frame, boolean implicit);

    /**
     * Performs a commit to the repository 
     *  
     * @param frame  The current Frame   
     *
     * @returns True if successful, false if not
     */
    boolean commitPkg(PkgMgrFrame frame);

    /**
     * Adds a Class to the package
     *  
     * @param frame  The current Frame   
     * @param className  The name of the class  
     * @returns True if successful, false if not
     */
    boolean addClass(PkgMgrFrame frame, String className);

    /**
     * Removes a Class from the package
     *  
     * @param frame  The current Frame   
     * @param className  The name of the class 
     * @returns True if successful, false if not
     */
    boolean removeClass(PkgMgrFrame frame, String className);

    /**
     * Checks the status of the files in the Package
     *  
     * @param frame  The current Frame   
     * @param
     * @returns True if successful, false if not
     */
    boolean pkgStatus(PkgMgrFrame frame);

    /**
     * Displays the log messages for the files in the Package
     *  
     * @param frame  The current Frame   
     * @param
     * @returns True if successful, false if not
     */
    boolean pkgLog(PkgMgrFrame frame);

    /**
     * Removes the local working directory in a safe way,
     * i.e. makes sure that there no changes etc. that have not 
     * been committed.
     *  
     * @param frame        The current Frame   
     * @param releaseFile  The file that we want to release
     *                     (project directory)
     * @returns True if successful, false if not
     */
    boolean releasePkg(PkgMgrFrame frame, File releaseFile);

} // end interface GroupPkgManager








