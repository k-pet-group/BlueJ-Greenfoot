package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;
import bluej.guibuilder.graphics.Separator;


/**
 * MessageBox.java
 * A Class for showing a simple MessageBox, blocking the input queue
 * until the user selects 'OK'
 *
 * Created: Wed Sep  2 11:19:22 1998
 *
 * @author Morten Knudsen
 * @version 1.0
 */
public class MessageBox extends Dialog {

    private Panel p = new Panel();
    private Panel buttonPanel = new Panel();
    private TextField tfMessage;
    private Font font;
    private Button okButton = new Button("OK");
    private ButtonListener buttonListener = new ButtonListener();
    private Frame frame;


    /**
     * Constructs a MessageBox and displays it.
     * It is possible to give a message String and a title String
     * The type is ignored but reserved for future enhancement.
     *
     * @param f Frame to which the dialog belongs.
     * @param type    The type of the MessageBox. Ignored at the moment.
     * @param message A String containing the message to be shown.
     * @param title A String containing the title of the MessageBox
     */
    public MessageBox(Frame f,int type,String message,String title) {
        super(f,true );
        setTitle(title);
        // add Listeners
        
        okButton.addActionListener(buttonListener);
        buttonPanel.add(okButton);
        //buttonPanel.add(cancelButton);
        
        setLayout(new BorderLayout());
        tfMessage = new TextField(message);
        tfMessage.setEditable(false);
        add(tfMessage,"Center");
        add(buttonPanel,"South");
        pack();
        show();
    }


    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
            {
                if(e.getSource().equals(okButton))
                {
                    dispose();
                }
            }
    }
 
                       
                    
} // MessageBox
