/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
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
package bluej.utility.javafx;

import bluej.Config;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An FX dialog containing all repeated to code needed when creating
 * version control dialogs.
 *
 * @author amjad
 */
public class FXCustomizedDialog<R> extends Dialog<R>
{
    private boolean modal = false;

    protected FXCustomizedDialog()
    {
        // Provide a content pane by default:
        Pane content = new Pane();
        getDialogPane().setContent(content);
        initModality(Modality.NONE);
    }

    protected FXCustomizedDialog(FXPlatformSupplier<Window> owner)
    {
        this();
        initOwner(owner.get());
    }
    
    protected void setModal(boolean makeModal)
    {
        this.modal = makeModal;
        initModality(makeModal ? Modality.APPLICATION_MODAL : Modality.NONE);
    }

    protected void rememberPosition(String locationPrefix)
    {
        setOnShown(e -> Config.rememberPosition(asWindow(), locationPrefix));
    }

    protected Node getContentPane()
    {
        return getDialogPane().getContent();
    }

    protected void setContentPane(Node content)
    {
        getDialogPane().setContent(content);
    }

    public Window asWindow()
    {
        Scene scene = getDialogPane().getScene();
        if (scene == null)
            return null;
        else
            return scene.getWindow();
    }
    
    public void setLocationRelativeTo(Node comp)
    {
        
    }

    @OnThread(Tag.FXPlatform)
    public void setLocationRelativeTo(Window window)
    {

    }
    
    protected void dialogThenHide(FXPlatformRunnable action)
    {
            action.run();
            hide();
    }
}
