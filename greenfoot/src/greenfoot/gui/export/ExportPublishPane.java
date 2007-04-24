/*
 * ExportPublishPane.java
 *
 * @author Michael Kolling
 * @version $Id: ExportPublishPane.java 5006 2007-04-24 21:31:11Z mik $
 */

package greenfoot.gui.export;

import bluej.BlueJTheme;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class ExportPublishPane extends ExportPane
{
    public static final String FUNCTION = "PUBLISH";
    public static final Color background = new Color(166, 188, 202);
    
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
                
                JLabel text = new JLabel("Information for display on MyGame");
                text.setForeground(Color.GRAY);
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

                URLField = new JTextField();
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

                JLabel text = new JLabel("Login information. Create you account at MyGame.");
                text.setAlignmentX(LEFT_ALIGNMENT);
                text.setForeground(Color.GRAY);
                loginPanel.add(text);                
                loginPanel.add(Box.createVerticalStrut(5));

                JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                {
                    flowPanel.setBackground(background);
                    flowPanel.add(new JLabel("Username:"));
                    userNameField = new JTextField(12);
                    flowPanel.add(userNameField);
                    flowPanel.add(new JLabel("   Password:"));
                    passwordField = new JPasswordField(12);
                    flowPanel.add(passwordField);
                }
                loginPanel.add(flowPanel);
            }
            inputPanel.add(loginPanel);
            inputPanel.add(Box.createVerticalStrut(16));
            
            inputPanel.add(Box.createVerticalStrut(5));
            
            inputPanel.add(extraControls);
        }

        add(inputPanel);
        add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));
    }
}
