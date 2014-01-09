/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.export;

import greenfoot.core.GProject;
import greenfoot.export.mygame.ExistingScenarioChecker;
import greenfoot.export.mygame.MyGameClient;
import greenfoot.export.mygame.ScenarioInfo;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.httpclient.ConnectTimeoutException;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.MiksGridLayout;
import bluej.utility.SwingWorker;

/**
 * Pane used for exporting to Greenfoot Gallery
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 */
public class ExportPublishPane extends ExportPane implements ChangeListener
{
    public static final int IMAGE_WIDTH = 120;
    public static final int IMAGE_HEIGHT = 70;

    public static final String FUNCTION = "PUBLISH";
    private static final Color background = new Color(166, 188, 202);
    private static final Color headingColor = new Color(40, 75, 125);
    private static final String serverURL = ensureTrailingSlash(Config.getPropString("greenfoot.gameserver.address",
            "http://www.greenfoot.org/"));
    private static final String createAccountUrl = Config.getPropString("greenfoot.gameserver.createAccount.address",
            "http://www.greenfoot.org/users/new");
    private static final String serverName = Config.getPropString("greenfoot.gameserver.name", "Greenfoot Gallery");

    private static final String helpLine1 = Config.getString("export.publish.help") + " " + serverName;
    private static final String WITH_SOURCE_TAG = "with-source";

    private JComponent leftPanel;
    private JPanel titleAndDescPanel;
    private JPanel infoPanel;
    private JTextField titleField;
    private JTextField shortDescriptionField;
    private JTextArea descriptionArea;
    private JTextArea updateArea;
    private JTextField urlField;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private ImageEditPanel imagePanel;
    private JCheckBox includeSource;
    private JCheckBox keepScenarioScreenshot;

    private SwingWorker commonTagsLoader;
    private JCheckBox[] popTags = new JCheckBox[7];
    private JTextArea tagArea;
    private GProject project;
    private boolean firstActivation = true;

    private ScenarioInfo publishedScenarioInfo;
    private String publishedUserName;

    private ExistingScenarioChecker scenarioChecker;
    private Font font;
    private boolean isUpdate = false;
    private ExportDialog exportDialog;

    /** Creates a new instance of ExportPublishPane */
    public ExportPublishPane(GProject project, ExportDialog exportDialog)
    {
        super();
        this.project = project;
        this.exportDialog = exportDialog;
        makePane();
    }

    /**
     * Get the image that is to be used as icon for this scenario.
     * 
     * @return The image, or null if it couldn't be created.
     */
    public BufferedImage getImage()
    {
        return imagePanel.getImage();
    }

    /**
     * Set the screenshot image.
     */
    public void setImage(BufferedImage snapShot)
    {
        imagePanel.setImage(snapShot);
        imagePanel.repaint();
    }

    /**
     * Get the scenario title, specified in the title field.
     */
    public String getTitle()
    {
        if (titleField!=null)
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
        if (updateArea!=null){
            return updateArea.getText();
        }
        return null;
    }

    /**
     * Return the password.
     */
    public String getPassword()
    {
        return new String(passwordField.getPassword());
    }

    /**
     * True if the source code should be included.
     */
    public boolean includeSourceCode()
    {
        return includeSource.isSelected();
    }

    private void setHasSource(boolean hasSource)
    {
        includeSource.setSelected(hasSource);
    }
    
    /**
     * True if the screenshot should *not* be overwritten; false if it should
     */
    public boolean keepSavedScenarioScreenshot()
    {
        if (isUpdate && keepScenarioScreenshot != null) {
            return keepScenarioScreenshot.isSelected();
        }
        return false;
    }

    private void setLocked(boolean locked)
    {
        lockScenario.setSelected(locked);
    }

