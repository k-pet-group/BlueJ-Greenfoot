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
    //ImportPanel imPanel;

    public MainGrpPanel( GroupWorkDialog groupWorkDialog )
    {
	super();
	
	this.groupWorkDialog = groupWorkDialog;
	
	this.setLayout( new BorderLayout() );

	ResourceMgr rmgr = ResourceMgr.getInstance();
    }

    public GroupWorkDialog getGroupWorkDialog()
    {
	return this.groupWorkDialog;
    }

    public void loadPreferences()
    {
	this.coPanel.loadPreferences();
	//this.imPanel.loadPreferences();
    }

    public void savePreferences()
    {
	this.coPanel.savePreferences();
        //	this.imPanel.savePreferences();
	
    }

    public void setAllTabsEnabled( boolean enabled )
    {

    }

    public void displayCheckout()
    {
	//Creating the check out panel
	this.coPanel = new CheckOutPanel( this );
	this.coPanel.loadPreferences();
	this.add( this.coPanel );
    }

//     public void displayImport()
//     {
// 	//Creating the Import Panel
// 	this.imPanel = new ImportPanel( this );
// 	this.imPanel.loadPreferences();
// 	this.add( this.imPanel );
//     }
    
    public CheckOutPanel getCheckOutPanel()
    {
        this.coPanel = new CheckOutPanel( this );
	this.coPanel.loadPreferences();
        return this.coPanel;
    }

    public void actionPerformed( ActionEvent event )
    {
	String command = event.getActionCommand();
	
	System.err.println( "UNKNOWN Command '" + command + "'" );
    }
    
}
