package greenfoot.actions;

import greenfoot.gui.MessageDialog;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;

import bluej.BlueJTheme;
import bluej.Config;


/**
 * Removes a class.
 * 
 * @author Poul Henriksen
 * @version $Id:$
 */
public class RemoveClassAction extends AbstractAction
{
    private ClassView cls;
    private JFrame frame;
    

    private static String confirmRemoveTitle = Config.getString("remove.confirm.title");
    private static String confirmRemoveText1 = Config.getString("remove.confirm.text1");
    private static String confirmRemoveText2 = Config.getString("remove.confirm.text2");
    
    public RemoveClassAction(ClassView view, JFrame frame)
    {
        super(Config.getString("remove.class"));
        this.cls = view;
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (confirmRemoveClass(cls, frame)) {
            cls.remove();
        }
    }
    
    public static boolean confirmRemoveClass(ClassView cls, JFrame frame)
    {
        JButton okButton = BlueJTheme.getOkButton();
        JButton cancelButton = BlueJTheme.getCancelButton();
        MessageDialog confirmRemove = new MessageDialog(frame, confirmRemoveText1 + " " + cls.getClassName()
                + ". " + confirmRemoveText2, confirmRemoveTitle, 100, new JButton[]{okButton, cancelButton});
        return confirmRemove.displayModal() == okButton;
    }
}
