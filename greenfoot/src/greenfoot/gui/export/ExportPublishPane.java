

package greenfoot.gui.export;

import greenfoot.export.WebPublisher;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.ui.MiksGridLayout;
import bluej.utility.SwingWorker;

/**
 * ExportPublishPane.java
 *
 * @author Michael Kolling
 * @version $Id: ExportPublishPane.java 5713 2008-04-23 18:16:35Z polle $
 */
public class ExportPublishPane extends ExportPane
{
    public static final int IMAGE_WIDTH = 120;
    public static final int IMAGE_HEIGHT = 70;
    
    public static final String FUNCTION = "PUBLISH";
    private static final Color background = new Color(166, 188, 202);
    private static final Color headingColor = new Color(40, 75, 125);
    private static final String serverURL = Config.getPropString("greenfoot.gameserver.address", "http://www.greenfootgallery.org");
    private static final String createAccountUrl = Config.getPropString("greenfoot.gameserver.createAccount.address", "http://www.greenfootgallery.org/users/new");
    private static final String serverName = Config.getPropString("greenfoot.gameserver.name", "Greenfoot Gallery");

    private static final String helpLine1 = Config.getString("export.publish.help") + " " + serverName ;

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
    private JTextField tagField;
    private Vector<String> popularTags = new Vector<String>();
    protected boolean commonTagsLoaded;
    private SwingWorker commonTagsLoader;

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
        setBorder(BlueJTheme.dialogBorder);

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

