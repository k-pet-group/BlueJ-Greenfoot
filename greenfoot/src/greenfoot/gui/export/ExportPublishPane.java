/*
 * ExportPublishPane.java
 *
 * @author Michael Kolling
 * @version $Id: ExportPublishPane.java 5021 2007-04-26 10:14:40Z mik $
 */

package greenfoot.gui.export;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.Utility;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class ExportPublishPane extends ExportPane
{
    public static final String FUNCTION = "PUBLISH";
    private static final Color background = new Color(166, 188, 202);
    private static final Color urlColor = new Color(0, 90, 200);
    private static final Color headingColor = new Color(40, 75, 125);
    private static final String serverURL = Config.getPropString("greenfoot.gameserver.address", "http://stompt.org");
    private static final String serverName = Config.getPropString("greenfoot.gameserver.name", "stompt.org");
    
    private static final String helpLine1 = "Publish the scenario to MyGame (mygame.java.sun.com).";
    
    private JTextField shortDescriptionField;
    private JTextArea descriptionArea;
    private JTextField URLField;
    private JTextField userNameField;
    private JPasswordField passwordField;
        

    /** Creates a new instance of ExportPublishPane */
    public ExportPublishPane(String scenarioName) 
    {
        super();
        makePane();
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
        return URLField.getText();
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
     * Open the games server in a web browser.
     */
    private void openServerPage()
    {
        Utility.openWebBrowser(serverURL);
    }
    
    /**
     * Build the component.
     */
    private void makePane()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.dialogBorder);

        JLabel helpText1 = new JLabel(helpLine1);
        add(helpText1);

        Font smallFont = helpText1.getFont().deriveFont(Font.ITALIC, 11.0f);

        add(Box.createVerticalStrut(5));

        JPanel inputPanel = new JPanel();
        {
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            inputPanel.add(Box.createVerticalStrut(5));

            JPanel infoPanel = new JPanel();
            {
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setAlignmentX(LEFT_ALIGNMENT);
                infoPanel.setBackground(background);

                Border border = BorderFactory.createCompoundBorder(
                                    BorderFactory.createLoweredBevelBorder(),
                                    BorderFactory.createEmptyBorder(12, 12, 12, 12));
                infoPanel.setBorder(border);
                
                JLabel text = new JLabel("Information for display on MyGame (optional).");
                text.setForeground(headingColor);
                infoPanel.add(text);                
                infoPanel.add(Box.createVerticalStrut(5));

                text = new JLabel("A one-line scenario description:");
                text.setFont(smallFont);
                infoPanel.add(text);
                infoPanel.add(Box.createVerticalStrut(5));

                shortDescriptionField = new JTextField();
                shortDescriptionField.setAlignmentX(LEFT_ALIGNMENT);
                infoPanel.add(shortDescriptionField);
                infoPanel.add(Box.createVerticalStrut(16));

                text = new JLabel("A slightly longer scenario description:");
                text.setFont(smallFont);
                infoPanel.add(text);
                infoPanel.add(Box.createVerticalStrut(5));

                descriptionArea = new JTextArea();
                descriptionArea.setRows(3);
                JScrollPane description = new JScrollPane(descriptionArea);
                description.setAlignmentX(LEFT_ALIGNMENT);
                infoPanel.add(description);
                infoPanel.add(Box.createVerticalStrut(16));

                text = new JLabel("Your own web page (a URL):");
                text.setFont(smallFont);
                infoPanel.add(text);
                infoPanel.add(Box.createVerticalStrut(5));

                URLField = new JTextField("http://");
                URLField.setAlignmentX(LEFT_ALIGNMENT);
                infoPanel.add(URLField);
            }
            inputPanel.add(infoPanel);
            inputPanel.add(Box.createVerticalStrut(16));

            JPanel loginPanel = new JPanel();
            {
                loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
                loginPanel.setAlignmentX(LEFT_ALIGNMENT);
                loginPanel.setBackground(background);
                
                Border border = BorderFactory.createCompoundBorder(
                                    BorderFactory.createLoweredBevelBorder(),
                                    BorderFactory.createEmptyBorder(12, 12, 12, 12));
                loginPanel.setBorder(border);

                JLabel text = new JLabel("Login information. To create an account, go to MyGame.");
                text.setAlignmentX(LEFT_ALIGNMENT);
                text.setForeground(headingColor);
                loginPanel.add(text);                
                loginPanel.add(Box.createVerticalStrut(5));

                JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                {
                    flowPanel.setAlignmentX(LEFT_ALIGNMENT);
                    flowPanel.setBackground(background);
                    flowPanel.add(new JLabel("Username:"));
                    userNameField = new JTextField(10);
                    flowPanel.add(userNameField);
                    flowPanel.add(new JLabel("   Password:"));
                    passwordField = new JPasswordField(10);
                    flowPanel.add(passwordField);
                }
                loginPanel.add(flowPanel);
            }
            inputPanel.add(loginPanel);
            inputPanel.add(Box.createVerticalStrut(20));
            
            inputPanel.add(extraControls);
            inputPanel.add(Box.createVerticalStrut(5));            
            
            JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                urlPanel.add(new JLabel("Go to"));
                JLabel urlLabel = new JLabel(serverName);
                {
                    urlLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    urlLabel.setForeground(urlColor);
                    urlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                    urlLabel.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) { openServerPage(); }
                    });
                }
                urlPanel.add(urlLabel);
                urlPanel.setAlignmentX(LEFT_ALIGNMENT);
            }
            inputPanel.add(urlPanel);
        }

        add(inputPanel);
    }
}
