package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.OkayCancelDialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: NewClassDialog.java 36 1999-04-27 04:04:54Z mik $
 ** @author Justin Tan
 ** @author Michael Kolling
 **
 ** Dialog for creating a new class
 **/

//PENDING: use swing JOptionPane as base class rather than self-made dialog

public class NewClassDialog extends OkayCancelDialog
{
    static final int NC_DEFAULT = 0;
    static final int NC_ABSTRACT = 1;
    static final int NC_INTERFACE = 2;

    // Internationalisation
    static final String newClassTitle = Config.getString("pkgmgr.newClass.title");
    static final String newClassLabel = Config.getString("pkgmgr.newClass.label");
    static final String classTypeStr = Config.getString("pkgmgr.newClass.classType");
    static final String newClassStr = Config.getString("pkgmgr.newClass.newClass");
    static final String newAbstractClassStr = Config.getString("pkgmgr.newClass.newAbstractClass");
    static final String newInterfaceStr = Config.getString("pkgmgr.newClass.newInterface");
    static final String invalidNameStr = Config.getString("error.newClass.invalidName");

    public String newClassName = "";
    public int classType = NC_DEFAULT;

    private JRadioButton defCB;
    private JRadioButton absCB;
    private JRadioButton impCB;
    private JTextField textFld;
	
    public NewClassDialog(JFrame thisFrame)
    {
	super(thisFrame, newClassTitle, true);

	JPanel compPanel = new JPanel();
	compPanel.setBorder(BorderFactory.createEmptyBorder(10,20,0,20));
	compPanel.setLayout(new GridLayout(0,1,20,2));

	compPanel.add(new JLabel(newClassLabel));
	compPanel.add(textFld = new JTextField(16));
	
	compPanel.add(new JLabel(classTypeStr));
	ButtonGroup bGroup = new ButtonGroup();
	compPanel.add(defCB = new JRadioButton(newClassStr, true));
	bGroup.add(defCB);
	compPanel.add(absCB = new JRadioButton(newAbstractClassStr, false));
	bGroup.add(absCB);
	compPanel.add(impCB = new JRadioButton(newInterfaceStr, false));
	bGroup.add(impCB);
		
	getContentPane().add("Center", compPanel);
		
	// Set some attributes for this DialogBox
	Utility.centreDialog(this);
	textFld.addActionListener(this);
	textFld.setActionCommand(Config.getString("okay"));
    }

    // Provide function to allow setting of focuses before showing
    public void beforeShow() 
    {
	textFld.selectAll();
	textFld.requestFocus();
    }

    // Close Action when OK is pressed
    public void doOK()
    {
	newClassName = textFld.getText().trim();

	if (Utility.isIdentifier(newClassName)) {
	    if(absCB.isSelected())
		classType = NC_ABSTRACT;
	    else if(impCB.isSelected())
		classType = NC_INTERFACE;
	    super.doOK();
	}
	else {
	    Utility.showError((JFrame)this.getParent(), invalidNameStr);
	    textFld.selectAll();
	    textFld.requestFocus();
	}
    }
}
