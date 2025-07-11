/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017,2018,2019,2020,2021  Michael Kolling and John Rosenberg

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

import java.util.Arrays;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import bluej.Config;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamworkProvider;
import bluej.groupwork.actions.ValidateConnectionAction;
import bluej.utility.javafx.JavaFXUtil;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A panel for team settings.
 * 
 * @author fisker
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class TeamSettingsPanel extends VBox
{
    private TeamSettingsController teamSettingsController;
    private TeamworkProvider teamworkProvider;
    private TeamSettingsDialog teamSettingsDialog;

    private GridPane personalPane;
    private GridPane locationPane;

    private Label uriLabel       = new Label(Config.getString("team.settings.uri"));
    private Label branchLabel    = new Label(Config.getString("team.settings.branch"));

    private Label yourNameLabel  = new Label(Config.getString("team.settings.yourName"));
    private Label yourEmailLabel = new Label(Config.getString("team.settings.yourEmail"));
    private Label userLabel      = new Label(Config.getString("team.settings.user"));
    private Label passwordLabel  = new Label(Config.getString("team.settings.password"));

    private final SimpleStringProperty serverField = new SimpleStringProperty();
    private final SimpleIntegerProperty portField = new SimpleIntegerProperty();
    private final SimpleStringProperty prefixField = new SimpleStringProperty();
    private final TextField uriField = new TextField();
    private final TextField branchField = new TextField();

    private final TextField yourNameField = new TextField();
    private final TextField yourEmailField = new TextField();
    private final TextField userField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final HBox savePasswordHBox = new HBox();
    private final CheckBox savePasswordCheckBox = new CheckBox();

    private final Label accessTokenHint = new Label(Config.getString("team.settings.accessTokenHint"));

    /** identifies which field is the primary server information field */
    private TextField locationPrimaryField;
    /** identifiers which field is the primary personal information field */
    private TextField personalPrimaryField;

    private boolean isShareAction = false;

    public TeamSettingsPanel(TeamSettingsController teamSettingsController, TeamSettingsDialog dialog, boolean isShareAction)
    {
        this.teamSettingsController = teamSettingsController;
        this.teamworkProvider = teamSettingsController.getTeamworkProvider();

        this.teamSettingsDialog = dialog;

        this.isShareAction = isShareAction;

        JavaFXUtil.addStyleClass(this, "panel");

        // The part for "save password". Because of some weird behaviour of JavaFX rendering the checkbox's label,
        // the checkbox has no label of its own, but a separate label component is used instead.
        Label checkboxLabel = new Label(Config.getString("team.settings.savepwd"));
        checkboxLabel.setLabelFor(savePasswordCheckBox);

        ImageView infoIcon = new ImageView(Config.getFixedImageAsFXImage("info.png"));
        savePasswordHBox.getChildren().addAll(savePasswordCheckBox, checkboxLabel, infoIcon);
        savePasswordHBox.setAlignment(Pos.CENTER_LEFT);
        JavaFXUtil.addStyleClass(savePasswordHBox, "pwd-hbox");
        Tooltip infoTooltip = new Tooltip(Config.getString("team.settings.savepwd.details"));
        JavaFXUtil.addStyleClass(infoTooltip, "team-settings-tooltip");
        Tooltip.install(savePasswordHBox, infoTooltip);

        JavaFXUtil.addChangeListenerPlatform(passwordField.textProperty(), newValue ->
            {
                int newValueLength = newValue.length();
                if (newValueLength == 0)
                {
                    savePasswordCheckBox.setSelected(false);
                }
                savePasswordCheckBox.setDisable(newValueLength == 0);
                checkboxLabel.setDisable(newValueLength == 0);
                infoIcon.setDisable(newValueLength == 0);
            }
        );
        // end of the part for "save password"

        locationPane = createGridPane();
        personalPane = createGridPane();
        preparePanes();

        ValidateConnectionAction validateConnectionAction = new ValidateConnectionAction(this, dialog::getOwner);
        Button validateButton = new Button();
        validateConnectionAction.useButton(teamSettingsController.getProject(), validateButton);

        StackPane hintContainer = new StackPane(accessTokenHint);
        StackPane.setAlignment(accessTokenHint, Pos.CENTER_LEFT);
        getChildren().addAll(createPropertiesContainer(Config.getString("team.settings.location"), locationPane),
                             createPropertiesContainer(Config.getString("team.settings.personal"), personalPane),
                             hintContainer,
                             savePasswordHBox,
                             validateButton);

        setupContent();
        updateOKButtonBinding();

        accessTokenHint.setWrapText(true);
        accessTokenHint.setPrefWidth(450);

        hintContainer.setMinHeight(Region.USE_PREF_SIZE);
        JavaFXUtil.addChangeListenerAndCallNow(uriField.textProperty(), uri -> {
            boolean needsToken = uri.toLowerCase().contains("github.com") || uri.toLowerCase().contains("gitlab");
            accessTokenHint.setVisible(needsToken);
            passwordLabel.setText(Config.getString(needsToken ? "team.settings.accessToken" : "team.settings.password"));
        });
    }

    /**
     * Request focus to whatever field seems the most likely to be filled out next.
     */
    public void doRequestFocus()
    {
        if (locationPrimaryField != null && locationPrimaryField.getText().isEmpty())
        {
            // If the location hasn't been specified, that should be first:
            locationPrimaryField.requestFocus();
        }
        else if (personalPrimaryField.getText().isEmpty())
        {
            // Otherwise, if the name/username hasn't been set, select that:
            personalPrimaryField.requestFocus();
        }
        else
        {
            // Otherwise, select the password field:
            passwordField.requestFocus();
        }
    }

    private GridPane createGridPane()
    {
        GridPane pane = new GridPane();
        pane.getStyleClass().add("grid");

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPrefWidth(102);
        // Second column gets any extra width
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPrefWidth(260);
        column2.setHgrow(Priority.ALWAYS);
        pane.getColumnConstraints().addAll(column1, column2);

        return pane;
    }

    private Pane createPropertiesContainer(String title, Pane gridPane)
    {
        VBox container = new VBox();
        container.setSpacing(-5);
        container.getChildren().addAll(new Label(title), gridPane);
        return container;
    }

    private void preparePanes()
    {
        prepareLocationPane();
        preparePersonalPane();

        setProviderSettings();
    }

    private void preparePersonalPane()
    {
        personalPane.getChildren().clear();
        personalPane.addRow(0, yourNameLabel, yourNameField);
        personalPane.addRow(1, yourEmailLabel, yourEmailField);
        personalPane.addRow(2, userLabel, userField);
        personalPane.addRow(3, passwordLabel, passwordField);
        personalPrimaryField = yourNameField;
    }

    private void prepareLocationPane()
    {
        locationPane.getChildren().clear();
        locationPane.addRow(0, uriLabel, uriField);
        locationPrimaryField = uriField;
        if(!this.isShareAction)
            locationPane.addRow(1, branchLabel, branchField);
        branchField.setPromptText(Config.getString("team.settings.defaultBranch"));
    }

    /**
     * Set a text field's text property, adjusting null to the empty string.
     * 
     * @param field  the text field to set the text for
     * @param value  the value to set the text property to, or null for the empty string
     */
    private void setTextFieldText(TextField field, String value)
    {
        field.setText(value == null ? "" : value);
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

        // There is no need for an additional property: if the password was saved, then the checkbox was checked
        String savePassword = teamSettingsController.getPropString("bluej.teamsettings.savedpwd");
        setSavePassword(savePassword != null);

        String providerName = teamSettingsController.getPropString("bluej.teamsettings.vcs");
        if ((teamworkProvider.getProviderName().equalsIgnoreCase(providerName)
            || (providerName == null)) && teamSettingsController.getProject() != null)
        {
            File respositoryRoot = teamSettingsController.getProject().getProjectDir();
            setTextFieldText(yourEmailField, teamworkProvider.getYourEmailFromRepo(respositoryRoot));
            setTextFieldText(yourNameField, teamworkProvider.getYourNameFromRepo(respositoryRoot));
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
            +  teamworkProvider.getProviderName().toLowerCase() + ".";

        String prefix = teamSettingsController.getPropString(keyBase + "repositoryPrefix");
        if (prefix != null) {
            setPrefix(prefix);
        }
        String server = teamSettingsController.getPropString(keyBase + "server");
        if (server != null) {
            setServer(server);
        }

        int port = teamSettingsController.getPropInt(keyBase + "port");
        if(port > 0) {
            setPort(port);
        }

        String branch = teamSettingsController.getPropString(keyBase + "branch");
        if (branch != null) {
            setBranch(branch);
        }
    }

    private String readProtocolString()
    {
        String keyBase = "bluej.teamsettings."
            + teamworkProvider.getProviderName().toLowerCase() + ".";
        return teamSettingsController.getPropString(keyBase + "protocol");
    }

    private void setBranch(String branch){
        branchField.setText(branch);
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

    private void setSavePassword(boolean savePassword)
    {
        savePasswordCheckBox.setSelected(savePassword);
    }

    private void setPrefix(String prefix)
    {
        prefixField.setValue(prefix);
    }

    private void setServer(String server)
    {
        serverField.setValue(server);
    }

    private void setPort(int port) { portField.setValue(port); }

    public TeamworkProvider getProvider()
    {
        return teamworkProvider;
    }

    private String getUser()
    {
        return userField.getText();
    }

    private String getPassword()
    {
        return passwordField.getText();
    }

    private Boolean getSavePassword()
    {
        return savePasswordCheckBox.isSelected();
    }

    private String getPrefix()
    {
        try {
            URI uri = new URI(uriField.getText());
            return uri.getPath();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String getBranch(){
        //the branch is an optional setting. So empty/null value is to be understood as default branch
        return branchField.getText();
    }


    private String getServer()
    {
        try {
            URI uri = new URI(uriField.getText());
            return uri.getHost();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private int getPort()
    {
        try {
            URI uri = new URI(uriField.getText());
            return uri.getPort();
        } catch (URISyntaxException ex) {
            return -1;
        }
    }

    private String getProtocolKey()
    {
        try {
            URI uri = new URI(uriField.getText());
            return uri.getScheme();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String getYourName()
    {
        return yourNameField.getText();
    }

    private String getYourEmail()
    {
        return yourEmailField.getText();
    }

    public TeamSettings getSettings()
    {
        TeamSettings result = new TeamSettings(getProtocolKey(),
                getServer(), getPort(), getPrefix(), getBranch(), getUser(), getPassword(), getSavePassword());
        result.setYourEmail(getYourEmail());
        result.setYourName(getYourName());
        return result;
    }

    /**
     * Check whether the "ok" button should be enabled or disabled according
     * to whether required fields have been provided.
     */
    private void updateOKButtonBinding()
    {
        teamSettingsDialog.getOkButton().disableProperty().unbind();

        BooleanBinding disabled = userField.textProperty().isEmpty()
                .or(uriField.textProperty().isEmpty())
                .or(yourNameField.textProperty().isEmpty())
                .or(yourEmailField.textProperty().isEmpty())
                .or(Bindings.createBooleanBinding(() -> !yourEmailField.getText().contains("@"), yourEmailField.textProperty()));

        teamSettingsDialog.getOkButton().disableProperty().bind(disabled);
    }

    /**
     * Disable the fields used to specify the repository:
     * prefix, server, branch and protocol
     */
    public void disableRepositorySettings()
    {
        branchField.setDisable(true);
        uriField.setDisable(true);

        if (uriField.isVisible() && uriField.getText().isEmpty()){
            //update uri.
            int port  = portField.get();
            uriField.setText(TeamSettings.getURI(readProtocolString(), serverField.getValue(), port, prefixField.getValue()));
        }

        branchLabel.setDisable(true);
    }
}
