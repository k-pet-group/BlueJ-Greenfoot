/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2019  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.utility.JavaNames;
import bluej.utility.javafx.dialog.InputDialog;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Dialog for duplicating a Java/Stride class.
 *
 * @author Amjad Altadmri
 */
@OnThread(Tag.FXPlatform)
public class DuplicateClassDialog extends InputDialog<String>
{
    /**
     * Creates a DuplicateClassDialog, which prompts the user to enter (or confirm a suggested) name for the new class.
     *
     * @param parent      the PkgMgrFrame's window which represents the package working on.
     * @param name        the suggested name for the duplicated class.
     * @param sourceType  the type of the class's source; currently, Java or Stride.
     */
    public DuplicateClassDialog(Window parent, String name, SourceType sourceType)
    {
        super(Config.getString("pkgmgr.duplicate.title"), Config.getString("pkgmgr.newClass.label"),
                Config.getString("pkgmgr.newClass.prompt"), "new-class-dialog", "." + sourceType.name());
        initOwner(parent);
        setOKEnabled(false);
        field.setText(name);
    }

    @Override
    public String convert(String fieldText)
    {
        return fieldText.trim(); // Validation is done in convert
    }

    @Override
    public boolean validate(String oldInput, String newInput)
    {
        newInput = newInput.trim();
        
        if (!newInput.isEmpty() && JavaNames.isIdentifier(newInput))
        {
            setOKEnabled(true);
            setErrorText("");
            return true;
        }
        else
        {
            setErrorText(Config.getString("pkgmgr.duplicate.error.notValidClassName"));
            setOKEnabled(false);
            return true; // Let it be invalid, but show error
        }
    }
}
