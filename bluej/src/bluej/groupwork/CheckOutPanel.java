package bluej.groupwork;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.*;
import javax.accessibility.AccessibleContext;

import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

/**
** @version $Id: CheckOutPanel.java 426 2000-04-14 01:11:12Z markus $
** @author Modifications to jCVS CheckOutPanel.java by Markus Ostman
**
** Check out panel for bluej group support.
**/

public class CheckOutPanel extends GroupWorkPanel
implements ActionListener, CVSUserInterface
{
    protected CVSClient client;
    protected ConnectInfoPanel info;
    private JFileChooser localDirChooser;
    //Don't ask why we need this
    private PkgMgrFrame frame=null;
    private boolean openPkg=true;

    //protected JTextArea	outputText;
    //protected JLabel feedback;

    //private JLabel explain;
    protected JButton actionButton;
    //protected JButton helpButton;
    protected JButton cancelButton;

    public CheckOutPanel( MainGrpPanel parent )
    {
        super( parent );
        this.establishContents();
        //this.getRootPane().setDefaultButton(this.actionButton);
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
        Debug.message("chkoutpnl,line58 "+command);
        GroupWorkDialog g=(GroupWorkDialog)getTopLevelAncestor();

        //If no selection is made the action command is
        //CancelSelection from both buttons in FileChooser. 
        if(command.equalsIgnoreCase("CancelSelection")){
            Debug.message("chkoutpnl,line61 "+"No selection");
            DialogManager.showMessage((JDialog)this.getTopLevelAncestor(),
                                      "no-selection");
        }
        //If selection is approved, enabled Checkout button
        if(command.equalsIgnoreCase("ApproveSelection")){
            Debug.message("chkoutpnl,line61 "+localDirChooser.getSelectedFile().getPath());
            //this.actionButton.setEnabled(true);

        }
//         if(command.equalsIgnoreCase("HELP")){
//             Debug.message("chkoutpnl,line61 "+"HELP");
//             DialogManager.showMessage((JDialog)this.getTopLevelAncestor(),
//                                       "not-yet-implemented");
//         }

        if ( command.equalsIgnoreCase( "CHECKOUT" ) )
            {
                PkgMgrFrame parentFrame = (PkgMgrFrame)g.getOwner();
                g.doClose();
                if(this.localDirChooser.showDialog(parentFrame, "Choose")==JFileChooser.APPROVE_OPTION){ 
                    this.performCheckout();
                }
            }
        else if ( command.equalsIgnoreCase( "CANCEL" ) )
            {
                //this.cancelCheckout();
                g.doClose();
            }
    }

    private void cancelCheckout()
    {
        this.client.setCanceled( true );
    }

    private void performCheckout()
    {
        Debug.message("chkoutpnl,line660 ");
        Config cfg = Config.getInstance();
        UserPrefs prefs = cfg.getPreferences();
        ResourceMgr rmgr = ResourceMgr.getInstance();

        Debug.message("chkOutPnl,80 "+props.getProperty("group.server", null));

        String argumentStr = "";//user choice/hard coded/admin choice?
        String userName = this.info.getUserName();
        String passWord = this.info.getPassword();
        String hostname = props.getProperty("group.server", null);
        String repository = this.info.getModule();
        String rootDirectory = props.getProperty("group.repository.path",
                                                 null);
        //./temp here we talked about a file save dialog
        //File localDirFile = localDirChooser.getSelectedFile();
        //String localDirectory = this.info.getLocalDirectory();
        String localDirectory = localDirChooser.getSelectedFile().getPath();

        //user choice/hard coded/admin choice?
        boolean isPServer = true;//this.info.isPasswordSelected();

        //user choice/hard coded/admin choice?
        int connMethod = CVSRequest.METHOD_INETD;//CVSRequest.METHOD_RSH


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
             ( project.getLocalRootDirectory() ) ){
            String title = repository + " project";

            request.setPServer( isPServer );
            request.setUserName( userName );

            if ( isPServer ){
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
            request.setGzipStreamLevel(project.getGzipStreamLevel());

            request.setEntries( entries );

            request.appendArguments( arguments );

            request.setUserInterface( this );

            if(request.verifyRequest())
                Debug.message("ChkOutPnl, line270 "+request.getVerifyFailReason());


            CVSResponse response = new CVSResponse();
            Debug.message("bjChkOPnl, line274: "+project.getRootEntry());
            Debug.message("bjChkOPnl, line274: "+project.getRepository());
            Debug.message("bjChkOPnl, line274: "+project.getLocalRootPath());

            CVSJobQueue.getJobQueue().addJob("CheckOut",
                                             this.new MyRunner(project,
                                                               this.client,
                                                               request, 
                                                               response),
                                             this.new MyMonitor(request,
                                                                response));

            Debug.message("GrpPkgMgr line 456 Add "+request.getCommand()
                          +" (checkout) to the Job queue");
            //******************************************
            //this is now a relic from jCVS. Remember to change the monitor
            // implementation, if change back.
            //**************************************
            //CVSThread thread =
            //  new CVSThread( "CheckOut",
            //                 this.new MyRunner( project, 
            //                                    this.client, 
            //                                    request, 
            //                                    response ),
            //                 this.new MyMonitor( request, 
            //                                     response ));
            //thread.start();
        }
    }

    private class MyRunner
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
    implements CVSJob.Monitor
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
            //actionButton.setActionCommand( "CANCEL" );
            //actionButton.setText(ResourceMgr.getInstance().getUIString
            // ( "checkout.cancel.label" ) );
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

                //Here we open the project in bluej
                if(openPkg)
                    openProject(rootDirFile, request.getPassword());

            }
            else{
                uiDisplayProgressMsg(ResourceMgr.getInstance().getUIString
                                     ( "checkout.status.failure" ) );
            }

            Debug.message(resultStr);
            //outputText.setText( resultStr );
            //outputText.revalidate();
            //outputText.repaint();

            if ( this.response != null && ! this.request.saveTempFiles ){
                this.response.deleteTempFiles();
            }

            getMainGrpPanel().setAllTabsEnabled( true );
            Debug.message("chkoutPnl,line403 "+Thread.currentThread().getName()+" THREAD FINISHED");
        }

    }

    //
    // CVS USER INTERFACE METHODS
    //

    public void
    uiDisplayProgressMsg( String message )
    {
        Debug.message(message);
        //this.feedback.setText( message );
        //this.feedback.repaint( 0 );
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

        //this.info.setPServerMode( true );
        //this.info.setUsePassword( true );


        // ============== INPUT FIELDS PANEL ================

        int row = 0;

        AWTUtilities.constrain(
                               this, info,
                               GridBagConstraints.HORIZONTAL,
                               GridBagConstraints.WEST,
                               0, row++, 1, 1, 1.0, 0.0 );

        // this.feedback =
        // 	    new JLabel
        // 	    ( ResourceMgr.getInstance().getUIString
        // 	      ( "name.for.ready" ) );
        // 	this.feedback.setOpaque( true );
        // 	this.feedback.setBackground( Color.white );
        // 	this.feedback.setBorder
        // 	    ( new CompoundBorder
        // 	      ( new LineBorder( Color.darkGray ),
        // 		new EmptyBorder( 1, 3, 1, 3 ) ) );

        // 	AWTUtilities.constrain(
        // 			       this, this.feedback,
        // 			       GridBagConstraints.HORIZONTAL,
        // 			       GridBagConstraints.CENTER,
        // 			       0, row++, 1, 1, 1.0, 0.0,
        // 			       new Insets( 4, 0, 3, 0 ) );

        // 	this.outputText = new JTextArea(){
        // 	    public boolean isFocusTraversable() { return false; }
        // 	};

        // 	this.outputText.setEditable( false );

        // 	JScrollPane scroller =
        // 	    new JScrollPane( this.outputText );
        // 	scroller.setVerticalScrollBarPolicy
        // 	    ( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );

        // 	AWTUtilities.constrain(
        // 			       this, scroller,
        // 			       GridBagConstraints.BOTH,
        // 			       GridBagConstraints.CENTER,
        // 			       0, row++, 1, 1, 1.0, 1.0 );

        JSeparator sep = new JSeparator( SwingConstants.HORIZONTAL );

        AWTUtilities.constrain(
                               this, sep,
                               GridBagConstraints.HORIZONTAL,
                               GridBagConstraints.CENTER,
                               0, row++, 1, 1, 1.0, 0.0,
                               new Insets( 3, 0, 5, 0 ) );

        //         this.explain = new JLabel("Choose Directory to store local files");
        //         AWTUtilities.constrain(
        // 			       this, this.explain,
        // 			       GridBagConstraints.NONE,
        // 			       GridBagConstraints.CENTER,
        // 			       0, row++, 1, 1, 1.0, 0.0,
        // 			       new Insets( 3, 0, 5, 0 ) );

        this.localDirChooser = new JFileChooser();
        this.localDirChooser.setFileView(new PackageFileView());
        this.localDirChooser.setDialogTitle("Choose local directory");
        //this.localDirChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        //this.localDirChooser.setApproveButtonText("Choose");
        this.localDirChooser.setApproveButtonToolTipText("Choose selected directory"); 
        this.localDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
        this.localDirChooser.addActionListener(this); 

        //         AWTUtilities.constrain(
        // 			       this, localDirChooser,
        // 			       GridBagConstraints.BOTH,
        // 			       GridBagConstraints.CENTER,
        // 			       0, row++, 1, 1, 1.0, 1.0 );

        //         sep = new JSeparator( SwingConstants.HORIZONTAL );

        // 	AWTUtilities.constrain(
        // 			       this, sep,
        // 			       GridBagConstraints.HORIZONTAL,
        // 			       GridBagConstraints.CENTER,
        // 			       0, row++, 1, 1, 1.0, 0.0,
        // 			       new Insets( 3, 0, 5, 0 ) );

        // button panel at bottom of dialog
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        this.actionButton =
            new JButton
            ( ResourceMgr.getInstance().getUIString
              ( "checkout.perform.label" ) );

        this.actionButton.setActionCommand( "CHECKOUT" );
        this.actionButton.addActionListener( this );
        //this.actionButton.setEnabled(false);
        buttonPanel.add(this.actionButton);
        
        // AWTUtilities.constrain(
        // 			       this, this.actionButton,
        // 			       GridBagConstraints.NONE,
        // 			       GridBagConstraints.CENTER,
        // 			       0, row, 1, 1, 0.0, 0.0,
        // 			       new Insets( 5, 5, 5, 5 ) );

        //this.helpButton =  new JButton(bluej.Config.getString("menu.help"));

        //this.helpButton.setActionCommand( "HELP" );
        //this.helpButton.addActionListener( this );
        //buttonPanel.add(helpButton);
        // AWTUtilities.constrain(
        // 			       this, this.helpButton,
        // 			       GridBagConstraints.NONE,
        // 			       GridBagConstraints.CENTER,
        // 			       1, row, 1, 1, 0.0, 0.0,
        // 			       new Insets( 5, 5, 5, 5 ) );

        this.cancelButton =  new JButton(bluej.Config.getString("cancel"));

        this.cancelButton.setActionCommand( "CANCEL" );
        this.cancelButton.addActionListener( this );
        buttonPanel.add(cancelButton);
        AWTUtilities.constrain(
                               this, buttonPanel,
                               GridBagConstraints.NONE,
                               GridBagConstraints.CENTER,
                               0, row++, 1, 1, 0.0, 0.0,
                               new Insets( 5, 5, 5, 5 ) );

    }//End establishContents

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
        PkgMgrFrame parentFrame;
        if(this.frame == null){
            JDialog parentDialog = (JDialog)getTopLevelAncestor();
            parentFrame = (PkgMgrFrame)parentDialog.getOwner();
        }
        else
            parentFrame=this.frame;

        if (props.getProperty("group.usesPkgInfo", "false").equals("true")){
            //Here we open the package directly
            //Requires bluej.pkg file
            parentFrame.doOpenPackage(rootDirFile.getPath(), passwd);
        }
        else{
            //Here is another approach where we import a package
            //No bluej.pkg file is required
            Package.importPackage(rootDirFile, parentFrame, false);
            parentFrame.getPackage().load(rootDirFile.getPath(),
                                          null, true, false);
            parentFrame.getPackage().turnIntoGroupPackage(true);
            parentFrame.getPackage().save();
            //Hack! We need to remove the package before we can open it
            //Othervise we get some kind of conflict and it is not 
            //displayed properly.
            parentFrame.removePackage();
            ///.temp Decide what happens if checkout again in same frame
            parentFrame.doOpenPackage(rootDirFile.getPath(), 
                                      passwd);
        }
    }//End openProject()

    /**
     * Enables checkout without displaying the panel
     *
     * @param passwd     The project password
     * @param userName   Server user name
     * @param module     Project name in repository
     * @param localDir   Where to put the checked out files
     * @param frame      The current Frame object
     * @param openPkg    Do we want to open the checked out package
     * @returns void
     */
    public void doCheckOut(String passWord, String userName,
                           String module, String localDir,
                           PkgMgrFrame frame, boolean openPkg)
    {
        //this might look a bit weird but it is just to set up 
        //everything right before the call to performCheckout,
        //which is written for user input.
        this.frame=frame;
        this.openPkg=openPkg;
        this.info.setUserName(userName);
        this.info.setPassword(passWord);
        this.info.setModule(module);
        this.localDirChooser.setSelectedFile(new File(localDir));
        this.performCheckout();
    }


}


