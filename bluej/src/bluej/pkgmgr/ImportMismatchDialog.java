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
 * @version $Id: ImportMismatchDialog.java 1098 2002-01-16 04:53:33Z ajp $
 */
public class ImportMismatchDialog extends JDialog
    implements ActionListener
{
    private static final String cont = Config.getString("continue");
    private static final String cancel = Config.getString("cancel");

    private static final String dialogTitle = Config.getString("pkgmgr.importmismatch.title");

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

            JLabel helpText1 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine1"));
            helpText1.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText1);
            JLabel helpText2 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine2"));
            helpText2.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText2);
            JLabel helpText3 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine3"));
            helpText3.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText3);
            JLabel helpText4 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine4"));
            helpText4.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText4);
            JLabel helpText5 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine5"));
            helpText5.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText5);
            JLabel helpText6 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine6"));
            helpText6.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText6);
            JLabel helpText7 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine7"));
            helpText7.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText7);
            JLabel helpText8 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine8"));
            helpText8.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText8);
            JLabel helpText9 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine9"));
            helpText9.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText9);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);
            helpText3.setFont(smallFont);
            helpText4.setFont(smallFont);
            helpText5.setFont(smallFont);
            helpText6.setFont(smallFont);
            helpText7.setFont(smallFont);
            helpText8.setFont(smallFont);
            helpText9.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            JList failedList = new JList(new Vector(files));
            {
                failedList.setAlignmentX(LEFT_ALIGNMENT);
            }

            JScrollPane scrolly = new JScrollPane(failedList);
            scrolly.setAlignmentX(LEFT_ALIGNMENT);

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

                // try to make the continue and cancel buttons have equal width
                int biggest = Math.max(contButton.getPreferredSize().width,
                                        cancelButton.getPreferredSize().width);

                cancelButton.setPreferredSize(new Dimension(biggest,
                                                cancelButton.getPreferredSize().height));
                contButton.setPreferredSize(new Dimension(biggest,
                                             contButton.getPreferredSize().height));
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
        String cmd = evt.getActionCommand();

        if(cont.equals(cmd))
            result = true;
        else
            result = false;

        dispose();
    }
}
