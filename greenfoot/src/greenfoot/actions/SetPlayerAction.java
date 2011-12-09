package greenfoot.actions;

import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.SetPlayerDialog;
import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import bluej.Config;

public class SetPlayerAction extends AbstractAction
{
    private GreenfootFrame frame;
    
    public SetPlayerAction(GreenfootFrame frame)
    {
        super(Config.getString("set.player"));
        
        this.frame = frame;
    }
        
    @Override
    public void actionPerformed(ActionEvent e)
    {
        SetPlayerDialog dlg = new SetPlayerDialog(frame, GreenfootUtilDelegateIDE.getInstance().getUserName());
        dlg.setVisible(true);
        GreenfootUtilDelegateIDE.getInstance().setUserName(dlg.getPlayerName());
    }

}
