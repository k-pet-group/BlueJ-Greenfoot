package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.StringTokenizer;

/**
 * Dialog for creating a new class
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 * @version $Id: NewClassDialog.java 860 2001-04-23 02:07:10Z mik $
 */
class NewClassDialog extends JDialog
    implements ActionListener
{
    // Internationalisation
    static final String okay = Config.getString("okay");
    static final String cancel = Config.getString("cancel");
    static final String newClassTitle = Config.getString("pkgmgr.newClass.title");
    static final String newClassLabel = Config.getString("pkgmgr.newClass.label");
    static final String classTypeStr = Config.getString("pkgmgr.newClass.classType");

    private JTextField textFld;
    ButtonGroup templateButtons;

    private String newClassName = "";
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
			mainPanel.setBorder(Config.dialogBorder);

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

                addClassTypeButtons(choicePanel);
			}

			choicePanel.setMaximumSize(new Dimension(textFld.getMaximumSize().width,
                                                     choicePanel.getMaximumSize().height));
			choicePanel.setPreferredSize(new Dimension(textFld.getPreferredSize().width,
                                                       choicePanel.getPreferredSize().height));

			mainPanel.add(choicePanel);
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
     * Add the class type buttons (defining the class template to be used
     * to the panel. The templates are defined in the "defs" file.
     */
    private void addClassTypeButtons(JPanel panel)
    {
        String templates = Config.getPropString("bluej.classTemplates");

        JRadioButton button;
        JRadioButton previousButton = null;
        templateButtons = new ButtonGroup();

        StringTokenizer t = new StringTokenizer(templates);

        while (t.hasMoreTokens()) {
            String template = t.nextToken();
            String label = Config.getString("pkgmgr.newClass." + template);
            button = new JRadioButton(label, (previousButton==null));  // enable first
            button.setActionCommand(template);
            templateButtons.add(button);
            panel.add(button);
            if(previousButton != null)
                previousButton.setNextFocusableComponent(button);
            previousButton = button;
        }
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        textFld.requestFocus();
        setVisible(true);  // modal - we sit here until closed
        return ok;
    }

    public String getClassName()
    {
        return newClassName;
    }

    public String getTemplateName()
    {
        return templateButtons.getSelection().getActionCommand();
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

        if (JavaNames.isIdentifier(newClassName)) {
            ok = true;
            setVisible(false);
        }
        else {
            DialogManager.showError((JFrame)this.getParent(), "invalid-class-name");
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
