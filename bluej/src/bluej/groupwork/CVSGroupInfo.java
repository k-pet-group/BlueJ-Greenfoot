package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.*;

import bluej.utility.Debug;
import bluej.pkgmgr.*;

import com.ice.cvsc.*;
import com.ice.jcvsii.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;

/**
 ** This Class implements the Interface GroupPkgManager using jCVS classes 
 ** @author Markus Ostman
 **
 **/
public final class CVSGroupInfo 
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


    // =========================== PUBLIC METHODS ===========================

    public CVSGroupInfo()
    {	
	//Initialize jCVS components
	//This is necessary because of weird design in jCVS code
	JcvsInit.doInitialize();
    }


    // ------------------------------------------------------------------------

    /**Method 
     ** 
     ** The Method is inherited from GroupInfo 
     ** @param pkgDir The directory name of a Package
     ** @param 
     ** @returns Group information or null if something went wrong  
     **/
    public GroupInfo getGroupInfo(String pkgDir) 
    {
	groupInfo.project = new CVSProject();
	return groupInfo;
    }

    
    /**Method 
     ** 
     ** @param 
     ** @returns CVSProject  
     **/
    public CVSProject getProject() 
    {
	return this.project;
    }
    
    /**Method 
     ** 
     **The method is inherited from GroupInfo  
     ** @param localDirName The path to the local directory
     ** @param currentFrame The current Bluej Frame
     ** @param password The project password
     ** @returns void  
     **/
    public void initializeGroupInfo(String localDirName, 
				    JFrame currentFrame,
				    String password)
    {
	this.currentFrame=currentFrame;
        //Create path to the CVS directory for the project
	String rootDirPath = createRootPath(localDirName);
	File rootDirFile = new File(rootDirPath);
	Debug.message("CVSGrpInfo, line88:"+localDirName);
	Config cfg = Config.getInstance();
	UserPrefs prefs = cfg.getPreferences();
	this.traceReq = prefs.getBoolean( Config.GLOBAL_CVS_TRACE_ALL, false );

	CVSClient client = new CVSClient();
	this.project = new CVSProject( client );
	Debug.message("CVSGrpInfo, line96:"+cfg.getTemporaryDirectory());
	project.setTempDirectory( cfg.getTemporaryDirectory() );
	
	project.setAllowsGzipFileMode( prefs.getBoolean( Config.GLOBAL_ALLOWS_FILE_GZIP, true ) );
	
	project.setGzipStreamLevel( prefs.getInteger( Config.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );
	
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
	    Debug.message("CVSGrpInfo,Line128 "+title);
	    //ProjectFrame frame = new ProjectFrame( title, project );
	    
	    //ProjectFrameMgr.addProject( frame, rootDirFile.getPath() );
	    
	    //frame.toFront();
	    //frame.requestFocus();
	    
	    if ( password != null )
		{
		    project.setPassword( password );
		}
	    else
		{
		    verifyLogin();
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

    
    /*
     *Verifies the login to a CVS project
     */ 
    public void verifyLogin()
    {
	if ( ! this.project.isPServer() )
	    return;
	
	boolean valid;
	String password = this.project.getPassword();
	
	if ( password == null )
	    {
		this.performLogin();
	    }
    }

    /*
     *Displays a login dialog and performs the login
     */ 
    public void performLogin()
    {
	if ( ! this.project.isPServer() )
	    return;
	
	String password;
	String userName = this.project.getUserName();
	
	PasswordDialog passDialog = new PasswordDialog( currentFrame, userName );
	
	passDialog.show();

	userName = passDialog.getUserName();
	password = passDialog.getPassword();

	if ( userName != null && password != null )
	    {
		//currentFrame.setWaitCursor();
		
		boolean valid = this.project.verifyPassword(this, 
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
    }//end performLogin


    /*
    **Given the path to a Bluej group package it returns the path to 
    **the CVS directory.
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




