package bluej.groupwork;

import java.awt.*;
import java.awt.event.*; 
import javax.swing.*;
import javax.swing.border.*;

import com.ice.cvsc.CVSProject;
import com.ice.util.AWTUtilities;
import com.ice.jcvsii.*;

public class MainGrpPanel extends JPanel
    implements ActionListener
{
    GroupWorkDialog groupWorkDialog;
    CheckOutPanel coPanel;
    ImportPanel imPanel;
    //JTabbedPane tabbedPane;

    public MainGrpPanel( GroupWorkDialog groupWorkDialog )
    {
	super();
	
	this.groupWorkDialog = groupWorkDialog;
	
	this.setLayout( new BorderLayout() );

	// Create a tab pane
	//this.tabbedPane = new JTabbedPane();
	//this.add( tabbedPane );

	ResourceMgr rmgr = ResourceMgr.getInstance();
	
	//Creating the check out panel
	//String tabName = rmgr.getUIString( "mainpan.checkout.tab.name" );
	//this.coPanel = new CheckOutPanel( this );
	//this.tabbedPane.addTab( tabName, null, this.coPanel );

	//Creating the Import Panel
	//tabName = rmgr.getUIString( "mainpan.import.tab.name" );
	//this.imPanel = new ImportPanel( this );
	//this.tabbedPane.addTab( tabName, null, this.imPanel );

	//this.tabbedPane.setSelectedIndex( 0 );
      
    }

    public GroupWorkDialog getGroupWorkDialog()
    {
	return this.groupWorkDialog;
    }

    public void loadPreferences()
    {
	this.coPanel.loadPreferences();
	this.imPanel.loadPreferences();
    }

    public void savePreferences()
    {
	this.coPanel.savePreferences();
	this.imPanel.savePreferences();
	
    }

    public void setAllTabsEnabled( boolean enabled )
    {
	//  for ( int i = 0, cnt = this.tabbedPane.getTabCount()
//  		  ; i < cnt ; ++i )
//  	    {
//  		this.tabbedPane.setEnabledAt( i, enabled );
//  	    }
    }

    public void displayCheckout()
    {
	//Creating the check out panel
	//String tabName = rmgr.getUIString( "mainpan.checkout.tab.name" );
	this.coPanel = new CheckOutPanel( this );
	this.coPanel.loadPreferences();
	this.add( this.coPanel );
    }

    public void displayImport()
    {
	//Creating the Import Panel
	//tabName = rmgr.getUIString( "mainpan.import.tab.name" );
	this.imPanel = new ImportPanel( this );
	this.imPanel.loadPreferences();
	this.add( this.imPanel );
    }


    public void actionPerformed( ActionEvent event )
    {
	String command = event.getActionCommand();
	
	System.err.println( "UNKNOWN Command '" + command + "'" );
    }
    
}
