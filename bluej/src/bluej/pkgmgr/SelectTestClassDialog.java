package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import bluej.Config;
import bluej.utility.DialogManager;

/**
 * Dialog for showing the user a list of files which failed
 * an import.
 *
 * @author  Andrew Patterson
 * @version $Id: SelectTestClassDialog.java 1728 2003-03-28 02:01:36Z ajp $
 */
public class SelectTestClassDialog extends JDialog
{
	static final String okay = Config.getString("okay");
	static final String cancel = Config.getString("cancel");

    private static final String dialogTitle = Config.getString("pkgmgr.selecttestclass.title");
    private static final String helpLine1 = Config.getString("pkgmgr.selecttestclass.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.selecttestclass.helpLine2");
    private static final String helpLine3 = Config.getString("pkgmgr.selecttestclass.helpLine3");

	private String result;
	
    public SelectTestClassDialog(JFrame parent, Object[] objects)
    {
        super(parent, dialogTitle, true);

		result = null;
		
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

            final JList failedList = new JList(objects);
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

				JButton okButton = new JButton(okay);
				{
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt)
						{
							result = failedList.getSelectedValue().toString();
							dispose();
						}
					});
				}

				JButton cancelButton = new JButton(cancel);
				{
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt)
						{
							result = null;
							dispose();
						}
					});
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

	public String getResult()
	{
		return result;
	}
}
