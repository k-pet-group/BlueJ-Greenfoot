/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014,2016,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.editor.flow;

import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgr.PrintSize;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
    public PrintDialog(Window owner, Package pkg, boolean largePrintJob)
    {
        setTitle(Config.getString("editor.printDialog.title"));
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);


        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<PrintSize> comboSize = new ComboBox<>(FXCollections.observableArrayList(PrefMgr.PrintSize.values()));
        comboSize.getSelectionModel().select(PrefMgr.getPrintFontSize());
        HBox sizeRow = new HBox(new Label(Config.getString("editor.printDialog.fontSize")), comboSize);
        sizeRow.setAlignment(Pos.BASELINE_LEFT);
        sizeRow.setSpacing(10.0);

        CheckBox checkLineNumbers = new CheckBox(Config.getString("editor.printDialog.printLineNumbers"));
        checkLineNumbers.setSelected(PrefMgr.getFlag(PrefMgr.PRINT_LINE_NUMBERS));

        CheckBox checkHighlighting = new CheckBox(Config.getString("editor.printDialog.printHighlighting"));
        checkHighlighting.setSelected(PrefMgr.getFlag(PrefMgr.PRINT_SCOPE_HIGHLIGHTING));
        // Disabled items cannot show a tooltip.  So we wrap the checkbox (which may get disabled)
        // in an HBox and set the tooltip on the HBox instead.
        HBox checkHighlightingWrapper = new HBox(checkHighlighting);
        Tooltip.install(checkHighlightingWrapper, new Tooltip(Config.getString("editor.printDialog.printHighlighting.tooltip")));
        if (largePrintJob)
        {
            checkHighlighting.setSelected(false);
            checkHighlighting.setDisable(true);
        }

        VBox vBox = new VBox(sizeRow, checkLineNumbers, checkHighlightingWrapper);
        vBox.setSpacing(8);

        final CheckBox checkReadme;
        final CheckBox checkDiagram;
        final CheckBox checkSource;
        if (pkg != null)
        {
            checkSource = new CheckBox(Config.getString("pkgmgr.printDialog.printSource"));
            checkLineNumbers.disableProperty().bind(checkSource.selectedProperty().not());
            checkHighlighting.disableProperty().bind(checkSource.selectedProperty().not());
            checkSource.setSelected(PrefMgr.getFlag(PrefMgr.PACKAGE_PRINT_SOURCE));
            vBox.getChildren().add(0, checkSource);

            checkDiagram = new CheckBox(Config.getString("pkgmgr.printDialog.printDiagram"));
            checkDiagram.setSelected(PrefMgr.getFlag(PrefMgr.PACKAGE_PRINT_DIAGRAM));
            if (pkg.isUnnamedPackage())
            {
                checkReadme = new CheckBox(Config.getString("pkgmgr.printDialog.printReadme"));
                checkReadme.setSelected(PrefMgr.getFlag(PrefMgr.PACKAGE_PRINT_README));
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
                if (checkDiagram != null)
                    PrefMgr.setFlag(PrefMgr.PACKAGE_PRINT_DIAGRAM, checkDiagram.isSelected());
                if (checkReadme != null)
                    PrefMgr.setFlag(PrefMgr.PACKAGE_PRINT_README, checkReadme.isSelected());
                if (checkSource != null)
                    PrefMgr.setFlag(PrefMgr.PACKAGE_PRINT_SOURCE, checkSource.isSelected());
                PrefMgr.setFlag(PrefMgr.PRINT_LINE_NUMBERS, checkLineNumbers.isSelected());
                PrefMgr.setFlag(PrefMgr.PRINT_SCOPE_HIGHLIGHTING, checkHighlighting.isSelected());
                PrefMgr.setPrintFontSize(comboSize.getValue());
                
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
