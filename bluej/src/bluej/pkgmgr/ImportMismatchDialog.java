package bluej.pkgmgr;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.util.List;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for showing the user a list of files which
 * had mismatched package lines on an open non-BlueJ.
 *
 * @author  Andrew Patterson
 * @version $Id: ImportMismatchDialog.java 1088 2002-01-12 13:31:47Z ajp $
 */
public class ImportMismatchDialog extends JDialog
    implements ActionListener
{
    private static final String cont = Config.getString("continue");
    private static final String cancel = Config.getString("cancel");

    private static final String dialogTitle = Config.getString("pkgmgr.importmismatch.title");
    private static final String helpLine1 = Config.getString("pkgmgr.importmismatch.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.importmismatch.helpLine2");
    private static final String helpLine3 = Config.getString("pkgmgr.importmismatch.helpLine3");

    private boolean result = false;

    public ImportMismatchDialog(JFrame parent, List files,
                                 List packageNamesOriginal, List packageNamesChanged)
    {
        super(parent, dialogTitle, true);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(Config.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            helpText1.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText1);

            JLabel helpText2 = new JLabel(helpLine2);
            helpText2.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText2);

            JLabel helpText3 = new JLabel(helpLine3);
            helpText3.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText3);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);
            helpText3.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            JList failedList = new JList(new Vector(files));
            {
                failedList.setAlignmentX(LEFT_ALIGNMENT);
            }

            JScrollPane scrolly = new JScrollPane(failedList);

            mainPanel.add(scrolly);
            mainPanel.add(Box.createVerticalStrut(Config.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton contButton = new JButton(cont);
                {
                    contButton.addActionListener(this);
                }

                JButton cancelButton = new JButton(cancel);
                {
                    cancelButton.addActionListener(this);
                }

                buttonPanel.add(contButton);
                buttonPanel.add(cancelButton);

                getRootPane().setDefaultButton(cancelButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    public boolean getResult()
    {
        return result;
    }

    public void actionPerformed(ActionEvent evt)
    {
        dispose();
    }
}
