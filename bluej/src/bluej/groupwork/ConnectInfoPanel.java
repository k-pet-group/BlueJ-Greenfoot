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

import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSRequest;
import com.ice.pref.UserPrefs;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

import bluej.utility.Debug;

/**
 ** @version $Id: ConnectInfoPanel.java 504 2000-05-24 04:44:14Z markus $
 ** @author Modifications to jCVS ConnectInfoPanel.java by Markus Ostman
 **
 ** Connect information panel for bluej group support.
 **/
public class ConnectInfoPanel extends JPanel
    implements ItemListener, ActionListener
{
    private JTextField		moduleText;
    private JTextField	        groupNameText;
    private JTextField		exportDirText;
    
    private JLabel		userNameLbl;
    private JTextField		userNameText;
    private JLabel		passwordLbl;
    private JPasswordField	passwordText;
    
    
    public ConnectInfoPanel( String operation )
    {
	super();
	this.establishContents( operation );
    }
    
    public void loadPreferences( String panName )
    {
	UserPrefs prefs = Config.getPreferences();
	
	//./temp this should be handled differently
	this.setUserName(prefs.getProperty(panName + "." + 
					   Config.INFOPAN_USER_NAME, ""));
    }
    
    public void savePreferences( String panName )
    {
        UserPrefs prefs = Config.getPreferences();
        
        prefs.setProperty(panName + "." + Config.INFOPAN_USER_NAME,
                          this.getUserName());
        prefs.setProperty(panName + "." + Config.INFOPAN_MODULE_NAME,
                          this.getModule());
    }
    
    public String getUserName()
    {
	return this.userNameText.getText();
    }
    
    public void setUserName( String name )
    {
	this.userNameText.setText( name );
    }

    public String getGroupName()
    {
	return this.groupNameText.getText();
    }
    
    public void setGroupName( String name )
    {
	this.groupNameText.setText( name );
    }

    public String getPassword()
    {
	return new String( this.passwordText.getPassword() );
    }

    public void setPassword(String password)
    {
	this.passwordText.setText(password);
    }

    public String getModule()
    {
	return ( this.moduleText == null
		 ? "" : this.moduleText.getText() );
    }
    
    public void setModule( String name )
    {
	if ( this.moduleText != null )
	    this.moduleText.setText( name );
    }
    
    public String getImportDirectory()
    {
 	return ( this.exportDirText == null
 		 ? "" : this.exportDirText.getText() );
    }
    
    public void setImportDirectory( String dir )
    {
       	if ( exportDirText != null )
 	    this.exportDirText.setText( dir );
    }
    
    public void requestInitialFocus()
    {
	this.userNameText.requestFocus();
    }

    public void actionPerformed( ActionEvent evt )
    {
	if ( evt.getSource() == this.userNameText )
	    {
		this.passwordText.requestFocus();
	    }
    }

    //./temp this method is not necessary
    public void itemStateChanged( ItemEvent event )
    {
 	boolean relay = false;
 	Object item = event.getItemSelectable();	
    }
    
    private void establishContents( String operation )
    {
	JLabel lbl= this.new MyLabel("");
	int row = 0;
	
	this.setLayout( new GridBagLayout() );
	
	ResourceMgr rmgr = ResourceMgr.getInstance();
	
	// ============== INPUT FIELDS PANEL ================
	
	JPanel fldPan = new JPanel();
	fldPan.setLayout( new GridBagLayout() );
	
	// ------------------- Module -------------------
	if ( ! operation.equals( "test" ) ){
	    lbl = this.new MyLabel(bluej.Config.getString
                                   ("groupwork.module.namelbl")); 
            lbl.setForeground( Color.black );
	    AWTUtilities.constrain(fldPan, lbl,
				   GridBagConstraints.NONE,
				   GridBagConstraints.WEST,
				   0, row++, 1, 1, 0.0, 0.0 );

	    this.moduleText = new JTextField();
	    AWTUtilities.constrain(fldPan, this.moduleText,
				   GridBagConstraints.HORIZONTAL,
				   GridBagConstraints.CENTER,
				   0, row++, 1, 1, 1.0, 0.0 );
            
	}
		
	// ============== USER LOGIN INFO PANEL ================
	
	JPanel namePan = new JPanel();
	namePan.setLayout( new GridBagLayout() );
	
	row = 0;	

        lbl = this.new MyLabel("Group name"); 
        lbl.setForeground( Color.black );
        AWTUtilities.constrain(namePan, lbl,
                               GridBagConstraints.NONE,
                               GridBagConstraints.WEST,
                               0, row, 1, 1, 0.0, 0.0 );
        
        this.groupNameText = new JTextField();
        AWTUtilities.constrain(namePan, this.groupNameText,
                               GridBagConstraints.HORIZONTAL,
                               GridBagConstraints.CENTER,
                               1, row++, 1, 1, 1.0, 0.0,
                               new Insets( 5, 0, 5, 0 ));
	
	this.userNameLbl = this.new MyLabel(rmgr.getUIString("name.for.user.name"));
	this.userNameLbl.setForeground( Color.black );
	AWTUtilities.constrain(
			       namePan, this.userNameLbl,
			       GridBagConstraints.NONE,
			       GridBagConstraints.WEST,
			       0, row, 1, 1, 0.0, 0.0 );
	
	this.userNameText = new JTextField();
	this.userNameText.addActionListener( this );
	AWTUtilities.constrain(
			       namePan, this.userNameText,
			       GridBagConstraints.HORIZONTAL,
			       GridBagConstraints.WEST,
			       1, row++, 1, 1, 1.0, 0.0,
                               new Insets( 5, 0, 5, 0 ));
	
        this.passwordLbl = this.new MyLabel(rmgr.getUIString("name.for.user.pass"));
	this.passwordLbl.setForeground( Color.black );
	AWTUtilities.constrain(
			       namePan, this.passwordLbl,
			       GridBagConstraints.NONE,
			       GridBagConstraints.WEST,
			       0, row, 1, 1, 0.0, 0.0 );

	this.passwordText = new JPasswordField();
	this.passwordText.setEchoChar( '*' );
	AWTUtilities.constrain(
			       namePan, this.passwordText,
			       GridBagConstraints.HORIZONTAL,
			       GridBagConstraints.WEST,
			       1, row++, 1, 1, 1.0, 0.0,
                               new Insets( 5, 0, 5, 0 ));
	row = 0;
	
	AWTUtilities.constrain(
			       this, fldPan,
			       GridBagConstraints.HORIZONTAL,
			       GridBagConstraints.CENTER,
			       0, row++, 1, 1, 1.0, 0.0,
                               new Insets( 5, 3, 5, 3 ));
	
	JSeparator sep = new JSeparator( SwingConstants.HORIZONTAL );
	
	AWTUtilities.constrain(
			       this, sep,
			       GridBagConstraints.HORIZONTAL,
			       GridBagConstraints.CENTER,
			       0, row++, 1, 1, 0.0, 0.0,
			       new Insets( 5, 0, 5, 0 ) );
	
	AWTUtilities.constrain(
			       this, namePan,
			       GridBagConstraints.HORIZONTAL,
			       GridBagConstraints.CENTER,
			       0, row, 1, 1, 1.0, 0.0 );
    }
    
    private class MyLabel extends JLabel
    {
	public MyLabel( String text )
	{
	    super( text );
	    this.setBorder( new EmptyBorder( 0, 3, 0, 5 ) );
	}
    }
    
}












