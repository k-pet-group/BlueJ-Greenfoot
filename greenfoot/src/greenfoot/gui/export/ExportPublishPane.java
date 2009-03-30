/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.export.WebPublisher;
import greenfoot.export.mygame.ExistingScenarioChecker;
import greenfoot.export.mygame.ScenarioInfo;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import bluej.Config;
import bluej.utility.MiksGridLayout;
import bluej.utility.SwingWorker;

/**
 * Pane used for exporting to Greenfoot Gallery
 * 
 * @author Michael Kolling
 * @author Poul Henriksen
 * @version $Id: ExportPublishPane.java 6216 2009-03-30 13:41:07Z polle $
 */
public class ExportPublishPane extends ExportPane
{
    public static final int IMAGE_WIDTH = 120;
    public static final int IMAGE_HEIGHT = 70;

    public static final String FUNCTION = "PUBLISH";
    private static final Color background = new Color(166, 188, 202);
    private static final Color headingColor = new Color(40, 75, 125);
    private static final String serverURL = ensureTrailingSlash(Config.getPropString("greenfoot.gameserver.address",
            "http://greenfootgallery.org"));
    private static final String createAccountUrl = Config.getPropString("greenfoot.gameserver.createAccount.address",
            "http://greenfootgallery.org/users/new");
    private static final String serverName = Config.getPropString("greenfoot.gameserver.name", "Greenfoot Gallery");

    private static final String helpLine1 = Config.getString("export.publish.help") + " " + serverName;
    private static final String WITH_SOURCE_TAG = "with-source";

    private JTextField titleField;
    private JTextField shortDescriptionField;
    private JTextArea descriptionArea;
    private JTextField urlField;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private ImageEditPanel imagePanel;
    private JCheckBox includeSource;

    private SwingWorker commonTagsLoader;
    private JCheckBox[] popTags = new JCheckBox[7];
    private JTextArea tagArea;
    private GProject project;
    private boolean firstActivation = true;

    private ScenarioInfo publishedScenarioInfo;
    private String publishedUserName;

    private ExistingScenarioChecker scenarioChecker;
    private JButton continueButton;

