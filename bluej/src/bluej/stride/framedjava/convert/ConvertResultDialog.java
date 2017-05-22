/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016, 2017 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.convert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import bluej.utility.Utility;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A dialog shown when the user has tried to convert Java to Stride,
 * either by pasting from the clipboard or a full-class conversion,
 * and there has been either an error or warnings. 
 */
public class ConvertResultDialog
{
    private final Alert alert;
    
    /**
     * Constructor for when both parses failed during a paste.  The xmlParseError
     * parameter contains the failure for parsing the XML,
     * in case the problem was the clipboard had been messed with.
     * 
     * The convertError parameter is the error (usually a parse failure)
     * in doing the Java->Stride conversion.
     */
    public ConvertResultDialog(String xmlParseError, String convertError)
    {
        alert = new Alert(Alert.AlertType.ERROR, Config.getString("stride.convert.paste.errors"), ButtonType.OK);
        alert.setTitle(Config.getString("stride.convert.paste.errors.title"));
        alert.setHeaderText(alert.getTitle());
        StringBuilder s = new StringBuilder();
        s.append(Config.getString("stride.convert.error.stride") + ":\n  " + xmlParseError + "\n\n");
        s.append(Config.getString("stride.convert.error.java") + ":\n  " + convertError);
        addDetails(s.toString());
    }

    /**
     * Constructor for when both parsing failed during a full conversion.
     *
     * The convertError parameter is the error (usually a parse failure)
     * in doing the Java->Stride conversion.
     */
    public ConvertResultDialog(String convertError)
    {
        alert = new Alert(Alert.AlertType.ERROR, Config.getString("stride.convert.whole.errors"), ButtonType.OK);
        alert.setTitle(Config.getString("stride.convert.whole.errors.title"));
        alert.setHeaderText(alert.getTitle());
        StringBuilder s = new StringBuilder();
        s.append(Config.getString("stride.convert.error.java") + ":\n  " + convertError);
        addDetails(s.toString());
    }

    /**
     * Constructor for when a full conversion failed
     */

    /**
     * Constructor for when the conversion succeeded, but had warnings.
     * 
     * You should not pass an empty list; if there are no warnings, why are you
     * showing this dialog?
     */
    public ConvertResultDialog(List<String> warnings)
    {
        alert = new Alert(Alert.AlertType.WARNING, Config.getString("stride.convert.warnings"), ButtonType.OK);
        alert.setTitle(Config.getString("stride.convert.warnings.title"));
        alert.setHeaderText(alert.getTitle());
        alert.setOnShown(e -> Utility.bringToFrontFX(alert.getDialogPane().getScene().getWindow()));
        addDetails(warnings.stream().collect(Collectors.joining("\n")));
    }

    private void addDetails(String extra)
    {
        alert.getDialogPane().setMaxWidth(600.0);
        
        TextArea extraDisplay = new TextArea(extra);
        extraDisplay.setWrapText(true);
        extraDisplay.setPrefRowCount(8);
        extraDisplay.setEditable(false);
        Label label = new Label(alert.getContentText());
        label.setWrapText(true);
        VBox vBox = new VBox(label, extraDisplay);
        // Should really be in CSS, but I just want to change this one property:
        vBox.setSpacing(20.0);
        alert.getDialogPane().setContent(vBox);
    }

    public void showAndWait()
    {
        alert.showAndWait();
    }
}
