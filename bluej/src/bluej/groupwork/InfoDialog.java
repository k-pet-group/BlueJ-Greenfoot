package bluej.groupwork;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.ice.jcvsii.*;
import com.ice.cvsc.*;

/**
 ** @version $Id: InfoDialog.java 401 2000-02-29 01:42:12Z markus $
 ** @author Markus Ostman
 **
 ** Dialog for Group work information purposes
 **/

public class InfoDialog extends JDialog

    implements ActionListener, CVSUserInterface
{
    // Internationalisation
    static final String close = Config.getString("close");
    static final String InfoDialogTitle = "Info Dialog";
   
    private JFrame parent;
    private boolean ok;		// result: which button?
    private JTextArea infoText;
    private JScrollPane	scrollPane;

    public InfoDialog(JFrame parent)
    {
	super(parent, InfoDialogTitle, true);
	this.parent = parent;

	//Initialize jCVS components
	//This is necessary because of the design of the jCVS code
	//JcvsInit.doInitialize();

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		ok = false;
		setVisible(false);
	    }
	});
	
	//Text area showing information
	this.infoText = new JTextArea();
	this.scrollPane = new JScrollPane( this.infoText );
	getContentPane().add("Center", scrollPane);

	// button panel at bottom of dialog
	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	JButton button;
	buttonPanel.add(button = new JButton(close));
	button.addActionListener(this);
	getContentPane().add("South", buttonPanel); 		
    }

    /**
     * Show this dialog and return false if
     * closed.
     */
    public void display()
    {
	ok = false;
	pack();
        //Set the size to be the same as the parent's
        setSize(this.parent.getSize());
        //To Centre the dialog over the current Bluej Window
	DialogManager.centreDialog(this);
	setVisible(true);
    }
    
    /**
     * return parent to this dialog
     */
    public JFrame getParentFrame()
    {
	return this.parent;
    }

    public void actionPerformed(ActionEvent evt)
    {
	String cmd = evt.getActionCommand();

	if(close.equals(cmd))
	    doClose();
    }


    /**
     * Close action when checkout is pressed.
     */
    public void doOK()
    {
	if(!true) {
	    DialogManager.showError(parent, 
				    "This error message must be specified");
	}
	else { // collect information from fields
	    ok = true;
	    setVisible(false);
	}
    }

    public void setText(String info)
    {
	this.infoText.setText( info );
    }

    /**
     * Close action when Close is pressed.
     */
    public void doClose ()
    {
	ok = false;
	setVisible(false);
    }

    //
    // CVS USER INTERFACE METHODS
    //
    
    public void uiDisplayProgressMsg( String message )
    {
	Debug.message("InfoDialog, line126: "+message);
    }
    
    public void uiDisplayProgramError( String error )
    {
    }
    
    public void uiDisplayResponse( CVSResponse response )
    {
    }
    
    //
    // END OF CVS USER INTERFACE METHODS
    //
}

                         


