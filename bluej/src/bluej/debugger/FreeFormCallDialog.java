package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.MultiLineLabel;
import bluej.utility.JavaNames;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.debugger.CallHistory;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.List;
import java.util.ArrayList;


/**
 * This dialog is used for an interactive free form call. The call
 * can be any legal java statement or expression (no declarations).
 *
 * @author  Michael Kolling
 *
 * @version $Id: FreeFormCallDialog.java 1371 2002-10-14 08:26:48Z mik $
 */
public class FreeFormCallDialog extends CallDialog
{
    private JComboBox callField;
    private ClassHistory history;

    public FreeFormCallDialog(PkgMgrFrame pmf)
    {
        super(pmf, Config.getString("freeCallDialog.title"));
//        pkg = pmf.getPackage();
//        history = ClassHistory.getClassHistory(10);
        makeDialog();
    }

    /**
     * Set the visibility of the dialog
     */
    public void setVisible(boolean show)
    {
    	super.setVisible(show);
    	if (show) {
            okButton.setEnabled(false);
            callField.setModel(new DefaultComboBoxModel(history.getHistory()));
            callField.requestFocus();
    	}
    }

    /**
     * Process action events
     */
    public void actionPerformed(ActionEvent event)
    {
        Object eventSource = event.getSource();
        if(eventSource == callField)
            doOk();
        else 
            super.actionPerformed(event);   // handles Ok and cancel buttons
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     *  Collects arguments and calls watcher objects (Invoker).
     */
    public void doOk()
    {
//        history.addClass((String)classField.getEditor().getItem());
        setVisible(false);
        
    }

    /**
     * Process a "Cancel" event to cancel a Constructor or Method call.
     * Makes dialog invisible.
     */
    public void doCancel()
    {
        setVisible(false);
    }

    /**
     * Get the expression that was entered into the text field.
     */
    public String getExpression()
    {
        return "4+5";
    }

    /**
     * Get the value of the 'hasResult' switch.
     */
    public boolean getHasResult()
    {
        return true;
    }

    /**
     * Insert text into the text field.
     */
    public void insertText(String text)
    {
    }

    protected JComponent createTopComponent()
    {
        JPanel topPanel = new JPanel(new BorderLayout(4,6));
        topPanel.add(new JLabel(Config.getString("freeCallDialog.fieldLabel")),
                     BorderLayout.WEST);

        callField = new JComboBox(history.getHistory());
        callField.setEditable(true);
        callField.setMaximumRowCount(10);
        JTextField textField = (JTextField)callField.getEditor().getEditorComponent();
        textField.setColumns(30);
        callField.addActionListener(this);
        topPanel.add(callField, BorderLayout.CENTER);
        return topPanel;
    }

    protected JComponent createCenterComponent()
    {
        return getErrorLabel();
    }
}
