/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2017,2018  Poul Henriksen and Michael Kolling
 
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

import greenfoot.export.mygame.ExistingScenarioChecker;
import greenfoot.export.mygame.MyGameClient;
import greenfoot.export.mygame.ScenarioInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import org.apache.http.conn.ConnectTimeoutException;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Pane used for exporting to Greenfoot Gallery
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @author Amjad Altadmri
 */
public class ExportPublishPane extends ExportPane
{
    public static final String FUNCTION = "PUBLISH";

    public static final int IMAGE_WIDTH = 120;
    public static final int IMAGE_HEIGHT = 70;

    //TODO convert the numbers to JAVAFX color ranges and move them to CSS files.
    // private static final Color background = new Color(166, 188, 202, 1);
    // private static final Color headingColor = new Color(40, 75, 125, 1);
    private static final String serverURL = ensureTrailingSlash(
            Config.getPropString("greenfoot.gameserver.address", "http://www.greenfoot.org/"));
    private static final String createAccountUrl =
            Config.getPropString("greenfoot.gameserver.createAccount.address", "http://www.greenfoot.org/users/new");
    private static final String serverName = Config.getPropString("greenfoot.gameserver.name", "Greenfoot Gallery");

    private static final String helpLine = Config.getString("export.publish.help") + " " + serverName;
    private static final String WITH_SOURCE_TAG = "with-source";

    private Pane scenarioPane = new VBox();
    private BorderPane infoPanel;
    private TextField titleField;
    private TextField shortDescriptionField;
    private TextArea descriptionArea;
    private TextArea updateArea;
    private TextField urlField;
    private TextField userNameField;
    private PasswordField passwordField;
    private ImageEditPane imagePanel;
    private CheckBox includeSource;
    private CheckBox keepScenarioScreenshot;

    private CheckBox[] popTags = new CheckBox[7];
    private TextArea tagArea;
    private Project project;
    private boolean firstActivation = true;

    private String publishedUserName;

    private ExistingScenarioChecker scenarioChecker;
    private Font font;
    private boolean isUpdate = false;
    private final ExportDialog exportDialog;

    /**
     * Creates a new instance of ExportPublishPane
     *
     * @param project       The project that will be shared.
     * @param exportDialog  The export dialog containing this pane.
     */
    public ExportPublishPane(Project project, ExportDialog exportDialog)
    {
        super();
        this.project = project;
        this.exportDialog = exportDialog;
        buildContentPane();
    }

    /**
     * Get the image that is to be used as icon for this scenario.
     * 
     * @return The image, or null if it couldn't be created.
     */
    public Image getImage()
    {
        return imagePanel.getImage();
    }

