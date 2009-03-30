/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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
