package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for creating a new Package
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 * @version $Id: NewPackageDialog.java 1524 2002-11-28 02:37:34Z bquig $
 */
class NewPackageDialog extends JDialog
    implements ActionListener
{
    // Internationalisation
    static final String okay = Config.getString("okay");
    static final String cancel = Config.getString("cancel");
    static final String newPackageTitle = Config.getString("pkgmgr.newPackage.title");
    static final String newPackageLabel = Config.getString("pkgmgr.newPackage.label");
    static final String classTypeStr = Config.getString("pkgmgr.newPackage.classType");

    private String newPackageName = "";

    private JTextField textFld;

    private boolean ok;		// result: which button?

	public NewPackageDialog(JFrame parent)
	{
		super(parent, newPackageTitle, true);

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
			mainPanel.setBorder(Config.dialogBorder);

			JLabel newclassTag = new JLabel(newPackageLabel);
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

			mainPanel.add(Box.createVerticalStrut(Config.dialogCommandButtonsVertical));

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			{
				buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

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

    public String getPackageName()
    {
        return newPackageName;
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
        newPackageName = textFld.getText().trim();

        if (JavaNames.isQualifiedIdentifier(newPackageName)) {
            ok = true;
            setVisible(false);
        }
        else {
            DialogManager.showError((JFrame)this.getParent(), "invalid-package-name");
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