    /**
     * Set the screenshot image.
     *
     * @param snapShot The snapshot to be put in the image panel
     */
    public void setImage(Image snapShot)
    {
        imagePanel.setImage(snapShot);
        // imagePanel.repaint();
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
    public String getDescription()
    {
        return descriptionArea.getText();
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
    public String getUpdateDescription()
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
    public boolean includeSourceCode()
    {
        return includeSource.isSelected();
    }

    /**
     * True if the screenshot should *not* be overwritten; false if it should
     */
    public boolean keepSavedScenarioScreenshot()
    {
        if (isUpdate && keepScenarioScreenshot != null)
        {
            return keepScenarioScreenshot.isSelected();
        }
        return false;
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
     * Build the main pane.
     */
    private void buildContentPane()
    {
        //TODO make it Italic and 11 point.
        font = new Label().getFont();//.deriveFont(Font.ITALIC, 11.0f);

        Label publishInfoLabel = new Label(Config.getString("export.publish.info") + " " + serverName);
        publishInfoLabel.setAlignment(Pos.CENTER);
        // publishInfoLabel.setForeground(headingColor);

        createScenarioDisplay();

        infoPanel = new BorderPane(scenarioPane, publishInfoLabel, getTagDisplay(), null, null);
        // infoPanel.setAlignmentX(LEFT_ALIGNMENT);
        // infoPanel.setBackground(background);
        // infoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),
        //                          BorderFactory.createEmptyBorder(12, 22, 12, 22)));

        VBox content = new VBox(20, getHelpBox(), infoPanel, getLoginPanel());
        content.setBorder(Border.EMPTY);
        setContent(content);
    }
    
    /**
     * Creates a login panel with a username and password and a create account option
     * @return Login panel Component
     */
    private Pane getLoginPanel()
    {
        Label loginLabel = new Label(Config.getString("export.publish.login"));
        // loginLabel.setForeground(headingColor);
        loginLabel.setAlignment(Pos.BOTTOM_LEFT);

        Label usernameLabel = new Label(Config.getString("export.publish.username"));
        usernameLabel.setFont(font);

        userNameField = new TextField();
        userNameField.setPrefColumnCount(10);
        // TODO check that the share button is disabled if the userNameField is empty
        // userNameField.setInputVerifier(userNameField.getText().length() > 0);

        JavaFXUtil.addChangeListener(userNameField.focusedProperty(), focused -> {
            if (!focused)
            {
                checkForExistingScenario();
            }
        });
        Label passwordLabel = new Label(Config.getString("export.publish.password"));
        passwordLabel.setFont(font);
        passwordField = new PasswordField();
        passwordField.setPrefColumnCount(10);

        Hyperlink createAccountLabel = new Hyperlink(Config.getString("export.publish.createAccount"));
        // createAccountLabel.setBackground(background);
        createAccountLabel.setAlignment(Pos.BOTTOM_LEFT);
        createAccountLabel.setOnAction(event -> Utility.openWebBrowser(createAccountUrl));

        HBox loginPanel = new HBox(10, loginLabel,
                usernameLabel, userNameField,
                passwordLabel, passwordField,
                createAccountLabel);
        loginPanel.setAlignment(Pos.BASELINE_CENTER);
        return loginPanel;
    }
    
    /**
     * Build a help box with a link to appropriate help
     * @return help box
     */
    private Pane getHelpBox()
    {
        Label helpTextLabel = new Label(helpLine + " (");
        Hyperlink serverLink = new Hyperlink(serverURL);
        serverLink.setOnAction(event -> Utility.openWebBrowser(serverURL));

        HBox helpBox = new HBox(helpTextLabel, serverLink, new Label(")"));
        helpBox.setAlignment(Pos.BASELINE_LEFT);
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
        if(includeSourceCode() && !tagList.contains(WITH_SOURCE_TAG))
        {
            tagList.add(WITH_SOURCE_TAG);
        }
        else if (!includeSourceCode())
        {
            tagList.remove(WITH_SOURCE_TAG);
        }

        return tagList;
    }

    /**
     * Attempts to load details already stored for this scenario
     * at previous publish.
     * 
     * Must be called from the event thread.
     */
    private void loadStoredScenarioInfo()
    {
        ScenarioInfo info = new ScenarioInfo();
        //TODO load info from properties
        // if (info.load(project.getProjectProperties()))
        // {
            titleField.setText(info.getTitle());
            shortDescriptionField.setText(info.getShortDescription());
            descriptionArea.setText(info.getLongDescription());
            urlField.setText(info.getUrl());
            processTags(info.getTags());
            lockScenario.setSelected(info.isLocked());
            includeSource.setSelected(info.getHasSource());
            setUpdate(true);
        //}
    }

    /**
     * Update the display according to whether this is an update of an existing scenario,
     * or an upload of a new scenario.
     */
    private void updateScenarioDisplay()
    {
        removeLeftPanel();
        createScenarioDisplay();
        infoPanel.setCenter(scenarioPane);
        boolean enableImageControl = !isUpdate || !keepScenarioScreenshot.isSelected();
        imagePanel.enableImageEditPanel(enableImageControl);
    }

    /**
     * Updates the given scenarioInfo with the current values typed into the
     * dialog.
     */
    private void updateInfoFromFields(ScenarioInfo scenarioInfo)
    {
        scenarioInfo.setTitle(getTitle());
        scenarioInfo.setShortDescription(getShortDescription());
        scenarioInfo.setLongDescription(getDescription());
        scenarioInfo.setUrl(getURL());
        scenarioInfo.setTags(getTags());
        scenarioInfo.setLocked(lockScenario());
        scenarioInfo.setHasSource(includeSourceCode());
        scenarioInfo.setUpdateDescription(getUpdateDescription());
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
     * The first time this pane is activated we fetch the popular tags from the
     * server (if possible).
     * 
     * <p>And we load previously used values if they are stored.
     */
    @Override
    public void activated()
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
                        if (!hostAddress.endsWith("/")) {
                            hostAddress += "/";
                        }
                        // We add one to the number, because WITH_SOURCE is
                        // likely to be among them and we then will filter it out.
                        tags = client.getCommonTags(hostAddress, popTags.length + 1);
                        if (tags.contains(WITH_SOURCE_TAG)) {
                            tags.remove(WITH_SOURCE_TAG);
                        } else if (!tags.isEmpty()) {
                            tags.remove(tags.size() - 1);
                        }
                    } catch (ConnectTimeoutException ignored) {
                    } catch (IOException e) {
                        Debug.reportError("Error while publishing scenario", e);
                    }
                    return tags;
                }
            };
            commonTagsLoader.start();
        }
        
