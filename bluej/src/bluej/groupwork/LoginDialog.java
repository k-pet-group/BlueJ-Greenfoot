
package bluej.groupwork;

import bluej.utility.Debug;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;

import javax.swing.*;
import javax.swing.border.*;

import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

public class LoginDialog extends JDialog
implements ActionListener
{
    private String		userName;
    private String		password;
    private String              groupName;
    private JTextField		groupNameField;
    private JTextField		userNameField;
    private JPasswordField	passwordField;
    private boolean             cancel;

    public
    LoginDialog( Frame parent, String userName )
    {
        super( parent, "Login Information", true );

        this.userName = userName;
        this.cancel = false;

        this.establishDialogContents( userName );

        this.pack();

        this.addWindowListener(
                               new WindowAdapter()
                               {
                                   public void
                                       windowActivated(WindowEvent e)
                                       {
                                           groupNameField.requestFocus();
                                       }
                               }
                               );
    }
    
    public String getUserName()
    {
        return this.userName;
    }

    public String getgroupName()
    {
        return this.groupName;
    }
    
    public String getPassword()
    {
        return this.password;
    }

    public boolean getCancel()
    {
        return this.cancel;
    }

    public void actionPerformed( ActionEvent event )
    {
        boolean doDispose = true;

        String command = event.getActionCommand();

        if ( event.getSource() == this.passwordField )
            {
                this.userName = this.userNameField.getText();
                this.groupName = this.groupNameField.getText();
                this.password =
                    new String( this.passwordField.getPassword() );
            }
        else if ( event.getSource() == this.userNameField )
            {
                this.passwordField.requestFocus();
                this.passwordField.selectAll();
                doDispose = false;
            }
        else if ( command.compareTo( "OK" ) == 0 )
            {
                this.userName = this.userNameField.getText();
                this.groupName = this.groupNameField.getText();
                this.password = new String(this.passwordField.getPassword());
            }
        else if ( command.compareTo( "CANCEL" ) == 0 )
            {
                this.userName = null;
                this.groupName = null;
                this.password = null;
                this.cancel = true;
                Debug.message("logdlg,line101: login canceled");
            }

        if ( doDispose )
            {
                this.dispose();
            }
    }

    public void
    establishDialogContents( String userName ) 
    {
        JLabel		label;
        JButton		button;

        UserPrefs prefs = Config.getPreferences();
        ResourceMgr rmgr = ResourceMgr.getInstance();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout( new GridBagLayout() );
        mainPanel.setBorder
            ( new CompoundBorder
              ( new EtchedBorder( EtchedBorder.LOWERED ),
                new EmptyBorder( 3, 3, 3, 3 ) ) );

        Font lblFont =
            prefs.getFont
            ( "passwordDialog.label.font",
              new Font( "Dialog", Font.BOLD, 14 ) );

        label = new JLabel("Group Name" );
        label.setFont( lblFont );
        AWTUtilities.constrain(
                               mainPanel, label,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               0, 0, 1, 1, 1.0, 1.0,
                               new Insets( 1, 3, 1, 5 ) );

        this.groupNameField = new JTextField( 16 );
        this.groupNameField.setEditable( true );
        if ( groupName != null )
            this.groupNameField.setText( groupName );
        this.groupNameField.addActionListener( this );
        AWTUtilities.constrain(
                               mainPanel, this.groupNameField,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               1, 0, 1, 1, 1.0, 1.0,
                               new Insets( 10, 1, 5, 1 ) );

        
        label = new JLabel( rmgr.getUIString( "name.for.user.name" ) );
        label.setFont( lblFont );
        AWTUtilities.constrain(
                               mainPanel, label,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               0, 1, 1, 1, 1.0, 1.0,
                               new Insets( 1, 3, 1, 5 ) );

        this.userNameField = new JTextField( 16 );
        this.userNameField.setEditable( true );
        if ( userName != null )
            this.userNameField.setText( userName );
        this.userNameField.addActionListener( this );
        AWTUtilities.constrain(
                               mainPanel, this.userNameField,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               1, 1, 1, 1, 1.0, 1.0,
                               new Insets( 10, 1, 5, 1 ) );

        label = new JLabel( rmgr.getUIString( "name.for.user.pass" ) );
        label.setFont( lblFont );
        AWTUtilities.constrain(
                               mainPanel, label,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               0, 2, 1, 1, 1.0, 1.0,
                               new Insets( 1, 3, 1, 5 ) );

        this.passwordField = new JPasswordField( 16 );
        this.passwordField.setEditable( true );
        this.passwordField.setEchoChar( '*' );
        this.passwordField.addActionListener( this );
        AWTUtilities.constrain(
                               mainPanel, this.passwordField,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               1, 2, 1, 1, 1.0, 1.0,
                               new Insets( 5, 1, 10, 1 ) );

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout( new GridLayout( 1, 2, 5, 5 ) );

        button = new JButton( rmgr.getUIString( "name.for.ok" ) );
        button.addActionListener( this );
        button.setActionCommand( "OK" );
        controlPanel.add( button );
        this.getRootPane().setDefaultButton(button);

        button = new JButton( rmgr.getUIString( "name.for.cancel" ) );
        button.addActionListener( this );
        button.setActionCommand( "CANCEL" );
        controlPanel.add( button );
        

        JPanel southPan = new JPanel();
        southPan.setLayout( new BorderLayout() );
        southPan.add( BorderLayout.EAST, controlPanel );
        southPan.setBorder( new EmptyBorder( 12, 0, 2, 0 ) );

        Container content = this.getContentPane();
        content.setLayout( new BorderLayout() );

        JPanel contPan = new JPanel();
        contPan.setLayout( new BorderLayout( 2, 2 ) );
        contPan.setBorder( new EmptyBorder( 3, 5, 5, 5 ) );
        content.add( BorderLayout.CENTER, contPan );

        contPan.add( BorderLayout.CENTER, mainPanel );
        contPan.add( BorderLayout.SOUTH, southPan );
    }

}
