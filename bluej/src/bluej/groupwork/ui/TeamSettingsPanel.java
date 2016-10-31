/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.groupwork.ui;

import java.awt.FocusTraversalPolicy;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.actions.ValidateConnectionAction;
import bluej.utility.MiksGridLayout;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A panel for team settings.
 * 
 * @author fisker
 */
public class TeamSettingsPanel extends JPanel 
{
    private static final int fieldsize = 30;
    private TeamSettingsController teamSettingsController;
    private TeamSettingsDialog teamSettingsDialog;
    
    private JPanel locationPanel;
    private JTextField yourNameField;
    private JTextField yourEmailField;
    private JTextField userField;
    private JPasswordField passwordField;
    private JTextField groupField;
    private JTextField prefixField;
    private JComboBox serverTypeComboBox;
    private JTextField serverField;
    private JComboBox protocolComboBox;
    private JButton validateButton;
    private JCheckBox useAsDefault;
    private JTextField uriField;
    
    private JLabel serverTypeLabel;
    private JLabel groupLabel;
    private JLabel prefixLabel;
    private JLabel serverLabel;
    private JLabel protocolLabel;
    private JLabel uriLabel;
    
    private int selectedServerType = -1;
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
        validateButton = new JButton(new ValidateConnectionAction(
                Config.getString("team.settings.checkConnection"), this, dialog::asWindow));
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
        
