/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017,2025  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import bluej.prefmgr.PrefMgr;
import javafx.scene.control.ComboBox;
import javafx.stage.Window;

import bluej.Config;
import bluej.utility.javafx.dialog.InputDialog;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * Dialog for creating a new plain text file (.css, .txt, etc)
 */
@OnThread(Tag.FXPlatform)
class NewTextFileDialog extends InputDialog<String>
{
    private final ComboBox<String> extension;

    public NewTextFileDialog(Window parent)
    {
        super(Config.getString("pkgmgr.newText.title"), Config.getString("pkgmgr.newText.label"), Config.getString("pkgmgr.newText.prompt"), "new-textfile-dialog");
        initOwner(parent);
        setOKEnabled(false);
        setResizable(true);

        extension = new ComboBox<>();
        extension.getItems().add(".css");
        extension.getItems().addAll(PrefMgr.getEditorFormattedTextFileExtensionsList());
        extension.getSelectionModel().select(0);
        addContentAfterField(extension);
    }
    
    public String convert(String fieldText)
    {
        return fieldText.trim() + extension.getSelectionModel().getSelectedItem(); // Validation is done in convert
    }
    
    public boolean validate(String oldInput, String newInputUntrimmed)
    {
        final String newInput = newInputUntrimmed.trim();
        
        if (!newInput.isEmpty() && !newInput.contains("/") && !newInput.contains("\\"))
        {
            setOKEnabled(true);
            setErrorText("");
            return true;
        }
        else
        {
            setErrorText(Config.getString("pkgmgr.newText.error"));
            setOKEnabled(false);
            return true; // Let it be invalid, but show error
        }
    }
}
