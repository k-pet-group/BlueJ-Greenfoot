package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

/**
 * Dialog for creating a new class
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 * @version $Id: NewClassDialog.java 1819 2003-04-10 13:47:50Z fisker $
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
        String templateSuffix = ".tmpl";
        int suffixLength = templateSuffix.length();

        // first, get templates out of defined templates from bluej.defs
        // (we do this rather than usign the directory only to be able to
        // force an order on the templates.)

        String templateString = Config.getPropString("bluej.classTemplates");

        StringTokenizer t = new StringTokenizer(templateString);
        List templates = new ArrayList();

        while (t.hasMoreTokens())
            templates.add(t.nextToken());

        // next, get templates from files in template directory and
        // merge them in

        File templateDir = Config.getClassTemplateDir();
        if(!templateDir.exists()) {
            DialogManager.showError(this, "error-no-templates");
        }
        else {
            String[] files = templateDir.list();
            
            for(int i=0; i < files.length; i++) {
                if(files[i].endsWith(templateSuffix)) {
                    String template = files[i].substring(0, files[i].length() - suffixLength);
                    if(!templates.contains(template))
                        templates.add(template);
                }
            }
        }

        // create a radio button for each template found

        JRadioButton button;
        JRadioButton previousButton = null;
        templateButtons = new ButtonGroup();

        for(Iterator i=templates.iterator(); i.hasNext(); ) {
            String template = (String)i.next();
            String label = Config.getString("pkgmgr.newClass." + template, template);
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
