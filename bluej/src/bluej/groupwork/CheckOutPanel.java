package bluej.groupwork;

import com.ice.jcvsii.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.accessibility.AccessibleContext;

import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

import com.ice.cvsc.*;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

/**
 * Check out panel for BlueJ group support.
 *
 * @version $Id: CheckOutPanel.java 958 2001-06-21 03:45:54Z ajp $
 * @author  Markus Ostman, modifications to jCVS CheckOutPanel.java
 */
public class CheckOutPanel extends JPanel
    implements ActionListener, CVSUserInterface
{
    private CVSClient             client;
    private ConnectInfoPanel      info;
    private JFileChooser          localDirChooser;
    private PkgMgrFrame           frame=null;//Don't ask why we need this
    private boolean               openPkg=true;
    private JButton               actionButton;
    private JButton               cancelButton;
    private InfoDialog            errorInfo;
    //private MainGrpPanel          parent;
    //private Properties            props;
    private GroupWorkDialog       groupWorkDialog;
    //protected JButton helpButton;
    //protected JTextArea	outputText;
    //protected JLabel feedback;
    //private JLabel explain;

    public CheckOutPanel( GroupWorkDialog groupWorkDialog )
    {
        super();
        //this.parent = parent;
        this.groupWorkDialog = groupWorkDialog;
        this.setBorder( new EmptyBorder( 4, 4, 4, 4 ) );
        this.establishContents();
        this.errorInfo = new InfoDialog(this.groupWorkDialog.getParentFrame());
    }

//     public MainGrpPanel getMainGrpPanel()
//     {
// 	return this.parent;
//     }

    public GroupWorkDialog getGroupWorkDialog()
    {
	return this.groupWorkDialog;
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
        //GroupWorkDialog g=(GroupWorkDialog)getTopLevelAncestor();

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
                PkgMgrFrame parentFrame = (PkgMgrFrame)
                    groupWorkDialog.getOwner();
                groupWorkDialog.doClose();
                if(this.localDirChooser.showDialog(parentFrame, "Choose")==JFileChooser.APPROVE_OPTION){
                    this.performCheckout();
                }
            }
        else if ( command.equalsIgnoreCase( "CANCEL" ) )
            {
                //this.cancelCheckout();
                groupWorkDialog.doClose();
            }
    }

    private void cancelCheckout()
    {
        this.client.setCanceled( true );
    }

    private void performCheckout()
    {
        com.ice.jcvsii.Config cfg = com.ice.jcvsii.Config.getInstance();
        UserPrefs prefs = cfg.getPreferences();
        ResourceMgr rmgr = ResourceMgr.getInstance();

        String argumentStr = "";//user choice/hard coded/admin choice?
        String userName = this.info.getUserName();
        String passWord = this.info.getPassword();
        String hostname = Config.getPropString("group.server", null);
        String repository = this.info.getGroupName()+this.info.getModule();
        String rootDirectory = Config.getPropString("group.repository.path",
                                                 null);
        String localDirectory = localDirChooser.getSelectedFile().getPath();
        //this is a nasty work around the top level CVS dir problem
     //    if(openPkg){
//             localDirectory = localDirectory+File.separator+
//                 this.info.getModule();
//         }
        Debug.message("chkoutpnl,126 "+localDirChooser.getSelectedFile().getPath());
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

        //this.getMainGrpPanel().setAllTabsEnabled( false );

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
            ( prefs.getBoolean( com.ice.jcvsii.Config.GLOBAL_ALLOWS_FILE_GZIP, false ) );

        project.setGzipStreamLevel
            ( prefs.getInteger( com.ice.jcvsii.Config.GLOBAL_GZIP_STREAM_LEVEL, 0 ) );

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

        //maybe we should have a check here so we don't checkout
        //an already open project.
        if (true ){
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

            GroupJobQueue.getJobQueue().addJob("CheckOut",
                                             this.new MyRunner(project,
                                                               this.client,
                                                               request,
                                                               response),
                                             this.new MyMonitor(request,
                                                                response));

            Debug.message("GrpPkgMgr line 456 Add "+request.getCommand()
                          +" (checkout) to the Job queue");
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
    implements GroupJob.Monitor
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
                                             File.separator+
                                             info.getModule()/*request.getRepository()*/);
                Debug.message("CheckOutPanel.java,line 354" + rootDirFile.getPath());

                //Here we open the project in bluej
                if(openPkg)
                    openProject(rootDirFile, request.getPassword());
                //If someone is waiting for the checkout, lets notify them
                //that everthing went all right.
                Sync.s.callNotify(true);
            }
            else{
                uiDisplayProgressMsg(ResourceMgr.getInstance().getUIString
                                     ( "checkout.status.failure" ) );
                //If someone is waiting for the checkout, lets notify them
                //that something went wrong.
                Sync.s.callNotify(false);
                errorInfo.setText(resultStr);
                errorInfo.display(bluej.Config.getString
                                  ("groupwork.error.title"));
            }

            Debug.message(resultStr);
            //outputText.setText( resultStr );
            //outputText.revalidate();
            //outputText.repaint();

            if ( this.response != null && ! this.request.saveTempFiles ){
                this.response.deleteTempFiles();
            }

            //getMainGrpPanel().setAllTabsEnabled( true );
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


        JSeparator sep = new JSeparator( SwingConstants.HORIZONTAL );

        AWTUtilities.constrain(
                               this, sep,
                               GridBagConstraints.HORIZONTAL,
                               GridBagConstraints.CENTER,
                               0, row++, 1, 1, 1.0, 0.0,
                               new Insets( 3, 0, 5, 0 ) );



        this.localDirChooser = FileUtility.getFileChooser();
        this.localDirChooser.setDialogTitle("Choose local directory");
        this.localDirChooser.setApproveButtonToolTipText("Choose selected directory");
        this.localDirChooser.addActionListener(this);

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
        getGroupWorkDialog().getRootPane().
            setDefaultButton(this.actionButton);
        //this.actionButton.setEnabled(false);
        buttonPanel.add(this.actionButton);

        this.cancelButton =  new JButton(bluej.Config.getString("cancel"));

        this.cancelButton.setActionCommand( "CANCEL" );
        this.cancelButton.addActionListener( this );
        buttonPanel.add(cancelButton);
        AWTUtilities.constrain(this, buttonPanel,
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

        if (Config.getPropString("group.usesPkgInfo", "false").equals("true")){
            //Here we open the package directly
            //Requires bluej.pkg file. We also make sure that a checkout
            //works when we already have another project open (group or not).
            //---------------------------------------------------------
            //Maybe this should be a method in PkgFrame?
            //or a boolean in doopen().
            //Why isn't equals() used below? see PkgMgrFrame.doOpen()
            //where it comes from.
            //---------------------------------------------------------
            if(parentFrame.isEmptyFrame()){
                parentFrame.doOpenPackage(rootDirFile.getPath(), passwd);
                //after checkout, the project is up-to-date
                parentFrame.setStatus(bluej.Config.getString
                                       ("groupwork.notChanged"));
            }
            else {
                // Otherwise open it in a new window
                PkgMgrFrame newFrame = parentFrame.createFrame(rootDirFile.getPath());
                newFrame.setVisible(true);
                //If group package, initialize. This is a bit messy,
                //should be done in createFrame instead!?
                Package pkg = newFrame.getPackage();
                if(pkg.getIsGroupPackage()){
                    pkg.initializeGroupInfo(pkg.getDirName(), parentFrame,
                                            passwd);
                }
                //newFrame.enableFunctions(true);
                //after checkout, the project is up-to-date
                newFrame.setStatus(bluej.Config.getString
                                       ("groupwork.notChanged"));
            }
            //------------------------------------------------------
            //parentFrame.doOpenPackage(rootDirFile.getPath(), passwd);
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
     * @param groupName  Name of the project group
     * @param localDir   Where to put the checked out files
     * @param frame      The current Frame object
     * @param openPkg    Do we want to open the checked out package
     * @returns void
     */
    public void doCheckOut(String passWord, String userName,
                           String module, String groupName, String localDir,
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
        this.info.setGroupName(groupName);
        this.localDirChooser.setSelectedFile(new File(localDir));
        this.performCheckout();
    }
}


