package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: NewClassDialog.java 134 1999-06-21 02:34:23Z bruce $
 ** @author Justin Tan
 ** @author Michael Kolling
 **
 ** Dialog for creating a new class
 **/

public class NewClassDialog extends JDialog

	implements ActionListener
{
    static final int NC_DEFAULT = 0;
    static final int NC_ABSTRACT = 1;
    static final int NC_INTERFACE = 2;
    static final int NC_APPLET = 3;

    // Internationalisation
    static final String okay = Config.getString("okay");
    static final String cancel = Config.getString("cancel");
    static final String newClassTitle = Config.getString("pkgmgr.newClass.title");
    static final String newClassLabel = Config.getString("pkgmgr.newClass.label");
    static final String classTypeStr = Config.getString("pkgmgr.newClass.classType");
    static final String newClassStr = Config.getString("pkgmgr.newClass.newClass");
    static final String newAbstractClassStr = Config.getString("pkgmgr.newClass.newAbstractClass");
    static final String newInterfaceStr = Config.getString("pkgmgr.newClass.newInterface");
    static final String newAppletStr = Config.getString("pkgmgr.newClass.newApplet");
    static final String invalidNameStr = Config.getString("error.newClass.invalidName");


    private String newClassName = "";
    private int classType = NC_DEFAULT;

    private JRadioButton typeNormal;
    private JRadioButton typeAbstract;
    private JRadioButton typeInterface;
    private JRadioButton typeApplet;
    private JTextField textFld;

    private boolean ok;		// result: which button?

    public NewClassDialog(JFrame parent)
    {
	super(parent, newClassTitle, true);

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E)
		{
		    ok = false;
		    setVisible(false);
		}
	});
	JPanel mainPanel = (JPanel)getContentPane();  // has BorderLayout
	mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));		
	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout());
	JButton button;
	buttonPanel.add(button = new JButton(okay));
	button.addActionListener(this);
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	getRootPane().setDefaultButton(button);

	buttonPanel.add(button = new JButton(cancel));
	button.addActionListener(this);
	getContentPane().add("South", buttonPanel);

	JPanel compPanel = new JPanel();
	compPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	compPanel.setLayout(new GridLayout(0,1,20,2));

	compPanel.add(new JLabel(newClassLabel));
	compPanel.add(textFld = new JTextField(16));
	getContentPane().add("North", compPanel);

	compPanel = new JPanel();
	compPanel.setLayout(new BoxLayout(compPanel, BoxLayout.Y_AXIS));
	
	//compPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	//create compound border empty border outside of a titled border
	compPanel.setBorder(BorderFactory.createCompoundBorder(
			        BorderFactory.createTitledBorder(classTypeStr),
			        BorderFactory.createEmptyBorder(0, 20, 0, 20)));
	//compPanel.add(new JLabel(classTypeStr));
	ButtonGroup bGroup = new ButtonGroup();
	typeNormal = new JRadioButton(newClassStr, true);
	compPanel.add(typeNormal);
	bGroup.add(typeNormal);
	typeAbstract = new JRadioButton(newAbstractClassStr, false);
	compPanel.add(typeAbstract);
	bGroup.add(typeAbstract);
	typeInterface = new JRadioButton(newInterfaceStr, false);
	compPanel.add(typeInterface);
	bGroup.add(typeInterface);
	typeApplet = new JRadioButton(newAppletStr, false);
	compPanel.add(typeApplet);
	bGroup.add(typeApplet);	
	getContentPane().add("Center", compPanel);
		
	Utility.centreDialog(this);
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
	ok = false;
	textFld.requestFocus();
	setVisible(true);
	return ok;
    }

    public String getClassName()
    {
	return newClassName;
    }

    public int getClassType()
    {
	return classType;
    }

    public void actionPerformed(ActionEvent evt)
    {
	String cmd = evt.getActionCommand();
	if(okay.equals(cmd))
	    doOK();
	else if(cancel.equals(cmd))
	    doCancel();
    }

    /**
     * Close action when OK is pressed.
     */
    public void doOK()
    {
	newClassName = textFld.getText().trim();

	if (Utility.isIdentifier(newClassName)) {
	    if(typeAbstract.isSelected())
		classType = NC_ABSTRACT;
	    else if(typeInterface.isSelected())
		classType = NC_INTERFACE;
	    else if(typeApplet.isSelected())
		classType = NC_APPLET;
	    ok = true;
	    setVisible(false);
	}
	else {
	    Utility.showError((JFrame)this.getParent(), invalidNameStr);
	    textFld.selectAll();
	    textFld.requestFocus();
	}
    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
	ok = false;
	setVisible(false);
    }
}
