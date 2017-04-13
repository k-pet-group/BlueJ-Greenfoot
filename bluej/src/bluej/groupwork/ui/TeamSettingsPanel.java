/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017  Michael Kolling and John Rosenberg
 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.actions.ValidateConnectionAction;
import bluej.utility.javafx.HorizontalRadio;
import bluej.utility.javafx.JavaFXUtil;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A panel for team settings.
 * 
 * @author fisker
 */
public class TeamSettingsPanel extends VBox
{
    private static final int fieldsize = 30;
    private TeamSettingsController teamSettingsController;
    private TeamSettingsDialog teamSettingsDialog;

//  private Label serverTypeLabel;
    private Label groupLabel;
    private final HorizontalRadio<ServerType> serverTypes;

    private GridPane personalPane = new GridPane();
    private GridPane locationPane = new GridPane();

    private TextField yourNameField;
    private TextField yourEmailField;
    private TextField userField;
    private PasswordField passwordField;
    private TextField groupField;
    private TextField prefixField;
    private TextField serverField;
    private ComboBox protocolComboBox;
    private Button validateButton;

    private CheckBox useAsDefault;
    private TextField uriField;
    private Label prefixLabel;
    private Label serverLabel;
    private Label protocolLabel;
    private Label uriLabel;

    private ServerType selectedServerType = null;
    private boolean okEnabled = true;
    /**
     *
     */
    public enum ServerType
    {
        Subversion,
        Git

    }

//    private final DialogPaneAnimateError dialogPane;
//    private final Label errorLabel;

    String[] personalLabels = {
            "team.settings.yourName",
            "team.settings.yourEmail",
            "team.settings.user",
            "team.settings.password",
            "team.settings.group"
    };

    String[] locationLabels = {
            "team.settings.prefix",
            "team.settings.uri",
            "team.settings.protocol"
    };

