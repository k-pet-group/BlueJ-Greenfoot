package bluej.groupwork;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.*;

import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;

import com.ice.cvsc.*;
import com.ice.jcvsii.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;

/**
** This Class implements the Interface GroupPkgManager using jCVS 
** @author Markus Ostman
**
**/
public final class jCVSGroupPkgManager     
    implements GroupPkgManager
{
    // public static variables
    //./temp the manager object itself. This is not used
    //but maybe we should just work with a static manager
    public static jCVSGroupPkgManager groupPkgMgr=new jCVSGroupPkgManager(); 
 
    // private variables
    private CVSProject project;
    private InfoDialog info;
    //private OutputFrame output;

    //CVS variables
    private boolean traceReq = false;
    private boolean traceResp = false;
    private boolean traceProc = false;
    private boolean traceTCP = false;

    // user preferences


    // =========================== PUBLIC METHODS ===========================
    //./temp: Is this something we need?
    public jCVSGroupPkgManager()
    {	

    }


    // ------------------------------------------------------------------------

    /**
     * Open a Group package
     * The method is inherited from GroupPkgManager
     * @param
     * @param thisFrame  The current Bluej frame
     * @returns  a Group Package or null if someting went wrong
     */
    public void openGrpPackage(PkgMgrFrame thisFrame )
    {
	ConflictResolver res=new ConflictResolver();
        //String pkgfile=thisFrame.getPackage().getDirName();
        res.resolveConflict("/home/markus/bluejPrototype/resolving/test1.pkg");
    }

    /**
     * Checkout a local copy of a Group package from the Master source 
     * The method is inherited from GroupPkgManager
     * @param 
     * @param thisFrame  The current Bluej frame
     * @returns  a Group Package or null if something went wrong
     */
    public void checkoutPackage(PkgMgrFrame thisFrame )
    {
	GroupWorkDialog dialog=new GroupWorkDialog(thisFrame);
	if(dialog.displayCheckout())
	    ;
    }


    /**
     * Creates a new module in the Repository using a specified local package
     *
     * The local package temporarily changes into a group package, 
     * gets imported to the repository and then changes back into an ordinary
     * package
     * 
     * The method is inherited from GroupPkgManager
     * @param 
     * @param thisFrame  The current Bluej frame
     * @returns  void
     */
    public void importPackage(PkgMgrFrame thisFrame )
    {
	//Before import we must turn this package into a group package
	//./temp this should probably be done further down the track 
	//in case of a cancel
	Package pkg = thisFrame.getPackage();
	pkg.turnIntoGroupPackage(true);

	//And Save the change
	if(pkg.save())
	    {
		GroupWorkDialog dialog=new GroupWorkDialog(thisFrame);
		//If import failed, undo group package
		//Even if import was successful undo group package
		//./temp this is not set up properly
		if(!dialog.displayImport()){
		    pkg.turnIntoGroupPackage(false);
		    pkg.save();
		    //close the package
		    thisFrame.removePackage();
		}
	    }
	else
	    Debug.reportError("Could not save Package");

    }

    /**
     * Performs an update from the repository 
     * 
     * The method stores the project password and local directory 
     * temporarily and then closes the current package.
     * The update is performed and the updated package is opened. 
     * @param thisFrame  The current Frame   
     *
     * @returns True if successful, false if not
     */
    public boolean updatePkg(PkgMgrFrame thisFrame)
    {
	this.project=thisFrame.getPackage().getGroupInfo().getProject();
	String passw=this.project.getPassword();
	String pkgDir=thisFrame.getPackage().getDirName();
        Debug.message("jCVSGrpPkgMgr,140:" + pkgDir);
	thisFrame.removePackage();
	if(this.performCommand(thisFrame, UPDATE, null)){
          //thisFrame.doOpenPackage(pkgDir, passw);
	    return true;
	}
	else{
	    Debug.message("JCVSGrpPkgMgr,line147: trouble updating");
	    return false;
	}
    }

    /**
     * Performs a commit to the repository 
     *  
     * @param thisFrame  The current Frame   
     *
     * @returns True if successful, false if not
     */
    public boolean commitPkg(PkgMgrFrame thisFrame)
    {
	if(this.performCommand(thisFrame, COMMIT, null)){
	    return true;
	}
	else{
	    Debug.message("JCVSGrpPkgMgr,line165: truble commiting");
	    return false;
	}
    }

    /**
     * Adds a Class to the package. 
     *
     * This is only a local change so in order to change the Repository
     * a commit must be performed
     * The method is inherited from GroupPkgManager
     * 
     * @param frame  The current Frame   
     * @param classFilePath  The file path of the added class 
     * @returns True if successful, false if not
     */
    public boolean addClass(PkgFrame frame, String classFilePath)
    {
        //Get the CVS Project of the package 
	this.project=frame.getPackage().getGroupInfo().getProject();
	//Create the new entry. It is crucial that all the paths
        //are set correctly 
        CVSEntryVector entries = new CVSEntryVector();
        
	entries.appendEntry(createAddFileEntry(classFilePath, 
					       frame.getPackage().getDirName(),
					       this.project.getRepository()));

	//Perform the command
	if(this.performCommand((PkgMgrFrame)frame, ADD, entries)){
	    return true;
	}
	else{
	    Debug.message("JCVSGrpPkgMgr,line178: truble adding");
	    return false;
	}
    }

    /**
     * Removes a Class from the package. 
     *
     * This is only a local change so in order to change the Repository
     * a commit must be performed.
     * The method is inherited from GroupPkgManager
     * 
     * @param frame  The current Frame   
     * @param classFilePath  The file path of the removed class 
     * @returns True if successful, false if not
     */
    public boolean removeClass(PkgFrame frame, String classFilePath)
    {
        //Get the CVS Project of the package 
	this.project=frame.getPackage().getGroupInfo().getProject();
	//Create the new entry. It is crucial that all the paths
        //are set correctly 
        CVSEntryVector entries = new CVSEntryVector();
        
	entries.appendEntry(findRemoveEntry(classFilePath));

	//Perform the command
	if(this.performCommand((PkgMgrFrame)frame, REMOVE, entries)){
	    return true;
	}
	else{
	    Debug.message("JCVSGrpPkgMgr,line178: truble removing");
	    return false;
	}
    }

    /**
     * Checks the status of the files in the Package
     *  
     * @param thisFrame  The current Frame   
     * @param
     * @returns True if successful, false if not
     */
    public boolean pkgStatus(PkgFrame thisFrame)
    {
	if(this.performCommand((PkgMgrFrame)thisFrame, STATUS, null)){
	    return true;
	}
	else{
	    Debug.message("JCVSGrpPkgMgr,line247: truble with status check");
	    return false;
	}
    }

    /**
     * Perform group command
     *
     * Performs the various group commands by building a CVS request
     * from the given command. The active project in the given Frame then 
     * performs the request.
     * The method is inherited from GroupPkgManager 
     *
     * @param thisFrame  The current Frame
     * @param command  The command  
     * @param entries  The entries to act upon  
     * @returns True if ok
     */
    public boolean performCommand(PkgMgrFrame thisFrame, int command,
				  CVSEntryVector entries )
    {
	String cvsCommand=parseCommand(command);
	//./temp: this needs to be done more properly
	CVSArgumentVector arguments=CVSArgumentVector.parseArgumentString("-R");
	//CVSEntryVector	entries=null;
	String fdbkStr;
	boolean allok = true;
	ResourceMgr rmgr = ResourceMgr.getInstance();
	CVSRequest request = new CVSRequest();
	
	if(thisFrame.getPackage().getGroupInfo() != null)
	    this.project=thisFrame.getPackage().getGroupInfo().getProject();
	
	//./temp This is just to see what is happening 
	//displayProjectDetailsPlain();
	Debug.message("GrpPkgMgr, line213: "+project.getRootEntry());
	Debug.message("GrpPkgMgr, line214: "+project.getRepository());
	Debug.message("GrpPkgMgr, line215: "+project.getLocalRootPath());
	this.info = new InfoDialog(thisFrame);
	Debug.message("jCVSGrpPkgMgr,line217: "+cvsCommand);
	fdbkStr = rmgr.getUIString( "project.fdbk.buildcvsreq" );
	Debug.message("jCVSGrpPkgMgr,line219: "+fdbkStr);

	request.setArguments( new CVSArgumentVector() );
	request.setGlobalArguments( new CVSArgumentVector() );
	
	request.traceRequest = this.traceReq;
	request.traceResponse = this.traceResp;
	request.traceProcessing = this.traceProc;
	request.traceTCPData = this.traceTCP;

        //./temp this is temporary moved, should be just above ( ! allok)
        allok = request.parseControlString( cvsCommand  );

	if ( arguments != null ){
            //./temp this is very weird, probably we will not have to worry 
            //about arguments, but if add or remove we can't have -R
            if ( "add".equals(request.getCommand()))
                request.parseArgumentString("");
            else
                request.appendArguments( arguments );
	}
	else{
	    //./temp Here should probably some default arguments be set
	    //request.parseArgumentString( argStr );
	}
	
	
	if ( ! allok )
	    {
		String[] fmtArgs =
		{ cvsCommand , request.getVerifyFailReason() };
		String msg = ResourceMgr.getInstance().getUIFormat
		    ( "project.cmdparse.failed.msg", fmtArgs );
		String title = ResourceMgr.getInstance().getUIString
		    ( "project.cmdparse.failed.title" );
		JOptionPane.showMessageDialog
		    ( thisFrame, msg, title, JOptionPane.ERROR_MESSAGE );
	    }
	
	int portNum =
	    CVSUtilities.computePortNum
	    ( project.getClient().getHostName(),
	      project.getConnectionMethod(),
	      project.isPServer() );
	
	// Establish the request's response handler if it is
	// not to be queued.
	if ( ! request.queueResponse ){
	    request.responseHandler = project;
	}
	
	request.setPort( portNum );
	
	if ( request.redirectOutput ){
	    fdbkStr = rmgr.getUIString( "project.fdbk.canceled" );
	    Debug.message("jCVSGrpPkgMgr,line266: "+cvsCommand);
	    return true;
	}

	// Handle Entries selection
	// If entries is not null, use what was passed in.
	// Otherwise, fill entries according to the spec...
	if ( entries == null ){
	    if ( request.getEntrySelector() == CVSRequest.ES_NONE ){
		entries = new CVSEntryVector();
	    }
	    else{
		//Here we select the entries 
		entries = this.getProjectEntries(request.getEntrySelector());
		
		// Special case for 'Get User File' and 
		//'Get New Files' canceling...
		int selector = request.getEntrySelector();
		if ( ( selector == CVSRequest.ES_USER
		       || selector == CVSRequest.ES_NEW )
		     && entries == null ){
		    fdbkStr = rmgr.getUIString("project.fdbk.canceled");
		    Debug.message("jCVSGrpPkgMgr,line139: "+cvsCommand);
		    return true; // REVIEW - should this be false?
		}
		
		if ( request.execInCurDir 
		     && request.getEntrySelector()
		     == CVSRequest.ES_POPUP ){
		    CVSEntry dirEnt = entries.entryAt(0);
		    if ( dirEnt != null && dirEnt.isDirectory() ){
			request.setDirEntry( entries.entryAt(0) );
		    }
		    else{
			CVSTracer.traceWithStack( "dirEnt is WRONG!" );
		    }
		}
	    }
	}
	
	if ( entries == null ){
	    String[] fmtArgs = { request.getCommand() };
	    String msg = ResourceMgr.getInstance().getUIFormat
		( "project.no.selection.msg", fmtArgs );
	    String title = ResourceMgr.getInstance().getUIString
		( "project.no.selection.title" );
	    JOptionPane.showMessageDialog
		( thisFrame, msg, title, JOptionPane.ERROR_MESSAGE );
	    return true;
	}
	
	// Handle guarantee of a message argument...
	if ( allok ){
	    if ( request.guaranteeMsg ){
		CVSArgumentVector args = request.getArguments();
		if ( ! args.containsArgument( "-m" ) ){
		    String[] fmtArgs = { request.getCommand() };
		    String prompt = ResourceMgr.getInstance().getUIFormat
			( "project.message.required.prompt", fmtArgs );
		    
		    String msgStr ="";// this.requestMessageArgument( prompt );
		    
		    if ( msgStr != null ){
			args.addElement( "-m" );
			args.addElement( msgStr );
		    }
		    else{
			allok = false;
		    }
		}
	    }
	}
	
	//  UNDONE - it would be nice to "verifyRequest" here,
	//           but it is not _fully_ built (hostname, et.al.).
	
	if ( allok ){
	    request.setEntries( entries );
	    request.setUserInterface(info);
	    
	    CVSResponse response = new CVSResponse();
	    
	    CVSThread thread =
		new CVSThread( request.getCommand(),
			       this.new MyRunner( request, response ),
			       this.new MyMonitor( request, response ) );
	    
	    thread.start();
	}
	else{
	    //this.resetCursor();
	    Debug.message("GroupPkgMgr,line284: not allok");
	}
	
	//displayFinalResult(thisFrame);

	return allok;
    }
    
    private class MyRunner
	implements Runnable
    {
	private CVSRequest request;
	private CVSResponse response;
	
	public
	    MyRunner( CVSRequest req, CVSResponse resp )
	{
	    this.request = req;
	    this.response = resp;
	}
	
	public void run()
	{
	    boolean fail = false;
	    
	    if ( "add".equals( this.request.getCommand() ) )
		{
		    CVSEntry entry = this.request.getEntries().entryAt( 0 );
		    Debug.message("grpPkgMgr,line385:"+entry.getFullName());
                    CVSResponse addResponse =
			project.ensureRepositoryPath
			(info,
			 entry.getFullName(), this.response );
		    
		    if ( addResponse.getStatus() != CVSResponse.OK )
			{
			    fail = true;
			    String fdbkStr =
				ResourceMgr.getInstance().getUIString
				( "project.fdbk.errcreate" );
			    
			    Debug.message("GRpPkgMgr, line324: "+ fdbkStr );
			    this.response.appendStderr
				( "An error occurred while creating '"
				  + entry.getFullName() + "'" );
			}
		    else
			{
			    CVSEntry dirEntry =
				project.getDirEntryForLocalDir
				( entry.getLocalDirectory() );
			    
			    if ( dirEntry == null )
				{
				    CVSLog.logMsg
					( "ADD FILE COULD NOT FIND PARENT DIRECTORY" );
				    CVSLog.logMsg
					( "    locaDirectory = "
					  + entry.getLocalDirectory() );
				    (new Throwable( "COULD NOT FIND THE DIRECTORY!" )).
					printStackTrace();
				    
				    fail = true;
				    String fdbkStr =
					ResourceMgr.getInstance().getUIString
					( "project.fdbk.errcreate" );
				    
				    //showFeedback( fdbkStr );
				    Debug.message("GRpPkgMgr, line351: "+ fdbkStr );
				    this.response.appendStderr
					( "An error occurred while creating '"
					  + entry.getFullName() + "'" );
				}
			    else
				{
				    this.request.setDirEntry( dirEntry );
				    //
				    // NOTE
				    // SPECIAL CASE
				    // SEE "ADD SPECIAL CASE" BELOW
				    //
				    // In this special case, the user has 
				    //selected a file using the FileDialog. 
				    //Ergo, we have no context other than its
				    // local directory. And when we were 
				    //creating it, we were not even sure if 
				    //its parent directory existed yet!
				    //
				    // However, at this point, we have 
				    //"ensureRepositoryPath()-ed" the entry's 
				    //full name. This means that the entry's 
				    //parent directory exists. This is 
				    //critical, since that directory entry has
				    // the one piece of information that we 
				    //lacked when we created the entry to add 
				    //- the repository string. But now,
				    // we can get the parent and get the 
				    //repository from it!
				    //
				    if ( request.getEntrySelector() == CVSRequest.ES_USER )
					{
					    entry.setRepository( dirEntry.getRepository() );
					}
				}
			}
		}
	    
	    if ( ! fail ){
		//./temp this is not necessary since the request is not 
		//fully build yet
		if(!request.verifyRequest()){
		    Debug.message("jCVSGrpPkgMgr, line415 " +
				  request.getVerifyFailReason());
		}
		project.performCVSRequest(this.request, this.response);
	    }
	}
    }
    
    private class MyMonitor
	implements CVSThread.Monitor
    {
	private CVSRequest request;
	private CVSResponse response;
	
	public
	    MyMonitor( CVSRequest req, CVSResponse resp )
	{
	    this.request = req;
	    this.response = resp;
	}
	
	public void
	    threadStarted()
	{
	    //	actionButton.setText( "Cancel Export" );
	}
	
	public void
	    threadCanceled()
	{
	}
	
	public void
	    threadFinished()
	{
	    //	actionButton.setText( "Perform Export" );
	    boolean allok =( this.response.getStatus() == CVSResponse.OK );
	    if(allok)
		System.err.println( "THREAD FINISHED: pretty Nice '" );
	    
	    if ( request.isRedirected() )
		{
		    request.endRedirection();
		}

	    //Debug.message(this.response.getDisplayResults());
	    info.setText(this.response.getDisplayResults().trim());
	    info.display();
	    
	}
    }
    

    /**
     * Parses a integer command into a valid CVS command
     *
     * @param command  The command   
     * @returns A CVS command
     */
    private String parseCommand(int command)
    {	
	String cvsCommand=" ";
	
	switch(command) 
	    {
	    case UPDATE:
		cvsCommand="update:a:EAUFP:deou:";
		break;
	    case COMMIT:
		cvsCommand="ci:a:EAUFG:deou:";
		break;
	    case ADD:
		cvsCommand="add:G:SUAFX:due:";
		break;
	    case STATUS:
		cvsCommand="status:a:EUAF:de:";
		break;
            case REMOVE:
		cvsCommand="remove:a:EAF:de:";
		break;
	    default:
		Debug.reportError("GroupPkgManager: unknown command ID");
		break;
	    }
	return cvsCommand;
    }

     /**
     * Returns the entries of the active project
     *
     * @param
     * @returns
     */
    private CVSEntryVector getProjectEntries(int selector)
    {	
	int i;
	File entryFile;
	String localPath;
	CVSEntry entry = null;
	CVSEntryVector entries = null;

	this.project.getRootEntry().addAllSubTreeEntries
	    ( entries = new CVSEntryVector() );
		    

	if ( entries != null ){
	    for ( i = 0 ; i < entries.size() ; ++i ){
		entry = entries.entryAt(i);
		entryFile = this.project.getEntryFile( entry );
		
		if ( selector == CVSRequest.ES_ALLMOD
		     || selector == CVSRequest.ES_SELMOD ){
		    if ( ! this.project.isLocalFileModified( entry ) ){
			entries.removeElementAt(i);
			--i;
		    }
		}
		else if ( selector == CVSRequest.ES_ALLLOST
			  || selector == CVSRequest.ES_SELLOST ){
		    if ( ! entryFile.exists() ){
			entries.removeElementAt(i);
			--i;
		    }
		}
		else if ( selector == CVSRequest.ES_ALLUNC
			  || selector == CVSRequest.ES_SELUNC ){
		    if ( ! entryFile.exists()
			 || this.project.isLocalFileModified( entry ) ){
			entries.removeElementAt(i);
			--i;
		    }
		}
	    }
	}
	return entries;
    }//End getProjectEntries

     /**
     * Returns the entry to be added
     *
     * @param entryName Path to the sourcefile to be added
     * @returns
     */
    private CVSEntry createAddFileEntry( String entryName, 
					 String localDirectory, 
					 String repository )
    {
        //./temp this is just an attempt to fix the string problem
        //There is redundancy in information with all these paths
        String fileName = entryName.substring(entryName.lastIndexOf('/')+1);
        String dirName = localDirectory.substring(localDirectory.lastIndexOf('/')+1);
        Debug.message("jCVSGrpPkgMgr,line627 "+fileName);
        Debug.message("GrpPkgMgr,line628 "+dirName);
        Debug.message("GrpPkgMgr,line628 "+repository);

	CVSEntry entry = new CVSEntry();
	
	entry.setName( fileName );
	entry.setLocalDirectory( repository+'/'+dirName );
	entry.setRepository( repository );
	entry.setTimestamp( this.project.getEntryFile( entry ) );
	
	// that is a 'zero' to indicate 'New User File'
	entry.setVersion( "0" );
	
	return entry;
    }

     /**
     * Returns the entry to be removed
     *
     * @param entryPath Path to the sourcefile to be removed
     * @returns CVSEntry
     */
    private CVSEntry findRemoveEntry(String entryPath)
    {
	CVSEntry entry = null;
	CVSEntryVector entries = null;

        //Get all entries in the project
	this.project.getRootEntry().addAllSubTreeEntries
	    ( entries = new CVSEntryVector() );

        //Get the name of the file
	String entryName = entryPath.substring(entryPath.lastIndexOf('/')+1); 
        Debug.message("GrpPkgMgr,line702: "+entryName);

	if ( entries != null ){
	    for (int i = 0 ; i < entries.size() ; ++i){
		entry = entries.entryAt(i);
                Debug.message("GrpPkgMgr,line702: "+entry.getName());
                if(entry.getName().equals(entryName))
                   return entry;
            }
            Debug.reportError("GrpPkgMgr,line698: trying to remove a none excisting entry");
            return null;
        }
        else{
            Debug.reportError("GrpPkgMgr,line702: No entries assoc. with project");
            return null;
        }
    }
    
    /**
     * Displays the details of a project
     *
     * @param
     * @returns
     */
    public void displayProjectDetailsPlain()
    {
	Object[] fmtArgs ={this.project.getRepository(),
			   this.project.getRootDirectory(),
			   this.project.getClient().getHostName(),
			   new Integer( this.project.getClient().getPort() ),
			   this.project.getLocalRootDirectory()};
	
	String msgStr =
	    ResourceMgr.getInstance().getUIFormat
	    ( "project.details.dialog.text", fmtArgs );
	String title =
	    ResourceMgr.getInstance().getUIString
	    ( "project.details.dialog.title" );
	
	JOptionPane.showMessageDialog
	    ( info, msgStr, title, JOptionPane.INFORMATION_MESSAGE );
    }
    
    
    //./temp maybe add info here instead of currentFrame, don't think so

 //     /** Shows the final result of a CVS request
//       **
//       ** @param
//       ** @returns void
//       **/
//      private void displayFinalResult(PkgFrame currentFrame)
//      {	
//  	this.output = new OutputFrame( currentFrame, 
//  				       this.project.getRepository() + " Output" );
//  	Dimension sz = currentFrame.getSize();
//  	Point loc = currentFrame.getLocationOnScreen();
	
//  	Rectangle defBounds = new Rectangle( loc.x + 15, 
//  					     loc.y + 15, 
//  					     sz.width, 
//  					     sz.height );
	
//  	this.output.loadPreferences( defBounds );

//  	this.output.setText("Hej hopp");
//  	this.output.setVisible( true );
//  	this.output.requestFocus();
//      }
    
}
    