        yourNameField.getDocument().addDocumentListener(changeListener);
        yourEmailField.getDocument().addDocumentListener(changeListener);
        userField.getDocument().addDocumentListener(changeListener);
        serverField.getDocument().addDocumentListener(changeListener);
        uriField.getDocument().addDocumentListener(changeListener);
        
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
        if (yourNameField.isEditable() && getYourName().length()==0){
            return delegate;
        } else if ((yourEmailField.isEditable() && getYourEmail().length()==0)){
            return new TeamPanelFocusPolicy(yourEmailField, delegate);
        } else if ( getUser().length() == 0) {
            return new TeamPanelFocusPolicy(userField, delegate);
        } else {
            return new TeamPanelFocusPolicy(passwordField, delegate);
        }
    }
    
    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol
     */
    public void disableRepositorySettings()
    {
        serverTypeComboBox.setEnabled(false);
        groupField.setEnabled(false);
        prefixField.setEnabled(false);
        serverField.setEnabled(false);
        protocolComboBox.setEnabled(false);
        uriField.setEnabled(false);
        
        if (uriField.isVisible() && uriField.getText().isEmpty()){
            //update uri.
            uriField.setText(TeamSettings.getURI(readProtocolString(), serverField.getText(), prefixField.getText()));
        }
        
        
        serverTypeLabel.setEnabled(false);
        groupLabel.setEnabled(false);
        prefixLabel.setEnabled(false);
        serverLabel.setEnabled(false);
        protocolLabel.setEnabled(false);
    }
    
    private JPanel makePersonalPanel()
    {
        JPanel authentificationPanel = new JPanel();
        {
            authentificationPanel.setLayout(new MiksGridLayout(5,2,10,5));
            String docTitle = Config.getString("team.settings.personal");
            authentificationPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(docTitle),
                    BlueJTheme.generalBorder));
            authentificationPanel.setAlignmentX(LEFT_ALIGNMENT);
            
            JLabel yourNameLabel = new JLabel(Config.getString("team.settings.yourName"));
            yourNameField = new JTextField(fieldsize);
            JLabel yourEmailLabel = new JLabel(Config.getString("team.settings.yourEmail"));
            yourEmailField = new JTextField(fieldsize);
            
            JLabel userLabel = new JLabel(Config.getString("team.settings.user"));
            userField = new JTextField(fieldsize);
            JLabel passwordLabel = new JLabel(Config.getString("team.settings.password"));
            passwordField = new JPasswordField(fieldsize);
            groupLabel = new JLabel(Config.getString("team.settings.group"));
            groupField = new JTextField(fieldsize);
            
            yourNameLabel.setMaximumSize(yourNameLabel.getMinimumSize());
            yourNameField.setMaximumSize(yourNameField.getMinimumSize());
            yourEmailLabel.setMaximumSize(yourEmailLabel.getMinimumSize());
            yourEmailField.setMaximumSize(yourEmailField.getMinimumSize());
            userLabel.setMaximumSize(userLabel.getMinimumSize());
            userField.setMaximumSize(userField.getMinimumSize());
            passwordLabel.setMaximumSize(passwordLabel.getMinimumSize());
            passwordField.setMaximumSize(passwordField.getMinimumSize());
            groupLabel.setMaximumSize(groupLabel.getMinimumSize());
            groupField.setMaximumSize(groupField.getMinimumSize());
                        
            authentificationPanel.add(yourNameLabel);
            authentificationPanel.add(yourNameField);
            authentificationPanel.add(yourEmailLabel);
            authentificationPanel.add(yourEmailField);
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
        locationPanel = new JPanel(new MiksGridLayout(0,2,10,5));
        {
            String docTitle2 = Config.getString("team.settings.location");
            locationPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(docTitle2),
                    BlueJTheme.generalBorder));
            locationPanel.setAlignmentX(LEFT_ALIGNMENT);
            
            serverTypeLabel = new JLabel(Config.getString("team.settings.serverType"));
            serverTypeComboBox = new JComboBox();
            List<TeamworkProvider> teamProviders = teamSettingsController.getTeamworkProviders();
            for (TeamworkProvider provider : teamProviders) {
                serverTypeComboBox.addItem(provider.getProviderName());
            }
            
            
            serverLabel = new JLabel(Config.getString("team.settings.server"));
            serverField = new JTextField(fieldsize);
            
            prefixLabel = new JLabel(Config.getString("team.settings.prefix"));
            prefixField = new JTextField(fieldsize);
            
            protocolLabel = new JLabel(Config.getString("team.settings.protocol"));
            protocolComboBox = new JComboBox();
            protocolComboBox.setEditable(false);
            
            uriLabel = new JLabel(Config.getString("team.settings.uri"));
            uriField = new JTextField(fieldsize);
            
            prefixLabel.setMaximumSize(prefixLabel.getMinimumSize());
            prefixField.setMaximumSize(prefixField.getMinimumSize());
            serverLabel.setMaximumSize(serverLabel.getMinimumSize());
            serverField.setMaximumSize(serverField.getMinimumSize());
            serverTypeLabel.setMaximumSize(serverTypeLabel.getMinimumSize());
            serverTypeComboBox.setMaximumSize(serverTypeComboBox.getMinimumSize());
            uriLabel.setMaximumSize(uriLabel.getMinimumSize());
            uriField.setMaximumSize(uriField.getMinimumSize());
            
            
            serverTypeComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    setProviderSettings();
                    //if Git provider selected, enable your name and your email
                    //fields.
                    if (getSelectedProvider().needsEmail() &&  getSelectedProvider().needsName()){
                        //Git was selected. Enable fields.
                        yourNameField.setEnabled(true);
                        yourEmailField.setEnabled(true);
                        useAsDefault.setVisible(false);
                        useAsDefault.setSelected(true); // on git we always save.
                        //for git, we will use a URI field.
                        if (serverLabel.getParent() == locationPanel){
                            locationPanel.remove(serverLabel);
                            locationPanel.remove(serverField);
                            locationPanel.remove(prefixLabel);
                            locationPanel.remove(prefixField);
                            locationPanel.remove(protocolLabel);
                            locationPanel.remove(protocolComboBox);
                            locationPanel.add(uriLabel);
                            locationPanel.add(uriField);
                            locationPanel.revalidate();
                            groupLabel.setEnabled(false);
                            groupField.setEnabled(false);

                        }
                    } else {
                        useAsDefault.setVisible(true);
                        //Git is not selected. Disable fields.
                        yourNameField.setEnabled(false);
                        yourEmailField.setEnabled(false);
                        //for svn, we will use the old layout.
                        if (serverLabel.getParent() != locationPanel){
                            locationPanel.remove(uriLabel);
                            locationPanel.remove(uriField);
                            locationPanel.add(serverLabel);
                            locationPanel.add(serverField);
                            locationPanel.add(prefixLabel);
                            locationPanel.add(prefixField);
                            locationPanel.add(protocolLabel);
                            locationPanel.add(protocolComboBox);
                            locationPanel.revalidate();
                            groupLabel.setEnabled(true);
                            groupField.setEnabled(true);
                        }
                    }
                    checkOkEnabled();
                    teamSettingsDialog.pack();//adjust window size.
                }
            });
            
            locationPanel.add(serverTypeLabel);
            locationPanel.add(serverTypeComboBox);
            locationPanel.add(serverLabel);
            locationPanel.add(serverField);
            locationPanel.add(prefixLabel);
            locationPanel.add(prefixField);
            locationPanel.add(protocolLabel);
            locationPanel.add(protocolComboBox);
        }
        return locationPanel;
    }
    
    /**
     * Empty the protocol selection box, then fill it with the available protocols
     * from the currently selected teamwork provider.
     */
    private void fillProtocolSelections()
    {
        int selected = serverTypeComboBox.getSelectedIndex();
        if (selected != selectedServerType) {
            selectedServerType = selected;
            protocolComboBox.removeAllItems();
            List<TeamworkProvider> teamProviders = teamSettingsController.getTeamworkProviders();
            TeamworkProvider provider = teamProviders.get(selected);
            String [] protocols = provider.getProtocols();
            for (int i = 0; i < protocols.length; i++) {
                protocolComboBox.addItem(protocols[i]);
            }
        }
    }
        
    private void setupContent()
    {
        String user = teamSettingsController.getPropString("bluej.teamsettings.user");
        if (user != null) {
            setUser(user);
        }
        
        String yourName = teamSettingsController.getPropString("bluej.teamsettings.yourName");
        if (yourName != null){
            setYourName(yourName);
        }
        
        String yourEmail = teamSettingsController.getPropString("bluej.teamsettings.yourEmail");
        if (yourEmail != null){
            setYourEmail(yourEmail);
        }
        
        String password = teamSettingsController.getPasswordString();
        if (password != null) {
            setPassword(password);
        }
        String group = teamSettingsController.getPropString("bluej.teamsettings.groupname");
        if(group != null) {
            setGroup(group);
        }
        String useAsDefault = teamSettingsController.getPropString("bluej.teamsettings.useAsDefault");
        if (useAsDefault != null) {
            setUseAsDefault(Boolean.getBoolean(useAsDefault));
        }
        
        String providerName = teamSettingsController.getPropString("bluej.teamsettings.vcs");
        // We always go through the providers.  If the user had no preference,
        // we select the first one, and update the email/name enabled states accordingly:
        List<TeamworkProvider> teamProviders = teamSettingsController.getTeamworkProviders();
        for (int index = 0; index < teamProviders.size(); index++) {
            TeamworkProvider provider = teamProviders.get(index);
            if (provider.getProviderName().equalsIgnoreCase(providerName)
                || (providerName == null && index == 0)) { // Select first if no stored preference
                serverTypeComboBox.setSelectedIndex(index);
                //checks if this provider needs your name and your e-mail.
                if (provider.needsEmail()){
                    if (teamSettingsController.getProject() != null){
                        //settings panel being open within a project.
                        //fill the data.
                        File respositoryRoot = teamSettingsController.getProject().getProjectDir();
                        yourEmailField.setText(provider.getYourEmailFromRepo(respositoryRoot));
                        yourEmailField.setEnabled(false);
                        yourNameField.setText(provider.getYourNameFromRepo(respositoryRoot));
                        yourNameField.setEnabled(false);
                        this.useAsDefault.setSelected(true); // on git we always save.
                    }

                }
                break;
            }
        }
        
        setProviderSettings();
    }
    
    /**
     * Set settings to provider-specific values (repository prefix, server, protocol).
     * The values are remembered on a per-provider basis; this sets the fields to show
     * the remembered values for the selected provider. 
     */
    private void setProviderSettings()
    {
        String keyBase = "bluej.teamsettings."
            + getSelectedProvider().getProviderName().toLowerCase() + "."; 
        
        String prefix = teamSettingsController.getPropString(keyBase + "repositoryPrefix");
        if (prefix != null) {
            setPrefix(prefix);
        }
        String server = teamSettingsController.getPropString(keyBase + "server");
        if (server != null) {
            setServer(server);
        }
        
        fillProtocolSelections();
        
        String protocol = readProtocolString();
        if (protocol != null){
            setProtocol(protocol);
        }
    }
    
    private String readProtocolString()
    {
        String keyBase = "bluej.teamsettings."
            + getSelectedProvider().getProviderName().toLowerCase() + "."; 
        return teamSettingsController.getPropString(keyBase + "protocol");
    }
    
    /**
     * Check whether the "ok" button should be enabled or disabled according
     * to whether required fields have been provided.
     */
    private void checkOkEnabled()
    {
        boolean newOkEnabled = (userField.getText().length() != 0);

        if (yourEmailField.isEnabled() && yourNameField.isEnabled())
        {
            newOkEnabled &= (yourEmailField.getText().length() != 0) && (yourEmailField.getText().contains("@"));
            newOkEnabled &= yourNameField.getText().length() != 0;
        }
        if (uriField.isVisible())
        {
            newOkEnabled &= uriField.getText().length() != 0;
        }
        else
        {
            newOkEnabled &= serverField.getText().length() != 0;
        }
        if (newOkEnabled != okEnabled) {
            okEnabled = newOkEnabled;
            teamSettingsDialog.setOkButtonEnabled(okEnabled);
        }
    }
    
    private void setUser(String user)
    {
        userField.setText(user);
    }
    
    private void setYourName(String yourName)
    {
        yourNameField.setText(yourName);
    }
    
    private void setYourEmail(String yourEmail)
    {
        yourEmailField.setText(yourEmail);
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
    
    /**
     * Set the protocol to that identified by the given protocol key.
     */
    private void setProtocol(String protocolKey)
    {
        String protocolLabel = getSelectedProvider().getProtocolLabel(protocolKey);
        protocolComboBox.setSelectedItem(protocolLabel);
    }
    
    private void setUseAsDefault(boolean use)
    {
        useAsDefault.setSelected(use);
    }
    
    public TeamworkProvider getSelectedProvider()
    {
        int selected = serverTypeComboBox.getSelectedIndex();
        return teamSettingsController.getTeamworkProviders().get(selected);
    }
    
    private String getUser()
    {
        return userField.getText();
    }
    
    private String getPassword()
    {
        return new String(passwordField.getPassword());
    }
    
    private String getGroup()
    {
        //DCVS does not have group.
        if (getSelectedProvider().needsEmail()){
            return "";
        }
        return groupField.getText();
    }
    
    private String getPrefix()
    {
        if (getSelectedProvider().needsEmail()) {
            try {
                URI uri = new URI(uriField.getText());
                return uri.getPath();
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        return prefixField.getText();
    }
    
    private String getServer()
    {
        if (getSelectedProvider().needsEmail()) {
            try {
                URI uri = new URI(uriField.getText());
                return uri.getHost();
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        return serverField.getText();
    }
    
    private String getProtocolKey()
    {
        if (getSelectedProvider().needsEmail()) {
            try {
                URI uri = new URI(uriField.getText());
                return uri.getScheme();
            } catch (URISyntaxException ex) {
                return null;
            }
        }
        int protocol = protocolComboBox.getSelectedIndex();
        return getSelectedProvider().getProtocolKey(protocol);
    }
    
    public boolean getUseAsDefault()
    {
        return useAsDefault.isSelected();
    }
    
    private String getYourName(){
        return yourNameField.getText();
    }
    
    private String getYourEmail(){
        return yourEmailField.getText();
    }
    
    public TeamSettings getSettings() {
        TeamSettings result = new TeamSettings(getSelectedProvider(), getProtocolKey(),
                getServer(), getPrefix(), getGroup(), getUser(), getPassword());
        result.setYourEmail(getYourEmail());
        result.setYourName(getYourName());
        return result;
    }
}
