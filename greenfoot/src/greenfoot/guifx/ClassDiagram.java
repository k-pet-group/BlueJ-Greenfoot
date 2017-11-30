/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.guifx;

import bluej.debugmgr.objectbench.InvokeListener;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.Utility;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

/**
 * The class diagram on the right-hand side of the Greenfoot window.
 *
 * For now, this is very primitive, but is useful for implementing other Greenfoot functionality.
 */
public class ClassDiagram extends VBox
{
    private ClassTarget selected = null;

    public ClassDiagram(Project project)
    {
        getChildren().setAll(Utility.mapList(
                project.getUnnamedPackage().getClassTargets(),
                this::makeClassItem));
    }

    /**
     * Make the graphical item for a ClassTarget.  Currently just a Label.
     */
    private Node makeClassItem(ClassTarget classTarget)
    {
        Label label = new Label(classTarget.getBaseName());
        label.setOnContextMenuRequested(e -> {
            Class<?> cl = classTarget.getPackage().loadClass(classTarget.getQualifiedName());
            if (cl != null)
            {
                ContextMenu contextMenu = new ContextMenu();
                classTarget.getRole().createClassConstructorMenu(contextMenu.getItems(), classTarget, cl);
                if (!contextMenu.getItems().isEmpty())
                {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }
                classTarget.getRole().createClassStaticMenu(contextMenu.getItems(), classTarget, classTarget.hasSourceCode(), cl);
                contextMenu.show(label, e.getScreenX(), e.getScreenY());
            }
        });
        label.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1)
            {
                selected = classTarget;
                // Hacky, for now until we sort out graphics for class diagram:
                for (Node other : getChildren())
                {
                    other.setStyle("");
                }
                label.setStyle("-fx-underline: true;");
            }
        });
        return label;
    }

    /**
     * Gets the currently selected class in the diagram.  May be null if no selection
     */
    public ClassTarget getSelectedClass()
    {
        return selected;
    }
}
