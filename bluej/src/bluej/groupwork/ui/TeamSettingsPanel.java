package bluej.groupwork.ui;

import java.awt.BorderLayout;
import java.awt.FocusTraversalPolicy;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.actions.ValidateCvsConnectionAction;


/**
 * A panel for team settings.
 * 
 * @author fisker
 * @version $Id: TeamSettingsPanel.java 5079 2007-05-31 06:48:07Z davmac $
 */
public class TeamSettingsPanel extends JPanel 
{
    private static final int fieldsize = 20;
    private final String pserverLabel = Config.getString("team.settings.pserver");
    private final String extLabel = Config.getString("team.settings.ext");
    private final String[] conTypes = {extLabel, pserverLabel};
    private TeamSettingsController teamSettingsController;
    private TeamSettingsDialog teamSettingsDialog;
    
    JTextField userField;
    JPasswordField passwordField;
    JTextField groupField;
    JTextField prefixField;
    JTextField serverField;
    JComboBox protocolComboBox;
    JButton validateButton;
    JCheckBox useAsDefault;
    
    JLabel groupLabel;
    JLabel prefixLabel;
    JLabel serverLabel;
    JLabel protocolLabel;
    
    private boolean okEnabled = true;
    
    public TeamSettingsPanel(TeamSettingsController teamSettingsController, TeamSettingsDialog dialog)
    {
        this.teamSettingsController = teamSettingsController;
        this.teamSettingsDialog = dialog;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);
        add(Box.createVerticalGlue());
        
        add(makePersonalPanel());
        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        add(makeLocationPanel());
        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        useAsDefault = new JCheckBox(Config.getString("team.settings.rememberSettings"));
        add(useAsDefault);
        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        validateButton = new JButton(new ValidateCvsConnectionAction(
                Config.getString("team.settings.checkConnection"), this, dialog));
        add(validateButton);
        
