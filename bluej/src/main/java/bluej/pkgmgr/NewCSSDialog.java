/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017  Michael Kolling and John Rosenberg
 
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

import javafx.stage.Window;

import bluej.Config;
import bluej.utility.JavaNames;
import bluej.utility.javafx.dialog.InputDialog;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Dialog for creating a new CSS file
 */
@OnThread(Tag.FXPlatform)
class NewCSSDialog extends InputDialog<String>
{
    public NewCSSDialog(Window parent)
    {
        super(Config.getString("pkgmgr.newCSS.title"), Config.getString("pkgmgr.newCSS.label"), Config.getString("pkgmgr.newCSS.prompt"), "new-css-dialog", ".css");
        initOwner(parent);
        setOKEnabled(false);
    }
    
    public String convert(String fieldText)
    {
        return fieldText.trim() + ".css"; // Validation is done in convert
    }
    
    public boolean validate(String oldInput, String newInput)
    {
        newInput = newInput.trim();
        
        if (!newInput.isEmpty() && !newInput.contains("/") && !newInput.contains("\\"))
        {
            setOKEnabled(true);
            setErrorText("");
            return true;
        }
        else
        {
            setErrorText(Config.getString("pkgmgr.newCSS.error"));
            setOKEnabled(false);
            return true; // Let it be invalid, but show error
        }
    }
}