        exportDialog.setExportButtonText(Config.getString(isUpdate ? "export.dialog.update" : "export.dialog.share"));
    }

    @Override
    public boolean prePublish()
    {
        ScenarioInfo publishedScenarioInfo = new ScenarioInfo();
        updateInfoFromFields(publishedScenarioInfo);
        publishedUserName = userNameField.getText();
        return true;
    }

    @Override
    public void postPublish(boolean success)
    {
        if (success)
        {
            //TODO save properties info
            //publishedScenarioInfo.store(project.getProjectProperties());
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
     * Creates the scenario information display including information such as title, description, url.
     * For an update (isUpdate = true), the displayed options are slightly different.
     */
    private void createScenarioDisplay()
    {
        GridPane titleAndDescPanel = new GridPane();
        titleAndDescPanel.setVgap(8);
        titleAndDescPanel.setHgap(8);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPrefWidth(102);
        column1.setHalignment(HPos.RIGHT);
        // Second column gets any extra width
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPrefWidth(260);
        column2.setHgrow(Priority.ALWAYS);
        column2.setHalignment(HPos.CENTER);

        titleAndDescPanel.getColumnConstraints().addAll(column1, column2);

        // titleAndDescLayout.setVerticallyExpandingRow(3);
        // titleAndDescPanel.setBackground(background);

        if (imagePanel == null)
        {
            imagePanel = new ImageEditPane(IMAGE_WIDTH, IMAGE_HEIGHT);
            // imagePanel.setBackground(background);
        }

        Label image1Label = new Label(Config.getString("export.publish.image1"));
        image1Label.setAlignment(Pos.BASELINE_RIGHT);
        image1Label.setFont(font);
        Label image2Label = new Label(Config.getString("export.publish.image2"));
        image2Label.setAlignment(Pos.BASELINE_RIGHT);
        image2Label.setFont(font);
        Pane textPanel = new VBox(image1Label, image2Label);

        int currentRow = 0;
        titleAndDescPanel.addRow(currentRow++, textPanel, imagePanel);

        if (isUpdate)
        {
            Label snapshotLabel = new Label(Config.getString("export.snapshot.label"));
            snapshotLabel.setFont(font);
            keepScenarioScreenshot = new CheckBox(Config.getString("export.publish.keepScenario"));
            keepScenarioScreenshot.setSelected(true);
            // "keep screenshot" defaults to true, therefore the image panel should be disabled
            imagePanel.enableImageEditPanel(false);
            JavaFXUtil.addChangeListener(keepScenarioScreenshot.selectedProperty(),
                    selected -> imagePanel.enableImageEditPanel(!selected));
            titleAndDescPanel.addRow(currentRow++, snapshotLabel, keepScenarioScreenshot);
        }

        Label titleLabel = new Label(Config.getString("export.publish.title"));
        titleLabel.setFont(font);

        
        titleField = new TextField(getTitle() != null ? getTitle() : project.getProjectName());
        // TODO check that the share button is disabled if the titleField is empty
        // titleField.setInputVerifier(titleField.getText().length() > 0);
        JavaFXUtil.addChangeListener(titleField.focusedProperty(), focused -> {
            if (!focused)
            {
                checkForExistingScenario();
            }
        });
        titleAndDescPanel.addRow(currentRow++, titleLabel, titleField);
        
        // If there is an update a "changes" description area is shown.
        // If not there a short description and long description area are shown.
        if (isUpdate)
        {
            Label updateLabel = new Label(Config.getString("export.publish.update"));
            updateLabel.setAlignment(Pos.TOP_LEFT);
            updateLabel.setFont(font);
         
            updateArea = new TextArea();
            updateArea.setPrefRowCount(6);
            updateArea.setWrapText(true);
            // updateArea.setWrapStyleWord(true);
            ScrollPane updatePane = new ScrollPane(updateArea);

            titleAndDescPanel.addRow(currentRow++, updateLabel, updatePane);
            // titleAndDescLayout.setVerticallyExpandingRow(4);
        }
        else
        {
            Label shortDescriptionLabel = new Label(Config.getString("export.publish.shortDescription"));
            shortDescriptionLabel.setFont(font);
            shortDescriptionField = new TextField();
            titleAndDescPanel.addRow(currentRow++, shortDescriptionLabel, shortDescriptionField);
            shortDescriptionLabel = new Label(Config.getString("export.publish.longDescription"));
            shortDescriptionLabel.setAlignment(Pos.TOP_LEFT);
            shortDescriptionLabel.setFont(font);
            
            descriptionArea = new TextArea();
            descriptionArea.setPrefRowCount(6);
            descriptionArea.setWrapText(true);
            // descriptionArea.setWrapStyleWord(true);
            ScrollPane description = new ScrollPane(descriptionArea);
            titleAndDescPanel.addRow(currentRow++, shortDescriptionLabel, description);
        }

        Label publishUrlLabel = new Label(Config.getString("export.publish.url"));
        publishUrlLabel.setFont(font);

        urlField = new TextField();
        titleAndDescPanel.addRow(currentRow, publishUrlLabel, urlField);


        HBox sourceAndLockPanel = new HBox();
        // sourceAndLockPanel.setBackground(background);
        includeSource = new CheckBox(Config.getString("export.publish.includeSource"));
        includeSource.setSelected(false);
        includeSource.setFont(font);
        lockScenario.setFont(font);
        sourceAndLockPanel.getChildren().addAll(includeSource, lockScenario);
        sourceAndLockPanel.setMaxSize(sourceAndLockPanel.getPrefWidth(), sourceAndLockPanel.getPrefHeight());

        scenarioPane.getChildren().addAll(titleAndDescPanel, sourceAndLockPanel);
    }
    
    /**
     * Removes the scenario information display
     */
    private void removeLeftPanel()
    {
        scenarioPane.getChildren().removeAll();
        infoPanel.getChildren().remove(scenarioPane);
    }
    
    /**
     * Creates the tag display with popular tags and an option to add tags
     */
    private Pane getTagDisplay ()
    {
        Label popLabel = new Label(Config.getString("export.publish.tags.popular"));
        popLabel.setFont(font);

        VBox popPanel = new VBox(2, popLabel);
        //popPanel.setBackground(background);
        for (int i = 0; i < popTags.length; i++)
        {
            CheckBox popTag = new CheckBox(Config.getString("export.publish.tags.loading"));
            // popTag.setBackground(background);
            popTag.setFont(font);
            popTag.setDisable(true);
            popTags[i] = popTag;
        }
        popPanel.getChildren().addAll(popTags);

        Label additionalLabel1 = new Label(Config.getString("export.publish.tags.additional1"));
        additionalLabel1.setFont(font);

        Label additionalLabel2 = new Label(Config.getString("export.publish.tags.additional2"));
        additionalLabel2.setFont(font);

        tagArea = new TextArea();
        tagArea.setPrefRowCount(3);
        ScrollPane tagScroller = new ScrollPane(tagArea);
        tagScroller.setPrefSize(100, 100);
        tagScroller.setFitToWidth(true);
        tagScroller.setFitToHeight(true);
        VBox textPanel = new VBox(additionalLabel1, additionalLabel2, tagScroller);

        VBox tagPanel = new VBox(20, popPanel, textPanel);
        // tagPanel.setBackground(background);
        return tagPanel;
    }

    /**
     * Returns true if it is an update and false if it is a first export
     */
    public boolean isUpdate()
    {
        return isUpdate;
    }

    /**
     * Specify whether this scenario will be updated, or is a new export.
     */
    private void setUpdate(boolean isUpdate)
    {
        if (this.isUpdate != isUpdate)
        {
            this.isUpdate = isUpdate;
            exportDialog.setExportButtonText(Config.getString(isUpdate ? "export.dialog.update" : "export.dialog.share"));
            updateScenarioDisplay();
        }
    }
}