        DocumentListener changeListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e)
            {
                checkOkEnabled();
            }
            
            public void insertUpdate(DocumentEvent e)
            {
                checkOkEnabled();
            }
            
            public void removeUpdate(DocumentEvent e)
            {
                checkOkEnabled();
            }
        };
        
        userField.getDocument().addDocumentListener(changeListener);
        serverField.getDocument().addDocumentListener(changeListener);
        
        //add(new JSeparator());
        add(Box.createVerticalGlue());
        setupContent();
        checkOkEnabled();
        if (!teamSettingsController.hasProject()){
            useAsDefault.setSelected(true);
            // useAsDefault.setEnabled(false);
        }
    }
    
    /**
     * Get the focus traversal policy for the parent window. The new policy
     * overrides some functionality and delegates everything else back to
     * the original policy (the delegate).
     * 
     * @param delegate  The original traversal policy
     */
    public FocusTraversalPolicy getTraversalPolicy(FocusTraversalPolicy delegate)
    {
        if (getUser().length() != 0) {
            return new TeamPanelFocusPolicy(passwordField, delegate);
        }
        else {
            return delegate;
        }
    }
    
    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol
     */
    public void disableRepositorySettings()
    {
        groupField.setEnabled(false);
        prefixField.setEnabled(false);
        serverField.setEnabled(false);
        protocolComboBox.setEnabled(false);
        
        // useAsDefault.setEnabled(false);
        
        groupLabel.setEnabled(false);
        prefixLabel.setEnabled(false);
        serverLabel.setEnabled(false);
        protocolLabel.setEnabled(false);
    }
    
    private JPanel makePersonalPanel()
    {
        JPanel authentificationPanel = new JPanel();
        {
            authentificationPanel.setLayout(new MiksGridLayout(3,2,10,5));
            String docTitle = Config.getString("team.settings.personal");
            authentificationPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(docTitle),
                    BlueJTheme.generalBorder));
            authentificationPanel.setAlignmentX(LEFT_ALIGNMENT);
            
            JLabel userLabel = new JLabel(Config.getString("team.settings.user"));
            userField = new JTextField(fieldsize);
            JLabel passwordLabel = new JLabel(Config.getString("team.settings.password"));
            passwordField = new JPasswordField(fieldsize);
            groupLabel = new JLabel(Config.getString("team.settings.group"));
            groupField = new JTextField(fieldsize);
            
            userLabel.setMaximumSize(userLabel.getMinimumSize());
            userField.setMaximumSize(userField.getMinimumSize());
            passwordLabel.setMaximumSize(passwordLabel.getMinimumSize());
            passwordField.setMaximumSize(passwordField.getMinimumSize());
            groupLabel.setMaximumSize(groupLabel.getMinimumSize());
            groupField.setMaximumSize(groupField.getMinimumSize());
                        
            authentificationPanel.add(userLabel);
            authentificationPanel.add(userField);
            authentificationPanel.add(passwordLabel);
            authentificationPanel.add(passwordField);
            authentificationPanel.add(groupLabel);
            authentificationPanel.add(groupField);
            
        }
        return authentificationPanel;
    }
    
    private JPanel makeLocationPanel()
    {
        JPanel locationPanel = new JPanel(new BorderLayout(5, 0));
        {
            locationPanel.setLayout(new MiksGridLayout(3,2,10,5));
            String docTitle2 = Config.getString("team.settings.location");
            locationPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(docTitle2),
                    BlueJTheme.generalBorder));
            locationPanel.setAlignmentX(LEFT_ALIGNMENT);
            
            
            serverLabel = new JLabel(Config.getString("team.settings.server"));
            serverField = new JTextField(fieldsize);
            
            prefixLabel = new JLabel(Config.getString("team.settings.prefix"));
            prefixField = new JTextField(fieldsize);
            
            protocolLabel = new JLabel(Config.getString("team.settings.protocol"));
            protocolComboBox = new JComboBox(conTypes);
            protocolComboBox.setEditable(false);
            
            
            prefixLabel.setMaximumSize(prefixLabel.getMinimumSize());
            prefixField.setMaximumSize(prefixField.getMinimumSize());
            serverLabel.setMaximumSize(serverLabel.getMinimumSize());
            serverField.setMaximumSize(serverField.getMinimumSize());
            
            locationPanel.add(serverLabel);
            locationPanel.add(serverField);
            locationPanel.add(prefixLabel);
            locationPanel.add(prefixField);
            locationPanel.add(protocolLabel);
            locationPanel.add(protocolComboBox);
        }
        return locationPanel;
    }
    
    private void setupContent()
    {
        String user = teamSettingsController.getPropString("bluej.teamsettings.user");
        if (user != null){
            setUser(user);
        }
        String password = teamSettingsController.getPasswordString();
        if (password != null){
            setPassword(password);
        }
        String group = teamSettingsController.getPropString("bluej.teamsettings.groupname");
        if(group != null) {
            setGroup(group);
        }
        String prefix = teamSettingsController.getPropString("bluej.teamsettings.cvs.repositoryPrefix");
        if (prefix != null) {
            setPrefix(prefix);
        }
        String server = teamSettingsController.getPropString("bluej.teamsettings.cvs.server");
        if (server != null) {
            setServer(server);
        }
        String useAsDefault = teamSettingsController.getPropString("bluej.teamsettings.useAsDefault");
        if (useAsDefault != null) {
            setUseAsDefault(Boolean.getBoolean(useAsDefault));
        }
        String protocol = teamSettingsController.getPropString("bluej.teamsettings.cvs.protocol");
        if (protocol != null){
            setProtocol(protocol);
        }
    }
    
    /**
     * Check whether the "ok" button should be enabled or disabled according
     * to whether required fields have been provided.
     */
    private void checkOkEnabled()
    {
        boolean newOkEnabled = userField.getText().length() != 0;
        newOkEnabled &= serverField.getText().length() != 0;
        if (newOkEnabled != okEnabled) {
            okEnabled = newOkEnabled;
            teamSettingsDialog.setOkButtonEnabled(okEnabled);
        }
    }
    
    private void setUser(String user)
    {
        userField.setText(user);
    }
    
    private void setPassword(String password)
    {
        passwordField.setText(password);
    }
    
    private void setGroup(String group)
    {
        groupField.setText(group);
    }
    
    private void setPrefix(String prefix)
    {
        prefixField.setText(prefix);
    }
    
    private void setServer(String server)
    {
        serverField.setText(server);
    }
    
    private void setProtocol(String connectionType)
    {
        protocolComboBox.setSelectedItem(connectionType);
    }
    
    private void setUseAsDefault(boolean use)
    {
        useAsDefault.setSelected(use);
    }
    
    public String getUser()
    {
        return userField.getText();
    }
    
    public String getPassword()
    {
        return new String(passwordField.getPassword());
    }
    
    public String getGroup()
    {
        return groupField.getText();
    }
    
    public String getPrefix()
    {
        return prefixField.getText();
    }
    
    public String getServer()
    {
        return serverField.getText();
    }
    
    public String getProtocol()
    {
        return (String)protocolComboBox.getSelectedItem();
    }
    
    public boolean getUseAsDefault()
    {
        return useAsDefault.isSelected();
    }
    
}
