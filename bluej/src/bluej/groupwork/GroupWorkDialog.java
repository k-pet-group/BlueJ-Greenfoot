package bluej.groupwork;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
** @version $Id: GroupWorkDialog.java 504 2000-05-24 04:44:14Z markus $
** @author Markus Ostman
**
** Dialog for Group work purposes
**/

public class GroupWorkDialog extends JDialog

implements ActionListener
{
    // Internationalisation
    static final String close = Config.getString("close");
    static final String GroupWorkDialogTitle = Config.getString("groupwork.groupworkdialog.title");
    static final String checkOutTitle = Config.getString("groupwork.checkout.title");


    private JList parameterList;
    private JFrame parent;
    private MainGrpPanel mainGrpPanel;
    private boolean ok;		// result: which button?

    public GroupWorkDialog(JFrame parent)
    {
        super(parent, GroupWorkDialogTitle, true);
        this.parent = parent;

        //Initialize jCVS components
        //This is necessary because of the design of the jCVS code
        JcvsInit.doInitialize();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E) {
                ok = false;
                setVisible(false);
            }
        });

        this.mainGrpPanel=new MainGrpPanel(this);
        //this.mainGrpPanel.displayCheckout();
        //this.getContentPane().add("North",mainGrpPanel);

        //     // button panel at bottom of dialog
        //     JPanel buttonPanel = new JPanel();
        //     buttonPanel.setLayout(new FlowLayout());
        //     buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        //     JButton button;
        //     buttonPanel.add(button = new JButton(close));
        //     button.addActionListener(this);
        //     getContentPane().add("South", buttonPanel); 		
    }

    /**
     * Show this dialog 
     * 
     */
    public boolean display()
    {
        ok = false;
        pack();
        setVisible(true);
        return ok;
    }

    /**
     * Show this dialog with the Checkout panel
     *
     */
    public boolean displayCheckout()
    {
        ok = false;
        this.mainGrpPanel.displayCheckout();
        this.getContentPane().add("Center",mainGrpPanel);
        this.setTitle(checkOutTitle);
        pack();
        //To Centre the dialog over the current Bluej Window
        DialogManager.centreDialog(this);
        setVisible(true);
        return ok;
    }

//     /**
//      * Show this dialog with the Import panel
//      *
//      */
//     public boolean displayImport()
//     {
//         ok = false;
//         this.mainGrpPanel.displayImport();
//         this.getContentPane().add("Center",mainGrpPanel);
//         this.setTitle("Import");
//         pack();
//         //To Centre the dialog over the current Bluej Window
//         DialogManager.centreDialog(this);
//         setVisible(true);
//         return ok;
//     }

    /**
     * return parent to this dialog
     */
    public JFrame getParentFrame()
    {
        return this.parent;
    }

    public CheckOutPanel getCheckOutPanel()
    {
        return this.mainGrpPanel.getCheckOutPanel();
    }

    /**
     * Handle action
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();

        if(close.equals(cmd))
            doClose();
    }


    /**
     * Close action when checkout is pressed.
     */
    public void doOK()
    {
        if(!true) {
            DialogManager.showError(parent, 
                                    "This error message must be specified");
        }
        else { // collect information from fields
            ok = true;
            setVisible(false);
        }
    }


    /**
     * Close action when Close is pressed.
     */
    public void doClose()
    {
        //./temp this.mainGrpPanel.savePreferences();
        ok = false;
        setVisible(false);
    }

    /**
     * Sets the size of the dialog by overriding getPrefferedSize()
     * The method setSize() does not work with all layouts, neither does this?
     */
    //     public Dimension getPreferredSize()
    //     { 
    //         Dimension d = this.parent.getSize();
    //         Debug.message("GrpWrkDlg,line164 "+new Double(d.getWidth()).toString());
    //         d.setSize((d.getWidth()/2), (d.getHeight()/2));
    //         return d;
    //     }
}
