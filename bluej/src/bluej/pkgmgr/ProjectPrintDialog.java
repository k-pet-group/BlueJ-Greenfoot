package bluej.pkgmgr;

import bluej.Config;

import bluej.utility.DialogManager;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * Dialog for creating a new Package
 * 
 * @version $Id: ProjectPrintDialog.java 1819 2003-04-10 13:47:50Z fisker $
 * @author Bruce Quig
 */
public class ProjectPrintDialog extends JDialog implements ActionListener
{
    // Internationalisation
    static final String okay = Config.getString("okay");
    static final String cancel = Config.getString("cancel");
    static final String projectPrintTitle = Config.getString(
                                                    "pkgmgr.printDialog.title");
    static final String printDiagramLabel = Config.getString(
                                                    "pkgmgr.printDialog.printDiagram");
    static final String printSourceLabel = Config.getString(
                                                   "pkgmgr.printDialog.printSource");
    static final String printReadmeLabel = Config.getString(
                                                   "pkgmgr.printDialog.printReadme");
    private boolean ok; // result: which button?
    private JCheckBox printDiagram;
    private JCheckBox printSource;
    private JCheckBox printReadme;

    /**
     * Creates a new ProjectPrintDialog object.
     * 
     * @param parent the frame that called the print dialog
     */
    public ProjectPrintDialog(PkgMgrFrame parent)
    {
        super(parent, projectPrintTitle, true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                ok = false;
                setVisible(false);
            }
        });

        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(Config.dialogBorder);
        mainPanel.add(Box.createVerticalStrut(
                              Config.dialogCommandButtonsVertical));

        printDiagram = new JCheckBox(printDiagramLabel);
        printDiagram.setSelected(true);
        mainPanel.add(printDiagram);
                
        printSource = new JCheckBox(printSourceLabel);
        mainPanel.add(printSource);
                
        if(((parent.getPackage()).getParent() == null)) {
            printReadme = new JCheckBox(printReadmeLabel);
            mainPanel.add(printReadme);
        }
        mainPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

        JButton okButton = new JButton(okay);
        okButton.addActionListener(this);

        JButton cancelButton = new JButton(cancel);
        cancelButton.addActionListener(this);

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        getRootPane().setDefaultButton(okButton);

        // try to make the OK and cancel buttons have equal width
        okButton.setPreferredSize(
                new Dimension(cancelButton.getPreferredSize().width, 
                              okButton.getPreferredSize().height));

        mainPanel.add(buttonPanel);

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     * 
     * @return the status of the print job, proceed if true, cancel if false
     */
    public boolean display()
    {
        ok = false;
        setVisible(true);

        return ok;
    }

    /**
     * ActionListener for buttons
     * 
     * @param evt button event (Cancel or OK)
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();

        if (okay.equals(cmd)) {
            doOK();
        } else if (cancel.equals(cmd)) {
            doCancel();
        }
    }

    /**
     * Close action called when OK button is pressed.  It only sets ok boolean
     * flag to true as long as one of the check boxes is selected
     */
    public void doOK()
    {
        ok = (printDiagram() || printSource() || printReadme());
        setVisible(false);
    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
        ok = false;
        setVisible(false);
    }

    /**
     * Print class diagram selection status
     * 
     * @return true if radio button is selected meaning  diagram should be
     *         printed
     */
    public boolean printDiagram()
    {
        return printDiagram.isSelected();
    }

    /**
     * Print all source code selection status
     * 
     * @return true if radio button is selected meaning  source code should be
     *         printed
     */
    public boolean printSource()
    {
        return printSource.isSelected();
    }

    /**
     * Print project's readme selection status
     * 
     * @return true if radio button is selected meaning  readme should be
     *         printed
     */
    public boolean printReadme()
    {
        return (printReadme != null && printReadme.isSelected());
    }
}