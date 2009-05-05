package greenfoot.gui;

import javax.swing.JButton;
import javax.swing.JOptionPane;

/**
 * A button used for editing files. The editor is supplied and given the file
 * to edit as its first argument.
 *
 * @author Michael Berry (mjrb4)
 * @version 05/05/09
 */
public class EditButton extends JButton
{
    /** Whether this is the first time the button has been clicked. */
    private boolean firstRun;
    /** The location of the editor. */
    private String editor;

    /**
     * Create a new button with a label
     * @param label the label to display on the button.
     */
    public EditButton(String label)
    {
        super(label);
        firstRun = true;
        /* Just a default value for now */
        editor = "C:\\windows\\system32\\mspaint.exe";
    }

    /**
     * Get the location of the editor used. Display a prompt for the editor if
     * this hasn't been used before.
     * @return the absolute location of the editor.
     */
    public String getEditor()
    {
        if(firstRun) {
            String message = "Please enter the location of your image editing program (Blank for default):";
            String input = JOptionPane.showInputDialog(null, message);
            if(input!=null && ! input.equals("")) {
                editor = input;
            }
            firstRun = false;
        }
        return editor;
    }

}
