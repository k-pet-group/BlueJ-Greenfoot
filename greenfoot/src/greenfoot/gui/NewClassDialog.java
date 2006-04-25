package greenfoot.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    JTextField classNameTextField = new JTextField();
    private boolean okPressed = false;

    public NewClassDialog(JFrame parent)
    {
        super(parent, "New class", true);

        JLabel label = new JLabel("New class name: ");
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.generalBorder);
        mainPanel.add(label);
        mainPanel.add(classNameTextField);
        getContentPane().add(mainPanel, BorderLayout.NORTH);

        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            JButton okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    ok();
                }
            });
            buttonPanel.add(okButton);

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt)
                {
                    cancel();
                }
            });
            buttonPanel.add(cancelButton);

            getRootPane().setDefaultButton(okButton);
        }
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();

        this.setLocationRelativeTo(parent);
        this.setLocation((parent.getSize().width - getSize().width) / 2,(parent.getSize().width - getSize().width) / 2);

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