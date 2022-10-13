/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg
 
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

import bluej.*;
import bluej.Config;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;
import bluej.utility.javafx.dialog.InputDialog;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Dialog for creating a new Package
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
class NewPackageDialog extends InputDialog<String>
{
    public NewPackageDialog(Window parent)
    {
        super(Config.getString("pkgmgr.newPackage.title"), Config.getString("pkgmgr.newPackage.label"), Config.getString("pkgmgr.newPackage.prompt"), "new-package-dialog");
        initOwner(parent);
        setOKEnabled(false);
    }
    
    public String convert(String fieldText)
    {
        return fieldText.trim(); // Validation is done in convert
    }
    
    public boolean validate(String oldInput, String newInput)
    {
        newInput = newInput.trim();
        
        if (!newInput.isEmpty() && JavaNames.isQualifiedIdentifier(newInput))
        {
            setOKEnabled(true);
            setErrorText("");
            return true;
        }
        else
        {
            setErrorText(Config.getString("pkgmgr.newPackage.error"));
            setOKEnabled(false);
            return true; // Let it be invalid, but show error
        }
    }
}
