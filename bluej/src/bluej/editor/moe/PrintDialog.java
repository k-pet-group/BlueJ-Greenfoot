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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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
    public static enum PrintSize
    {
        SMALL, STANDARD, LARGE;


        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public String toString()
        {
            return Config.getString("editor.printDialog.fontSize." + this.name().toLowerCase());
        }
    }

    @OnThread(Tag.Any)
    public static class PrintChoices
    {
        // These three only apply for whole package:
        public final boolean printDiagram;
        public final boolean printReadme;
        public final boolean printSource;
        // These three apply for single class, or when
        // printSource is true for whole package:
        public final PrintSize printSize;
        public final boolean printLineNumbers;
        public final boolean printHighlighting;

        public PrintChoices(boolean printDiagram, boolean printReadme, boolean printSource, PrintSize printSize, boolean printLineNumbers, boolean printHighlighting)
        {
            this.printDiagram = printDiagram;
            this.printReadme = printReadme;
            this.printSource = printSource;
            this.printSize = printSize;
            this.printLineNumbers = printLineNumbers;
            this.printHighlighting = printHighlighting;
        }
    }

    // Has to be a field to make sure weak binding doesn't get GCed.
    // Store as negative (cannot, rather than can) because we bind from this to
    // disabledProperty (and there is no enabled property):
    private final BooleanExpression cannotPrint;

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

        ComboBox<PrintSize> comboSize = new ComboBox<>(FXCollections.observableArrayList(PrintSize.values()));
        comboSize.getSelectionModel().select(PrintSize.STANDARD);
        HBox sizeRow = new HBox(new Label(Config.getString("editor.printDialog.fontSize")), comboSize);
        sizeRow.setAlignment(Pos.BASELINE_LEFT);
        sizeRow.setSpacing(10.0);

        CheckBox checkLineNumbers = new CheckBox(Config.getString("editor.printDialog.printLineNumbers"));
        checkLineNumbers.setSelected(true);

        CheckBox checkHighlighting = new CheckBox(Config.getString("editor.printDialog.printHighlighting"));
        checkHighlighting.setSelected(false);

        VBox vBox = new VBox(sizeRow, checkLineNumbers, checkHighlighting);
        vBox.setSpacing(8);

        final CheckBox checkReadme;
        final CheckBox checkDiagram;
        final CheckBox checkSource;
        if (pkg != null)
        {
            checkSource = new CheckBox(Config.getString("pkgmgr.printDialog.printSource"));
            checkLineNumbers.disableProperty().bind(checkSource.selectedProperty().not());
            checkHighlighting.disableProperty().bind(checkSource.selectedProperty().not());
            vBox.getChildren().add(0, checkSource);

            checkDiagram = new CheckBox(Config.getString("pkgmgr.printDialog.printDiagram"));
            if (pkg.isUnnamedPackage())
            {
                checkReadme = new CheckBox(Config.getString("pkgmgr.printDialog.printReadme"));
                vBox.getChildren().add(0, checkReadme);
                cannotPrint = Bindings.createBooleanBinding(
                    () -> !checkSource.isSelected() && !checkDiagram.isSelected() && !checkReadme.isSelected(),
                    checkSource.selectedProperty(),
                    checkDiagram.selectedProperty(),
                    checkReadme.selectedProperty());
            }
            else
            {
                checkReadme = null;
                cannotPrint = Bindings.createBooleanBinding(
                    () -> !checkSource.isSelected() && !checkDiagram.isSelected(),
                    checkSource.selectedProperty(), checkDiagram.selectedProperty());
            }

            vBox.getChildren().add(0, checkDiagram);
        }
        else
        {
            checkReadme = null;
            checkDiagram = null;
            checkSource = null;
            // Should be able to print, so cannot-print is false:
            cannotPrint = new ReadOnlyBooleanWrapper(false);
        }

        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(cannotPrint);

        getDialogPane().setContent(vBox);
        setResultConverter(bt -> {
            if (bt == ButtonType.OK)
            {
                return new PrintChoices(
                    checkDiagram == null ? false : checkDiagram.isSelected(),
                    checkReadme == null ? false : checkReadme.isSelected(),
                    checkSource == null ? false : checkSource.isSelected(),
                    comboSize.getValue(),
                    checkLineNumbers.isSelected(),
                    checkHighlighting.isSelected());
            }
            return null;
        });
    }
}
