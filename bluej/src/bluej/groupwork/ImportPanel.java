/*
** Java CVS client application package.
** Copyright (c) 1997 by Timothy Gerard Endres
** 
** This program is free software.
** 
** You may redistribute it and/or modify it under the terms of the GNU
** General Public License as published by the Free Software Foundation.
** Version 2 of the license should be included with this distribution in
** the file LICENSE, as well as License.html. If the license is not
** included	with this distribution, you may find a copy at the FSF web
** site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
** Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
**
** THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND,
** NOT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR
** OF THIS SOFTWARE, ASSUMES _NO_ RESPONSIBILITY FOR ANY
** CONSEQUENCE RESULTING FROM THE USE, MODIFICATION, OR
** REDISTRIBUTION OF THIS SOFTWARE. 
** 
*/


package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

import bluej.utility.Debug;
import bluej.pkgmgr.*;

/**
 ** @version $Id: ImportPanel.java 426 2000-04-14 01:11:12Z markus $
 ** @author Modifications to jCVS ImportPanel.java by Markus Ostman
 **
 ** Import panel for bluej group support.
 **/


public class ImportPanel extends GroupWorkPanel
    implements ActionListener, CVSUserInterface
{
    protected CVSClient			client;
    protected JTextField		argumentsText;
    protected JTextField		releaseText;
    protected JTextField		vendorText;
    protected JTextArea			outputText;
    protected JTextArea			messageText;
    protected JTextArea			ignoreText;
    protected JTextArea			binariesText;
    protected JCheckBox			descendCheck;
    protected JLabel			feedback;
    protected JButton			actionButton;
    protected StringBuffer		scanText;
    protected String			ignoreName;

    protected JTabbedPane		tabbed;
    protected AdditionalInfoPanel	addPan;
    protected ConnectInfoPanel	        infoPan;

 
    public ImportPanel( MainGrpPanel parent )
    {
	super( parent );
	this.scanText = new StringBuffer();
	this.establishContents();
    }

    public void loadPreferences()
    {
	this.infoPan.loadPreferences( "import" );

  	//./temp this overrides the old preferenses by setting 
	//the import directory and Module. For some reason this 
        //messes up Bluej if called before new package is created
  	PkgMgrFrame parentFrame = (PkgMgrFrame)getGroupWorkDialog().getParent();
  	if(parentFrame.getPackage().getBaseDir() != null){
  	    this.infoPan.setImportDirectory(parentFrame.getPackage().getBaseDir());
	    //./temp need to get the package name from somewhere, 
	    //this doesn't seem to work
	    //this.infoPan.setModule(parentFrame.getPackage().getName());
	    this.infoPan.setModule("");
	}

        //./temp This is just temporary, why doesn't it work!?
//  	JDialog parentDialog = (JDialog)getTopLevelAncestor();
//    	PkgMgrFrame parentFrame = (PkgMgrFrame)parentDialog.getOwner();
//    	if(parentFrame.getPackage().getBaseDir() != null)
//    	    this.infoPan.setImportDirectory(parentFrame.getPackage().getBaseDir());
    
    }

    public void savePreferences()
    {
	this.infoPan.savePreferences( "import" );
    }

    public void actionPerformed( ActionEvent event )
    {
	String command = event.getActionCommand();
	
	if ( command.equalsIgnoreCase( "IMPORT" ) )
	    {
		this.performImport();
	    }
	else if ( command.equalsIgnoreCase( "CANCEL" ) )
	    {
		this.cancelImport();
	    }
    }
    
    private void cancelImport()
    {
	this.client.setCanceled( true );
    }

    private void performImport()
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

	String userName = this.infoPan.getUserName();
	String passWord = this.infoPan.getPassword();
	String hostname = props.getProperty("group.server", null);//this.infoPan.getServer();
	String repository = this.infoPan.getModule();
	String rootDirectory = props.getProperty("group.repository.path", null);//this.infoPan.getRepository();
	String importDirectory = this.infoPan.getImportDirectory();
	
	String vendorTag = this.addPan.getVendorTag();
	String releaseTag = this.addPan.getReleaseTag();
	String messageStr = this.addPan.getLogMessage();
	
	if ( repository.startsWith( "/" ) )
	    repository = repository.substring( 1 );
	
	if ( repository.endsWith( "/" ) )
	    repository = repository.substring( 0, repository.length()-1 );
	
	if ( rootDirectory.endsWith( "/" ) )
	    rootDirectory =
		rootDirectory.substring( 0, rootDirectory.length()-1 );

	String	rootRepository = rootDirectory + "/" + repository;
	
	int connMethod = CVSRequest.METHOD_INETD;
            // ( this.infoPan.isInetdSelected()
            // ? CVSRequest.METHOD_INETD
            //: CVSRequest.METHOD_RSH );
	
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
		
		String msg = rmgr.getUIFormat( "import.needs.input.msg", fmtArgs );
		String title = rmgr.getUIString( "import.needs.input.title" );
		JOptionPane.showMessageDialog
		    ( (JDialog)this.getTopLevelAncestor(),
		      msg, title, JOptionPane.ERROR_MESSAGE );
		return;
	    }
	
	if ( connMethod == CVSRequest.METHOD_RSH
	     && userName.length() < 1 )
	    {
		String msg = rmgr.getUIString("common.rsh.needs.user.msg" );
		String title = rmgr.getUIString("common.rsh.needs.user.title" );
		JOptionPane.showMessageDialog
		    ( (JDialog)this.getTopLevelAncestor(),
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
	
	String ignoreStr = this.addPan.getIgnores();
	if ( ignoreStr.length() > 0 )
	    {
		ignore.addIgnoreSpec( ignoreStr );
	    }
	
	// We leverage the ignores mechanism to indicate binaries!
	CVSIgnore binaries = new CVSIgnore( "" );
	
	String binariesStr = this.addPan.getBinaries();
	if ( binariesStr.length() > 0 )
	    {
		binaries.addIgnoreSpec( binariesStr );
	    }
	
	boolean descend = this.addPan.isDescendChecked();
	
	allok =
	    this.importScan
	    ( rootDirectory, repository, importDirectory,
	      descend, entries, ignore, binEntries, binaries );
	
	if ( ! allok )
	    {
		String msg = rmgr.getUIString( "import.scan.error.msg" );
		String title = rmgr.getUIString( "import.scan.error.title" );
		JOptionPane.showMessageDialog
		    ( (JDialog)this.getTopLevelAncestor(),
		      msg, title, JOptionPane.ERROR_MESSAGE );
		this.outputText.setText( this.scanText.toString() );
		this.outputText.repaint( 500 );
		return;
	    }
	
	boolean isPServer = true;// this.infoPan.isPasswordSelected();
	
	int cvsPort =
	    CVSUtilities.computePortNum
	    ( hostname, connMethod, isPServer );
	
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
	
	CVSThread thread =
	    new CVSThread( "Import",
			   this.new MyRunner
			   ( this.client, request, response, binEntries ),
			   this.new MyMonitor( request, response ) );
	
	thread.start();
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
			( "\n\n--------- "
			  + ResourceMgr.getInstance().getUIString
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
    
	private
	class		MyMonitor
	implements	CVSThread.Monitor
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
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "import.cancel.label" ) );
			}

		public void
		threadCanceled()
			{
			}

		public void
		threadFinished()
			{
			actionButton.setText
				( ResourceMgr.getInstance().getUIString
					( "import.perform.label" ) );

			String resultStr = this.response.getDisplayResults();

			if ( this.response.getStatus() == CVSResponse.OK )
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "import.status.success" ) );
				}
			else
				{
				uiDisplayProgressMsg
					( ResourceMgr.getInstance().getUIString
						( "import.status.failure" ) );
				}

			outputText.setText( resultStr );
			outputText.revalidate();
			outputText.repaint();

			if ( this.response != null
					&& ! this.request.saveTempFiles )
				{
				this.response.deleteTempFiles();
				}

			getMainGrpPanel().setAllTabsEnabled( true );
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
				"ImportDescend["+i+"] fileName '"
				+ fileName + "' isDir '"
				+ file.isDirectory() + "' filePath '"
				+ file.getPath() + "'" );

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

	//
	// CVS USER INTERFACE METHODS
	//

	public void
	uiDisplayProgressMsg( String message )
		{
		this.feedback.setText( message );
		this.feedback.repaint( 0 );
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
		JLabel		lbl;
		JPanel		panel;
		JButton		button;

		ResourceMgr rmgr = ResourceMgr.getInstance();

		this.setLayout( new GridBagLayout() );

		this.tabbed = new JTabbedPane();

		this.infoPan = new ConnectInfoPanel( "import" );
		//this.infoPan.setPServerMode( true );
		//this.infoPan.setUsePassword( true );

		this.tabbed.addTab
			( rmgr.getUIString( "import.tab.connection" ),
				null, this.infoPan );

		this.addPan = this.new AdditionalInfoPanel();
		this.tabbed.addTab
			( rmgr.getUIString( "import.tab.additional" ),
				null, this.addPan );

		// ============== INPUT FIELDS PANEL ================

		int row = 0;

		JSeparator sep;

		AWTUtilities.constrain(
			this, this.tabbed,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST,
			0, row++, 1, 1, 1.0, 0.0 );

		this.actionButton =
			new JButton( rmgr.getUIString( "import.perform.label" ) );
		this.actionButton.setActionCommand( "IMPORT" );
		this.actionButton.addActionListener( this );
		AWTUtilities.constrain(
			this, this.actionButton,
			GridBagConstraints.NONE,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 0.0, 0.0,
			new Insets( 5, 5, 5, 5 ) );

		this.feedback =
			new JLabel( rmgr.getUIString( "name.for.ready" ) );
		this.feedback.setOpaque( true );
		this.feedback.setBackground( Color.white );
		this.feedback.setBorder
			( new CompoundBorder
				( new LineBorder( Color.darkGray ),
					new EmptyBorder( 1, 3, 1, 3 ) ) );

		AWTUtilities.constrain(
			this, this.feedback,
			GridBagConstraints.HORIZONTAL,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 0.0,
			new Insets( 4, 0, 3, 0 ) );

		this.outputText =
			new JTextArea()
				{
				public boolean isFocusTraversable() { return false; }
				};
		this.outputText.setEditable( false );

		JScrollPane scroller =
			new JScrollPane( this.outputText );
		scroller.setVerticalScrollBarPolicy
			( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );

		AWTUtilities.constrain(
			this, scroller,
			GridBagConstraints.BOTH,
			GridBagConstraints.CENTER,
			0, row++, 1, 1, 1.0, 1.0 );
		}

	private
	class		AdditionalInfoPanel
	extends		JPanel
		{
		private JTextArea		ignores;
		private JTextArea		binaries;
		private JTextArea		logmsg;
		private JTextField		vendor;
		private JTextField		release;
		private JCheckBox		descend;

		public String
		getIgnores()
			{
			return this.ignores.getText();
			}

		public String
		getBinaries()
			{
			return this.binaries.getText();
			}

		public String
		getLogMessage()
			{
			return this.logmsg.getText();
			}

		public String
		getVendorTag()
			{
			return this.vendor.getText();
			}

		public String
		getReleaseTag()
			{
			return this.release.getText();
			}

		public boolean
		isDescendChecked()
			{
			return this.descend.isSelected();
			}

		public
		AdditionalInfoPanel()
			{
			super();
			this.setLayout( new GridLayout( 2, 2, 4, 4 ) );
			ResourceMgr rmgr = ResourceMgr.getInstance();

			JPanel tagPan = new JPanel();
			tagPan.setLayout( new GridBagLayout() );

			int row = 0;

			this.descend =
				new JCheckBox
					( rmgr.getUIString( "import.subdir.checkbox.label" ) );
			
			this.descend.setSelected( true );
			AWTUtilities.constrain(
				tagPan, this.descend,
				GridBagConstraints.NONE,
				GridBagConstraints.CENTER,
				0, row++, 2, 1, 1.0, 0.0,
				new Insets( 1, 3, 1, 3 ) );

			AWTUtilities.constrain(
				tagPan, new JLabel( rmgr.getUIString( "import.vendortag.label" ) ),
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0,
				new Insets( 0, 3, 1, 0 ) );

			this.vendor = new JTextField();
			//temporary
			this.vendor.setText("yoyo");
			AWTUtilities.constrain(
				tagPan, this.vendor,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.CENTER,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 3, 3, 3, 3 ) );

			AWTUtilities.constrain(
				tagPan,
				new JLabel
					( rmgr.getUIString( "import.releasetag.label" ) ),
				GridBagConstraints.NONE,
				GridBagConstraints.WEST,
				0, row, 1, 1, 0.0, 0.0,
				new Insets( 0, 3, 1, 0 ) );

			this.release = new JTextField();
			//temporary
			this.release.setText("start");
			AWTUtilities.constrain(
				tagPan, this.release,
				GridBagConstraints.HORIZONTAL,
				GridBagConstraints.CENTER,
				1, row++, 1, 1, 1.0, 0.0,
				new Insets( 1, 3, 3, 3 ) );

			this.logmsg = new JTextArea();
			//temporary
			this.logmsg.setText("Testing import");
			JPanel logPan = new JPanel();
			logPan.setLayout( new BorderLayout() );
			logPan.add( BorderLayout.CENTER, this.logmsg );
			logPan.setBorder
				( new TitledBorder
					( new EtchedBorder
						( EtchedBorder.RAISED ),
						rmgr.getUIString( "import.logmsg.label" ) ) );

			this.ignores = new JTextArea();
			JPanel ignPan = new JPanel();
			ignPan.setLayout( new BorderLayout() );
			ignPan.add( BorderLayout.CENTER, this.ignores );
			ignPan.setBorder
				( new TitledBorder
					( new EtchedBorder
						( EtchedBorder.RAISED ),
						rmgr.getUIString( "import.ignores.label" ) ) );

			this.binaries = new JTextArea();
			JPanel binPan = new JPanel();
			binPan.setLayout( new BorderLayout() );
			binPan.add( BorderLayout.CENTER, this.binaries );
			binPan.setBorder
				( new TitledBorder
					( new EtchedBorder
						( EtchedBorder.RAISED ),
						rmgr.getUIString( "import.binaries.label" ) ) );

			this.add( tagPan );
			this.add( logPan );
			this.add( ignPan );
			this.add( binPan );
			}
		}

	}
