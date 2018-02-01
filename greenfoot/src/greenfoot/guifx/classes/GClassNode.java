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
package greenfoot.guifx.classes;

import bluej.Config;
import bluej.utility.javafx.FXRunnable;
import greenfoot.guifx.GreenfootStage;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Information about a class in the tree: its name, image (can be null),
 * its direct subclasses (may be empty), and the display items for it (once shown)
 */
public class GClassNode
{
    private final String fullyQualifiedName;
    private final String displayName;
    private final Image image;
    private final List<GClassNode> subClasses = new ArrayList<>();
    // If non-null, exists *and* is already a child of the enclosing ClassGroup
    protected ClassDisplay display;
    // If non-null, exists *and* is already a child of the enclosing ClassGroup
    // The arrow (which may have several offshoot arms from multiple subclasses).
    private InheritArrow arrowFromSub;
    private final ClassDisplaySelectionManager selectionManager;
    protected ContextMenu curContextMenu = null;

    public GClassNode(String fullyQualifiedName, String displayName, Image image, List<GClassNode> subClasses, ClassDisplaySelectionManager selectionManager)
    {
        this.selectionManager = selectionManager;
        this.fullyQualifiedName = fullyQualifiedName;
        this.displayName = displayName;
        this.image = image;
        this.subClasses.addAll(subClasses);
        Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.displayName));
    }

    /**
     * Gets the qualified name of the class.
     */
    public String getQualifiedName()
    {
        return fullyQualifiedName;
    }

    /**
     * Adds a subclass to the list of subclasses.
     * Don't forget to call updateAfterAdd() on the enclosing ClassGroup.
     */
    public void add(GClassNode classInfo)
    {
        subClasses.add(classInfo);
        Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.displayName));
    }

    /**
     * Get the list of subclasses of this class.
     */
    public List<GClassNode> getSubClasses()
    {
        return Collections.unmodifiableList(subClasses);
    }

    /**
     * Gets the display name for the class (the unqualified name)
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Gets the ClassDisplay for this item.  Will always return the same ClassDisplay
     * for the lifetime of this GClassNode object, although internally it is lazily created.
     */
    public ClassDisplay getDisplay(GreenfootStage greenfootStage)
    {
        if (display == null)
        {
            display = new ClassDisplay(displayName, fullyQualifiedName, image, selectionManager);
            setupClassDisplay(greenfootStage, display);
        }
        return display;
    }

    /**
     * Set up any listeners on the ClassDisplay item.  Here ready for overriding
     * in subclasses.
     */
    protected void setupClassDisplay(GreenfootStage greenfootStage, ClassDisplay display)
    {
        if (display.getQualifiedName().startsWith("greenfoot."))
        {
            FXRunnable showDocs = () -> {
                greenfootStage.openBrowser(display.getQualifiedName().replace(".", "/") + ".html");
            };
            
            display.setOnContextMenuRequested(e -> {
                e.consume();
                if (curContextMenu != null)
                {
                    curContextMenu.hide();
                    curContextMenu = null;
                }
                curContextMenu = new ContextMenu();

                
                curContextMenu.getItems().add(GClassDiagram.contextInbuilt(Config.getString("show.apidoc"), showDocs));
                curContextMenu.getItems().add(new SeparatorMenuItem());
                curContextMenu.getItems().add(GClassDiagram.contextInbuilt(Config.getString("new.sub.class"), () -> {
                    greenfootStage.newImageSubClassOf(display.getQualifiedName());
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

    /**
     * Gets the InheritArrow for this item.  Will always return the same InheritArrow
     * for the lifetime of this GClassNode object, although internally it is lazily created.
     */
    public InheritArrow getArrowFromSub()
    {
        if (arrowFromSub == null)
        {
            arrowFromSub = new InheritArrow();
        }
        return arrowFromSub;
    }

    /**
     * Called when this GClassNode is being disposed of.  Remove
     * any listeners, etc.
     */
    public void tidyup()
    {   
    }
}
