/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018,2022  Poul Henriksen and Michael Kolling
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of f
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.guifx.classes;

import bluej.Config;
import bluej.utility.javafx.FXPlatformRunnable;
import greenfoot.guifx.GreenfootStage;
import greenfoot.guifx.classes.GClassDiagram.GClassType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;

import java.util.List;

/**
 * A GClassNode for the built-in Actor and World classes.
 */
public class BuiltInGClassNode extends GClassNode
{
    // Will always be ACTOR or WORLD, never OTHER
    private final GClassType type;
    private final GClassDiagram classDiagram;

    /**
     * Constructor for a GClassNode for one of the API base classes: World or Actor.
     *
     * @param type   the class type
     * @param subClasses   all nodes for the direct subclasses of this node
     * @param selectionManager   the selection manager
     */
    public BuiltInGClassNode(GClassType type, List<GClassNode> subClasses, GClassDiagram classDiagram)
    {
        super(null, subClasses, classDiagram.getSelectionManager());
        this.type = type;
        this.classDiagram = classDiagram;
    }

    @Override
    public String getQualifiedName()
    {
        return "greenfoot." + shortName(type);
    }

    @Override
    public String getDisplayName()
    {
        return shortName(type);
    }

    /**
     * Turns the enum into the class name within greenfoot package.
     */
    private static String shortName(GClassType type)
    {
        switch (type)
        {
            case ACTOR:
                return "Actor";
            case WORLD:
                return "World";
            default:
                throw new RuntimeException();
        }
    }

    @Override
    protected void setupClassDisplay(GreenfootStage greenfootStage, ClassDisplay display)
    {
        FXPlatformRunnable showDocs = () -> greenfootStage.openGreenfootDocTab(display.getQualifiedName());

        display.setOnContextMenuRequested(e -> {
            e.consume();
            if (curContextMenu != null)
            {
                curContextMenu.hide();
                curContextMenu = null;
                // Must hide our parent node's context menu manually:
                classDiagram.hideContextMenu();
            }
            curContextMenu = new ContextMenu();


            curContextMenu.getItems().add(GClassDiagram.contextInbuilt(
                    Config.getString("show.apidoc"), showDocs));
            curContextMenu.getItems().add(new SeparatorMenuItem());
            curContextMenu.getItems().add(GClassDiagram.contextInbuilt(
                    Config.getString("new.sub.class"), () -> {
                        greenfootStage.newSubClassOf(display.getQualifiedName(), type);
                    }));

            // Select item when we show context menu for it:
            selectionManager.select(display);
            curContextMenu.show(display, e.getScreenX(), e.getScreenY());
        });

        display.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
            {
                showDocs.run();
            }
        });
    }
}