    private void setTags(List<String> tags)
    {
        StringBuilder newTags = new StringBuilder();
        boolean isFirstNewTag = true;
        for (Iterator<String> iterator = tags.iterator(); iterator.hasNext();) {
            String tag = iterator.next();
            if(WITH_SOURCE_TAG.equals(tag)) {
                // we never want the with-source tag to show up.
                continue;
            }
            boolean isPopTag = false;
            for (int i = 0; i < popTags.length; i++) {
                JCheckBox popTag = popTags[i];
                if (popTag.getText().equals(tag)) {
                    popTag.setSelected(true);
                    isPopTag = true;
                    break;
                }
            }
            if (!isPopTag) {
                if (!isFirstNewTag) {
                    // Only insert newline if it is not the first new tag
                    newTags.append(System.getProperty("line.separator"));
                }
                isFirstNewTag = false;
                newTags.append(tag);
            }
        }
        tagArea.setText(newTags.toString());
    }

    private void setUrl(String url)
    {
        urlField.setText(url);
    }

    private void setLongDescription(String longDescription)
    {
        descriptionArea.setText(longDescription);
    }

    private void setShortDescripton(String shortDescription)
    {
        shortDescriptionField.setText(shortDescription);
    }

    private void setTitle(String title)
    {
        titleField.setText(title);
    }

    private void setUserName(String name)
    {
        userNameField.setText(name);
    }

    /**
     * Build the component.
     */
    private void makePane()
    {
        font = (new JLabel()).getFont().deriveFont(Font.ITALIC, 11.0f);
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        setBackground(backgroundColor);
       
        add(getHelpBox());
        add(Box.createVerticalStrut(12));

        infoPanel = new JPanel(new BorderLayout(22, 18));
        {
            infoPanel.setAlignmentX(LEFT_ALIGNMENT);
            infoPanel.setBackground(background);

            Border border = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory
                    .createEmptyBorder(12, 22, 12, 22));
            infoPanel.setBorder(border);

            JLabel text = new JLabel(Config.getString("export.publish.info") + " " + serverName, SwingConstants.CENTER);
            text.setForeground(headingColor);
            infoPanel.add(text, BorderLayout.NORTH);

            createScenarioDisplay();
            infoPanel.add(leftPanel, BorderLayout.CENTER);            
            infoPanel.add(getTagDisplay(), BorderLayout.EAST);
        }

