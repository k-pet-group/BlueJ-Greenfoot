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
 * @version $Id: FreeFormCallDialog.java 1503 2002-11-15 13:22:28Z mik $
 */
public class FreeFormCallDialog extends CallDialog
{
    private static final String NOT_VOID = "not void";

    private JComboBox callField;
    private FreeCallHistory history;
    private ButtonGroup radioButtons;
    
    public FreeFormCallDialog(PkgMgrFrame pmf)
    {
        super(pmf, Config.getString("freeCallDialog.title"));
        history = FreeCallHistory.getCallHistory(10);
        makeDialog();
    }

    /**
     * Set the visibility of the dialog
     */
    public void setVisible(boolean show)
    {
    	if (show) {
            setErrorMessage("");
            callField.setModel(new DefaultComboBoxModel(history.getHistory().toArray()));
    	    show();
            startObjectBenchListening();
            callField.getEditor().getEditorComponent().requestFocus();
    	}
    	else {
            stopObjectBenchListening();
    	    hide();
    	}
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     *  Collects arguments and calls watcher objects (Invoker).
     */
    public void doOk()
    {
        String expression = getExpression();
        if(expression.length() > 0) {
            history.add(expression);
            setWaitCursor(true);
            callWatcher(OK);
        }
        else
            doCancel();
    }

    /**
     * Process a "Cancel" event to cancel a Constructor or Method call.
     * Makes dialog invisible.
     */
    public void doCancel()
    {
        callWatcher(CANCEL);
    }

    /**
     * Get the expression that was entered into the text field.
     */
    public String getExpression()
    {
        return (String)callField.getEditor().getItem();
    }

    /**
     * Get the value of the 'hasResult' switch.
     */
    public boolean getHasResult()
    {
        return radioButtons.getSelection().getActionCommand() == NOT_VOID;
    }

    /**
     * Insert text into the text field.
     */
    public void insertText(String text)
    {
        ((JTextField)callField.getEditor().getEditorComponent()).setText(text);
        show();  // bring to front
    }


    /**
     * Build the Swing dialog.
     */
    private void makeDialog()
    {
        JPanel topPanel = new JPanel(new BorderLayout(4,6));
        topPanel.add(new JLabel(Config.getString("freeCallDialog.fieldLabel")),
                     BorderLayout.WEST);

        callField = new JComboBox(history.getHistory().toArray());
        callField.setEditable(true);
        callField.setMaximumRowCount(10);
        JTextField textField = (JTextField)callField.getEditor().getEditorComponent();
        textField.setColumns(18);
        textField.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) { doOk(); }
                    });

        topPanel.add(callField, BorderLayout.CENTER);

        JPanel choicePanel = new JPanel();
        {
            choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
            //choicePanel.setAlignmentX(LEFT_ALIGNMENT);
            radioButtons = new ButtonGroup(); 
    
            JRadioButton button = new JRadioButton(Config.getString("freeCallDialog.returnsResult"), true);
            button.setActionCommand(NOT_VOID);
            radioButtons.add(button);
            choicePanel.add(button);
            
            button = new JRadioButton(Config.getString("freeCallDialog.returnsNoResult"), false);
            button.setActionCommand(null);
            radioButtons.add(button);
            choicePanel.add(button);
        }

        JPanel labelPanel = new JPanel();
        {
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
            labelPanel.add(new JLabel(Config.getString("freeCallDialog.resultLabel")));
            labelPanel.add(new JLabel(" "));
        }

        JPanel selectionPanel = new JPanel();
        {
            selectionPanel.add(labelPanel);
            selectionPanel.add(choicePanel);
        }
        topPanel.add(selectionPanel, BorderLayout.SOUTH);

        super.makeDialog(topPanel, getErrorLabel());
    }
}
