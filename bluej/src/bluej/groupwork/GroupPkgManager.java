package bluej.groupwork;

import bluej.pkgmgr.*;
import com.ice.cvsc.*;

/**
 ** @version $Id: GroupPkgManager.java 401 2000-02-29 01:42:12Z markus $
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

    /**
     * Method that opens a Group package in Bluej 
     * ./temp this method is not being used at all 
     * @param
     * @param thisFrame  The current Frame
     * @returns   A Group package or null if someting went wrong
     */
    void openGrpPackage(PkgMgrFrame thisFrame);

    /**
     * Checkout a local copy from the Master source
     *  
     * @param thisFrame  The current Frame   
     * @returns void
     */
    void checkoutPackage(PkgMgrFrame thisFrame);
    
    /**
     * Creates a new module in the Repository using a specified local package
     *  
     * @param thisFrame  The current Frame   
     * @returns void
     */
    void importPackage(PkgMgrFrame thisFrame);

    /**
     * Performs the various group commands 
     *  
     * @param thisFrame  The current Frame 
     * @param command  The command 
     * @param entries  The entries to act upon 
     * @returns True if successful, false if not
     */
    boolean performCommand(PkgMgrFrame thisFrame, int command,
			   CVSEntryVector entries );

     /**
     * Performs an update from the repository 
     *  
     * @param thisFrame  The current Frame   
     *
     * @returns True if successful, false if not
     */
    boolean updatePkg(PkgMgrFrame thisFrame);

     /**
     * Performs a commit to the repository 
     *  
     * @param thisFrame  The current Frame   
     *
     * @returns True if successful, false if not
     */
    boolean commitPkg(PkgMgrFrame thisFrame);

     /**
     * Adds a Class to the package
     *  
     * @param thisFrame  The current Frame   
     * @param className  The name of the class  
     * @returns True if successful, false if not
     */
    boolean addClass(PkgFrame thisFrame, String className);

     /**
     * Removes a Class from the package
     *  
     * @param thisFrame  The current Frame   
     * @param className  The name of the class 
     * @returns True if successful, false if not
     */
    boolean removeClass(PkgFrame thisFrame, String className);

     /**
     * Checks the status of the files in the Package
     *  
     * @param thisFrame  The current Frame   
     * @param
     * @returns True if successful, false if not
     */
    boolean pkgStatus(PkgFrame thisFrame);

} // end interface GroupPkgManager








