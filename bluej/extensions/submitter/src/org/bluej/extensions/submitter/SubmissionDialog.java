package org.bluej.extensions.submitter;

import org.bluej.extensions.submitter.transport.*;
import org.bluej.extensions.submitter.properties.*;

import bluej.extensions.BlueJ;
import bluej.extensions.BPackage;

import java.util.Collection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

/**
 * Handles the dialog with the user to determine which
 * scheme they would like to use, and oversees the
 * processing of that scheme
 *
 *
 * @author     Clive Miller
 * @version    $Id: SubmissionDialog.java 1610 2003-01-27 09:49:03Z damiano $
 */
class SubmissionDialog extends JDialog implements ActionListener
{
    private final BlueJ bj;
    private final BPackage pkg;
    private JButton submitButton, cancelButton, browseButton;
    private boolean updateDialog = true;
    private JTextField schemeField;
    private JLabel statusLabel;
    private boolean running;
    private Thread submitThread;
    private Stat stat;
    JDialog resultFrame;


    /**
     *Constructor for the SubmissionDialog object
     *
     * @param  i_stat  Description of the Parameter
     * @param  pkg     Description of the Parameter
     */
    public SubmissionDialog(Stat i_stat, BPackage pkg)
    {
        super(pkg.getFrame(), i_stat.bluej.getLabel("dialog.title"), true);

        stat = i_stat;
        bj = stat.bluej;
        this.pkg = pkg;
        addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    setVisible(false);
                }
            });

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(bj.getDialogBorder());
         {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel tag = new JLabel(bj.getLabel("dialog.scheme") + ":  ");

            schemeField = new JTextField("", 40);
            schemeField.addActionListener(this);
            // so that pressing return in the field does the submit
            schemeField.getDocument().addDocumentListener(
                new DocumentListener()
                {
                    public void changedUpdate(DocumentEvent de)
                    {
                        schemeFieldChanged();
                    }


                    public void insertUpdate(DocumentEvent de)
                    {
                        schemeFieldChanged();
                    }


                    public void removeUpdate(DocumentEvent de)
                    {
                        schemeFieldChanged();
                    }
                });

            browseButton = new JButton(bj.getLabel("button.browse"));
            browseButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        doBrowseTree();
                    }
                });
            panel.add(tag, BorderLayout.WEST);
            panel.add(schemeField);
            panel.add(browseButton, BorderLayout.EAST);
            content.add(panel);
        }

        content.add(Box.createVerticalStrut(10));
         {
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(1, 1));
            // Centres the status line
            statusLabel =
                new JLabel(" ", JLabel.CENTER)
                {
                    public Dimension getPreferredSize()
                    {
                        return new Dimension(200, 20);
                    }


                    public Dimension getMinimumSize()
                    {
                        return new Dimension(200, 20);
                    }
                };
            panel.add(statusLabel);

            content.add(panel);
        }

        content.add(Box.createVerticalStrut(10));
         {
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

            submitButton = new JButton(bj.getLabel("button.submit"));
            submitButton.addActionListener(this);
            buttonPanel.add(submitButton);

            cancelButton = new JButton(bj.getLabel("cancel"));
            cancelButton.addActionListener(this);
            buttonPanel.add(cancelButton);

            content.add(buttonPanel);
        }
        setContentPane(content);
        pack();
        bj.centreWindow(this);
        running = false;
        stat.submiProp.addTreeModelListener(
            new TreeModelListener()
            {
                public void treeNodesChanged(TreeModelEvent e)
                {
                    updateDialog();
                }


                public void treeNodesInserted(TreeModelEvent e)
                {
                    updateDialog();
                }


                public void treeNodesRemoved(TreeModelEvent e)
                {
                    updateDialog();
                }


                public void treeStructureChanged(TreeModelEvent e)
                {
                    updateDialog();
                }
            });
        schemeField.setText(stat.submiProp.getSelectedScheme());
    }


    // Response to a change in the tree model
    /**
     *  Description of the Method
     */
    public void updateDialog()
    {
        if (updateDialog) {
            submitButton.setEnabled(stat.submiProp.isValidScheme());
            setStatus(Node.isLookingUp() ? bj.getLabel("message.lookingupschemes") : "");
        }
    }


    // Reponse to the user changing what's in the selected scheme box
    /**
     *  Description of the Method
     */
    public void schemeFieldChanged()
    {
        stat.submiProp.setSelectedScheme(schemeField.getText());
        updateDialog();
    }


    /**
     *  Description of the Method
     *
     * @param  e  Description of the Parameter
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();
        if (source == cancelButton)
            doCancel();

        else if (source == submitButton || source == schemeField) {
            if (submitButton.isEnabled())
                doSubmit();
        }
    }


    /**
     *  Sets the status attribute of the SubmissionDialog object
     *
     * @param  status   The new status value
     * @param  isError  The new status value
     */
    public void setStatus(String status, boolean isError)
    {
        statusLabel.setForeground(isError ? Color.red : Color.black);
        statusLabel.setText(status);
    }


    /**
     *  Sets the status attribute of the SubmissionDialog object
     *
     * @param  status  The new status value
     */
    public void setStatus(String status)
    {
        setStatus(status, false);
    }


    /**
     *  Description of the Method
     */
    private void doSubmit()
    {
        stat.submiProp.setDefaultScheme();
        updateDialog = false;
        // stop the update listener from re-enabling the submit button
        submitButton.setEnabled(false);
        cancelButton.setText(bj.getLabel("button.stop"));
        setStatus(bj.getLabel("message.started"));
        running = true;
        submitThread = new SubmitThread();
        submitThread.start();
    }


    /**
     *  Description of the Method
     */
    private void doCancel()
    {
        if (running) {
            submitThread.interrupt();
            running = false;
            setStatus(" ");
            cancelButton.setText(bj.getLabel("cancel"));
            updateDialog = true;
            updateDialog();
        }
        else
            hide();

        if (resultFrame != null)
            resultFrame.dispose();
    }


    /**
     *  Description of the Method
     */
    private void doBrowseTree()
    {
        new TreeDialog(stat.submiProp);
    }


    /**
     *  Description of the Method
     */
    public void reset()
    {
        setStatus("");
        cancelButton.setText(bj.getLabel("cancel"));
        updateDialog = true;
        updateDialog();
    }


    /**
     * This is used to show the choiches to the user.
     */
    class TreeDialog extends JDialog implements ActionListener, TreeSelectionListener, MouseListener
    {
        private JButton okButton, cancelButton;
        private SubmissionProperties sp;
        private JTree tree;


        /**
         *Constructor for the TreeDialog object
         *
         * @param  sp  Description of the Parameter
         */
        public TreeDialog(SubmissionProperties sp)
        {
            super(SubmissionDialog.this, bj.getLabel("message.selectscheme"));
            this.sp = sp;
            JPanel content = new JPanel();
            content.setLayout(new BorderLayout());

            tree = sp.getTree();
            tree.addTreeSelectionListener(this);
            tree.addMouseListener(this);

            JScrollPane scrollpane = new JScrollPane(tree);
            scrollpane.setPreferredSize(new Dimension(400, 300));
            content.add(scrollpane);
             {
                JPanel buttonPanel = new JPanel();
                okButton = new JButton(bj.getLabel("okay"));
                okButton.addActionListener(this);
                okButton.setEnabled(sp.isValidScheme());
                buttonPanel.add(okButton);
                cancelButton = new JButton(bj.getLabel("cancel"));
                cancelButton.addActionListener(this);
                buttonPanel.add(cancelButton);
                content.add(buttonPanel, BorderLayout.SOUTH);
            }

            setContentPane(content);
            pack();
            BlueJ.centreWindow(this, SubmissionDialog.this);
            show();
        }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void valueChanged(TreeSelectionEvent e)
        {
            Object selected = e.getPath().getLastPathComponent();
            okButton.setEnabled(selected instanceof Node && ((Node) selected).isLeaf());
        }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void mouseClicked(MouseEvent e)
        {
            if (e.getClickCount() == 2 && !e.isPopupTrigger() && okButton.isEnabled()) {
                sp.setSchemeFromTree();
                schemeField.setText(sp.getSelectedScheme());
                sp.setDefaultScheme();
                dispose();
            }
        }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void mouseEntered(MouseEvent e) { }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void mouseExited(MouseEvent e) { }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void mousePressed(MouseEvent e) { }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void mouseReleased(MouseEvent e) { }


        /**
         *  Description of the Method
         *
         * @param  e  Description of the Parameter
         */
        public void actionPerformed(ActionEvent e)
        {
            Object src = e.getSource();
            if (src == okButton) {
                sp.setSchemeFromTree();
                schemeField.setText(sp.getSelectedScheme());
                sp.setDefaultScheme();
            }
            else {
                // sp.reload();
            }
            dispose();
        }
    }


    /**
     *  Description of the Method
     *
     * @param  ex  Description of the Parameter
     * @return     Description of the Return Value
     */
    private String translateException(Throwable ex)
    {
        String message = null;
        if (ex instanceof AbortOperationException)
            ex = ((AbortOperationException) ex).getException();
        if (ex instanceof java.net.UnknownHostException)
            message = bj.getLabelInsert("exception.unknownhost", ex.getMessage());
        if (ex instanceof java.net.NoRouteToHostException)
            message = bj.getLabel("exception.notroutetohost");
        if (ex instanceof java.net.ProtocolException)
            message = ex.getMessage();
        if (ex instanceof java.io.FileNotFoundException)
            message = bj.getLabelInsert("exception.filenotfound", ex.getMessage());
        if (ex instanceof IllegalArgumentException && ex.getMessage().equals("SMTP Host has not been set"))
            message = bj.getLabel("exception.hostnotset");
        if (ex instanceof IllegalArgumentException && ex.getMessage().equals("User Email address invalid"))
            message = bj.getLabel("exception.addrnotset");
        return message;
    }



    /**
     * -------------- This is a split from the inline that was before ----------------
     * It is aligned to the LEFT since the indentation is already too far on the right...
     * What it does is submitting a project. This happens when somebody press the submit button.
     */
    class SubmitThread extends Thread
    {
        File projNamePrefix;
        String jarName = null;


        /**
         * Split for the earlier inline. This is sending a bunch of files over to the remote side
         *
         * @param  urlProps                     Description of the Parameter
         * @return                              Description of the Return Value
         * @exception  AbortOperationException  Description of the Exception
         */
        private String sendFiles(URLProperties urlProps) throws AbortOperationException
        {
            TransportSession ts = null;

            FileHandler fh = new FileHandler(bj, pkg, stat.submiProp);
            File[] files = fh.getFiles();
            if (files == null)
                return "";

            try {
                Collection jarNames = stat.submiProp.getProps(".file.jar");
                if (!jarNames.isEmpty()) {
                    jarName = (String) jarNames.iterator().next();
                    ts = TransportSession.createJarTransportSession(urlProps.getURL(), stat.submiProp.getGlobalProps(), jarName);
                }
                else
                    ts = TransportSession.createTransportSession(urlProps.getURL(), stat.submiProp.getGlobalProps());

                ts.addStatusListener(
                    new StatusListener()
                    {
                        public void statusChanged(String status)
                        {
                            setStatus(status);
                        }
                    });

                ts.connect();

                // ere we are sending files, the issue here is to put the prefix
                // as wanted by Michael. Damiano.
                String projNamePrefix = null;
                String tsProtocol = ts.getProtocol();
                if (tsProtocol.equals("ftp") || tsProtocol.equals("file") || jarName != null)
                    projNamePrefix = pkg.getProject().getName();

                for (int i = 0, n = files.length; i < n; i++) {
                    boolean binary = FileHandler.isBinary(files[i]);
                    FileInputStream fis = new FileInputStream(files[i]);

                    String name = fh.getSubName(files[i]);

                    if (projNamePrefix != null)
                        name = projNamePrefix + File.separator + name;

                    name = name.replace(File.separatorChar, '/');
                    setStatus(bj.getLabel("message.sending") + " " + name);
                    ts.send(fis, name, binary);
                }

                ts.disconnect();
            } catch (IOException ex) {
                if (ts != null)
                    System.err.println(ts.getLog());
                ex.printStackTrace();
                throw new AbortOperationException(ex);
            }
            String result = ts.getResult();
            if (result == null)
                result = files.length + " " + bj.getLabel("message.filessent");

            return result;
        }


        /**
         * The starting method for the thread. Execution will begin here.
         */
        public void run()
        {
            TransportSession ts = null;
            String result = null;

            try {
                URLProperties urlProps = new URLProperties(bj, stat.submiProp);
                // This tryes to get whaever is requested from the user. By a dialog box.
                if (!urlProps.process(SubmissionDialog.this)) {
                    reset();
                    running = false;
                    return;
                }

                if (urlProps.isMessage()) {
                    ts = TransportSession.createTransportSession(urlProps.getURL(), stat.submiProp.getGlobalProps());
                    ts.connect();
                    result = ts.getResult();
                }
                else
                    result = sendFiles(urlProps);

                cancelButton.setText(bj.getLabel("button.done"));
                cancelButton.requestFocus();
                setStatus(bj.getLabel("message.complete"));
                browseButton.setEnabled(false);
                schemeField.removeActionListener(SubmissionDialog.this);
                validate();

                if (running) {
                    setStatus(result);
                    if (statusLabel.getMaximumSize().getHeight() > statusLabel.getSize().getHeight()) {
                        // It doesn't fit!
                        statusLabel.setText(null);
                        JPanel panel = new JPanel();
                        JLabel label = new JLabel(result);
                        panel.setBorder(bj.getDialogBorder());
                        panel.add(label);
                        resultFrame = bj.showGeneralDialog(bj.getLabel("title.results"), new JScrollPane(panel), SubmissionDialog.this);
                        dispose();
                    }
                }
            } catch (Throwable ex) {
                if (running) {
                    cancelButton.setText(bj.getLabel("cancel"));
                    updateDialog = true;
                    updateDialog();
                    String message = translateException(ex);
                    boolean inhibitDialog = false;
                    if (message == null) {
                        if (ex instanceof AbortOperationException)
                            message = ex.getMessage();
                        else {
                            message = ex.getMessage();
                            if (message == null || message.indexOf(':') == -1)
                                message = ex.toString();
                        }
                    }
                    else
                        inhibitDialog = true;

                    if (inhibitDialog || ts == null || ts.getLog() == null)
                        setStatus(message, true);
                    else {
                        int press;
                        do {
                            setStatus(bj.getLabel("message.error"), true);
                            Object[] options = (ts == null || ts.getLog() == null) ? new Object[]{bj.getLabel("close")} : new Object[]{bj.getLabel("close"), bj.getLabel("button.showlog")};
                            press = JOptionPane.showOptionDialog
                                    (SubmissionDialog.this,
                            // parentComponent
                            message,
                            // message
                            bj.getLabel("message.error"),
                            // title
                            JOptionPane.DEFAULT_OPTION,
                            // optionType
                            JOptionPane.ERROR_MESSAGE,
                            // messageType
                            null,
                            // icon
                            options,
                            // options
                            bj.getLabel("close"));
                            // initialValue
                            if (press == 1) {
                                JPanel panel = new JPanel();
                                JTextArea label = new JTextArea(ts.getLog());
                                label.setEditable(false);
                                label.setBorder(bj.getDialogBorder());
                                panel.add(new JScrollPane(label));
                                bj.showGeneralDialog(bj.getLabel("title.log"), panel, SubmissionDialog.this);
                            }
                        } while (press != 0);
                        setStatus(" ");
                    }
                }
            }
            running = false;
        }
    }
    // End of the actual SubmitThread

}
// End of MAIN class, be patient, please....
