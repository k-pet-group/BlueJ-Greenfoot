package bluej.groupwork.ui;

import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.UpdateServerResponse;
import bluej.utility.EscapeDialog;

/**
 * A dialog for selecting a module to checkout.
 * 
 * @author Davin McCall
 * @version $Id: ModuleSelectDialog.java 4838 2007-02-07 01:01:21Z davmac $
 */
public class ModuleSelectDialog extends EscapeDialog implements ListSelectionListener
{
    private Repository repository;
    
    private ActivityIndicator progressBar;
    private JTextField moduleField;
    private JButton okButton;
    private JList moduleList;
    
    private boolean wasOk;
    
    public ModuleSelectDialog(Frame owner, Repository repository)
    {
        super(owner, Config.getString("team.moduleselect.title"), true);
        this.repository = repository;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        pack();
    }
    
    /**
     * Get the selected module name, or null if no module was selected
     * (dialog was cancelled).
     */
    public String getModuleName()
    {
        if (wasOk) {
            return moduleField.getText();
        }
        else {
            return null;
        }
    }
    
    /**
     * Start the progress bar. Safe to call from any thread.
     */
    private void startProgressBar()
    {
        progressBar.setRunning(true);
    }
    
    /**
     * Stop the progress bar. Safe to call from any thread.
     */
    private void stopProgressBar()
    {
        progressBar.setRunning(false);
    }
    
    private void setModuleList(List modules)
    {
        Object [] listData = modules.toArray();
        moduleList.setListData(listData);
    }
    
    private void buildUI()
    {
        // Content pane
        JPanel contentPane = new JPanel();
        BoxLayout layout = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
        contentPane.setLayout(layout);    
        contentPane.setBorder(BlueJTheme.dialogBorder);
        setContentPane(contentPane);
        
        // Module text field
        Box moduleBox = new Box(BoxLayout.X_AXIS);
        moduleBox.add(new JLabel(Config.getString("team.moduleselect.label")));
        moduleBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        moduleField = new JTextField(20);
        moduleField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e)
            {
                resetOk();
            }
            
            public void insertUpdate(DocumentEvent e)
            {
                resetOk();
            }
            
            public void removeUpdate(DocumentEvent e)
            {
                resetOk();
            }
            
            public void resetOk()
            {
                okButton.setEnabled(moduleField.getText().length() != 0);
            }
        });
        moduleBox.add(moduleField);
        addXAligned(contentPane, moduleBox, 0.0f);
        contentPane.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        
        addXAligned(contentPane, new JSeparator(), 0.0f);
        contentPane.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        // Modules list
        addXAligned(contentPane, new JLabel(Config.getString("team.moduleselect.available")), 0f);
        
        Box moduleListBox = new Box(BoxLayout.X_AXIS);
        moduleList = new JList();
        moduleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moduleList.getSelectionModel().addListSelectionListener(this);
        moduleList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    int index = moduleList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        wasOk = true;
                        dispose();
                    }
                }
            }
        });
        moduleList.setAlignmentY(0f);
        JScrollPane moduleListSP = new JScrollPane(moduleList);
        moduleListSP.setAlignmentY(0f);
        moduleListBox.add(moduleListSP);
        moduleListBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        final JButton listButton = new JButton(Config.getString("team.moduleselect.show"));
        listButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                listButton.setEnabled(false);
                startProgressBar();
                new ModuleListerThread(repository, ModuleSelectDialog.this).start();
            }
        });
        listButton.setAlignmentY(0f);
        moduleListBox.add(listButton);
        
        addXAligned(contentPane, moduleListBox, 0f);

        contentPane.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));
        
        // Button box
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        progressBar = new ActivityIndicator();
        buttonBox.add(progressBar);
        buttonBox.add(Box.createHorizontalGlue());
        addXAligned(contentPane, buttonBox, 0.0f);
        
        // Ok button
        buttonBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        okButton = BlueJTheme.getOkButton();
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                wasOk = true;
                dispose();
            }
        });
        okButton.setEnabled(false);
        buttonBox.add(okButton);
        
        // Cancel button
        buttonBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        JButton cancelButton = BlueJTheme.getCancelButton();
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        buttonBox.add(cancelButton);
    }
    
    private void addXAligned(Container parent, JComponent child, float alignment)
    {
        child.setAlignmentX(alignment);
        parent.add(child);
    }
    
    // ---- ListSelectionListener interface ----
    
    public void valueChanged(ListSelectionEvent e)
    {
        if (! e.getValueIsAdjusting()) {
            int selected = moduleList.getSelectedIndex();
            if (selected != -1) {
                String module = moduleList.getModel().getElementAt(selected).toString();
                moduleField.setText(module);
            }
        }
    }
    
    /**
     * A thread to find the available modules in the background.
     * 
     * @author Davin McCall
     */
    private class ModuleListerThread extends Thread
    {
        private Repository repository;
        private ModuleSelectDialog moduleDialog;
        
        private UpdateServerResponse response;
        
        public ModuleListerThread(Repository repository, ModuleSelectDialog moduleDialog)
        {
            this.repository = repository;
            this.moduleDialog = moduleDialog;
        }
        
        public void run()
        {
            final List modules = new ArrayList();
            final UpdateServerResponse response = getResponse(modules);
            
            EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    if (response != null && ! response.isError()) {
                        moduleDialog.setModuleList(modules);
                    }
                    else {
                        TeamUtils.handleServerResponse(response, moduleDialog);
                    }
                }
            });
        }
        
        public UpdateServerResponse getResponse(List modules)
        {
            try {
                response = repository.getModules(modules);
                stopProgressBar();
                return response;
            } catch (CommandAbortedException e) {
                e.printStackTrace();
                stopProgressBar();
            } catch (CommandException e) {
                e.printStackTrace();
                stopProgressBar();
            } catch (AuthenticationException e) {
                stopProgressBar();
                TeamUtils.handleAuthenticationException(moduleDialog);
            } catch (InvalidCvsRootException e) {
                stopProgressBar();
                TeamUtils.handleInvalidCvsRootException(moduleDialog);
            }
            
            return response;
        }
    }
}
