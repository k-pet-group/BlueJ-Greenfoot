package bluej.pkgmgr;

import bluej.*;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for showing the user a list of files which failed
 * an import.
 *
 * @author  Andrew Patterson
 * @version $Id: ImportFailedDialog.java 3175 2004-11-25 14:33:52Z fisker $
 */
public class ImportFailedDialog extends EscapeDialog
    implements ActionListener
{
    private static final String cont = Config.getString("continue");

    private static final String dialogTitle = Config.getString("pkgmgr.importfailed.title");
    private static final String helpLine1 = Config.getString("pkgmgr.importfailed.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.importfailed.helpLine2");
    private static final String helpLine3 = Config.getString("pkgmgr.importfailed.helpLine3");

    public ImportFailedDialog(JFrame parent, Object[] objects)
    {
        super(parent, dialogTitle, true);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

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

            JList failedList = new JList(objects);
            {
                failedList.setAlignmentX(LEFT_ALIGNMENT);
            }

            JScrollPane scrolly = new JScrollPane(failedList);
            scrolly.setAlignmentX(LEFT_ALIGNMENT);

            mainPanel.add(scrolly);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton contButton = new JButton(cont);
                {
                    contButton.addActionListener(this);
                }

                buttonPanel.add(contButton);

                getRootPane().setDefaultButton(contButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    public void actionPerformed(ActionEvent evt)
    {
        dispose();
    }
}
