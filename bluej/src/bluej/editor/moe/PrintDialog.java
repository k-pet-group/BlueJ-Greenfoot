/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

import bluej.pkgmgr.Package;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A print dialog with options specific to the editor.
 */
public class PrintDialog extends Dialog<PrintDialog.PrintChoices>
{
    @OnThread(Tag.Any)
    public static class PrintChoices
    {
        public final boolean printLineNumbers;
        public final boolean printHighlighting;

        public PrintChoices(boolean printLineNumbers, boolean printHighlighting)
        {
            this.printLineNumbers = printLineNumbers;
            this.printHighlighting = printHighlighting;
        }
    }

    /**
     * Creates a new PrintDialog object.
     *
     * @param pkg The Package, if printing a whole package.
     *            Null if printing a single class.
     */
    public PrintDialog(Window owner, Package pkg)
    {
        setTitle(Config.getString("editor.printDialog.title"));
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);


        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        CheckBox checkLineNumbers = new CheckBox(Config.getString("editor.printDialog.printLineNumbers"));
        checkLineNumbers.setSelected(true);

        CheckBox checkHighlighting = new CheckBox(Config.getString("editor.printDialog.printHighlighting"));
        checkHighlighting.setSelected(false);

        VBox vBox = new VBox(checkLineNumbers, checkHighlighting);
        vBox.setSpacing(8);
        getDialogPane().setContent(vBox);
        setResultConverter(bt -> {
            if (bt == ButtonType.OK)
            {
                return new PrintChoices(checkLineNumbers.isSelected(), checkHighlighting.isSelected());
            }
            return null;
        });
    }
}
