package bluej.debugmgr;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import bluej.BlueJTheme;
import bluej.debugmgr.objectbench.*;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.*;

/**
 * Superclass for interactive call dialogs (method calls or free
 * form calls.
 *
 * @author  Michael Kolling
 *
 * @version $Id: CallDialog.java 2629 2004-06-19 14:24:17Z polle $
 */
public abstract class CallDialog extends JDialog
	implements ObjectBenchListener
{
    static final int OK = 0;
    static final int CANCEL = 1;

    private MultiLineLabel errorLabel;

    private ObjectBench bench;
    private CallDialogWatcher watcher;

    public CallDialog(PkgMgrFrame pmf, String title)
    {
        super(pmf, title, false);
        bench = pmf.getObjectBench();
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
     * Return the frame's object bench.
     */
    protected ObjectBench getObjectBench()
    {
        return bench;
    }

    /**
     * Start listening to object bench events.
     */
    protected void startObjectBenchListening()
    {
        bench.addObjectBenchListener(this);
    }

    /**
     * Stop listening to object bench events.
     */
    protected void stopObjectBenchListening()
    {
        bench.removeObjectBenchListener(this);
    }

    /**
     * setMessage - Sets a status bar style message for the dialog mainly
     *  for reporting back compiler errors upon method calls.
     */
    public void setErrorMessage(String message)
    {
        // cut the "location: __SHELL3" bit from some error messages
        int index = message.indexOf("location:");
        if(index != -1)
            message = message.substring(0,index-1);

        errorLabel.setText(message);
        pack();
        invalidate();
        validate();
    }

    /**
     * Insert text into the text field that has the focus.
     */
    public abstract void insertText(String text);

    // ---- ObjectBenchListener interface ----

    /**
     * The object was selected interactively (by clicking
     * on it with the mouse pointer).
     */
    public void objectEvent(ObjectBenchEvent obe)
    {
        ObjectWrapper wrapper = obe.getWrapper();
        insertText(wrapper.getName());
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

            JButton okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) { doOk(); }
                    });
            buttonPanel.add(okButton);

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) { doCancel(); }
                    });
            buttonPanel.add(cancelButton);

            getRootPane().setDefaultButton(okButton);
        }

        //contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BlueJTheme.generalBorder);

        if(topComponent != null)
            contentPane.add(topComponent, BorderLayout.NORTH);
        if(centerComponent != null)
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
}
