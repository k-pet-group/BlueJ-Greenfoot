import java.applet.Applet;
import java.awt.Graphics;
import java.awt.TextField;
import java.awt.Button;
import java.awt.Label;
import java.awt.event.*;

/**
 * Class CaseConverter - A simple applet that takes input from a text field 
 * and converts to upper or lower case in response to user button selection. 
 * Works well with a width of 250 and height of 100.  It uses the default
 * layout (FlowLayout) so different sizing will alter the appearance.
 *
 * @author Bruce Quig 
 * @version 08/07/1999
 */

public class CaseConverter extends Applet implements ActionListener
{
    // instance variables - replace the example below with your own
    private TextField inputField;
    private final String UPPERCASE = "UPPERCASE";
    private final String LOWERCASE = "lowercase";
    private final String CLEAR = "Clear";


    /**
     * Called by the browser or applet viewer to inform this Applet that it
     * has been loaded into the system. It is always called before the first 
     * time that the start method is called.
     */
    public void init()
    {
        // provide any initialisation necessary for your Applet
        add(new Label("CASE CONVERTER APPLET"));
        add(new Label("Enter a String"));
        inputField = new TextField(16);
        add(inputField);
        Button uppercase = new Button(UPPERCASE);
        uppercase.addActionListener(this);
        add(uppercase);
        Button lowercase = new Button(LOWERCASE);
        lowercase.addActionListener(this); 
        add(lowercase);
        Button clear = new Button(CLEAR);
        clear.addActionListener(this);
        add(clear);
    }

 
    /**
     * Returns information about this applet. 
     * An applet should override this method to return a String containing 
     * information about the author, version, and copyright of the Applet.
     *
     * @return a String representation of information about this Applet
     */
    public String getAppletInfo()
    {
        // provide information about the applet
        return "Title: Case Converter  \nAuthor: Bruce Quig  \nA simple applet that converts text to upper or lower case. ";
    }

 
    /**
     * ActionListener Interface method.
     * Called when action events occur with registered components that
     * can fire action events.
     * @param  ae   the ActionEvent object created by the event
     */
    public void actionPerformed(ActionEvent ae)
    {
        String command = ae.getActionCommand();
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