    public TeamSettingsPanel(TeamSettingsController teamSettingsController, TeamSettingsDialog dialog)
    {
        this.teamSettingsController = teamSettingsController;
        this.teamSettingsDialog = dialog;

//        errorLabel = JavaFXUtil.withStyleClass(new Label(), "dialog-error-label");

        JavaFXUtil.addStyleClass(this, "new-class-dialog");//

        serverTypes = new HorizontalRadio(Arrays.asList(ServerType.Subversion, ServerType.Git));
        serverTypes.select(ServerType.Subversion);

        HBox langBox = new HBox();
        JavaFXUtil.addStyleClass(langBox, "new-class-dialog-hbox");//
        langBox.getChildren().add(new Label(Config.getString("team.settings.server")));
        langBox.getChildren().addAll(serverTypes.getButtons());
        langBox.setAlignment(Pos.BASELINE_LEFT);
        this.getChildren().add(langBox);

        prepareLocationPane(locationPane, serverTypes.selectedProperty().get()); // addPane(locationLabels);
        preparePersonalPane(personalPane, serverTypes.selectedProperty().get()); // addPane(personalLabels);
        this.getChildren().addAll(new Label("Location"), locationPane,
                                  new Separator(),
                                  new Label("Personal"), personalPane
//                , errorLabel
        );

        JavaFXUtil.addChangeListenerPlatform(serverTypes.selectedProperty(), type -> {
            prepareLocationPane(locationPane, type); // addPane(locationLabels);
            preparePersonalPane(personalPane, type); // addPane(personalLabels);
//            update();
        });


        useAsDefault = new CheckBox(Config.getString("team.settings.rememberSettings"));
        getChildren().add(useAsDefault);
        ValidateConnectionAction validateConnectionAction = new ValidateConnectionAction(
                Config.getString("team.settings.checkConnection"), this, dialog::getOwner);
        validateButton = new Button(validateConnectionAction.getName());
        validateButton.setOnAction(event -> validateConnectionAction.actionPerformed(null));
        getChildren().add(validateButton);
        
        JavaFXUtil.addChangeListener(yourNameField.textProperty(), s -> checkOkEnabled());
        JavaFXUtil.addChangeListener(yourEmailField.textProperty(), s -> checkOkEnabled());
        JavaFXUtil.addChangeListener(userField.textProperty(), s -> checkOkEnabled());
        JavaFXUtil.addChangeListener(serverField.textProperty(), s -> checkOkEnabled());
        JavaFXUtil.addChangeListener(uriField.textProperty(), s -> checkOkEnabled());


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
//    public FocusTraversalPolicy getTraversalPolicy(FocusTraversalPolicy delegate)
//    {
//        if (yourNameField.isEditable() && getYourName().length()==0){
//            return delegate;
//        } else if ((yourEmailField.isEditable() && getYourEmail().length()==0)){
//            return new TeamPanelFocusPolicy(yourEmailField, delegate);
//        } else if ( getUser().length() == 0) {
//            return new TeamPanelFocusPolicy(userField, delegate);
//        } else {
//            return new TeamPanelFocusPolicy(passwordField, delegate);
//        }
//    }
    
    /**
     * Disable the fields used to specify the repository:
     * group, prefix, server and protocol
     */
    public void disableRepositorySettings()
    {
        groupField.setDisable(true);
        prefixField.setDisable(true);
        serverField.setDisable(true);
        protocolComboBox.setDisable(true);
        uriField.setDisable(true);
        
        if (uriField.isVisible() && uriField.getText().isEmpty()){
            //update uri.
            uriField.setText(TeamSettings.getURI(readProtocolString(), serverField.getText(), prefixField.getText()));
        }

        groupLabel.setDisable(true);
        prefixLabel.setDisable(true);
        serverLabel.setDisable(true);
        protocolLabel.setDisable(true);
    }

    private GridPane addPane(String[] labels)
    {
        GridPane gridPane = new GridPane();
//        JavaFXUtil.addStyleClass(gridPane, ".call-dialog-content .grid");

        List<TextField> fields = new ArrayList<>();

        for (int i = 0; i < labels.length; i++) {
            Label label = new Label(Config.getString(labels[i]));
            label.setPrefWidth(100);
            gridPane.add(label, 0, i);

            TextField field = new TextField();
            fields.add(field);
            JavaFXUtil.addChangeListener(field.textProperty(), text -> updateOKButton());
            gridPane.add(field, 1, i);
        }

        return gridPane;
    }

    private void preparePersonalPane(GridPane authentificationPanel, ServerType type)
    {
//            String docTitle = Config.getString("team.settings.personal");

            Label yourNameLabel = new Label(Config.getString("team.settings.yourName"));
            yourNameField = new TextField();
            yourNameField.setPrefWidth(fieldsize);

            Label yourEmailLabel = new Label(Config.getString("team.settings.yourEmail"));
            yourEmailField = new TextField();
            yourEmailField.setPrefWidth(fieldsize);

            Label userLabel = new Label(Config.getString("team.settings.user"));
            userField = new TextField();
            userField.setPrefWidth(fieldsize);

            Label passwordLabel = new Label(Config.getString("team.settings.password"));
            passwordField = new PasswordField();
            passwordField.setPrefWidth(fieldsize);

            groupLabel = new Label(Config.getString("team.settings.group"));
            groupField = new TextField();
            groupField.setPrefWidth(fieldsize);
            
            yourNameLabel.setMaxSize(yourNameLabel.getMinWidth(), yourNameLabel.getMinHeight());
            yourNameField.setMaxSize(yourNameField.getMinWidth(), yourNameField.getMinHeight());
            yourEmailLabel.setMaxSize(yourEmailLabel.getMinWidth(), yourEmailLabel.getMinHeight());
            yourEmailField.setMaxSize(yourEmailField.getMinWidth(), yourEmailField.getMinHeight());

            userLabel.setMaxSize(userLabel.getMinWidth(), userLabel.getMinHeight());
            userField.setMaxSize(userField.getMinWidth(), userField.getMinHeight());
            passwordLabel.setMaxSize(passwordLabel.getMinWidth(), passwordLabel.getMinHeight());
            passwordField.setMaxSize(passwordField.getMinWidth(), passwordField.getMinHeight());
            groupLabel.setMaxSize(groupLabel.getMinWidth(), groupLabel.getMinHeight());
            groupField.setMaxSize(groupField.getMinWidth(), groupField.getMinHeight());

            authentificationPanel.getChildren().add(yourNameLabel);
            authentificationPanel.getChildren().add(yourNameField);
            authentificationPanel.getChildren().add(yourEmailLabel);
            authentificationPanel.getChildren().add(yourEmailField);
            authentificationPanel.getChildren().add(userLabel);
            authentificationPanel.getChildren().add(userField);
            authentificationPanel.getChildren().add(passwordLabel);
            authentificationPanel.getChildren().add(passwordField);
            authentificationPanel.getChildren().add(groupLabel);
            authentificationPanel.getChildren().add(groupField);
    }

    /*
    if (type.equals(ServerType.Git))
                    {
                        locationPane.getChildren().stream()
                                .filter(node -> node instanceof TextField)
                                .findFirst().ifPresent(node -> node.setVisible(false));

                        locationPane.getChildren().stream()
                                .filter(node -> node instanceof Label)
                                .findFirst().ifPresent(node -> node.setVisible(false));
                    }
                    if (type.equals(ServerType.SVN))
                    {
                        locationPane.getChildren().stream()
                                .filter(node -> node instanceof TextField)
                                .findFirst().ifPresent(node -> node.setVisible(true));

                        locationPane.getChildren().stream()
                                .filter(node -> node instanceof Label)
                                .findFirst().ifPresent(node -> node.setVisible(true));
                    }
                    update()
                    hideError();
                    updateOKButton();
                });
    */
    
    private void prepareLocationPane(GridPane locationPanel, ServerType type)
    {

        locationPanel.getChildren().clear();
//        String docTitle2 = Config.getString("team.settings.location");

//        List<TeamworkProvider> teamProviders = teamSettingsController.getTeamworkProviders();
//        for (TeamworkProvider provider : teamProviders) {
//            serverTypeComboBox.addItem(provider.getProviderName());
//        }

        serverLabel = new Label(Config.getString("team.settings.server"));
        serverField = new TextField();
        serverField.setPrefWidth(fieldsize);

        prefixLabel = new Label(Config.getString("team.settings.prefix"));
        prefixField = new TextField();
        prefixField.setPrefWidth(fieldsize);

        protocolLabel = new Label(Config.getString("team.settings.protocol"));
        protocolComboBox = new ComboBox();
        protocolComboBox.setEditable(false);

        uriLabel = new Label(Config.getString("team.settings.uri"));
        uriField = new TextField();
        uriField.setPrefWidth(fieldsize);

        prefixLabel.setMaxSize(prefixLabel.getMinWidth(), prefixLabel.getMinHeight());
        prefixField.setMaxSize(prefixField.getMinWidth(), prefixField.getMinHeight());
        serverLabel.setMaxSize(serverLabel.getMinWidth(), serverLabel.getMinHeight());
        serverField.setMaxSize(serverField.getMinWidth(), serverField.getMinHeight());
        uriLabel.setMaxSize(uriLabel.getMinWidth(), uriLabel.getMinHeight());
        uriField.setMaxSize(uriField.getMinWidth(), uriField.getMinHeight());

        JavaFXUtil.addChangeListener(serverTypes.selectedProperty(), event -> {
            setProviderSettings();
            //if Git provider selected, enable your name and your email
            //fields.
            if (getSelectedProvider().needsEmail() &&  getSelectedProvider().needsName()){
                //Git was selected. Enable fields.
                yourNameField.setDisable(false);
                yourEmailField.setDisable(false);
                useAsDefault.setVisible(false);
                useAsDefault.setSelected(true); // on git we always save.
                //for git, we will use a URI field.
                if (serverLabel.getParent() == locationPanel){
                    locationPanel.getChildren().remove(serverLabel);
                    locationPanel.getChildren().remove(serverField);
                    locationPanel.getChildren().remove(prefixLabel);
                    locationPanel.getChildren().remove(prefixField);
                    locationPanel.getChildren().remove(protocolLabel);
                    locationPanel.getChildren().remove(protocolComboBox);
                    locationPanel.getChildren().add(uriLabel);
                    locationPanel.getChildren().add(uriField);
//                            locationPanel.revalidate();
                    groupLabel.setDisable(true);
                    groupField.setDisable(true);

                }
            } else {
                useAsDefault.setVisible(true);
                //Git is not selected. Disable fields.
                yourNameField.setDisable(true);
                yourEmailField.setDisable(true);
                //for svn, we will use the old layout.
                if (serverLabel.getParent() != locationPanel){
                    locationPanel.getChildren().remove(uriLabel);
                    locationPanel.getChildren().remove(uriField);
                    locationPanel.getChildren().add(serverLabel);
                    locationPanel.getChildren().add(serverField);
                    locationPanel.getChildren().add(prefixLabel);
                    locationPanel.getChildren().add(prefixField);
                    locationPanel.getChildren().add(protocolLabel);
                    locationPanel.getChildren().add(protocolComboBox);
//                            locationPanel.revalidate();
                    groupLabel.setDisable(false);
                    groupField.setDisable(false);
                }
            }
            checkOkEnabled();
//                    teamSettingsDialog.pack();//adjust window size.
        });

        locationPanel.getChildren().add(serverLabel);
        locationPanel.getChildren().add(serverField);
        locationPanel.getChildren().add(prefixLabel);
        locationPanel.getChildren().add(prefixField);
        locationPanel.getChildren().add(protocolLabel);
        locationPanel.getChildren().add(protocolComboBox);
    }
    
    /**
     * Empty the protocol selection box, then fill it with the available protocols
     * from the currently selected teamwork provider.
     */
    private void fillProtocolSelections()
    {
        ServerType type = serverTypes.selectedProperty().get();
        if (type != selectedServerType) {
            selectedServerType = type;
            protocolComboBox.getItems().clear();
            List<TeamworkProvider> teamProviders = teamSettingsController.getTeamworkProviders();

            TeamworkProvider provider = teamProviders.stream()
                    .filter(teamworkProvider -> teamworkProvider.getProviderName().equals(type.name()))
                    .findAny().get();

            String [] protocols = provider.getProtocols();
            for (int i = 0; i < protocols.length; i++) {
                protocolComboBox.getItems().add(protocols[i]);
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
                serverTypes.select(ServerType.valueOf(teamProviders.get(index).getProviderName()));
                //checks if this provider needs your name and your e-mail.
                if (provider.needsEmail()){
                    if (teamSettingsController.getProject() != null){
                        //settings panel being open within a project.
                        //fill the data.
                        File respositoryRoot = teamSettingsController.getProject().getProjectDir();
                        yourEmailField.setText(provider.getYourEmailFromRepo(respositoryRoot));
                        yourEmailField.setDisable(true);
                        yourNameField.setText(provider.getYourNameFromRepo(respositoryRoot));
                        yourNameField.setDisable(true);
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

        if (!yourEmailField.isDisabled() && !yourNameField.isDisable())
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
        protocolComboBox.getSelectionModel().select(protocolLabel);
    }
    
    private void setUseAsDefault(boolean use)
    {
        useAsDefault.setSelected(use);
    }
    
    public TeamworkProvider getSelectedProvider()
    {
        return teamSettingsController.getTeamworkProviders().stream()
                .filter(provider -> provider.getProviderName().equals(serverTypes.selectedProperty().get().name()))
                .findAny().get();
    }
    
    private String getUser()
    {
        return userField.getText();
    }

    private String getPassword()
    {
        return passwordField.getText();
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
        int protocol = protocolComboBox.getSelectionModel().getSelectedIndex();
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

    private void updateOKButton()
    {
//        boolean present = locationPane.getChildren().stream()
//                .filter(node -> node instanceof TextField)
//                .anyMatch(node -> ((TextField)node).textProperty().isEmpty().get());
//
//        if (present) {
//            showError("Field path is empty", true);
//        }
//        else {
//            hideError();
//        }
//
//        setOKEnabled(!present);
    }

    private void hideError()
    {
//        errorLabel.setText("");
//        JavaFXUtil.setPseudoclass("bj-dialog-error", false, nameField);
    }

    private void showError(String error, boolean problemIsName)
    {
        // show error, highlight field red if problem is name:
//        errorLabel.setText(error);
//        JavaFXUtil.setPseudoclass("bj-dialog-error", problemIsName, nameField);
    }

    private void setOKEnabled(boolean okEnabled)
    {
        teamSettingsDialog.getOkButton().setDisable(!okEnabled);
    }


    class specialTextField
    {
        public TextField field;
        public Label label;
        public int special = 0;

        public specialTextField(String name)
        {
            label = new Label(name);
            field = new TextField();
            field.setPromptText(name);
        }

        public specialTextField(String name, int special)
        {
            this(name);
            this.special = special;
        }
    }
}