    /** Creates a new instance of ExportPublishPane */
    public ExportPublishPane(GProject project)
    {
        super();
        this.project = project;
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
     * Must be called from Swing Event Thread.
     * 
     * @param snapShot
     */
    public void setImage(BufferedImage snapShot)
    {
        imagePanel.setImage(snapShot);
        imagePanel.repaint();
    }

    public String getTitle()
    {
        return titleField.getText();
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

    private void setLocked(boolean locked)
    {
        lockScenario.setSelected(locked);
    }

    private void setTags(List<String> tags)
    {
        StringBuilder newTags = new StringBuilder();
        boolean isFirstNewTag = true;;
        for (Iterator<String> iterator = tags.iterator(); iterator.hasNext();) {
            String tag = (String) iterator.next();
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
        checkForExistingScenario();
    }

    private void setUserName(String name)
    {
        userNameField.setText(name);
        checkForExistingScenario();
    }

    /**
     * Build the component.
     */
    private void makePane()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        Box helpBox = new Box(BoxLayout.X_AXIS);
        {
            helpBox.setAlignmentX(LEFT_ALIGNMENT);
            JLabel helpText1 = new JLabel(helpLine1 + " (");
            helpBox.add(helpText1);
            JLabel serverLink = new JLabel(serverURL);
            GreenfootUtil.makeLink(serverLink, serverURL);
            helpBox.add(serverLink);
            helpBox.add(new JLabel(")"));
        }
        add(helpBox);

        Font smallFont = (new JLabel()).getFont().deriveFont(Font.ITALIC, 11.0f);

        add(Box.createVerticalStrut(12));

        JPanel infoPanel = new JPanel(new BorderLayout(22, 18));
        {
            infoPanel.setAlignmentX(LEFT_ALIGNMENT);
            infoPanel.setBackground(background);

            Border border = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory
                    .createEmptyBorder(12, 22, 12, 22));
            infoPanel.setBorder(border);

            JLabel text = new JLabel(Config.getString("export.publish.info") + " " + serverName, SwingConstants.CENTER);
            text.setForeground(headingColor);
            infoPanel.add(text, BorderLayout.NORTH);

            JComponent leftPanel = new Box(BoxLayout.Y_AXIS);
            JPanel titleAndDescPanel = new JPanel(new MiksGridLayout(6, 2, 8, 8));
            {
                titleAndDescPanel.setBackground(background);

                imagePanel = new ImageEditPanel(IMAGE_WIDTH, IMAGE_HEIGHT);
                imagePanel.setBackground(background);
                Box textPanel = new Box(BoxLayout.Y_AXIS);
                {
                    text = new JLabel(Config.getString("export.publish.image1"));
                    text.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    text.setFont(smallFont);
                    textPanel.add(text);
                    text = new JLabel(Config.getString("export.publish.image2"));
                    text.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    text.setFont(smallFont);
                    textPanel.add(text);
                }
                titleAndDescPanel.add(textPanel);
                titleAndDescPanel.add(imagePanel);

                text = new JLabel(Config.getString("export.publish.title"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                titleAndDescPanel.add(text);

                titleField = new JTextField(project.getName());
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

                text = new JLabel(Config.getString("export.publish.shortDescription"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                titleAndDescPanel.add(text);

                shortDescriptionField = new JTextField();
                titleAndDescPanel.add(shortDescriptionField);

                text = new JLabel(Config.getString("export.publish.longDescription"), SwingConstants.TRAILING);
                text.setVerticalAlignment(SwingConstants.TOP);
                text.setFont(smallFont);
                titleAndDescPanel.add(text);

                descriptionArea = new JTextArea();
                descriptionArea.setRows(6);
                JScrollPane description = new JScrollPane(descriptionArea);
                titleAndDescPanel.add(description);

                text = new JLabel(Config.getString("export.publish.url"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                titleAndDescPanel.add(text);

                urlField = new JTextField();
                titleAndDescPanel.add(urlField);

                titleAndDescPanel.add(Box.createVerticalStrut(8));
            }

            leftPanel.add(titleAndDescPanel, BorderLayout.CENTER);

            JComponent sourceAndLockPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            {
                sourceAndLockPanel.setBackground(background);

                includeSource = new JCheckBox(Config.getString("export.publish.includeSource"));
                includeSource.setOpaque(false);
                includeSource.setSelected(false);
                includeSource.setFont(smallFont);
                sourceAndLockPanel.add(includeSource);
                lockScenario.setFont(smallFont);
                sourceAndLockPanel.add(lockScenario);
            }
            leftPanel.add(sourceAndLockPanel, BorderLayout.SOUTH);

            infoPanel.add(leftPanel, BorderLayout.CENTER);

            JComponent tagPanel = new JPanel(new MiksGridLayout(3, 1, 8, 8));
            {
                tagPanel.setBackground(background);
                JComponent popPanel = new JPanel(new MiksGridLayout(8, 1, 8, 0));
                popPanel.setBackground(background);
                JLabel popLabel = new JLabel(Config.getString("export.publish.tags.popular"), SwingConstants.LEADING);
                popLabel.setFont(smallFont);
                popPanel.add(popLabel);
                for (int i = 0; i < popTags.length; i++) {
                    JCheckBox popTag = new JCheckBox(Config.getString("export.publish.tags.loading"));
                    popTag.setBackground(background);
                    popTag.setFont(smallFont);
                    popTag.setEnabled(false);
                    popTags[i] = popTag;
                    popPanel.add(popTag);
                }

                tagPanel.add(popPanel);

                Box textPanel = new Box(BoxLayout.Y_AXIS);
                {
                    JLabel additionalLabel = new JLabel(Config.getString("export.publish.tags.additional1"),
                            SwingConstants.LEADING);
                    additionalLabel.setFont(smallFont);
                    textPanel.add(additionalLabel);

                    JLabel additionalLabel2 = new JLabel(Config.getString("export.publish.tags.additional2"),
                            SwingConstants.CENTER);
                    additionalLabel2.setFont(smallFont);
                    textPanel.add(additionalLabel2);
                }
                tagPanel.add(textPanel);

                tagArea = new JTextArea();
                tagArea.setRows(3);
                JScrollPane tagScroller = new JScrollPane(tagArea);
                tagPanel.add(tagScroller);
            }
            infoPanel.add(tagPanel, BorderLayout.EAST);
        }

        add(infoPanel);
        add(Box.createVerticalStrut(16));

        JComponent loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        {
            loginPanel.setBackground(background);
            loginPanel.setAlignmentX(LEFT_ALIGNMENT);
            Border border = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory
                    .createEmptyBorder(12, 12, 12, 12));
            loginPanel.setBorder(border);

            JLabel text = new JLabel(Config.getString("export.publish.login"));
            text.setForeground(headingColor);
            text.setVerticalAlignment(SwingConstants.TOP);
            loginPanel.add(text);

            text = new JLabel(Config.getString("export.publish.username"), SwingConstants.TRAILING);
            text.setFont(smallFont);
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
            text.setFont(smallFont);
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

        }
        add(loginPanel);
        add(Box.createVerticalStrut(10));

    }

    /**
     * Set the tags in the UI from the given list.
     * <p>
     * Should be called from event thread
     */
    private void setPopularTags(List<String> tags)
    {
        int minLength = popTags.length < tags.size() ? popTags.length : tags.size();
        for (int i = 0; i < minLength; i++) {
            JCheckBox checkBox = popTags[i];
            checkBox.setText(tags.get(i));
            checkBox.setEnabled(true);
            setTags(getTags());
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
        } else if (!includeSourceCode()){
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
        }
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
    }


    private void checkForExistingScenario()
    {
        checkForExistingScenario(false);
    }
    
    private void checkForExistingScenario(boolean forceRecheck)
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
                private String updateText = " " + Config.getString("export.dialog.continue.update");

                @Override
                public void scenarioExistenceCheckFailed(Exception reason)
                {
                    // If an error occurs, we just reset the text on the export
                    // button.
                    continueButton.setText(getStrippedText());
                }

                @Override
                public void scenarioExistenceChecked(ScenarioInfo info)
                {
                    String currentText = getStrippedText();

                    if (info != null) {
                        // Update existing scenario
                        continueButton.setText(currentText + updateText);
                    }
                    else {
                        continueButton.setText(currentText);
                    }
                }

                private String getStrippedText()
                {
                    String currentText = continueButton.getText();
                    int i = currentText.indexOf(updateText);
                    if (i != -1) {
                        currentText = currentText.substring(0, i);
                    }
                    return currentText;
                }
            };
        }
        scenarioChecker.startScenarioExistenceCheck(serverURL, userName, title, forceRecheck);
    }

    /**
     * The first time this pane is activated we fetch the popular tags from the
     * server (if possible).
     * <P>
     * And we load previously used values if they are stored.
     * 
     */
    @Override
    public void activated(JButton continueButton)
    {
        this.continueButton = continueButton;
        checkForExistingScenario();
        if (firstActivation) {
            firstActivation = false;
            commonTagsLoader = new SwingWorker() {
                @SuppressWarnings("unchecked")
                @Override
                public void finished()
                {
                    List<String> l = (List<String>) getValue();
                    if (l != null) {
                        setPopularTags(l);
                    }
                }

                @Override
                public Object construct()
                {
                    WebPublisher client = new WebPublisher();
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
                        } else {
                            tags.remove(tags.size() - 1);
                        }
                    }
                    catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    return tags;
                }
            };
            commonTagsLoader.start();
            SwingUtilities.invokeLater(new Thread() {
                public void run()
                {
                    setUserName(Config.getPropString("publish.username", ""));
                    loadStoredScenarioInfo();
                }
            });
        }
    }

    @Override
    public boolean prePublish()
    {
        publishedScenarioInfo = new ScenarioInfo();
        updateInfoFromFields(publishedScenarioInfo);
        publishedUserName = userNameField.getText();
        // TODO: Check if scenario exists, and confirm that user wants to
        // continue?
        /*
         * checkForExistingScenario(); Object existResult =
         * scenarioChecker.getResult(); if (existResult == null) { // It is a
         * new scenario - just continue } if (existResult instanceof
         * ScenarioInfo) { // The scenario exists - confirm ScenarioInfo info =
         * (ScenarioInfo) existResult; JButton updateButton = new
         * JButton("Update"); JButton cancelButton = new JButton("Cancel");
         * 
         * JButton[] buttons = new JButton[]{cancelButton, updateButton};
         * MessageDialog d = new MessageDialog( (Dialog)
         * SwingUtilities.getWindowAncestor(this), "The scenario " +
         * info.getTitle() + " exists. If you continue exporting the existing
         * scenario will be updated with this version. Continue exporting?",
         * "Confirm Update", 50, buttons); JButton result = d.displayModal(); if
         * (result != updateButton) { System.out.println("return false"); return
         * false; }
         *  }
         */
        return true;
    }

    @Override
    public void postPublish(boolean success)
    {
        if (success) {
            publishedScenarioInfo.store(project.getProjectProperties());
            Config.putPropString("publish.username", publishedUserName);
            checkForExistingScenario(true);
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
        else {
            return hostname + "/";
        }
    }
}