        add(infoPanel);
        add(Box.createVerticalStrut(16));       
        add(getLoginPanel());
        add(Box.createVerticalStrut(10));
    }
    
    /**
     * Creates a login panel with a username and password and a create account option
     * @return Login panel Component
     */
    private JComponent getLoginPanel()
    {
        JComponent loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));

        loginPanel.setBackground(background);
        loginPanel.setAlignmentX(LEFT_ALIGNMENT);
        Border border = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(12, 12, 12, 12));
        loginPanel.setBorder(border);

        JLabel text = new JLabel(Config.getString("export.publish.login"));
        text.setForeground(headingColor);
        text.setVerticalAlignment(SwingConstants.TOP);
        loginPanel.add(text);

        text = new JLabel(Config.getString("export.publish.username"), SwingConstants.TRAILING);
        text.setFont(font);
        loginPanel.add(text);
        userNameField = new JTextField(10);
        userNameField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input)
            {
                String text = userNameField.getText();
                return text.length() > 0;
            }
        });
        userNameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                checkForExistingScenario();
            }
        });
        loginPanel.add(userNameField);
        text = new JLabel(Config.getString("export.publish.password"), SwingConstants.TRAILING);
        text.setFont(font);
        loginPanel.add(text);
        passwordField = new JPasswordField(10);
        loginPanel.add(passwordField);

        JLabel createAccountLabel = new JLabel(Config.getString("export.publish.createAccount"));
        {
            createAccountLabel.setBackground(background);

            createAccountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            GreenfootUtil.makeLink(createAccountLabel, createAccountUrl);
        }
        loginPanel.add(createAccountLabel);
        return loginPanel;
    }
    
    /**
     * Build a help box with a link to appropriate help
     * @return help box
     */
    private static Box getHelpBox()
    {
        Box helpBox = new Box(BoxLayout.X_AXIS);
        helpBox.setAlignmentX(LEFT_ALIGNMENT);
        JLabel helpText1 = new JLabel(helpLine1 + " (");
        helpBox.add(helpText1);
        JLabel serverLink = new JLabel(serverURL);
        GreenfootUtil.makeLink(serverLink, serverURL);
        helpBox.add(serverLink);
        helpBox.add(new JLabel(")"));
        return helpBox;
    }

    /**
     * Set the tags in the UI from the given list (null if the server couldn't be contacted or
     * didn't respond as expected).
     * 
     * <p>Should be called from event thread
     */
    private void setPopularTags(List<String> tags)
    {
        if (tags == null) {
            // Couldn't get the tags list.
            popTags[0].setText("Unavailable");
            for (int i = 1; i < popTags.length; i++) {
                popTags[i].setText("");
            }
            return;
        }
        
        int minLength = popTags.length < tags.size() ? popTags.length : tags.size();
        for (int i = 0; i < minLength; i++) {
            JCheckBox checkBox = popTags[i];
            checkBox.setText(tags.get(i));
            checkBox.setEnabled(true);
            setTags(getTags());
        }
        
        // Clear any remaining checkboxes.
        for (int i = minLength; i < popTags.length; i++) {
            popTags[i].setText("");
        }
    }

    /**
     * Returns a list of the tags that the user chose for this scenario.
     */
    public List<String> getTags()
    {
        List<String> tagList = new LinkedList<String>();

        for (int i = 0; i < popTags.length; i++) {
            JCheckBox checkBox = popTags[i];
            if (checkBox.isSelected()) {
                tagList.add(checkBox.getText());
            }
        }

        String currentTags = tagArea.getText().trim();
        String[] tags = currentTags.split("\\s");
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i].trim();
            if (!tag.equals("")) {
                tagList.add(tag);
            }
        }
        
        if(includeSourceCode() && !tagList.contains(WITH_SOURCE_TAG)) {
            tagList.add(WITH_SOURCE_TAG);
        }
        else if (!includeSourceCode()){
            tagList.remove(WITH_SOURCE_TAG);
        }
        return tagList;
    }

    /**
     * Attempts to load details already stored for this scenario at previous
     * publish.
     * 
     * Must be called from the event thread.
     */
    private void loadStoredScenarioInfo()
    {
        ScenarioInfo info = new ScenarioInfo();
        if (info.load(project.getProjectProperties())) {
            setTitle(info.getTitle());
            setShortDescripton(info.getShortDescription());
            setLongDescription(info.getLongDescription());
            setUrl(info.getUrl());
            setTags(info.getTags());
            setLocked(info.isLocked());
            setHasSource(info.getHasSource());
            setUpdate(true);
        }
    }

    /**
     * Update the display according to whether this is an update of an existing scenario,
     * or an upload of a new scenario.
     */
    private void updateScenarioDisplay()
    {
        removeLeftPanel();
        createScenarioDisplay();
        infoPanel.add(leftPanel, BorderLayout.CENTER);
        boolean enableImageControl = !isUpdate || !keepScenarioScreenshot.isSelected();
        imagePanel.enableImageEditPanel(enableImageControl);
        revalidate();
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
        if (userName == null || userName.equals("")) {
            return;
        }
        if (title == null || title.equals("")) {
            return;
        }

        if (scenarioChecker == null) {
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
                    if (info != null) {
                        setUpdate(true);
                    }
                    else {
                        setUpdate(false);
                    }
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
        if (firstActivation) {
            firstActivation = false;
            
            setUserName(Config.getPropString("publish.username", ""));
            loadStoredScenarioInfo();
            checkForExistingScenario();
            
            commonTagsLoader = new SwingWorker() {
                @SuppressWarnings("unchecked")
                @Override
                public void finished()
                {
                    List<String> l = (List<String>) getValue();
                    setPopularTags(l);
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
                        // likely to be among them and we then will filter it
                        // out.
                        tags = client.getCommonTags(hostAddress, popTags.length + 1);
                        if(tags.contains(WITH_SOURCE_TAG)) {
                            tags.remove(WITH_SOURCE_TAG);
                        }
                        else {
                            if (! tags.isEmpty()) {
                                tags.remove(tags.size() - 1);
                            }
                        }
                    }
                    catch (ConnectTimeoutException ctoe) { }
                    catch (UnknownHostException e) {
                        Debug.reportError("Error while publishing scenario", e);
                    }
                    catch (IOException e) {
                        Debug.reportError("Error while publishing scenario", e);
                    }
                    return tags;
                }
            };
            commonTagsLoader.start();
        }
        
        String updateText;
        if (isUpdate) {
            updateText = Config.getString("export.dialog.update") ;
        }
        else {
            updateText = Config.getString("export.dialog.share");
        }
         
        exportDialog.setExportButtonText(updateText);
    }

    @Override
    public boolean prePublish()
    {
        publishedScenarioInfo = new ScenarioInfo();
        updateInfoFromFields(publishedScenarioInfo);
        publishedUserName = userNameField.getText();
        return true;
    }

    @Override
    public void postPublish(boolean success)
    {
        if (success) {
            publishedScenarioInfo.store(project.getProjectProperties());
            Config.putPropString("publish.username", publishedUserName);
            setUpdate(true);
        }
    }
    
    /**
     * Make sure a host name ends with a slash.
     */
    public static String ensureTrailingSlash(String hostname)
    {
        if (hostname.endsWith("/")) {
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
        leftPanel = new Box(BoxLayout.Y_AXIS);
        JLabel text;
        MiksGridLayout titleAndDescLayout = new MiksGridLayout(6, 2, 8, 8);
        titleAndDescLayout.setVerticallyExpandingRow(3);

        titleAndDescPanel = new JPanel(titleAndDescLayout);
        titleAndDescPanel.setBackground(background);

        if (imagePanel == null) {
            imagePanel = new ImageEditPanel(IMAGE_WIDTH, IMAGE_HEIGHT);
            imagePanel.setBackground(background);
        }

        Box textPanel = new Box(BoxLayout.Y_AXIS);
        {
            text = new JLabel(Config.getString("export.publish.image1"));
            text.setAlignmentX(Component.RIGHT_ALIGNMENT);
            text.setFont(font);
            textPanel.add(text);
            text = new JLabel(Config.getString("export.publish.image2"));
            text.setAlignmentX(Component.RIGHT_ALIGNMENT);
            text.setFont(font);
            textPanel.add(text);
        }
        titleAndDescPanel.add(textPanel);
        titleAndDescPanel.add(imagePanel);
        
        if (isUpdate) {
            text = new JLabel(Config.getString("export.snapshot.label"), SwingConstants.TRAILING);
            text.setFont(font);
            titleAndDescPanel.add(text);
            
            keepScenarioScreenshot = new JCheckBox();
            keepScenarioScreenshot.setSelected(true);
            // "keep screenshot" defaults to true, therefore the image panel should be disabled
            imagePanel.enableImageEditPanel(false);
            keepScenarioScreenshot.setName(Config.getString("export.publish.keepScenario"));
            keepScenarioScreenshot.setOpaque(false);
            keepScenarioScreenshot.addChangeListener(this);
            titleAndDescPanel.add(keepScenarioScreenshot);  
        }

        text = new JLabel(Config.getString("export.publish.title"), SwingConstants.TRAILING);
        text.setFont(font);
        titleAndDescPanel.add(text);
        
        String title = project.getName();
        if (getTitle() != null) {
           title = getTitle();
        }
        titleField = new JTextField(title);
        titleField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input)
            {
                String text = titleField.getText();
                return text.length() > 0;
            }
        });
        titleField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                checkForExistingScenario();
            }
        });
        titleAndDescPanel.add(titleField);
        
        // If there is an update a "changes" description area is shown.
        // If not there a short description and long description area are shown.
        if (isUpdate) {
            JLabel updateLabel = new JLabel(Config.getString("export.publish.update"), SwingConstants.TRAILING);
            updateLabel.setVerticalAlignment(SwingConstants.TOP);
            updateLabel.setFont(font);
         
            updateArea = new JTextArea();
            updateArea.setRows(6);
            updateArea.setLineWrap(true);
            updateArea.setWrapStyleWord(true);
            JScrollPane updatePane = new JScrollPane(updateArea);
            
            titleAndDescPanel.add(updateLabel);
            titleAndDescPanel.add(updatePane);
            titleAndDescLayout.setVerticallyExpandingRow(4);
        }
        else {
            text = new JLabel(Config.getString("export.publish.shortDescription"), SwingConstants.TRAILING);
            text.setFont(font);
            shortDescriptionField = new JTextField();
            titleAndDescPanel.add(text);
            titleAndDescPanel.add(shortDescriptionField);
            text = new JLabel(Config.getString("export.publish.longDescription"), SwingConstants.TRAILING);
            text.setVerticalAlignment(SwingConstants.TOP);
            text.setFont(font);
            
            descriptionArea = new JTextArea();
            descriptionArea.setRows(6);
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            JScrollPane description = new JScrollPane(descriptionArea);
            titleAndDescPanel.add(text);
            titleAndDescPanel.add(description);
        }

        text = new JLabel(Config.getString("export.publish.url"), SwingConstants.TRAILING);
        text.setFont(font);
        titleAndDescPanel.add(text);

        urlField = new JTextField();
        titleAndDescPanel.add(urlField);

        leftPanel.add(titleAndDescPanel, BorderLayout.SOUTH);
        
        JComponent sourceAndLockPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        {
            sourceAndLockPanel.setBackground(background);
            includeSource = new JCheckBox(Config.getString("export.publish.includeSource"));
            includeSource.setOpaque(false);
            includeSource.setSelected(false);
            includeSource.setFont(font);
            sourceAndLockPanel.add(includeSource);
            lockScenario.setFont(font);
            sourceAndLockPanel.add(lockScenario);
            sourceAndLockPanel.setMaximumSize(sourceAndLockPanel.getPreferredSize());
        }

        leftPanel.add(sourceAndLockPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Removes the scenario information display
     */
    private void removeLeftPanel()
    {
        leftPanel.removeAll();
        infoPanel.remove(leftPanel);
    }
    
    /**
     * Creates the tag display with popular tags and an option to add tags
     */
    private JComponent getTagDisplay ()
    {
        JComponent tagPanel = new JPanel(new MiksGridLayout(3, 1, 8, 8));
        {
            tagPanel.setBackground(background);
            JComponent popPanel = new JPanel(new MiksGridLayout(8, 1, 8, 0));
            popPanel.setBackground(background);
            JLabel popLabel = new JLabel(Config.getString("export.publish.tags.popular"), SwingConstants.LEADING);
            popLabel.setFont(font);
            popPanel.add(popLabel);
            for (int i = 0; i < popTags.length; i++) {
                JCheckBox popTag = new JCheckBox(Config.getString("export.publish.tags.loading"));
                popTag.setBackground(background);
                popTag.setFont(font);
                popTag.setEnabled(false);
                popTags[i] = popTag;
                popPanel.add(popTag);
            }

            tagPanel.add(popPanel);

            Box textPanel = new Box(BoxLayout.Y_AXIS);
            {
                JLabel additionalLabel = new JLabel(Config.getString("export.publish.tags.additional1"),
                        SwingConstants.LEADING);
                additionalLabel.setFont(font);
                textPanel.add(additionalLabel);

                JLabel additionalLabel2 = new JLabel(Config.getString("export.publish.tags.additional2"),
                        SwingConstants.CENTER);
                additionalLabel2.setFont(font);
                textPanel.add(additionalLabel2);
            }
            tagPanel.add(textPanel);

            tagArea = new JTextArea();
            tagArea.setRows(3);
            JScrollPane tagScroller = new JScrollPane(tagArea);
            tagPanel.add(tagScroller);
        }
        return tagPanel;
    }

    /**
     * Of interest is the state of the keep scenario checkbox as that
     * determines whether the image panel is enabled or disabled
     */
    @Override
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource().equals(keepScenarioScreenshot)){
            if (keepScenarioScreenshot.isSelected()) {
                imagePanel.enableImageEditPanel(false);
            }
            else {
                imagePanel.enableImageEditPanel(true);
            }
        }
        
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
        if (this.isUpdate != isUpdate) {
            this.isUpdate = isUpdate;
            if (isUpdate) {
                String updateText = Config.getString("export.dialog.update");
                exportDialog.setExportButtonText(updateText);
            }
            else {
                String exportText = Config.getString("export.dialog.share");
                exportDialog.setExportButtonText(exportText);
            }
            updateScenarioDisplay();
        }
    }
}
