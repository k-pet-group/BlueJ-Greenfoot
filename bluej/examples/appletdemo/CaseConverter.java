import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class CaseConverter - A simple applet that takes input from a text field 
 * and converts to upper or lower case in response to user button selection. 
 * Works well with a width of 300 and height of 120.
 *  
 * Aug 2004: Updated from Applet to JApplet (mik)
 *
 * @author Bruce Quig 
 * @author Michael Kölling
 * 
 * @version 2004-08-04
 */
public class CaseConverter extends JApplet
     implements ActionListener
{
    private JTextField inputField;
    private final String UPPERCASE = "UPPERCASE";
    private final String LOWERCASE = "lowercase";
    private final String CLEAR = "Clear";

     /**
     * Called by the browser or applet viewer to inform this JApplet that it
     * has been loaded into the system. It is always called before the first 
     * time that the start method is called.
     */
    public void init()
    {
        // GUI elements are added to the applet's content pane, so get it for us.
        Container contentPane = getContentPane();
        
        // set a layout with some spacing
        contentPane.setLayout(new BorderLayout(12,12));
        
        // add the title label
        JLabel title = new JLabel("Case Converter - A BlueJ demo applet");
        contentPane.add(title, BorderLayout.NORTH);
       
        // create the center part with prompt and text field and add it
        JPanel centerPanel = new JPanel();
        JLabel prompt = new JLabel("Enter a string:");
        centerPanel.add(prompt);
        inputField = new JTextField(16);
        centerPanel.add(inputField);
        
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // make a panel for the buttons
        JPanel buttonPanel = new JPanel();
        
        // add the buttons to the button panel
        JButton uppercase = new JButton(UPPERCASE);
        uppercase.addActionListener(this);
        buttonPanel.add(uppercase);
        
        JButton lowercase = new JButton(LOWERCASE);
        lowercase.addActionListener(this); 
        buttonPanel.add(lowercase);
        
        JButton clear = new JButton(CLEAR);
        clear.addActionListener(this);
        buttonPanel.add(clear);
        
        // add the buttons panel to the content pane
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Returns information about this applet. 
     * An applet should override this method to return a String containing 
     * information about the author, version, and copyright of the JApplet.
     *
     * @return a String representation of information about this JApplet
     */
    public String getAppletInfo()
    {
        return "Title: Case Converter  \n" +
               "Author: Bruce Quig  \n" +
               "A simple applet that converts text to upper or lower case. ";
    }

    /**
     * ActionListener Interface method.
     * Called when action events occur with registered components that
     * can fire action events.
     * @param  ae   the ActionEvent object created by the event
     */
    public void actionPerformed(ActionEvent evt)
    {
        String command = evt.getActionCommand();
        // if clear button pressed
        if(CLEAR.equals(command))
            inputField.setText("");
        // uppercase button pressed
        else if(UPPERCASE.equals(command))
            inputField.setText(inputField.getText().toUpperCase());
        // lowercase button pressed
        else if(LOWERCASE.equals(command))
            inputField.setText(inputField.getText().toLowerCase());
    }
}
