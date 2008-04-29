package greenfoot.gui.export;

import greenfoot.export.WebPublisher;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.UnknownHostException;
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

import bluej.Config;
import bluej.groupwork.ui.MiksGridLayout;
import bluej.utility.SwingWorker;

/**
 * ExportPublishPane.java
 * 
 * @author Michael Kolling
 * @version $Id: ExportPublishPane.java 5718 2008-04-29 17:48:33Z polle $
 */
public class ExportPublishPane extends ExportPane
{
    public static final int IMAGE_WIDTH = 120;
    public static final int IMAGE_HEIGHT = 70;

    public static final String FUNCTION = "PUBLISH";
    private static final Color background = new Color(166, 188, 202);
    private static final Color headingColor = new Color(40, 75, 125);
    private static final String serverURL = Config.getPropString("greenfoot.gameserver.address",
            "http://greenfootgallery.org");
    private static final String createAccountUrl = Config.getPropString("greenfoot.gameserver.createAccount.address",
            "http://greenfootgallery.org/users/new");
    private static final String serverName = Config.getPropString("greenfoot.gameserver.name", "Greenfoot Gallery");

    private static final String helpLine1 = Config.getString("export.publish.help") + " " + serverName;

    private JTextField titleField;
    private JTextField shortDescriptionField;
    private JTextArea descriptionArea;
    private JTextField urlField;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private ImageEditPanel imagePanel;
    private JCheckBox includeSource;

    /**
     * Name of the scenario as Greenfoot knows it. This is NOT necessarily the
     * title that will be used on MyGame
     */
    private String scenarioName;
    private SwingWorker commonTagsLoader;
    private JCheckBox[] popTags = new JCheckBox[7];
    private JTextArea tagArea;

    /** Creates a new instance of ExportPublishPane */
    public ExportPublishPane(String scenarioName)
    {
        super();
        this.scenarioName = scenarioName;
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

                titleField = new JTextField(scenarioName);
                titleField.setInputVerifier(new InputVerifier() {
                    @Override
                    public boolean verify(JComponent input)
                    {
                        String text = titleField.getText();
                        return text.length() > 0;
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
     * The first time this pane is activated we fetch the popular tags from the
     * server (if possible).
     * 
     * 
     */
    @Override
    public void activated()
    {        
        if (commonTagsLoader == null) {
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
                        tags = client.getCommonTags(hostAddress, popTags.length);
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
        }
    }

    /**
     * Set the tags in the UI from the given list.
     * <p>
     * Not thread safe.
     */
    private void setPopularTags(List<String> tags)
    {
        for (int i = 0; i < popTags.length; i++) {
            JCheckBox checkBox = popTags[i];
            checkBox.setText(tags.get(i));
            checkBox.setEnabled(true);
        }
    }

    /**
     * Returns a list of the tags that the user chose for this scenario.
     */
    public List<String> getTags()
    {
        System.out.println("get tags");
        List<String> tagList = new LinkedList<String>();
        
        for (int i = 0; i < popTags.length; i++) {
            JCheckBox checkBox = popTags[i];
            if(checkBox.isSelected()) {
                System.out.println("Adding tag: " + checkBox.getText());
                tagList.add(checkBox.getText());
            }
        }
        
        
        String currentTags = tagArea.getText().trim();
        String[] tags = currentTags.split("\\s");
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i].trim();
            if (!tag.equals("")) {
                System.out.println("Adding tag: " + tag);
                tagList.add(tag);
            }
        }
        return tagList;
    }

}
