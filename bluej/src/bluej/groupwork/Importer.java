
package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.pkgmgr.*;

/**
 ** @version $Id: Importer.java 504 2000-05-24 04:44:14Z markus $
 ** @author Markus Ostman, some parts are copied from jCVS ImportPanel
 **
 ** Import class for bluej group support.
 **/


public class Importer 
implements CVSUserInterface
{
    protected CVSClient			client;
    private PkgFrame                    currentFrame;
    private String                      password;
    private String                      userName;
    private String                      module;
    private String                      groupName;
    protected Properties                props;
    protected StringBuffer		scanText;
    protected String			ignoreName;
    private LoginDialog                 passDialog;
    private InfoDialog                  info;
 
    public Importer(PkgFrame currentFrame)
    {
        this.currentFrame = currentFrame;
	this.scanText = new StringBuffer();
	this.establishContents();
        this.password = null;
        this.userName = null;
        this.module = null;
        this.groupName = null;
        this.info = new InfoDialog(currentFrame);
    }

    /*
     * Load Defaults from group.defs
     */
    public void loadPreferences()
    {
        String propsFile = bluej.Config.sys_confdir + File.separatorChar + "group.defs";
        
        if (props == null) {
            // try to load the Properties for the group stuff
            try {
                FileInputStream input = new FileInputStream(propsFile);
		
                this.props = new Properties();
                this.props.load(input);
            } catch(IOException e) {
                Debug.reportError("Error loading group properties file" + 
                                  propsFile + ": " + e);
            }
	}
    }
    
    public void savePreferences()
    {
	//this.infoPan.savePreferences( "import" );
    }

 //    private void cancelImport()
//     {
// 	this.client.setCanceled( true );
//     }

    /*
     * Directory to import, if the info is not available
     * maybe a dialog?
     */
    public String getImportDirectory()
    {
        //If we could trust dirName to be a complete path 
        //then these String exercises wouldn't be necessary
        String baseDir = currentFrame.getPackage().getBaseDir();
        Debug.message("importer,line116 "+baseDir);
        String dirName = currentFrame.getPackage().getDirName();
        Debug.message("importer,line118 "+dirName);
        String importDir = baseDir+dirName.substring(dirName.lastIndexOf(File.separator));
        Debug.message("importer,line120 "+importDir);

        if(importDir!= null)
            return importDir;
        else
            return JOptionPane.showInputDialog(this.currentFrame,
                                               "Give Import Directory");
    }
    
    /*
     * Name of directory in repository. This needs to be arranged 
     * so that it is guaranteed to be unique
     */
    public String getModule()
    {
        if(this.module==null){
            this.module = currentFrame.getPackage().getDirName();
            Debug.message("importer,line129 "+module.substring(module.lastIndexOf(File.separator)+1));
        }
        
        if(this.module != null)
            return this.module.substring(module.lastIndexOf(File.separator)+1);
        else
            return JOptionPane.showInputDialog(this.currentFrame,
                                               "Give Module name"); 
    }  

    /*
     * Get name of user.
     */
    public String getUserName()
    {
        if(this.userName == null){
//             LoginDialog passDialog = new LoginDialog(this.currentFrame, 
//                                                      userName );
//             passDialog.setTitle(bluej.Config.getString("groupwork.login.title"));
//             DialogManager.centreDialog(passDialog);
            passDialog.show();
            
            this.groupName = passDialog.getgroupName();
            this.userName = passDialog.getUserName();
            this.password = passDialog.getPassword();
            return this.userName;
        }
        else
            return this.userName;
    }

    /*
     * Get password.
     */
    public String getPassword()
    {
        if(this.password == null){
//             LoginDialog passDialog = new LoginDialog(this.currentFrame, 
//                                                            userName );
//             passDialog.setTitle(bluej.Config.getString("groupwork.login.title"));
//             DialogManager.centreDialog(passDialog);
            passDialog.show();

            this.groupName = passDialog.getgroupName();
            this.userName = passDialog.getUserName();
            this.password = passDialog.getPassword();
            return this.password;
        }
        else
            return this.password;
    }

    /*
     * Get groupName.
     */
    public String getGroupName()
    {
        if(this.password == null || this.userName == null ){
//             LoginDialog passDialog = new LoginDialog(this.currentFrame, 
//                                                      userName );
//             passDialog.setTitle(bluej.Config.getString("groupwork.login.title"));
//             DialogManager.centreDialog(passDialog);
            passDialog.show();
            
            this.groupName = passDialog.getgroupName();
            this.userName = passDialog.getUserName();
            this.password = passDialog.getPassword();
            return this.groupName;
        }
        else
            return this.groupName;
    }

    public boolean getCancel()
    {
        return this.passDialog.getCancel();
    }

    public void performImport()
    {
	Config cfg = Config.getInstance();
	UserPrefs prefs = cfg.getPreferences();
	ResourceMgr rmgr = ResourceMgr.getInstance();
	
	CVSRequest		request;
	boolean			allok = true;
	
	CVSEntryVector		entries = new CVSEntryVector();
	CVSEntryVector		binEntries = new CVSEntryVector();
	
        //Since the infoPan doesn't provide any arguments
        //we just set them to null
	CVSArgumentVector arguments=CVSArgumentVector.parseArgumentString("");

	String userName = this.getUserName();
	String passWord = this.getPassword();

        //here we must check if the user have choosen to cancel import
        if(this.passDialog.getCancel())
            return;

	String hostname = props.getProperty("group.server", null);
        //Name of dir in repos
	String repository = this.getGroupName()+"/"+this.getModule();
	String rootDirectory = props.getProperty("group.repository.path",
                                                 null);
	String importDirectory = this.getImportDirectory();
	
	String vendorTag = props.getProperty("group.import.vendor");
	String releaseTag = props.getProperty("group.import.release");
	String messageStr = this.getLogMessage(this.getModule());
	
        //Should it be File.separator here?
	if ( repository.startsWith("/"))
	    repository = repository.substring( 1 );
	
	if ( repository.endsWith( "/" ) )
	    repository = repository.substring( 0, repository.length()-1 );
	
	if ( rootDirectory.endsWith( "/" ) )
	    rootDirectory =
		rootDirectory.substring( 0, rootDirectory.length()-1 );

	String	rootRepository = rootDirectory + "/" + repository;
	
        //./temp This could also be rsh, not sure?
	int connMethod = CVSRequest.METHOD_INETD; //CVSRequest.METHOD_RSH
	
	//
	// SANITY
	//
	if ( hostname.length() < 1 || repository.length() < 1
	     || rootDirectory.length() < 1 || vendorTag.length() < 1
	     || releaseTag.length() < 1 || messageStr.length() < 1
	     || importDirectory.length() < 1 )
	    {
		String[] fmtArgs = new String[1];
		fmtArgs[0] =
		    ( hostname.length() < 1
		      ? rmgr.getUIString( "name.for.cvsserver" ) :
		      ( repository.length() < 1
			? rmgr.getUIString( "name.for.cvsmodule" ) :
			( rootDirectory.length() < 1
			  ? rmgr.getUIString( "name.for.cvsrepos" ) :
			  ( importDirectory.length() < 1
			    ? rmgr.getUIString( "name.for.importdir" ) :
			    ( vendorTag.length() < 1
			      ? rmgr.getUIString( "name.for.vendortag" ) :
			      ( releaseTag.length() < 1
				? rmgr.getUIString( "name.for.releasetag" )
				: rmgr.getUIString( "name.for.logmsg" ) ))))));
		
		String msg = rmgr.getUIFormat("import.needs.input.msg", fmtArgs );
		String title = rmgr.getUIString("import.needs.input.title");
		JOptionPane.showMessageDialog
		    (this.currentFrame,
		      msg, title, JOptionPane.ERROR_MESSAGE );
		return;
	    }
	
	if ( connMethod == CVSRequest.METHOD_RSH
	     && userName.length() < 1 )
	    {
		String msg = rmgr.getUIString("common.rsh.needs.user.msg" );
		String title = rmgr.getUIString("common.rsh.needs.user.title" );
		JOptionPane.showMessageDialog
		    ( this.currentFrame,
		      msg, title, JOptionPane.ERROR_MESSAGE );
		return;
	    }
	
	this.scanText.setLength( 0 );
	
	this.ignoreName =
	    prefs.getProperty( Config.GLOBAL_IGNORE_FILENAME, null );
	
	CVSIgnore ignore = new CVSIgnore();
	
	//Ignore some file types
	String userIgnores = props.getProperty("group.import.ignores", null );
	
	Debug.message("ImportPnl,line220 ignore: "+userIgnores);
	
	if ( userIgnores != null )
	    {
		ignore.addIgnoreSpec( userIgnores );
	    }

	//./temp this is not necessary, because the user will never do this
        //user choice/hard coded/admin choice? 
	String ignoreStr = "";//this.getIgnores(); 
	if ( ignoreStr.length() > 0 )
	    {
		ignore.addIgnoreSpec( ignoreStr );
	    }
	
	// We leverage the ignores mechanism to indicate binaries!
	CVSIgnore binaries = new CVSIgnore( "" );
	
	String binariesStr = "";//Do we want ignore binaries getBinaries()
	if ( binariesStr.length() > 0 )
	    {
		binaries.addIgnoreSpec( binariesStr );
	    }
	
        //./temp import subdir or not, user choice/hard coded/admin choice? 
	boolean descend = true;
	
	allok =
	    this.importScan
	    ( rootDirectory, repository, importDirectory,
	      descend, entries, ignore, binEntries, binaries );
	
	if ( ! allok )
	    {
		String msg = rmgr.getUIString( "import.scan.error.msg" );
		String title = rmgr.getUIString( "import.scan.error.title" );
		JOptionPane.showMessageDialog
		    ( currentFrame, msg, title, JOptionPane.ERROR_MESSAGE );
                Debug.message(this.scanText.toString());
		return;
	    }
	
        //./temp user choice/hard coded/admin choice?
	boolean isPServer = true;//this.isPasswordSelected();
	
	int cvsPort = CVSUtilities.computePortNum(hostname,
                                                  connMethod,
                                                  isPServer );
	
	String serverCommand = 
            CVSUtilities.establishServerCommand
	    ( hostname, connMethod, isPServer );
	
	this.client = new CVSClient( hostname, cvsPort );
	
	this.client.setTempDirectory( cfg.getTemporaryDirectory() );
	
	request = new CVSRequest();
	
	request.setPServer( isPServer );
	request.setUserName( userName );
	
	if ( isPServer )
	    {
		String scrambled =
		    CVSScramble.scramblePassword( passWord, 'A' );
		
		request.setPassword( scrambled );
	    }
	
	request.setConnectionMethod( connMethod );
	request.setServerCommand( serverCommand );
	
	if ( connMethod == CVSRequest.METHOD_RSH )
	    CVSUtilities.establishRSHProcess( request );
	
	request.setPort( this.client.getPort() );
	request.setHostName( this.client.getHostName() );
	
	request.setRepository( repository );
	request.setRootDirectory( rootDirectory );
	request.setRootRepository( rootRepository );
	
	request.setLocalDirectory( importDirectory );
	
	request.setSetVariables
	    ( CVSUtilities.getUserSetVariables( this.client.getHostName() ) );
	
	request.setCommand( "import" );
	// NOTE DO NOT use 'sendModule' here!
	request.sendModifieds = true;
	request.sendArguments = true;
	request.includeNotifies = false;
	
	request.traceRequest = CVSProject.overTraceRequest;
	request.traceResponse = CVSProject.overTraceResponse;
	request.traceTCPData = CVSProject.overTraceTCP;
	request.traceProcessing = CVSProject.overTraceProcessing;
	
	request.allowGzipFileMode =
	    ( prefs.getBoolean( Config.GLOBAL_ALLOWS_FILE_GZIP, false ) );
	
	request.setGzipStreamLevel
	    ( prefs.getInteger( Config.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );
	
	arguments.appendArgument( "-m" );
	arguments.appendArgument( messageStr );
	
	arguments.appendArgument( repository );
	
	arguments.appendArgument( vendorTag );
	
	arguments.appendArgument( releaseTag );
	
	request.setEntries( entries );
	
	request.setArguments( arguments );
	
	request.setUserInterface( this );
	
	CVSResponse response = new CVSResponse();
	
        GroupJobQueue.getJobQueue().addJob("Import",
                                         this.new MyRunner(this.client,
                                                           request, 
                                                           response,
                                                           binEntries),
                                         this.new MyMonitor(request,
                                                            response));
        
        Debug.message("GrpPkgMgr line 456 Add "+request.getCommand()
                      +" (import) to the Job queue");
    }

    
    private class MyRunner implements Runnable
    {
	private CVSClient client;
	private CVSProject project;
	private CVSRequest request;
	private CVSResponse response;
	private CVSEntryVector binEntries;
	
	
	public
        MyRunner( CVSClient client, CVSRequest request,
                  CVSResponse response, CVSEntryVector binEntries )
	{
	    this.client = client;
	    this.request = request;
	    this.response = response;
	    this.binEntries = binEntries;
	}
	
	public void
        run()
	{
	    this.client.processCVSRequest( this.request, this.response );
	    
	    this.response.appendStderr( scanText.toString() );
	    
	    boolean success =
		( response.getStatus() == CVSResponse.OK );
	    
	    if ( this.binEntries.size() >  0 )
		{
		    CVSResponse binResponse = new CVSResponse();
		    
		    this.request.setEntries( this.binEntries );
		    
		    this.request.getArguments().insertElementAt( "-kb", 0 );
		    
		    client.processCVSRequest( this.request, binResponse );
		    
		    if ( binResponse.getStatus() != CVSResponse.OK )
			success = false;
		    
		    this.response.appendStdout
			( "\n\n--------- "+ 
                          ResourceMgr.getInstance().getUIString
                          ( "name.for.binary.files" )
                          + " ---------\n" );
		    
		    this.response.appendStdout
			( binResponse.getDisplayResults() );
		    
		    if ( ! this.request.saveTempFiles )
			{
			    binResponse.deleteTempFiles();
			}
		}
	    
	    if ( success )
		{
		    uiDisplayProgressMsg
			( ResourceMgr.getInstance().getUIString
			  ( "import.perform.label" ) );
		}
	    else
		{
		    uiDisplayProgressMsg
			( ResourceMgr.getInstance().getUIString
			  ( "import.status.failure" ) );
		}
	    
	    if ( ! this.request.saveTempFiles )
		{
		    this.response.deleteTempFiles();
		}
	}
    }

    private class MyMonitor
    implements	GroupJob.Monitor
    {
        private CVSRequest request;
        private CVSResponse response;
        
        public
        MyMonitor( CVSRequest request, CVSResponse response )
        {
            this.request = request;
            this.response = response;
        }
        
        public void
        threadStarted()
        {
            Debug.message("thread started");
        }
        
        public void
        threadCanceled()
        {
        }
        
        public void
        threadFinished()
        {
            String resultStr = this.response.getDisplayResults();
            
            if ( this.response.getStatus() == CVSResponse.OK ){
                uiDisplayProgressMsg
                    ( ResourceMgr.getInstance().getUIString
                      ( "import.status.success" ) );
                currentFrame.setStatus
                    (bluej.Config.getString("groupwork.importingDone"));
            }
            else{
                //If there are other jobs in the Queue, it is very likely
                //that they depend on the result of this job. Therefore we
                //abort all of them.
                //GroupJobQueue.getJobQueue().clearQueue();
                
                uiDisplayProgressMsg
                    ( ResourceMgr.getInstance().getUIString
                      ( "import.status.failure" ) );
                //If import encounter an error we need to wait a while 
                //before we notify the others.
                synchronized(this) {
                    try {
                        wait(500);
                    }catch(InterruptedException e) {}
                }
                //Sync.s.callNotify(false);
                GroupJobQueue.getJobQueue().clearQueue();
                Debug.message("importer,556: after sync "+Thread.currentThread().getName());
                info.setText(resultStr);
                info.display(bluej.Config.getString("groupwork.error.title"));
            }
            
            Debug.message(resultStr);
            
            if ( this.response != null
                 && ! this.request.saveTempFiles ){
                this.response.deleteTempFiles();
            }
        }    
    }
    
    public boolean
    importScan(
               String repository, String module, String importPath,
               boolean descend, CVSEntryVector entries, CVSIgnore ignore,
               CVSEntryVector binEntries, CVSIgnore binaries )
    {
        boolean result = true;
        
        File dirFile = new File( importPath );
        
        if ( ! dirFile.exists() )
            {
                result = false;
                String[] fmtArgs = { dirFile.getPath() };
                this.scanText.append
                    ( ResourceMgr.getInstance().getUIFormat
                      ( "import.scan.dir.doesnotexist", fmtArgs ) );
                this.scanText.append
                    ( "   " +
                      ResourceMgr.getInstance().getUIString
                      ( "import.scan.aborted" ) );
            }
        else if ( ! dirFile.isDirectory() )
            {
                result = false;
                String[] fmtArgs = { dirFile.getPath() };
                this.scanText.append
                    ( ResourceMgr.getInstance().getUIFormat
                      ( "import.scan.dir.notdir", fmtArgs ) );
                this.scanText.append
                    ( "   " +
                      ResourceMgr.getInstance().getUIString
                      ( "import.scan.aborted" ) );
            }
        else
            {
                result = this.importScanDescend
                    ( repository, module, "", dirFile,
                      descend, entries, ignore, binEntries, binaries );
            }
        
        return result;
    }

    
    /**
     * Descends a local source tree looking for files to
     * be imported in this command.
     *
     * @param repository The repository's root directory.
     * @param module The 'single' module name of the repository
     * @param subModule is the 'aliased' module name.
     *     subModule is only different from 'module' in the case
     *     of aliases, and is the alias's path. This allows
     *     us to deal with an alias 'jcvs com/ice/jcvs' in
     *     that module will be 'jcvs' and subModule 'com/ice/jcvs'.
     * @param localDirectory The 'local-directory' of imported entries.
     * @param dirFile The current import directory's 'File'.
     * @param descend Determines if the scanning descend into subdirectories.
     * @param entries The CVSEntryVector in which to place the imported entries.
     * @param ignore The globals ignores.
     */
    
    private boolean
    importScanDescend(
                      String repository, String module,
                      String localDirectory, File dirFile, boolean descend,
                      CVSEntryVector entries, CVSIgnore ignore,
                      CVSEntryVector binEntries, CVSIgnore binaries )
    {
        boolean result = true;
        String[] contents = dirFile.list();

        if ( contents == null )
            {
                // REVIEW Why does this occur?!
                return true;
            }

        CVSIgnore	dirIgnore = null;

        if ( false )
            CVSTracer.traceIf( true,
                               "ImportScanDescend: \n"
                               + "   Repository     '" + repository + "'\n"
                               + "   Module         '" + module + "'\n"
                               + "   localDirectory '" + localDirectory + "'\n"
                               + "   dirFile (path) '" + dirFile.getPath() + "'\n"
                               + "   descend        '" + descend + "'\n" );

        File	ignFile;
        // TODO should I have a loop here and a space separated property?!
        // This would allow for multiple ignore file possibilities.

        ignFile = new File( dirFile, this.ignoreName );

        if ( ignFile.exists() )
            {
                dirIgnore = new CVSIgnore( "" );
                dirIgnore.addIgnoreFile( ignFile );
                CVSTracer.traceIf( false,
                                   "ImportDescend: DIRECTORY IGNORE '" + ignFile.getPath()
                                   + "' added '" + dirIgnore.size() + "' ignores." );
            }

        ignFile = new File( dirFile, ".cvsignore" );
        if ( ignFile.exists() )
            {
                dirIgnore = new CVSIgnore( "" );
                dirIgnore.addIgnoreFile( ignFile );
                CVSTracer.traceIf( false,
                                   "ImportDescend: DIRECTORY IGNORE '" + ignFile.getPath()
                                   + "' added '" + dirIgnore.size() + "' ignores." );
            }

        for ( int i = 0 ; result && i < contents.length ; ++i )
            {
                String	fileName = contents[i];

                File file = new File( dirFile, fileName );

                CVSTracer.traceIf( false,
                                   "ImportDescend["+i+"] fileName '"+ 
                                   fileName + "' isDir '"+ 
                                   file.isDirectory() + "' filePath '"+
                                   file.getPath() + "'" );

                if ( fileName.equals( this.ignoreName ) )
                    continue;
                if ( fileName.equals( ".cvsignore" ) )
                    continue;

                if ( ignore.isFileToBeIgnored( fileName )
                     || ( dirIgnore != null
                          && dirIgnore.isFileToBeIgnored( fileName ) ) )
                    {
                        CVSTracer.traceIf( false,
                                           "ImportDescend["+i+"] IGNORE '" +fileName+ "'" );

                        this.scanText.append
                            ( "I " + localDirectory + fileName + "\n" );

                        continue;
                    }

                if ( file.isDirectory() )
                    {
                        String newLocal =
                            localDirectory + fileName + "/";

                        if ( false )
                            CVSTracer.traceIf( true,
                                               "ImportDescend[" +i+ "] DIRECTORY\n"
                                               + "  newLocal '" + newLocal + "'\n"
                                               + "  newDir   '" + file.getPath() + "'" );

                        if ( descend )
                            {
                                result =
                                    this.importScanDescend
                                    ( repository, module, newLocal, file, descend,
                                      entries, ignore, binEntries, binaries );
                            }
                    }
                else
                    {
                        CVSEntry entry = new CVSEntry();

                        String modPath = module + "/" + localDirectory;

                        String localDir = localDirectory;
                        if ( localDir.length() == 0 )
                            localDir = "./";

                        String reposPath = repository + "/" + module;
                        if ( localDirectory.length() > 0 )
                            {
                                reposPath = reposPath + "/" +
                                    localDirectory.substring
                                    ( 0, (localDirectory.length() - 1) );
                            }

                        entry.setName( fileName );
                        entry.setLocalDirectory( localDir );
                        entry.setRepository( reposPath );
                        entry.setMode( new CVSMode() );
                        entry.setNewUserFile( true );

                        if ( false )
                            CVSTracer.traceIf( true,
                                               "ImportDescend[" +i+ "] ENTRY\n"
                                               + "  name '" + entry.getName() + "'\n"
                                               + "  fullName '" + entry.getFullName() + "'\n"
                                               + "  localDir '" + entry.getLocalDirectory() + "'\n"
                                               + "  repos   '" + entry.getRepository() + "'" );

                        if ( binaries.isFileToBeIgnored( fileName )	)
                            {
                                entry.setOptions( "-kb" );
                                binEntries.appendEntry( entry );
                            }
                        else
                            {
                                entries.appendEntry( entry );
                            }
                    }
            }

        return result;
    }

    /*
     * Get the Log message for the imported project
     *
     */
    private String getLogMessage(String project)
    {
        return project+" "+bluej.Config.getString("groupwork.log.importmsg");
    }
        
    //
    // CVS USER INTERFACE METHODS
    //

    public void
    uiDisplayProgressMsg( String message )
    {
        Debug.message(message);
    }

    public void
    uiDisplayProgramError( String error )
    {
    }

    public void
    uiDisplayResponse( CVSResponse response )
    {
    }

    //
    // END OF CVS USER INTERFACE METHODS
    //

    private void
    establishContents()
    {
        ResourceMgr rmgr = ResourceMgr.getInstance();
        loadPreferences();
        this.passDialog = new LoginDialog(this.currentFrame, 
                                          userName );
        this.passDialog.setTitle(bluej.Config.getString
                                 ("groupwork.login.title"));
        DialogManager.centreDialog(passDialog);
    }
}

