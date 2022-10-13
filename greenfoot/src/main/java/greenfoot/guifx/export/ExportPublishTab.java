/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2017,2018,2022  Poul Henriksen and Michael Kolling
 
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
package greenfoot.guifx.export;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.FXWorker;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;

import static greenfoot.export.Exporter.ExportFunction;
import greenfoot.export.mygame.ExistingScenarioChecker;
import greenfoot.export.mygame.ExportInfo;
import greenfoot.export.mygame.MyGameClient;
import greenfoot.export.mygame.ScenarioInfo;
import greenfoot.export.ScenarioSaver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.binding.BooleanBinding;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.apache.http.conn.ConnectTimeoutException;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;

/**
 * Pane used for exporting to Greenfoot Gallery
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class ExportPublishTab extends ExportTab
{
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 70;

    private static final String serverURL = ensureTrailingSlash(
            Config.getPropString("greenfoot.gameserver.address", "https://www.greenfoot.org/"));
    private static final String createAccountUrl =
            Config.getPropString("greenfoot.gameserver.createAccount.address",
                    "https://www.greenfoot.org/users/new");
    private static final String serverName =
            Config.getPropString("greenfoot.gameserver.name", "Greenfoot Website");

    private static final String helpLine = Config.getString("export.publish.help") + " " + serverName;
    private static final String WITH_SOURCE_TAG = "with-source";

    private final Label emptyLabel = new Label();
    private final Label titleLabel = new Label(Config.getString("export.publish.title"));
    private final Label updateLabel = new Label(Config.getString("export.publish.update"));
    private final Label shortDescriptionLabel = new Label(Config.getString("export.publish.shortDescription"));
    private final Label longDescriptionLabel = new Label(Config.getString("export.publish.longDescription"));

    private TextField titleField;
    private TextField shortDescriptionField;
    private TextArea longDescriptionArea;
    private TextArea updateArea;
    private TextField urlField;
    private TextField userNameField;
    private PasswordField passwordField;
    private ImageEditPane imagePane;
    private CheckBox includeSource;
    private CheckBox keepScenarioScreenshot;
    private GridPane titleAndDescPane;
    private ScrollPane updatePane;
    private ScrollPane description;

    private CheckBox[] popTags = new CheckBox[7];
    private TextArea tagArea;
    private Project project;
    private boolean firstActivation = true;

    private String publishedUserName;

    private ExistingScenarioChecker scenarioChecker;
    private boolean update = false;
    private final ExportDialog exportDialog;
    private final ScenarioSaver scenarioSaver;

    private BooleanBinding userNameValidity;
    private BooleanBinding passwordValidity;
    private BooleanBinding titleValidity;

    /**
     * Creates a new instance of ExportPublishTab
     *
     * @param project       The project that will be shared.
     * @param exportDialog  The export dialog containing this tab.
     * @param scenarioSaver The listener that will enable us to save the scenario when exporting.
     * @param scenarioInfo  The previously stored scenario info in the properties file.
     */
    public ExportPublishTab(Project project, ExportDialog exportDialog,
                            ScenarioSaver scenarioSaver, ScenarioInfo scenarioInfo)
    {
        super(scenarioInfo, "export-publish.png");
        this.project = project;
        this.exportDialog = exportDialog;
        this.scenarioSaver = scenarioSaver;

        buildContentPane();
        applySharedStyle();
        getContent().getStyleClass().add("export-publish-tab");

        // Properties are put in fields rather than inlined as long parameter,
        // not only for readability, but mainly because the way FX do binding
        // is by creating weak references with chain binding.
        userNameValidity = userNameField.textProperty().isNotEmpty();
        passwordValidity = passwordField.textProperty().isNotEmpty();
        titleValidity = titleField.textProperty().isNotEmpty();
        validProperty.bind(userNameValidity.and(passwordValidity).and(titleValidity));

        JavaFXUtil.addChangeListenerPlatform(selectedProperty(), selected -> activated());
    }

    @Override
    public ExportFunction getFunction()
    {
        return ExportFunction.PUBLISH;
    }

    /**
     * Get the image that is to be used as icon for this scenario.
     * 
     * @return The image, or null if it couldn't be created.
     */
    public Image getImage()
    {
        return imagePane.getImage();
    }

    /**
     * Set the screenshot image.
     *
     * @param snapShot The snapshot to be put in the image pane.
     */
    public void setImage(Image snapShot)
    {
        imagePane.setImage(snapShot);
    }

    /**
     * Get the scenario title, specified in the title field.
     */
    public String getTitle()
    {
        if (titleField != null)
        {
            return titleField.getText();
        }
        return null;
    }

    /**
     * Return the short description string.
     */
    public String getShortDescription()
    {
        return shortDescriptionField.getText();
    }

    /**
     * Return the description string.
     */
    public String getLongDescription()
    {
        return longDescriptionArea.getText();
    }

    /**
     * Return the URL.
     */
    public String getURL()
    {
        return urlField.getText();
    }

    /**
     * Return the user name.
     */
    public String getUserName()
    {
        return userNameField.getText();
    }

    /**
     * Return the changes to update string (if applicable)
     */
    private String getUpdateDescription()
    {
        return updateArea != null ? updateArea.getText() : null;
    }

    /**
     * Return the password.
     */
    public String getPassword()
    {
        return passwordField.getText();
    }

    /**
     * True if the source code should be included.
     */
    public boolean isIncludeSource()
    {
        return includeSource.isSelected();
    }

    /**
     * True if the screenshot should *not* be overwritten; false if it should
     */
    private boolean isKeepSavedScreenshot()
    {
        return update && keepScenarioScreenshot.isSelected();
    }

    /**
     * Takes a list of tags select the popular ones placed as checkboxes,
     * and format the rest in separate lines in the tagTextArea.
     *
     * @param tags a list of String tags.
     */
    private void processTags(List<String> tags)
    {
        for (CheckBox popTag : popTags)
        {
            if (tags.contains(popTag.getText()))
            {
                popTag.setSelected(true);
            }
        }

        // Build String of joined tags.
        String newTags = tags.stream()
                // we never want the with-source tag to show up.
                .filter(tag -> !WITH_SOURCE_TAG.equals(tag))
                // Don't show pop tags.
                .filter(tag -> !getPopTagsTexts().contains(tag))
                .collect(Collectors.joining(System.getProperty("line.separator")));

        tagArea.setText(newTags);
    }

    /**
     * Returns a list of the text in the pop tags checkboxes.
     */
    private List<String> getPopTagsTexts()
    {
        return Stream.of(popTags).map(CheckBox::getText).collect(Collectors.toList());
    }

    /**
     * Build the contents' pane.
     */
    private void buildContentPane()
    {
        Label publishInfoLabel = new Label(Config.getString("export.publish.info") + " " + serverName);
        publishInfoLabel.getStyleClass().add("intro-label");
        BorderPane.setAlignment(publishInfoLabel, Pos.CENTER);

        BorderPane infoPane = new BorderPane(createScenarioPane(), publishInfoLabel,
                getTagDisplay(), null, null);
        infoPane.getStyleClass().add("info-pane");

        setContent(new VBox(getHelpBox(), infoPane, getLoginPane()));
    }
    
    /**
     * Creates a login pane with a username and password and a create account option
     * @return Login pane Component
     */
    private Pane getLoginPane()
    {
        Label loginLabel = new Label(Config.getString("export.publish.login"));
        loginLabel.getStyleClass().add("intro-label");

        Label usernameLabel = new Label(Config.getString("export.publish.username"));
        userNameField = new TextField();
        userNameField.setPrefColumnCount(10);
        JavaFXUtil.addChangeListenerPlatform(userNameField.focusedProperty(), focused -> {
            if (!focused)
            {
                checkForExistingScenario();
            }
        });

        Label passwordLabel = new Label(Config.getString("export.publish.password"));
        passwordField = new PasswordField();
        passwordField.setPrefColumnCount(10);

        Hyperlink createAccountLabel = new Hyperlink(Config.getString("export.publish.createAccount"));
        createAccountLabel.getStyleClass().add("create-account-label");
        createAccountLabel.setOnAction(event -> SwingUtilities.invokeLater(() -> Utility.openWebBrowser(createAccountUrl)));

        HBox loginPane = new HBox(loginLabel,
                usernameLabel, userNameField,
                passwordLabel, passwordField,
                createAccountLabel);
        loginPane.getStyleClass().add("login-pane");
        return loginPane;
    }
    
    /**
     * Build a help box with a link to appropriate help
     * @return help box
     */
    private Pane getHelpBox()
    {
        Hyperlink serverLink = new Hyperlink(serverURL);
        serverLink.setOnAction(event -> SwingUtilities.invokeLater(() -> Utility.openWebBrowser(serverURL)));

        HBox helpBox = new HBox(new Label(helpLine + " ("), serverLink, new Label(")"));
        helpBox.getStyleClass().add("help-box");
        return helpBox;
    }

    /**
     * Set the tags in the UI from the given list (null if the server couldn't be contacted or
     * didn't respond as expected).
     * 
     * <p>Should be called from event thread
     */
    //TODO thread checker : Should be called from event thread
    private void setPopularTags(List<String> tags)
    {
        if (tags == null)
        {
            // Couldn't get the tags list.
            popTags[0].setText("Unavailable");
            for (int i = 1; i < popTags.length; i++)
            {
                popTags[i].setText("");
            }
            return;
        }
        
        int minLength = popTags.length < tags.size() ? popTags.length : tags.size();
        for (int i = 0; i < minLength; i++)
        {
            CheckBox checkBox = popTags[i];
            checkBox.setText(tags.get(i));
            checkBox.setDisable(false);
            processTags(getTags());
        }
        
        // Clear any remaining checkboxes.
        for (int i = minLength; i < popTags.length; i++)
        {
            popTags[i].setText("");
        }
    }

    /**
     * Returns a list of the tags that the user chose for this scenario.
     */
    public List<String> getTags()
    {
        // Get the pop tags from the selected checkboxes
        ArrayList<String> tagList = Arrays.stream(popTags)
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toCollection(ArrayList::new));

        // Add text area tags
        tagList.addAll(Arrays.asList(tagArea.getText().split("\\s+")));

        // Include/Exclude the "with source" tag.
        if(isIncludeSource() && !tagList.contains(WITH_SOURCE_TAG))
        {
            tagList.add(WITH_SOURCE_TAG);
        }
        else if (!isIncludeSource())
        {
            tagList.remove(WITH_SOURCE_TAG);
        }

        return tagList;
    }

    /**
     * Loads details already stored for this scenario at previous publish (if they exist).
     */
    private void loadStoredScenarioInfo()
    {
        titleField.setText(scenarioInfo.getTitle());
        shortDescriptionField.setText(scenarioInfo.getShortDescription());
        longDescriptionArea.setText(scenarioInfo.getLongDescription());
        urlField.setText(scenarioInfo.getUrl());
        processTags(scenarioInfo.getTags());
        lockScenario.setSelected(scenarioInfo.isLocked());
        includeSource.setSelected(scenarioInfo.isIncludeSource());
        setUpdate(!scenarioInfo.getTitle().isEmpty());
    }

    @Override
    protected void updateInfoFromFields()
    {
        scenarioInfo.setTitle(getTitle());
        scenarioInfo.setShortDescription(getShortDescription());
        scenarioInfo.setLongDescription(getLongDescription());
        scenarioInfo.setUrl(getURL());
        scenarioInfo.setTags(getTags());
        scenarioInfo.setLocked(isLockScenario());
        scenarioInfo.setIncludeSource(isIncludeSource());
    }
    
    @Override
    protected ExportInfo getExportInfo()
    {
        ExportInfo info = new ExportInfo(scenarioInfo);
        info.setUpdateDescription(getUpdateDescription());
        info.setUserName(getUserName());
        info.setPassword(getPassword());
        info.setImage(getImage());
        info.setKeepSavedScreenshot(isKeepSavedScreenshot());
        info.setUpdate(isUpdate());
        return info;
    }

    private void checkForExistingScenario()
    {
        String userName = getUserName();
        String title = getTitle();

        // First check if everything is ready and bail out if it is not.
        if (userName == null || userName.equals(""))
        {
            return;
        }
        if (title == null || title.equals(""))
        {
            return;
        }

        if (scenarioChecker == null)
        {
            scenarioChecker = new ExistingScenarioChecker() {

                @Override
                public void scenarioExistenceCheckFailed(Exception reason)
                {
                    // Don't do anything. Failure could be due to proxy requiring authentication,
                    // or network disconnection.
                }

                @Override
                public void scenarioExistenceChecked(ScenarioInfo info)
                {
                    setUpdate(info != null);
                }
            };
        }
        scenarioChecker.startScenarioExistenceCheck(serverURL, userName, title);
    }

    /**
     * The first time this tab is activated we fetch the popular tags from the
     * server (if possible).
     * 
     * <p>And we load previously used values if they are stored.
     */
    private void activated()
    {
        if (firstActivation)
        {
            firstActivation = false;

            userNameField.setText(Config.getPropString("publish.username", ""));
            loadStoredScenarioInfo();
            checkForExistingScenario();

            FXWorker commonTagsLoader = new FXWorker()
            {
                @SuppressWarnings("unchecked")
                @Override
                public void finished()
                {
                    setPopularTags((List<String>) getValue());
                }

                @Override
                @OnThread(Tag.FXPlatform)
                public void abort()
                {

                }

                @Override
                public Object construct()
                {
                    MyGameClient client = new MyGameClient(null);
                    List<String> tags = null;
                    try {
                        String hostAddress = serverURL;
                        if (!hostAddress.endsWith("/"))
                        {
                            hostAddress += "/";
                        }
                        // We add one to the number, because WITH_SOURCE is
                        // likely to be among them and we then will filter it out.
                        tags = client.getCommonTags(hostAddress, popTags.length + 1);
                        if (tags.contains(WITH_SOURCE_TAG))
                        {
                            tags.remove(WITH_SOURCE_TAG);
                        }
                        else if (!tags.isEmpty())
                        {
                            tags.remove(tags.size() - 1);
                        }
                    }
                    catch (ConnectTimeoutException ignored) { }
                    catch (IOException e)
                    {
                        Debug.reportError("Error while publishing scenario", e);
                    }
                    return tags;
                }
            };
            commonTagsLoader.start();
        }

        setExportButtonText();
    }

    /**
     * Sets the dialog export button's text to either update or share.
     */
    private void setExportButtonText()
    {
        exportDialog.setExportButtonText(
                Config.getString(update ? "export.dialog.update" : "export.dialog.share"));
    }

    @Override
    public boolean prePublish()
    {
        super.prePublish();
        publishedUserName = getUserName();
        return true;
    }

    @Override
    public void postPublish(boolean success)
    {
        if (success)
        {
            scenarioSaver.doSave();
            Config.putPropString("publish.username", publishedUserName);
            setUpdate(true);
        }
    }
    
    /**
     * Make sure a host name ends with a slash.
     */
    private static String ensureTrailingSlash(String hostname)
    {
        if (hostname.endsWith("/"))
        {
            return hostname;
        }
        return hostname + "/";
    }
    
    /**
     * Creates the scenario information display including information
     * such as title, description, url. For an update (update = true),
     * the displayed options are slightly different.
     *
     * @return The pane containing main scenario controls.
     */
    private Pane createScenarioPane()
    {
        titleAndDescPane = new GridPane();
        titleAndDescPane.getStyleClass().add("title-desc-pane");

        ColumnConstraints column1 = new ColumnConstraints();
        // 130 fits the different labels nicely
        column1.setPrefWidth(130);
        column1.setHalignment(HPos.RIGHT);
        // Second column gets any extra width
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPrefWidth(220);
        column2.setHgrow(Priority.ALWAYS);
        column2.setHalignment(HPos.CENTER);
        titleAndDescPane.getColumnConstraints().addAll(column1, column2);

        VBox iconTextPane = new VBox(new Label(Config.getString("export.publish.image1")),
                new Label(Config.getString("export.publish.image2")));
        iconTextPane.getStyleClass().add("icon-text-pane");

        imagePane = new ImageEditPane(IMAGE_WIDTH, IMAGE_HEIGHT);
        titleAndDescPane.addRow(0, iconTextPane, imagePane);

        keepScenarioScreenshot = new CheckBox(Config.getString("export.snapshot.label"));
        keepScenarioScreenshot.setSelected(true);
        JavaFXUtil.addChangeListenerPlatform(keepScenarioScreenshot.selectedProperty(),
                selected -> imagePane.enableImageEditPanel(!selected));

        titleField = new TextField(getTitle() != null ? getTitle() : project.getProjectName());
        JavaFXUtil.addChangeListenerPlatform(titleField.focusedProperty(), focused -> {
            if (!focused)
            {
                checkForExistingScenario();
            }
        });

        // Controls which only show if "updated" scenario
        updateLabel.setAlignment(Pos.TOP_LEFT);
        updateArea = new TextArea();
        updateArea.setPrefRowCount(5);
        updateArea.setWrapText(true);
        updatePane = new ScrollPane(updateArea);
        updatePane.setFitToWidth(true);
        GridPane.setVgrow(updatePane, Priority.ALWAYS);

        // Controls which only show if new (not "updated") scenario
        shortDescriptionField = new TextField();
        shortDescriptionLabel.setAlignment(Pos.TOP_LEFT);
        longDescriptionArea = new TextArea();
        longDescriptionArea.setPrefRowCount(5);
        longDescriptionArea.setWrapText(true);
        description = new ScrollPane(longDescriptionArea);
        description.setFitToWidth(true);
        GridPane.setVgrow(description, Priority.ALWAYS);

        //Add suitable controls
        chooseNewOrUpdatedScenarioFields();

        Label publishUrlLabel = new Label(Config.getString("export.publish.url"));
        urlField = new TextField();
        titleAndDescPane.addRow(4, publishUrlLabel, urlField);

        includeSource = new CheckBox(Config.getString("export.publish.includeSource"));
        includeSource.setSelected(false);
        HBox sourceAndLockPane = new HBox(includeSource, lockScenario);
        sourceAndLockPane.getStyleClass().add("source-lock-pane");

        VBox scenarioPane = new VBox(titleAndDescPane, sourceAndLockPane);
        scenarioPane.getStyleClass().add("scenario-pane");
        return scenarioPane;
    }

    /**
     * Update the display according to whether this is an update of an existing
     * scenario, or an upload of a new scenario. If there is an update,
     * a "changes" description area is shown. If not there a short description
     * and long description areas are shown.
     */
    private void chooseNewOrUpdatedScenarioFields()
    {
        titleAndDescPane.getChildren().removeAll(titleLabel, titleField);

        if (update)
        {
            titleAndDescPane.getChildren().removeAll(shortDescriptionLabel, shortDescriptionField,
                    longDescriptionLabel, description);

            titleAndDescPane.addRow(1, emptyLabel, keepScenarioScreenshot);
            titleAndDescPane.addRow(2, titleLabel, titleField);
            titleAndDescPane.addRow(3, updateLabel, updatePane);
        }
        else
        {
            titleAndDescPane.getChildren().removeAll(emptyLabel, keepScenarioScreenshot,
                    updateLabel, updatePane);
            titleAndDescPane.addRow(1, titleLabel, titleField);
            titleAndDescPane.addRow(2, shortDescriptionLabel, shortDescriptionField);
            titleAndDescPane.addRow(3, longDescriptionLabel, description);
        }

        imagePane.enableImageEditPanel(!update || !keepScenarioScreenshot.isSelected());
    }

    /**
     * Creates the tag display with popular tags and an option to add tags
     */
    private Pane getTagDisplay ()
    {
        Label popLabel = new Label(Config.getString("export.publish.tags.popular"));

        VBox popPane = new VBox(popLabel);
        popPane.getStyleClass().add("pop-pane");
        for (int i = 0; i < popTags.length; i++)
        {
            CheckBox popTag = new CheckBox(Config.getString("export.publish.tags.loading"));
            popTag.setDisable(true);
            popTags[i] = popTag;
        }
        popPane.getChildren().addAll(popTags);

        tagArea = new TextArea();
        ScrollPane tagScroller = new ScrollPane(tagArea);
        tagScroller.setPrefSize(125, 100);
        tagScroller.setFitToWidth(true);
        tagScroller.setFitToHeight(true);
        VBox textPane = new VBox(new Label(Config.getString("export.publish.tags.additional1")),
                new Label(Config.getString("export.publish.tags.additional2")), tagScroller);

        VBox tagPane = new VBox(popPane, textPane);
        tagPane.getStyleClass().add("tag-pane");
        return tagPane;
    }

    /**
     * Returns true if it is an update and false if it is a first export
     */
    public boolean isUpdate()
    {
        return update;
    }

    /**
     * Specify whether this scenario will be updated, or is a new export.
     */
    private void setUpdate(boolean isUpdate)
    {
        if (this.update != isUpdate)
        {
            this.update = isUpdate;
            setExportButtonText();
            chooseNewOrUpdatedScenarioFields();
        }
    }
}
