package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 ** @version $Id: NewClassDialog.java 281 1999-11-18 03:58:18Z axel $
 ** @author Justin Tan
 ** @author Michael Kolling
 **
 ** Dialog for creating a new class
 **/

class NewClassDialog extends JDialog

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

		JPanel mainPanel = new JPanel();
		{
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			mainPanel.setBorder(Config.generalBorder);

			JLabel newclassTag = new JLabel(newClassLabel);
			{
				newclassTag.setAlignmentX(LEFT_ALIGNMENT);
			}

			textFld = new JTextField(24);
			{
				textFld.setAlignmentX(LEFT_ALIGNMENT);
			}

			mainPanel.add(newclassTag);
			mainPanel.add(textFld);
			mainPanel.add(Box.createVerticalStrut(5));

			JPanel choicePanel = new JPanel();
			{
				choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
				choicePanel.setAlignmentX(LEFT_ALIGNMENT);
	
				//create compound border empty border outside of a titled border
				choicePanel.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(classTypeStr),
						BorderFactory.createEmptyBorder(0, 10, 0, 10)));

				typeNormal = new JRadioButton(newClassStr, true);
				typeAbstract = new JRadioButton(newAbstractClassStr, false);
				typeInterface = new JRadioButton(newInterfaceStr, false);
				typeApplet = new JRadioButton(newAppletStr, false);

				ButtonGroup bGroup = new ButtonGroup();
				{
					bGroup.add(typeNormal);
					bGroup.add(typeAbstract);
					bGroup.add(typeInterface);
					bGroup.add(typeApplet);	
				}

				choicePanel.add(typeNormal);
				choicePanel.add(typeAbstract);
				choicePanel.add(typeInterface);
				choicePanel.add(typeApplet);
			}

			choicePanel.setMaximumSize(new Dimension(textFld.getMaximumSize().width,
						choicePanel.getMaximumSize().height));
			choicePanel.setPreferredSize(new Dimension(textFld.getPreferredSize().width,
						choicePanel.getPreferredSize().height));

			mainPanel.add(choicePanel);
			mainPanel.add(Box.createVerticalStrut(5));

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			{
				buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

				//buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

				JButton okButton = new JButton(okay);
				{
					okButton.addActionListener(this);
				}

				JButton cancelButton = new JButton(cancel);
				{
					cancelButton.addActionListener(this);
				}

				buttonPanel.add(okButton);
				buttonPanel.add(cancelButton);

				getRootPane().setDefaultButton(okButton);

				// try to make the OK and cancel buttons have equal width
				okButton.setPreferredSize(new Dimension(cancelButton.getPreferredSize().width,
						okButton.getPreferredSize().height));
			}

			mainPanel.add(buttonPanel);
		}

		getContentPane().add(mainPanel);
		pack();
		
		DialogManager.centreDialog(this);
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
	    DialogManager.showError((JFrame)this.getParent(), "invalid-name");
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
