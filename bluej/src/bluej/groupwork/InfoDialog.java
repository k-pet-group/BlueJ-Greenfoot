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
 ** @version $Id: InfoDialog.java 604 2000-06-29 06:41:26Z markus $
 ** @author Markus Ostman, some code copied from jCVS
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
	super(parent, InfoDialogTitle, false);
	this.parent = parent;
        
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E) {
		ok = false;
		setVisible(false);
	    }
	});
	
        JPanel mainPanel =  new JPanel();
        mainPanel.setLayout(new BorderLayout(5, 5));
        mainPanel.setBorder(Config.generalBorder);

	//Text area showing information
	this.infoText = new JTextArea();
        this.infoText.setEditable(false);
        //this.infoText.setRows(10);
        //this.infoText.setPreferredSize(new Dimension(400, 200));
        //this.infoText.setFont(new Font("helvetica", Font.BOLD, 14));
        //this.infoText.setBackground(new Color(192,192,192));
        //this.infoText.setBorder(BorderFactory.createLineBorder
        //                        (Color.black, 2));
	this.scrollPane = new JScrollPane( this.infoText );
        //this.scrollPane.setPreferredSize(new Dimension(400, 200));
	mainPanel.add(scrollPane, BorderLayout.CENTER);
        
	// button panel at bottom of dialog
	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	//buttonPanel.setBorder(BorderFactory.createEmptyBorder
        //                    (10, 10, 10, 10));
	JButton button;
	buttonPanel.add(button = new JButton(close));
	button.addActionListener(this);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH); 
	getContentPane().add(mainPanel);
        
    }

    /**
     * Show this dialog and return false if
     * closed.
     */
    public void display(String title)
    {
	ok = false;
        if(title.equals(Config.getString("groupwork.log.title"))){
            this.infoText.setColumns(40);
            this.infoText.setRows(15);
        }
        if(title.equals(Config.getString("groupwork.status.title"))){
            this.infoText.setColumns(18);
            this.infoText.setRows(10);
        }
        setTitle(title);
        //Set the size to be the same as the parent's
        //setSize(this.parent.getSize());
        //To Centre the dialog over the current Bluej Window
        pack();
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


//     /**
//      * Close action when checkout is pressed.
//      */
//     public void doOK()
//     {
// 	if(!true) {
// 	    DialogManager.showError(parent, 
// 				    "This error message must be specified");
// 	}
// 	else { // collect information from fields
// 	    ok = true;
// 	    setVisible(false);
// 	}
//     }

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

                         


