/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017,2019  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.guifx.GreenfootStage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * A hierarchical tree display of classes.  There can be zero-to-unlimited root classes,
 * each of which can have zero-to-unlimited subclasses, and each of those can
 * have zero-unlimited subclasses, and so on, all the way down.
 * 
 * Inheritance arrows are drawn for each subclass relation.  Classes are sorted
 * alphabetically at each level in the hierarchy.
 */
@OnThread(Tag.FXPlatform)
public class ClassGroup extends Pane implements ChangeListener<Number>
{
    public static final int VERTICAL_SPACING = 8;

    // For Actor and World groups, just those base classes.  For other, can be many top-level:
    private final List<GClassNode> topLevel = new ArrayList<>();
    private final GreenfootStage greenfootStage;

    public ClassGroup(GreenfootStage greenfootStage)
    {
        this.greenfootStage = greenfootStage; 
        getStyleClass().add("class-group");
        // Set minimum to be preferred width/height:
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
    }

    /**
     * Sets the top-level classes for this class group.
     */
    public void setClasses(List<GClassNode> topLevel)
    {
        // Tidy up by removing height listeners on old ClassDisplays:
        for (Node child : getChildren())
        {
            if (child instanceof ClassDisplay)
            {
                ((ClassDisplay) child).widthProperty().removeListener(this);
                ((ClassDisplay) child).heightProperty().removeListener(this);
            }
        }
        getChildren().clear();
        for (GClassNode classInfo : this.topLevel)
        {
            classInfo.tidyup();
        }        
        this.topLevel.clear();
        this.topLevel.addAll(topLevel);
        updateAfterAdd();
    }

    /**
     * Gets the live list of top-level classes in this group.  This should only be used for adding, not for
     * removal.  If you add a class anywhere within, you should then call updateAfterAdd().
     * Note: only gets top-level classes, does not get subclasses.
     */
    public List<GClassNode> getLiveTopLevelClasses()
    {
        return topLevel;
    }

    /**
     * Gets a stream containing all the classes in this group, both top-level
     * and subclasses.
     */
    public Stream<GClassNode> streamAllClasses()
    {
        return topLevel.stream().flatMap(c -> streamInclSubclasses(c));
    }

    /**
     * Helper method to get a stream containing the given class and all its subclasses
     * (all the way down to the bottom: subsubclasses, etc)
     */
    private static Stream<GClassNode> streamInclSubclasses(GClassNode item)
    {
        return Stream.concat(Stream.of(item),
            item.getSubClasses().stream().flatMap(c -> streamInclSubclasses(c)));
    }

    /**
     * Refreshes display after a class has been added to the diagram.
     */
    public void updateAfterAdd()
    {
        // Sort in case they added at top-level:
        Collections.sort(this.topLevel, Comparator.comparing(GClassNode::getDisplayName));
        // Adjust positions:
        redisplay();
    }

    private void redisplay()
    {
        // Left indent by same amount as top indent:
        int leftIndent = VERTICAL_SPACING;
        redisplay(null, topLevel, leftIndent, 0);
        requestLayout();
    }

    /**
     * Lay out the list of classes vertically, at the same indent.
     * Also lay out any subclasses.
     * 
     * @param arrowToSuper Either null (no superclass) or a vertical inherit arrow to update
     *                     the position of, once we've laid out all classes in the stratum.
     * @param stratum The list of classes to layout (in list order)
     * @param x The current X position for all the classes
     * @param y The Y position for the top class.
     * @return The resulting Y position after doing the layout.
     */
    private int redisplay(InheritArrow arrowToSuper, List<GClassNode> stratum, int x, int y)
    { 
        final int startY = y;
        List<Double> arrowArms = new ArrayList<>();
        
        for (GClassNode classInfo : stratum)
        {
            y += VERTICAL_SPACING;
            
            // Make sure display is in our children:
            ClassDisplay classDisplay = classInfo.getDisplay(greenfootStage);
            if (!getChildren().contains(classDisplay))
            {
                getChildren().add(classDisplay);
                // Often, the width or height is zero at this point, so we need to listen
                // for when it gets set right in order to re-layout:
                classDisplay.widthProperty().addListener(this);
                classDisplay.heightProperty().addListener(this);
            }
            // The inherit arrow arm should point to the vertical midpoint of the class:
            double halfHeight = Math.floor(classDisplay.getHeight() / 2.0);
            arrowArms.add(y + halfHeight - startY);
            
            classDisplay.setLayoutX(x);
            classDisplay.setLayoutY(y);
            // If height changes, we will layout again because of the listener added above:
            y += classDisplay.getHeight();
            
            if (!classInfo.getSubClasses().isEmpty())
            {
                // If no existing arrow, make one and add to children:
                if (!getChildren().contains(classInfo.getArrowFromSub()))
                {
                    getChildren().add(classInfo.getArrowFromSub());
                }
                // Update the position.  Using 0.5 makes the lines lie exactly on a pixel and avoid anti-aliasing:
                classInfo.getArrowFromSub().setLayoutX(x + 5 + 0.5);
                classInfo.getArrowFromSub().setLayoutY(y + 0.5);

                // Now do the sub-classes of this class, indented to right:
                y = redisplay(classInfo.getArrowFromSub(), classInfo.getSubClasses(), x + 20, y);
            }
            else
            {
                // If no longer have any subclasses, clean up any previous arrow:
                getChildren().remove(classInfo.getArrowFromSub());
            }
        }
        
        if (arrowToSuper != null)
        {
            arrowToSuper.setArmLocations(15.0, arrowArms);
        }
        
        return y;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefHeight(double width)
    {
        // The total height of class displays, plus that many vertical spacing items
        // (Note: we have spacing at the top as well, not just inbetween)
        // We don't just sum all children, because we don't want to include the height
        // of the arrows that sit alongside the ClassDisplay items:
        return getChildren().stream()
                .filter(c -> c instanceof ClassDisplay)
                .mapToDouble(c -> VERTICAL_SPACING + c.prefHeight(width))
                .sum();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefWidth(double height)
    {
        return getChildren().stream()
                .filter(c -> c instanceof ClassDisplay)
                .mapToDouble(c -> c.getLayoutX() + c.prefWidth(-1))
                .max().orElse(0.0)
                + VERTICAL_SPACING; // Use vertical spacing for right spacer
    }

    /**
     * Listener for when width or height changes on any of the classes.
     * All we want to do in that case is request a layout.  Note we don't want to
     * just use a lambda, because we also want to remove the listener later.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
    {
        redisplay();
    }
    
    /**
     * Save image selections to a properties file.
     */
    public void saveImageSelections(Properties p)
    {
        for (GClassNode gcn : topLevel)
        {
            saveImageSelections(gcn, p);
        }
    }
    
    /**
     * Save the image selections for a particular class node, and its subclasses, to a properties
     * file.
     */
    private void saveImageSelections(GClassNode gcn, Properties p)
    {
        String imageName = gcn.getImageFilename();
        if (imageName != null) {
            p.put("class." + gcn.getQualifiedName() + ".image", imageName);
        }
        
        for (GClassNode gcnSubclass : gcn.getSubClasses())
        {
            saveImageSelections(gcnSubclass, p);
        }
    }
}
