package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.MultiLineLabel;
import bluej.utility.DialogManager;
import bluej.pkgmgr.PkgMgrFrame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Superclass for interactive call dialogs (method calls or free
 * form calls.
 *
 * @author  Michael Kolling
 *
 * @version $Id: CallDialog.java 1372 2002-10-14 08:43:35Z mik $
 */
public abstract class CallDialog extends JDialog
	implements ActionListener, ObjectBenchListener
{
    static final int OK = 0;
    static final int CANCEL = 1;

    protected JButton okButton;
    protected JButton cancelButton;
    private MultiLineLabel errorLabel;

    private CallDialogWatcher watcher;

    public CallDialog(PkgMgrFrame pmf, String title)
    {
        super(pmf, title, false);
    }

    /**
     * Process action events
     */
    public void actionPerformed(ActionEvent event)
    {
        Object eventSource = event.getSource();
        if (eventSource == okButton)
            doOk();
        else if (eventSource == cancelButton)
            doCancel();
    }

    /**
     * The Ok button was pressed.
     */
    public abstract void doOk();

    /**
     * The Cancel button was pressed.
     */
    public abstract void doCancel();

    /**
     * Set a watcher for events of this dialog.
     */
    public void setWatcher(CallDialogWatcher w)
    {
        watcher = w;
    }

    /**
     * callWatcher - notify watcher of dialog events.
     */
    public void callWatcher(int event)
    {
        if (watcher != null)
            watcher.callDialogEvent(this, event);
    }

    /**
     * setWaitCursor - Sets the cursor to "wait" style cursor, using swing
     *  bug workaround at present
     */
    public void setWaitCursor(boolean wait)
    {
        if(wait)
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Return the label to be used for showing error messages.
     */
    protected MultiLineLabel getErrorLabel()
    {
        if(errorLabel == null) {
            errorLabel = new MultiLineLabel("\n\n", LEFT_ALIGNMENT);
            errorLabel.setForeground(new Color(136,56,56));  // dark red
        }
        return errorLabel;
    }

    /**
     * setMessage - Sets a status bar style message for the dialog mainly
     *  for reporting back compiler errors upon method calls.
     */
    public void setErrorMessage(String message)
    {
        errorLabel.setText(message);
        pack();
        invalidate();
        validate();
    }

    /**
     * Insert text into the text field that has the focus.
     */
    public abstract void insertText(String text);

    // -- ObjectBenchListener interface --

    /**
     * The object was selected interactively (by clicking
     * on it with the mouse pointer).
     */
    public void objectEvent(ObjectBenchEvent obe)
    {
        ObjectWrapper wrapper = obe.getWrapper();
        insertText(wrapper.instanceName);
    }

    /**
     * Build the Swing dialog. The top and center components
     * are supplied by the specific subclasses. This method
     * add the Ok and Cancel buttons.
     */
    protected void makeDialog(JComponent topComponent, JComponent centerComponent)
    {
        JPanel contentPane = (JPanel)getContentPane();

        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            okButton = addButton(buttonPanel, Config.getString("okay"));
            cancelButton = addButton(buttonPanel, Config.getString("cancel"));

            getRootPane().setDefaultButton(okButton);
            okButton.setEnabled(false);

            // try to make the OK and cancel buttons have equal width
            okButton.setPreferredSize(
                       new Dimension(cancelButton.getPreferredSize().width,
                                     okButton.getPreferredSize().height));
        }

        //contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(Config.generalBorder);

        contentPane.add(topComponent, BorderLayout.NORTH);
        contentPane.add(centerComponent, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        DialogManager.centreDialog(this);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    setVisible(false);
                }
            });
    }
    
    /**
     * Helper method to add a button to a panel.
     */
    private JButton addButton(JPanel panel, String label)
    {
        JButton button = new JButton(label);
        panel.add(button);
        button.addActionListener(this);
        return button;
    }


}
