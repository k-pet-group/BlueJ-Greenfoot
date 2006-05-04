package greenfoot.gui;

import greenfoot.util.GreenfootUtil;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.utility.EscapeDialog;

/**
 * Dialog that asks for the name of a new class. This is only used for non Actor
 * classes.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class NewClassDialog extends EscapeDialog
{
    JTextField classNameTextField;
    private boolean okPressed = false;

    public NewClassDialog(JFrame parent)
    {
        super(parent, "Create new class", true);

        JPanel mainPanel = new JPanel();
        setContentPane(mainPanel);
        
        // help labels
        JLabel helpLabel1 = GreenfootUtil.createHelpLabel();
        JLabel helpLabel2 = GreenfootUtil.createHelpLabel();
        helpLabel1.setText("Enter a name for the new class. The class will");
        helpLabel2.setText("appear in the class browser when you hit Ok.");
        helpLabel1.setAlignmentX(0.0f);
        helpLabel2.setAlignmentX(0.0f);
        mainPanel.add(helpLabel1);
        mainPanel.add(helpLabel2);
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS, 2 * BlueJTheme.generalSpacingWidth));
        
        JLabel label = new JLabel("New class name: ");
        label.setAlignmentX(0.0f);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.generalBorder);
        mainPanel.add(label);
        
        classNameTextField = new JTextField();
        classNameTextField.setAlignmentX(0.0f);
        Dimension classNameMax = classNameTextField.getMaximumSize();
        classNameMax.height = classNameTextField.getPreferredSize().height;
        classNameTextField.setMaximumSize(classNameMax);
        mainPanel.add(classNameTextField);

        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

            // push buttons over to the right using a glue component
            buttonPanel.add(Box.createHorizontalGlue());
            
            JButton okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    ok();
                }
            });
            buttonPanel.add(okButton);

            buttonPanel.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
            
            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    cancel();
                }
            });
            buttonPanel.add(cancelButton);

            getRootPane().setDefaultButton(okButton);
            buttonPanel.setAlignmentX(0.0f);
            
            // Limit the growth of the button panel
            Dimension buttonPanelMax = buttonPanel.getMaximumSize();
            buttonPanelMax.height = buttonPanel.getPreferredSize().height;
            buttonPanel.setMaximumSize(buttonPanelMax);
        }
        
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        mainPanel.add(GreenfootUtil.createSpacer(GreenfootUtil.Y_AXIS,  2 * BlueJTheme.generalSpacingWidth));
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);
        pack();

        this.setLocationRelativeTo(parent);
    }

    private void ok()
    {
        okPressed = true;
        dispose();
    }

    private void cancel()
    {
        okPressed = false;
        dispose();
    }

    public String getClassName()
    {
        return classNameTextField.getText();
    }

    public boolean okPressed()
    {
        return okPressed;
    }
}