package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.SortedProperties;
import bluej.pkgmgr.*;

import com.ice.cvsc.*;
import com.ice.jcvsii.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;

/**
 * This Class implements the Interface GroupPkgManager using jCVS classes
 * @author Markus Ostman, with influences from jCVS source code written by
 * Tim Endres.
 */
public class CVSGroupInfo
    implements GroupInfo, CVSUserInterface
{
    // public static variables

    public static CVSGroupInfo groupInfo;  // the info object itself
    // private variables
    private CVSProject project;
    private JFrame currentFrame;
    //trace required?
    private boolean traceReq = false;
    // user preferences

    private CVSEntryVector addedOffLine;
    private CVSEntryVector removedOffLine;

    // =========================== PUBLIC METHODS ===========================

    public CVSGroupInfo()
    {
        //Initialize jCVS components
        //This is necessary because of weird design in jCVS code
        JcvsInit.doInitialize();
        addedOffLine = new CVSEntryVector();
        removedOffLine = new CVSEntryVector();
    }

    /**
     * The Method is inherited from GroupInfo
     * @param pkgDir The directory name of a Package
     * @param
     * @returns Group information or null if something went wrong
     */
    public GroupInfo getGroupInfo(String pkgDir)
    {
	groupInfo.project = new CVSProject();
	return groupInfo;
    }


    /*
     *
     * @param
     * @returns CVSProject
     */
    public CVSProject getProject()
    {
        return this.project;
    }

    /**
     *
     * The method is inherited from GroupInfo
     * @param localDirName The path to the local directory
     * @param currentFrame The current Bluej Frame
     * @param password The project password
     * @returns void
     */
    public void initializeGroupInfo(String localDirName,
                                        JFrame currentFrame,
                                        String password)
    {
        this.currentFrame=currentFrame;
        //Create path to the CVS directory for the project
        String rootDirPath = createRootPath(localDirName);
        File rootDirFile = new File(rootDirPath);
        //File rootDirFile = new File(localDirName);
        Config cfg = Config.getInstance();
        UserPrefs prefs = cfg.getPreferences();
        this.traceReq = prefs.getBoolean(Config.GLOBAL_CVS_TRACE_ALL, false );

        CVSClient client = new CVSClient();
        this.project = new CVSProject( client );
        Debug.message("CVSGrpInfo, line96:"+cfg.getTemporaryDirectory());
        project.setTempDirectory( cfg.getTemporaryDirectory() );

	project.setAllowsGzipFileMode(prefs.getBoolean
                                      (Config.GLOBAL_ALLOWS_FILE_GZIP, true));
	project.setGzipStreamLevel(prefs.getInteger
                                   (Config.GLOBAL_GZIP_STREAM_LEVEL, 0));

	try {
	    project.openProject( rootDirFile );

	    int cvsPort =
		CVSUtilities.computePortNum(project.getClient().getHostName(),
					    CVSRequest.METHOD_INETD,
					    project.isPServer() );

	    project.getClient().setPort( cvsPort );

	    if ( project.getConnectionMethod()== CVSRequest.METHOD_RSH )
		{
		    CVSUtilities.establishRSHProcess( project );
		}

	    project.setServerCommand(
				     CVSUtilities.establishServerCommand
				     ( project.getClient().getHostName(),
				       project.getConnectionMethod(),
				       project.isPServer() ) );

	    project.setSetVariables
		( CVSUtilities.getUserSetVariables
		  ( project.getClient().getHostName() ) );

	    String title = project.getRepository() + " Project";

	    if ( password != null )
		{
		    this.project.setPassword( password );
		}
	    else
		{
                    //skip login when opening
                    // verifyLogin();
		}
	}
	catch ( IOException ex )
	    {
		String[] fmtArgs =
		{ rootDirFile.getPath(), ex.getMessage() };
		String msg = ResourceMgr.getInstance().getUIFormat
		    ( "project.openproject.failed.msg", fmtArgs );
		String title = ResourceMgr.getInstance().getUIString
		    ( "project.openproject.failed.title" );
		JOptionPane.showMessageDialog
		    ( null, msg, title, JOptionPane.ERROR_MESSAGE );
	    }
    }


    /**
     * Verifies the login to a CVS project
     * The method is inherited from GroupInfo
     */
    public boolean verifyLogin()
    {
	if ( ! this.project.isPServer() )
	    return false;

	String password = this.project.getPassword();

	if ( password == null )
	    {
		return this.performLogin();
	    }
        else
            return true;
    }

    /*
     * Given the path to a Bluej group package it returns the path to
     * the CVS directory.
     */
    public String createRootPath(String projectPath)
    {
        String cvsDir = "CVS";
        StringBuffer buff = new StringBuffer(projectPath);
        int i = buff.length()-1;

        //If the path ends with "slash" or "backslash
        //remove it.
        if(projectPath.endsWith("/") || projectPath.endsWith("\\"))
        {
            buff.deleteCharAt(i);
            i--;
        }

        //If it is not a unix path look for "\"
	if(projectPath.indexOf('/') == -1)
	    {
		while(buff.charAt(i) != '\\')
		    {
			buff.deleteCharAt(i);
			i--;
		    }

		//buff.append(cvsDir);
		return buff.toString();
	    }
        else
	    {
		while(buff.charAt(i) != '/')
		    {
			buff.deleteCharAt(i);
			i--;
		    }

		//buff.append(cvsDir);
		return buff.toString();
	    }
    }//End createRootPath

    /**
     * Method to add classes when working OffLine
     *
     * @param
     * @param
     * @returns void
     */
    public void offLineAdd(CVSEntry add)
    {
        if(addedOffLine == null){
            addedOffLine = new CVSEntryVector();
        }

        //Try to remove entry from removedOffLine. It is enough to remove
        //it from there, if it is in there, othervise we need to go all
        //the way. (the method removeEntry in CVSEntryVector was for some
        //stupid reason private, I changed that.)
//XXX        if(!removedOffLine.removeEntry(add.getName())){
//            addedOffLine.appendEntry(add);
//        }
    }

    /**
     * Get Entries added off line
     *
     * @param
     * @param
     * @returns CVSEntryVector
     */
    public CVSEntryVector getAddedOffLine()
    {
        return addedOffLine;
    }

    /**
     * Clear Entries added off line
     *
     * If it is empty we want it to be null
     * @param
     * @param
     * @returns CVSEntryVector
     */
    public void clearAddedOffLine()
    {
        addedOffLine.clear();
    }

    /**
     * Method to remove classes when working OffLine
     *
     * @param
     * @param
     * @returns void
     */
    public void offLineRemove(CVSEntry remove)
    {
        if(removedOffLine == null){
            removedOffLine = new CVSEntryVector();
        }

        //Try to remove entry from addedOffLine, it is enough to remove
        //it from there, if it is in there, othervise we need to go all
        //the way.
//XXX        if(!addedOffLine.removeEntry(remove.getName())){
//            removedOffLine.appendEntry(remove);
//        }

        //If the entry is in addedOffLine, it is enough to remove it
        //from there.
//         if(addedOffLine.locateEntry(remove.getName()) == null){
//             removedOffLine.appendEntry(remove);
//         }
//         else{
//             addedOffLine.removeEntry(remove.getName());
//         }


    }

    /**
     * Get Entries removed offline
     *
     * @param
     * @param
     * @returns CVSEntryVector
     */
    public CVSEntryVector getRemovedOffLine()
    {
        return removedOffLine;
    }

    /**
     * Clear Entries removed offline
     *
     * @param
     * @param
     * @returns CVSEntryVector
     */
    public void clearRemovedOffLine()
    {
        removedOffLine.clear();
    }

    /**
     * Method to save group info to the bluej.pkg file
     *
     * @param
     * @param
     * @returns void
     */
    public void save(SortedProperties props)
    {
        StringBuffer added = new StringBuffer();
        StringBuffer removed = new StringBuffer();
        CVSEntry tmpEntry;

        //Create a comma separated string with all added entries.
        for(Iterator itr = addedOffLine.iterator();itr.hasNext();){
            tmpEntry = (CVSEntry)itr.next();
            added.append(tmpEntry.getName()+",");
        }

        //Create a comma separated string with all removed entries.
        for(Iterator itr = removedOffLine.iterator();itr.hasNext();){
            tmpEntry = (CVSEntry)itr.next();
            removed.append(tmpEntry.getName()+",");
        }

        props.setProperty("package.removedTargets", removed.toString());
        props.setProperty("package.addedTargets", added.toString());

    }

    /**
     * Method to load info from the bluej.pkg file
     *
     * The bluej.pkg file is given as a parameter.
     * For now only two properties are loaded:
     * package.addedTargets and package.removedTargets
     * Those are then used to recreate removedOffLine and addedOffLine.
     *
     * @param
     * @param File pkgFile
     * @returns void
     */
    public void load(File pkgFile)
    {
        SortedProperties props = null;

        // try to load the package file for this package
        // This is now done in several places, should maybe be a metod in
        // FileUtility?
        try {
            FileInputStream input = new FileInputStream(pkgFile);

            props = new SortedProperties();
            props.load(input);
        } catch(IOException e) {
            Debug.reportError("Error loading initialisation file" +
                              pkgFile + ": " + e);
        }

        if (props == null)
            return;

        Debug.message("grpinfo,411 "+pkgFile.getPath());
        //A lot of String exercises...
        String dirPath = pkgFile.getPath().substring
            (0, pkgFile.getPath().lastIndexOf(File.separator));
        String dirName = dirPath.substring
            (dirPath.lastIndexOf(File.separator)+1);
        Debug.message("grpinfo,414 "+dirName);
        String addedTargets = props.getProperty("package.addedTargets", " ");
        String removedTargets = props.getProperty("package.removedTargets",
                                                  " ");
        recreateAdded(addedTargets, dirName);
        recreateRemoved(removedTargets, dirName);
    }

    // =========================== PRIVATE METHODS ===========================

    /*
     *Displays a login dialog and performs the login
     */
    private boolean performLogin()
    {
	// if ( ! this.project.isPServer() )
        // 	    return;
	boolean valid = false;
	String password;
	String userName = this.project.getUserName();

	LoginDialog passDialog = new LoginDialog( currentFrame, userName );
	DialogManager.centreDialog(passDialog);
	passDialog.show();

	userName = passDialog.getUserName();
	password = passDialog.getPassword();

	if ( userName != null && password != null )
	    {
		//currentFrame.setWaitCursor();

		valid = this.project.verifyPassword(this,
                                                    userName,
                                                    password,
                                                    traceReq );

		//currentFrame.resetCursor();

		if ( ! valid )
		    {
			String[] fmtArgs = { userName };
			String msg = ResourceMgr.getInstance().getUIFormat
			    ( "project.login.failed.msg", fmtArgs );
			String title = ResourceMgr.getInstance().getUIString
			    ( "project.login.failed.title" );
			JOptionPane.showMessageDialog(currentFrame,
						      msg,
						      title,
						      JOptionPane.ERROR_MESSAGE );
		    }
	    }
        return valid;
    }//end performLogin

    /*
     * Recreate addedOffLine
     *
     */
    private void recreateAdded(String addedTargets, String dirName)
    {
        String nextTarget;
        CVSEntry entry;
        StringTokenizer st = new StringTokenizer(addedTargets);
        Debug.message("grpinfo,436 added: "+addedTargets);
        while (st.hasMoreTokens()){
            nextTarget = st.nextToken(",");//comma "," as delimiter
            Debug.message("grpinfo,439 "+nextTarget);
            //create a new entry for every added target.
            entry = new CVSEntry();
            entry.setName(nextTarget);
            //sometimes the slash have to be hard coded, sometimes not!
            entry.setLocalDirectory(this.project.getRepository()+"/"+dirName);
            entry.setRepository(this.project.getRepository());
            entry.setTimestamp(this.project.getEntryFile(entry));
            // that is a 'zero' to indicate 'New User File'
            entry.setVersion( "0" );
            //Add it to the entry vector
            offLineAdd(entry);
        }
    }

    /*
     * Recreate removedOffLine
     *
     */
    private void recreateRemoved(String removedTargets, String dirName)
    {
        String nextTarget;
        CVSEntry entry;
        CVSEntryVector entries;
        StringTokenizer st = new StringTokenizer(removedTargets);
        Debug.message("grpinfo,462 Removed: "+removedTargets);
        while (st.hasMoreTokens()){
            nextTarget = st.nextToken(",");//comma "," as delimiter
            Debug.message("grpinfo,465 "+nextTarget);
            //find the entry for every removed target by searching
            //all entries in the project
            this.project.getRootEntry().addAllSubTreeEntries
                (entries = new CVSEntryVector());

            if (!entries.isEmpty()){
                for(Iterator i = entries.iterator();i.hasNext();){
                    entry = (CVSEntry)i.next();
                    Debug.message("Grpinfo,line474: "+entry.getName());
                    if(entry.getName().equals(nextTarget)){
                        offLineRemove(entry);
                    }
                }
            }
            else{
                Debug.reportError("Grpinfo,481: "+
                                  "No entries assoc. with project");
            }
        }
    }

    /******************************
     ** CVS USER INTERFACE METHODS
     *******************************/

    public void uiDisplayProgressMsg( String message )
    {
	;//this.showFeedback( message );
    }

    public void uiDisplayProgramError( String error )
    {
	CVSLog.logMsg( error );
	CVSUserDialog.Error( error );
	CVSTracer.traceWithStack
	    ( "CVSProjectFrame.uiDisplayProgramError: " + error );
    }

    public void uiDisplayResponse( CVSResponse response )
    {
	;
	//this.displayStdout = response.getStdout();
	//this.displayStderr = response.getStderr();
    }

    /*************************************
     ** END OF CVS USER INTERFACE METHODS
     ************************************/

}