        JPanel infoPanel = new JPanel(new BorderLayout(0, 8));
        {
            infoPanel.setAlignmentX(LEFT_ALIGNMENT);
            infoPanel.setBackground(background);

            Border border = BorderFactory.createCompoundBorder(
                                BorderFactory.createLoweredBevelBorder(),
                                BorderFactory.createEmptyBorder(12, 12, 12, 12));
            infoPanel.setBorder(border);

            JLabel text = new JLabel(Config.getString("export.publish.info1") + " " + serverName + " "
                    + Config.getString("export.publish.info2"));
            text.setForeground(headingColor);
            infoPanel.add(text, BorderLayout.NORTH); 

            JPanel dataPanel = new JPanel(new MiksGridLayout(7, 2, 8, 8));
            {
                dataPanel.setBackground(background);
                
                imagePanel = new ImageEditPanel(IMAGE_WIDTH, IMAGE_HEIGHT);   
                imagePanel.setBackground(background);
                text = new JLabel(Config.getString("export.publish.image"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                dataPanel.add(imagePanel);                
                

                text = new JLabel(Config.getString("export.publish.title"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                
                titleField = new JTextField(scenarioName);
                titleField.setInputVerifier(new InputVerifier(){
                    @Override
                    public boolean verify(JComponent input)
                    {
                        String text = titleField.getText();
                        return text.length() > 0;
                    }});
                dataPanel.add(titleField);

                text = new JLabel(Config.getString("export.publish.tags"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                
                Box tagPanel = new Box(BoxLayout.X_AXIS);
                {
                    tagField = new JTextField("");
                    final Object popularTagsLabel = new Object() {
                        private String string = Config.getString("export.publish.tags.popular");
                        
                        public String toString()
                        {
                            return string;
                        }
                    };
                    final JComboBox popularTagsCombo = new JComboBox(popularTags);

                    popularTagsCombo.insertItemAt(popularTagsLabel, 0);
                    popularTagsCombo.setSelectedIndex(0);
                    popularTagsCombo.setEditable(false);

                    // Give it a nice size
                    popularTagsCombo.setPrototypeDisplayValue(popularTagsLabel);

                    // Add a popup listener that can remove the popular tags
                    // label while displaying the popup
                    popularTagsCombo.addPopupMenuListener(new PopupMenuListener() {
                        public void popupMenuCanceled(PopupMenuEvent e)
                        {
                        }
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
                        {
                            popularTagsCombo.insertItemAt(popularTagsLabel, 0);
                            popularTagsCombo.setSelectedIndex(0);
                            popularTagsCombo.removeItemAt(1);
                        }

                        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                        {
                            popularTagsCombo.insertItemAt("", 0);
                            popularTagsCombo.setSelectedIndex(0);
                            popularTagsCombo.removeItemAt(1);
                        }
                    });

                    popularTagsCombo.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e)
                        {
                            JComboBox cb = (JComboBox) e.getSource();
                            Object ob =  cb.getSelectedItem();
                            if(ob == popularTagsLabel) {
                                return;
                            }
                            String item = (String) cb.getSelectedItem();
                            String currentTags = tagField.getText().trim();

                            String[] tags = currentTags.split(" ");
                            boolean tagExists = false;
                            for (int i = 0; i < tags.length; i++) {
                                String tag = tags[i];
                                if (tag.trim().equals(item)) {
                                    tagExists = true;
                                }
                            }
                            if (tagExists || item.trim().equals("")) {
                                return;
                            }
                            else if (currentTags.equals("")) {
                                tagField.setText(item);
                            }
                            else {
                                tagField.setText(currentTags + " " + item);
                            }
                        }
                    });
                    tagPanel.add(tagField);
                    tagPanel.add(popularTagsCombo);
                }
                dataPanel.add(tagPanel);
                
                text = new JLabel(Config.getString("export.publish.shortDescription"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);

                shortDescriptionField = new JTextField();
                dataPanel.add(shortDescriptionField);

                text = new JLabel(Config.getString("export.publish.longDescription"), SwingConstants.TRAILING);
                text.setVerticalAlignment(SwingConstants.TOP);
                text.setFont(smallFont);
                dataPanel.add(text);

                descriptionArea = new JTextArea();
                descriptionArea.setRows(4);
                JScrollPane description = new JScrollPane(descriptionArea);
                dataPanel.add(description);

                text = new JLabel(Config.getString("export.publish.url"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);

                urlField = new JTextField();
                dataPanel.add(urlField);
                
                text = new JLabel(Config.getString("export.publish.includeSource"), SwingConstants.TRAILING);                
                text.setFont(smallFont);
                dataPanel.add(text);
                includeSource = new JCheckBox();
                includeSource.setOpaque(false);
                includeSource.setSelected(false);
                dataPanel.add(includeSource);
            }
            infoPanel.add(dataPanel, BorderLayout.CENTER);
        }

        add(infoPanel);
        add(Box.createVerticalStrut(16));

        JPanel loginPanel = new JPanel(new BorderLayout(40, 0));
        {
            loginPanel.setAlignmentX(LEFT_ALIGNMENT);
            loginPanel.setBackground(background);
                
            Border border = BorderFactory.createCompoundBorder(
                                BorderFactory.createLoweredBevelBorder(),
                                BorderFactory.createEmptyBorder(12, 12, 12, 12));
            loginPanel.setBorder(border);

            JLabel text = new JLabel(Config.getString("export.publish.login"));
            text.setForeground(headingColor);
            text.setVerticalAlignment(SwingConstants.TOP);
            loginPanel.add(text, BorderLayout.WEST); 

            JPanel dataPanel = new JPanel(new MiksGridLayout(2, 2, 8, 8));
            {
                dataPanel.setBackground(background);
                text = new JLabel(Config.getString("export.publish.username"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                userNameField = new JTextField(10);
                dataPanel.add(userNameField);
                text = new JLabel(Config.getString("export.publish.password"), SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                passwordField = new JPasswordField(10);
                dataPanel.add(passwordField);
            }
            loginPanel.add(dataPanel, BorderLayout.CENTER);
                
        
            JLabel createAccountLabel = new JLabel(Config.getString("export.publish.createAccount"));
            {
                createAccountLabel.setBackground(background);

                createAccountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                GreenfootUtil.makeLink(createAccountLabel, createAccountUrl);
            }
            loginPanel.add(createAccountLabel, BorderLayout.EAST);  
        }
        add(loginPanel);
        add(Box.createVerticalStrut(20));
        
        JPanel extraPanel = new JPanel(new BorderLayout(20, 0));
        {
            extraPanel.setAlignmentX(LEFT_ALIGNMENT);
            extraPanel.add(lockScenario, BorderLayout.WEST);
            extraPanel.add(lockScenarioDescription, BorderLayout.SOUTH);      
        }
        add(extraPanel);
    }

    /**
     * The first time this pane is activated we fetch the popular tags from the server (if possible).
     * 
     * <p>
     * 
     * TODO And, if the scenario exists, we issue a warning and give the user the option to fetch all info.
     * 
     * <p>
     * Not thread safe.
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
                    if(l != null) {
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
                        tags = client.getCommonTags(hostAddress, 10);
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

    public void setPopularTags(List<String> tags) {
        popularTags.addAll(tags);
    }

    /**
     * Returns a list of the tags that the user chose for this scenario.
     */
    public List<String> getTags()
    {
        String currentTags = tagField.getText().trim();
        String[] tags = currentTags.split(" ");
        List<String> tagList = new LinkedList<String>();
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i].trim();
            if(! tag.equals("")) {
                tagList.add(tag);
            }
        }
        return tagList;
    }

}
