/*
 * ExportPublishPane.java
 *
 * @author Michael Kolling
 * @version $Id: ExportPublishPane.java 5027 2007-04-26 14:50:15Z mik $
 */

package greenfoot.gui.export;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.ui.MiksGridLayout;
import bluej.utility.MultiLineLabel;
import bluej.utility.Utility;
import java.awt.BorderLayout;
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

        add(Box.createVerticalStrut(12));

        JPanel infoPanel = new JPanel(new BorderLayout(0, 8));
        {
            infoPanel.setAlignmentX(LEFT_ALIGNMENT);
            infoPanel.setBackground(background);

            Border border = BorderFactory.createCompoundBorder(
                                BorderFactory.createLoweredBevelBorder(),
                                BorderFactory.createEmptyBorder(12, 12, 12, 12));
            infoPanel.setBorder(border);
                
            JLabel text = new JLabel("Information for display on MyGame (optional)");
            text.setForeground(headingColor);
            infoPanel.add(text, BorderLayout.NORTH); 

            JPanel dataPanel = new JPanel(new MiksGridLayout(3, 2, 8, 8));
            {
                dataPanel.setBackground(background);
                text = new JLabel("A one-line scenario description:", SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);

                shortDescriptionField = new JTextField();
                dataPanel.add(shortDescriptionField);

                text = new JLabel("A slightly longer scenario description:", SwingConstants.TRAILING);
                text.setVerticalAlignment(SwingConstants.TOP);
                text.setFont(smallFont);
                dataPanel.add(text);

                descriptionArea = new JTextArea();
                descriptionArea.setRows(3);
                JScrollPane description = new JScrollPane(descriptionArea);
                dataPanel.add(description);

                text = new JLabel("Your own web page (a URL):", SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);

                URLField = new JTextField("http://");
                dataPanel.add(URLField);
            }
            infoPanel.add(dataPanel, BorderLayout.CENTER);
        }

        add(infoPanel);
        add(Box.createVerticalStrut(16));

        JPanel loginPanel = new JPanel(new BorderLayout(30, 0));
        {
            loginPanel.setAlignmentX(LEFT_ALIGNMENT);
            loginPanel.setBackground(background);
                
            Border border = BorderFactory.createCompoundBorder(
                                BorderFactory.createLoweredBevelBorder(),
                                BorderFactory.createEmptyBorder(12, 12, 12, 12));
            loginPanel.setBorder(border);

            JLabel text = new JLabel("Login information");
            text.setForeground(headingColor);
            text.setVerticalAlignment(SwingConstants.TOP);
            loginPanel.add(text, BorderLayout.WEST); 

            JPanel dataPanel = new JPanel(new MiksGridLayout(2, 2, 8, 8));
            {
                dataPanel.setBackground(background);
                text = new JLabel("Username:", SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                userNameField = new JTextField(10);
                dataPanel.add(userNameField);
                text = new JLabel("Password:", SwingConstants.TRAILING);
                text.setFont(smallFont);
                dataPanel.add(text);
                passwordField = new JPasswordField(10);
                dataPanel.add(passwordField);
            }
            loginPanel.add(dataPanel, BorderLayout.CENTER);
                
            MultiLineLabel helptext = new MultiLineLabel("To create an account,");
            helptext.setBackground(background);
            helptext.addText("go to MyGame.");
            helptext.setForeground(headingColor);
            loginPanel.add(helptext, BorderLayout.EAST);     
        }
        add(loginPanel);
        add(Box.createVerticalStrut(20));
        
        JPanel extraPanel = new JPanel(new BorderLayout(20, 0));
        {
            extraPanel.setAlignmentX(LEFT_ALIGNMENT);
            extraPanel.add(extraControls, BorderLayout.WEST);

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
            extraPanel.add(urlPanel, BorderLayout.EAST);
        }
        add(extraPanel);
    }
}
