package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Package;
import bluej.views.View;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.ViewFilter;

import java.util.Vector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * 
 *
 * @author  Michael Kolling
 *
 * @version $Id: LibraryCallDialog.java 816 2001-03-26 05:54:10Z mik $
 */
public class LibraryCallDialog extends JDialog
	implements ActionListener, ListSelectionListener
{
    // ======= static (factory) section =======

    private static LibraryCallDialog dlg = null;

    public static void showLibraryCallDialog(PkgMgrFrame pmf)
    {
        if(dlg == null)
            dlg = new LibraryCallDialog(pmf);
        dlg.setVisible(true);
    }

    // ======= instance section =======

    private JComboBox classField;
    private JList methodList;
    private JButton docButton;
    private JButton okButton;
    private JButton cancelButton;

    private ClassHistory history;
    private Package pkg;
    private CallableView viewToCall;
    private Vector currentViews;      // views currently displayed in list

    private LibraryCallDialog(PkgMgrFrame pmf)
    {
        super(pmf, Config.getString("pkgmgr.callLibraryDialog.title"), false);
        pkg = pmf.getPackage();
        currentViews = new Vector();
        viewToCall = null;
        history = new ClassHistory(10);
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
            classField.setModel(new DefaultComboBoxModel(history.getHistory()));
    	}
    	else {
        }
    }

    /**
     * Process action events
     */
    public void actionPerformed(ActionEvent event)
    {
        Object eventSource = event.getSource();
        if(eventSource == classField)
            classSelected();
        else if (eventSource == docButton)
            showDocumentation();
        else if (eventSource == okButton)
            doOk();
        else if (eventSource == cancelButton)
            doCancel();
    }

    /**
     * Show the javadoc documentation for the selected class in a browser.
     */
    private void showDocumentation()
    {
        String className = (String)classField.getEditor().getItem();
        Utility.showClassDocumentation(className, "#constructor_summary");
    }

    /**
     * doOk - Process an "Ok" event to invoke a Constructor or Method.
     *  Collects arguments and calls watcher objects (Invoker).
     */
    private void doOk()
    {
        history.addClass((String)classField.getEditor().getItem());
        setVisible(false);
        pkg.getEditor().raiseMethodCallEvent(pkg, viewToCall);
    }

    /**
     * Process a "Cancel" event to cancel a call.
     * Makes dialog invisible.
     */
    private void doCancel()
    {
        setVisible(false);
    }

    /**
     * A class was selected in the class selection box. Try to load that
     * class. If successful, display its constructors and methods. Otherwise
     * clear the method list and return.
     */
    private void classSelected()
    {
        Class cl = null;
        currentViews.clear();
        viewToCall = null;

        String className = (String)classField.getEditor().getItem();

        try {
            cl = Class.forName(className, true, 
                               ClassLoader.getSystemClassLoader());
        }
        catch(Exception exc) {
            methodList.setListData(new Vector());
            docButton.setEnabled(false);
            return;
        }
        displayMethodsForClass(cl);
    }

    /**
     * Given a class, display its constructors and methods in the method list.
     */
    private void displayMethodsForClass(Class cl)
    {
        View classView = View.getView(cl);
        ViewFilter filter;
        Vector list = new Vector();

        ConstructorView[] constructors = classView.getConstructors();
        filter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
        addMethods(list, constructors, filter);

        MethodView[] methods = classView.getAllMethods();
        filter = new ViewFilter(ViewFilter.STATIC | ViewFilter.PROTECTED);
        addMethods(list, methods, filter);

        methodList.setListData(list);
        methodList.clearSelection();
        docButton.setEnabled(true);
        okButton.setEnabled(false);
    }

    /**
     * Add some methods, filtered by a given view filter, to a vector.
     */
    public void addMethods(Vector list, CallableView[] methods, 
                            ViewFilter filter)
    {
        for(int i = 0; i < methods.length; i++) {
            if(filter.accept(methods[i])) {
                currentViews.add(methods[i]);
                list.add(methods[i].getShortDesc());
                //list.add(methods[i].toString());  // includes public static
            }
        }
    }

    // ----- ListSelectionListener interface -----

    /**
     *  A list item was selected. This can be either in the thread list,
     *  the stack list, or one of the variable lists.
     */
    public void valueChanged(ListSelectionEvent event)
    {
        if(event.getValueIsAdjusting())  // ignore mouse down, dragging, etc.
            return;

        int index = methodList.getSelectedIndex();
        if(index == -1)
            return;

        viewToCall = (CallableView)currentViews.get(index);
        okButton.setEnabled(true);
    }

    // ----- end of ListSelectionListener interface -----

    private void makeDialog()
    {
        JPanel contentPane = (JPanel)getContentPane();

        JPanel classPanel = new JPanel(new BorderLayout(4,6));
        {
            classPanel.add(new JLabel(
                  Config.getString("pkgmgr.callLibraryDialog.classLabel")), 
                  BorderLayout.WEST);

            Vector historyList = history.getHistory();
            classField = new JComboBox(historyList);
            classField.setEditable(true);
            classField.setMaximumRowCount(10);
            JTextField textField = (JTextField)classField.getEditor().getEditorComponent();
            textField.setColumns(16);
            classField.addActionListener(this);
            classPanel.add(classField, BorderLayout.CENTER);

            docButton = new JButton(Config.getString("pkgmgr.callLibraryDialog.docButton"));
            docButton.addActionListener(this);
            docButton.setEnabled(false);
            classPanel.add(docButton, BorderLayout.EAST);
        }

        // create the centre Panel
        JPanel centrePanel = new JPanel(new BorderLayout());
        {
            methodList = new JList(new DefaultListModel());
            methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            methodList.addListSelectionListener(this);
            methodList.setVisibleRowCount(8);
            JScrollPane methodScrollPane = new JScrollPane(methodList);
            methodScrollPane.setColumnHeaderView(new JLabel(
                 Config.getString("pkgmgr.callLibraryDialog.listHeading")));

            centrePanel.add(methodScrollPane, BorderLayout.CENTER);

        }

        // create the Button Panel
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

        contentPane.add(classPanel, BorderLayout.NORTH);
        //contentPane.add(Box.createVerticalStrut(Config.generalSpacingWidth));
        contentPane.add(centrePanel, BorderLayout.CENTER);
        //contentPane.add(Box.createVerticalStrut(Config.generalSpacingWidth));
        //contentPane.add(new JSeparator());
        //contentPane.add(Box.createVerticalStrut(Config.generalSpacingWidth));
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();

        // Set some attributes for this DialogBox
        DialogManager.centreDialog(this);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    setVisible(false);
                }
            });
    }

    private JButton addButton(JPanel panel, String label)
    {
        JButton button = new JButton(label);
        panel.add(button);
        button.addActionListener(this);
        return button;
    }
}
