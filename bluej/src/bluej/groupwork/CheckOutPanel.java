package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.*;

import bluej.utility.Debug;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

/**
 ** @version $Id: CheckOutPanel.java 401 2000-02-29 01:42:12Z markus $
 ** @author Modifications to jCVS CheckOutPanel.java by Markus Ostman
 **
 ** Check out panel for bluej group support.
 **/

public class CheckOutPanel extends GroupWorkPanel
    implements ActionListener, CVSUserInterface
{
    protected CVSClient client;
    protected ConnectInfoPanel info;
    protected JTextField argumentsText;
    protected JTextField localDirText;
    
    protected JTextArea	outputText;
    protected JLabel feedback;
    
    protected JButton actionButton;
    
    
    public CheckOutPanel( MainGrpPanel parent )
    {
	super( parent );
	this.establishContents();
    }

    public void loadPreferences()
    {
	this.info.loadPreferences( "chkout" );
    }

    public void savePreferences()
    {
	this.info.savePreferences( "chkout" );
    }

    public void actionPerformed( ActionEvent event )
    {
	String command = event.getActionCommand();
	
	if ( command.equalsIgnoreCase( "CHECKOUT" ) )
	    {
		this.performCheckout();
	    }
	else if ( command.equalsIgnoreCase( "CANCEL" ) )
	    {
		this.cancelCheckout();
	    }
    }

    private void cancelCheckout()
    {
	this.client.setCanceled( true );
    }

    private void performCheckout()
    {
	Config cfg = Config.getInstance();
	UserPrefs prefs = cfg.getPreferences();
	ResourceMgr rmgr = ResourceMgr.getInstance();

	Debug.message("chkOutPnl,80 "+props.getProperty("group.server", null));
	
	String argumentStr = info.getArguments();
	String userName = this.info.getUserName();
	String passWord = this.info.getPassword();
	String hostname = props.getProperty("group.server", null);//this.info.getServer();
	String repository = this.info.getModule();
	String rootDirectory = props.getProperty("group.repository.path", null);//this.info.getRepository();
	String localDirectory = this.info.getLocalDirectory();
	
	boolean isPServer = this.info.isPasswordSelected();
	
	int connMethod = ( this.info.isInetdSelected()
			   ? CVSRequest.METHOD_INETD
			   : CVSRequest.METHOD_RSH );


	int cvsPort = CVSUtilities.computePortNum( hostname, 
						   connMethod, 
						   isPServer );

	// SANITY
	if ( hostname.length() < 1
	     || repository.length() < 1
	     || rootDirectory.length() < 1
	     || localDirectory.length() < 1 )
	    {
		String[] fmtArgs = new String[1];
		fmtArgs[0] = ( hostname.length() < 1
			       ? rmgr.getUIString( "name.for.cvsserver" ) :
			       ( repository.length() < 1
				 ? rmgr.getUIString( "name.for.cvsmodule" ) :
				 ( rootDirectory.length() < 1
				   ? rmgr.getUIString( "name.for.cvsrepos" )
				   : rmgr.getUIString( "name.for.checkoutdir" ) )));

		String msg = rmgr.getUIFormat( "checkout.needs.input.msg", fmtArgs );
		String title = rmgr.getUIString( "checkout.needs.input.title" );
		JOptionPane.showMessageDialog((JDialog)this.getTopLevelAncestor(),
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

		File localRootDir = new File( localDirectory );
		if ( ! localRootDir.exists() )
			{
			if ( ! localRootDir.mkdirs() )
				{
				String [] fmtArgs = { localRootDir.getPath() };
				String msg = ResourceMgr.getInstance().getUIFormat
					("checkout.create.dir.failed.msg", fmtArgs );
				String title = ResourceMgr.getInstance().getUIString
					("checkout.create.dir.failed.title" );
				JOptionPane.showMessageDialog
					( (JDialog)this.getTopLevelAncestor(),
						msg, title, JOptionPane.ERROR_MESSAGE );
				return;
				}
			}

		CVSRequest request = new CVSRequest();

		String checkOutCommand =
			prefs.getProperty
				( "global.checkOutCommand", ":co:N:ANP:deou:" );

		if ( ! request.parseControlString( checkOutCommand ) )
			{
			String [] fmtArgs =
				{ checkOutCommand, request.getVerifyFailReason() };
			String msg = rmgr.getUIFormat("checkout.cmd.parse.failed.msg", fmtArgs );
			String title = rmgr.getUIString("checkout.cmd.parse.failed.title" );
			JOptionPane.showMessageDialog
				( (JDialog)this.getTopLevelAncestor(),
					msg, title, JOptionPane.ERROR_MESSAGE );
			return;
			}

		//
		// DO IT
		//
		CVSEntryVector entries = new CVSEntryVector();

		CVSArgumentVector arguments =
			CVSArgumentVector.parseArgumentString( argumentStr );

		arguments.appendArgument( repository );

		this.getMainGrpPanel().setAllTabsEnabled( false );

		this.client = new CVSClient( hostname, cvsPort );
		CVSProject project = new CVSProject( this.client );

		project.setUserName( userName );

		project.setTempDirectory( cfg.getTemporaryDirectory() );
		project.setRepository( repository );
		project.setRootDirectory( rootDirectory );
		project.setLocalRootDirectory( localDirectory );
		project.setPServer( isPServer );
		project.setConnectionMethod( connMethod );

		project.setSetVariables
			( CVSUtilities.getUserSetVariables( hostname ) );
			
		project.setServerCommand(
			CVSUtilities.establishServerCommand
				( hostname, connMethod, isPServer ) );

		project.setAllowsGzipFileMode
			( prefs.getBoolean( Config.GLOBAL_ALLOWS_FILE_GZIP, false ) );

		project.setGzipStreamLevel
			( prefs.getInteger( Config.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

		if ( isPServer )
			{
			String scrambled =
				CVSScramble.scramblePassword( passWord, 'A' );

			project.setPassword( scrambled );
			}
			
		if ( connMethod == CVSRequest.METHOD_RSH )
			{
			CVSUtilities.establishRSHProcess( project );
			}

		// Finally, we must make sure that the Project has its root 
		//entry, as CVSProject will not be able to create it from the 
		//context that the server will send with the checkout.
		Debug.message("ChkOutPanel,line220: "+rootDirectory);
		project.establishRootEntry( rootDirectory );

		// UNDONE
		// IF IT IS ALREADY OPEN, we should tell the ProjectFrame
		// to do this!!!
		if ( ! ProjectFrameMgr.checkProjectOpen
				( project.getLocalRootDirectory() ) )
			{
			String title = repository + " project";

			request.setPServer( isPServer );
			request.setUserName( userName );

			if ( isPServer )
				{
				request.setPassword( project.getPassword() );
				}

			request.setConnectionMethod( connMethod );
			request.setServerCommand( project.getServerCommand() );
			request.setRshProcess( project.getRshProcess() );

			request.setPort( cvsPort );
			request.setHostName( this.client.getHostName() );

			request.setRepository( repository );
			request.setRootDirectory( rootDirectory );
			request.setRootRepository( rootDirectory );
			request.setLocalDirectory( localRootDir.getPath() );

			request.setSetVariables( project.getSetVariables() );

			request.responseHandler = project;

			request.traceRequest = CVSProject.overTraceRequest;
			request.traceResponse = CVSProject.overTraceResponse;
			request.traceTCPData = CVSProject.overTraceTCP;
			request.traceProcessing = CVSProject.overTraceProcessing;

			request.allowGzipFileMode = project.allowsGzipFileMode();
			request.setGzipStreamLevel( project.getGzipStreamLevel() );

			request.setEntries( entries );

			request.appendArguments( arguments );

			request.setUserInterface( this );

			if(request.verifyRequest())
			    Debug.message("ChkOutPnl, line270 "+request.getVerifyFailReason());
			    

			CVSResponse response = new CVSResponse();
			Debug.message("bjChkOPnl, line274: "+project.getRootEntry());
			Debug.message("bjChkOPnl, line274: "+project.getRepository());
			Debug.message("bjChkOPnl, line274: "+project.getLocalRootPath());
			CVSThread thread =
				new CVSThread( "CheckOut",
					       this.new MyRunner( project, 
								  this.client, 
								  request, 
								  response ),
					       this.new MyMonitor( request, 
								   response ));

			thread.start();
			}
		}

	private
	class		MyRunner
	implements	Runnable
		{
		private CVSClient client;
		private CVSProject project;
		private CVSRequest request;
		private CVSResponse response;

		public
		MyRunner( CVSProject project, CVSClient client,
					CVSRequest request, CVSResponse response )
			{
			this.client = client;
			this.project = project;
			this.request = request;
			this.response = response;
			}

		public void
		run()
			{
			this.client.processCVSRequest( this.request, this.response );
			this.project.processCVSResponse( this.request, response );
			}
		}

    private class MyMonitor
	implements CVSThread.Monitor
    {
	private CVSRequest request;
	private CVSResponse response;
	
	public MyMonitor( CVSRequest request, CVSResponse response )
	{
	    this.request = request;
	    this.response = response;
	}
	
	public void threadStarted()
	{
	    actionButton.setActionCommand( "CANCEL" );
	    actionButton.setText(ResourceMgr.getInstance().getUIString
				 ( "checkout.cancel.label" ) );
	}

	public void threadCanceled()
	{
	}
	
	public void threadFinished()
	{
	    actionButton.setActionCommand( "CHECKOUT" );
	    actionButton.setText(ResourceMgr.getInstance().getUIString
				 ("checkout.perform.label" ) );
	    
	    String resultStr = this.response.getDisplayResults();
	    
	    if ( this.response.getStatus() == CVSResponse.OK ){
		uiDisplayProgressMsg
		    ( ResourceMgr.getInstance().getUIString
		      ( "checkout.status.success" ) );
		
		File rootDirFile = new File( request.getLocalDirectory()+
					     "/" + 
					     request.getRepository());
		Debug.message("CheckOutPanel.java,line 354" + rootDirFile.getPath());
		    
		// //./temp This is just temporary?
// 		JDialog parentDialog = (JDialog)getTopLevelAncestor();
// 		PkgMgrFrame parentFrame = (PkgMgrFrame)parentDialog.getOwner();
//                 //Here we open the package directly
//                 //Requires bluej.pkg file
// 		//parentFrame.doOpenPackage(rootDirFile.getPath(), 
//                 //request.getPassword());
//             //--------------------------------------------------------------
//                 //Here is another approach where we import a package
//                 //No bluej.pkg file is required
//                 Package.importPackage(rootDirFile, parentFrame);
//                 parentFrame.getPackage().load(rootDirFile.getPath(),
//                                               null, true, false);
//                 parentFrame.getPackage().turnIntoGroupPackage(true);
//                 parentFrame.getPackage().save();
//                 //Hack! We need to remove the package before we can open it
//                 //Othervise we get some kind of conflict and it is not 
//                 //displayed properly.
//                 parentFrame.removePackage();
//                 parentFrame.doOpenPackage(rootDirFile.getPath(), 
// 					  request.getPassword());
                
                //Here we open the project in bluej
                openProject(rootDirFile, request.getPassword());
		
                //ProjectFrame.openProject
		//  ( rootDirFile, request.getPassword() );
	    }
	    else{
		uiDisplayProgressMsg(ResourceMgr.getInstance().getUIString
				     ( "checkout.status.failure" ) );
	    }
			
	    outputText.setText( resultStr );
	    outputText.revalidate();
	    outputText.repaint();
	    
	    if ( this.response != null && ! this.request.saveTempFiles ){
		this.response.deleteTempFiles();
	    }
	    
	    getMainGrpPanel().setAllTabsEnabled( true );
	}
	
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

    private void establishContents()
    {
	JLabel		lbl;
	JPanel		panel;
	JButton		button;
	
	this.setLayout( new GridBagLayout() );
	
	this.info = new ConnectInfoPanel( "checkout" );
	this.info.setPServerMode( true );
	this.info.setUsePassword( true );
	
	
	// ============== INPUT FIELDS PANEL ================
	
	int row = 0;
	
	JSeparator sep;
	
	AWTUtilities.constrain(
			       this, info,
			       GridBagConstraints.HORIZONTAL,
			       GridBagConstraints.WEST,
			       0, row++, 1, 1, 1.0, 0.0 );
	
	this.actionButton =
	    new JButton
	    ( ResourceMgr.getInstance().getUIString
	      ( "checkout.perform.label" ) );
	
	this.actionButton.setActionCommand( "CHECKOUT" );
	this.actionButton.addActionListener( this );
	AWTUtilities.constrain(
			       this, this.actionButton,
			       GridBagConstraints.NONE,
			       GridBagConstraints.CENTER,
			       0, row++, 1, 1, 0.0, 0.0,
			       new Insets( 5, 5, 5, 5 ) );
	
	this.feedback =
	    new JLabel
	    ( ResourceMgr.getInstance().getUIString
	      ( "name.for.ready" ) );
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
	
	this.outputText = new JTextArea(){
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

    /**
     * Opens the checked out project in bluej
     *
     * @param pkgFile File object representing the project
     * @param passwd The project password    
     * @returns void
     */
    private void openProject(File rootDirFile, String passwd)
    {
        //./temp This is one way of getting the Owner Frame 
        JDialog parentDialog = (JDialog)getTopLevelAncestor();
        PkgMgrFrame parentFrame = (PkgMgrFrame)parentDialog.getOwner();

        if (props.getProperty("group.usesPkgInfo", "false").equals("true")){
            //Here we open the package directly
            //Requires bluej.pkg file
            parentFrame.doOpenPackage(rootDirFile.getPath(), 
                                      passwd);
        }
        else{
            //Here is another approach where we import a package
            //No bluej.pkg file is required
            Package.importPackage(rootDirFile, parentFrame);
            parentFrame.getPackage().load(rootDirFile.getPath(),
                                          null, true, false);
            parentFrame.getPackage().turnIntoGroupPackage(true);
            parentFrame.getPackage().save();
            //Hack! We need to remove the package before we can open it
            //Othervise we get some kind of conflict and it is not 
            //displayed properly.
            parentFrame.removePackage();
            parentFrame.doOpenPackage(rootDirFile.getPath(), 
                                      passwd);
        }
    }
}


