package bluej.compiler;

import bluej.Config;

import bluej.utility.MultiLineLabel;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Dialog for Compiler Warning messages.  Should be used as a Singleton.  
 * The dialog is non-modal, allowing minimisation to ignore further warnings.
 * 
 * @version $Id: CompilerWarningDialog.java 1732 2003-04-01 06:28:39Z bquig $
 * @author Bruce Quig
 */
public class CompilerWarningDialog extends JFrame implements ActionListener
{
    // Internationalisation
    static final String close = Config.getString("close");
    static final String dialogTitle = Config.getString("compiler.warningDialog.title");
    static final String subTitle = Config.getString("compiler.warningDialog.label");
    static final String noWarnings = Config.getString("compiler.warningDialog.noWarnings");
    
    private MultiLineLabel warningLabel;
    
    // singleton
    private static CompilerWarningDialog dialog = new CompilerWarningDialog();
    
    /**
     * Creates a new CompilerWarningDialog object.  Needs to be accessed through
     *  static factory method, getDialog()
     * 
     * @param parent the frame that called the print dialog
     */
    private CompilerWarningDialog()
    {
        super(dialogTitle);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                setVisible(false);
            }
        });

        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5,12,12,40));
        mainPanel.add(Box.createVerticalStrut(
                              Config.dialogCommandButtonsVertical));

        JLabel subTitleLabel = new JLabel(subTitle);
        mainPanel.add(subTitleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
                
        warningLabel = new MultiLineLabel();
        mainPanel.add(warningLabel);
     
        mainPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);

        JButton closeButton = new JButton(close);
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);
        getRootPane().setDefaultButton(closeButton);

        mainPanel.add(buttonPanel);

        getContentPane().add(mainPanel);
        pack();

    }
    
    public static CompilerWarningDialog getDialog()
    {
        return dialog;
    }


    /**
     * ActionListener for buttons
     * 
     * @param evt button event (Cancel or OK)
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();

        if (close.equals(cmd)) {
            doClose();
        }
      
    }

  
    /**
     * Close action when Cancel is pressed.
     */
    public void doClose()
    {
        setVisible(false);
    }
    
    public void setWarningMessage(String warning)
    {
        warningLabel.setText(warning);
        pack();
        if(!isVisible()) {
            setVisible(true);            
        }
    }
    
    public void reset()
    {
        warningLabel.setText(noWarnings);
        pack();    
    }
    
}
  