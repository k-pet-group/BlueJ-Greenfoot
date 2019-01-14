/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017,2018,2019  Poul Henriksen and Michael Kolling 
 
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
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import greenfoot.guifx.GreenfootStage;
import greenfoot.guifx.classes.GClassDiagram.GClassType;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
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
public abstract class GClassNode
{
    private final List<GClassNode> subClasses = new ArrayList<>();
    protected Image image;

    protected final ClassDisplaySelectionManager selectionManager;
    protected ContextMenu curContextMenu = null;
    
    // If non-null, exists *and* is already a child of the enclosing ClassGroup
    protected ClassDisplay display;
    
    // The arrow (which may have several offshoot arms from multiple subclasses).
    private InheritArrow arrowFromSub;

    /**
     * Create a GClassNode
     * @param image The image to use (null if none)
     * @param subClasses The list of subclasses to display beneath us
     * @param selectionManager The central manager for class selection.
     */
    protected GClassNode(Image image,
            List<GClassNode> subClasses, ClassDisplaySelectionManager selectionManager)
    {
        this.selectionManager = selectionManager;
        this.image = image;
        this.subClasses.addAll(subClasses);
        Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.getDisplayName()));
    }

    /**
     * Gets the qualified name of the class.
     */
    public abstract String getQualifiedName();

    /**
     * Adds a subclass to the list of subclasses.
     * Don't forget to call updateAfterAdd() on the enclosing ClassGroup.
     */
    public void add(GClassNode classInfo)
    {
        subClasses.add(classInfo);
        Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.getDisplayName()));
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
    public abstract String getDisplayName();

    /**
     * Gets the ClassDisplay for this item.  Will always return the same ClassDisplay
     * for the lifetime of this GClassNode object, although internally it is lazily created.
     */
    public ClassDisplay getDisplay(GreenfootStage greenfootStage)
    {
        if (display == null)
        {
            display = new ClassDisplay(getDisplayName(), getQualifiedName(), image, selectionManager);
            setupClassDisplay(greenfootStage, display);
        }
        return display;
    }

    /**
     * Set up any listeners on the ClassDisplay item.  Here ready for overriding
     * in subclasses.
     */
    protected abstract void setupClassDisplay(GreenfootStage greenfootStage, ClassDisplay display);

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
     * any listeners, etc.  This implementation calls tidy-up on sub-classes,
     * and any class which overrides this method must call super.tidyup()
     * in order for sub classes to also be tidied up.
     */
    public void tidyup()
    {
        if (display != null)
        {
            selectionManager.removeClassDisplay(display);
        }
        for (GClassNode subClass : subClasses)
        {
            subClass.tidyup();
        }
    }
    
    /**
     * Get the image filename for the image associated with this class. If not specifically set,
     * this will return null (i.e. it will not return the image associated with the superclass,
     * if any).
     */
    public String getImageFilename()
    {
        return null;
    }
    
    /**
     * Set the image for this class node.
     */
    protected void setImage(Image newImage)
    {
        image = newImage;
        if (display != null)
        {
            display.setImage(newImage);
        }
    }
}
