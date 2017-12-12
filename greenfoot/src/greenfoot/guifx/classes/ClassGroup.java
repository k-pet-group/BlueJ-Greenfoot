package greenfoot.guifx.classes;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A hierarchical tree display of classes.  There can be zero-to-unlimited root classes,
 * each of which can have zero-to-unlimited subclasses, and each of those can
 * have zero-unlimited subclasses, and so on, all the way down.
 * 
 * Inheritance arrows are drawn for each subclass relation.  Classes are sorted
 * alphabetically at each level in the hierarchy.
 */
public class ClassGroup extends Pane implements ChangeListener<Number>
{
    private static final int VERTICAL_SPACING = 6;

    /**
     * Information about a class in the tree: its display name, image (can be null),
     * its direct subclasses, and the display item for it (once shown)
     */
    public static class ClassInfo
    {
        private final String fullyQualifiedName;
        private final String displayName;
        private final Image image;
        private final List<ClassInfo> subClasses = new ArrayList<>();
        // If non-null, exists *and* is already a child of the enclosing ClassGroup
        private ClassDisplay display;
        // If non-null, exists *and* is already a child of the enclosing ClassGroup
        private InheritArrow arrowFromSub;

        public ClassInfo(String fullyQualifiedName, String displayName, Image image, List<ClassInfo> subClasses)
        {
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
        public void add(ClassInfo classInfo)
        {
            subClasses.add(classInfo);
            Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.displayName));
        }

        /**
         * Get the list of subclasses of this class.
         */
        public List<ClassInfo> getSubClasses()
        {
            return Collections.unmodifiableList(subClasses);
        }
    }
    // For Actor and World groups, just those base classes.  For other, can be many top-level:
    private final List<ClassInfo> topLevel = new ArrayList<>();
        
    public ClassGroup()
    {
        getStyleClass().add("class-group");
        // Set minimum to be preferred width/height:
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        
        // Default size is zero, will get expanded once we have content:
        setPrefWidth(0.0);
        setPrefHeight(0.0);
    }

    /**
     * Sets the top-level classes for this class group.
     */
    public void setClasses(List<ClassInfo> topLevel)
    {
        // Tidy up by removing height listeners on old ClassDisplays:
        for (Node child : getChildren())
        {
            if (child instanceof ClassDisplay)
                ((ClassDisplay)child).heightProperty().removeListener(this);
        }
        getChildren().clear();
        this.topLevel.clear();
        this.topLevel.addAll(topLevel);
        Collections.sort(this.topLevel, Comparator.comparing(ci -> ci.displayName));

        requestLayout();
    }

    /**
     * Gets the live list of classes in this group.  This should only be used for adding, not for
     * removal.  If you add a class anywhere within, you should then call updateAfterAdd().
     */
    public List<ClassInfo> getLiveClasses()
    {
        return topLevel;
    }

    /**
     * Refreshes display after a class has been added to the diagram.
     */
    public void updateAfterAdd()
    {
        // Sort in case they added at top-level:
        Collections.sort(this.topLevel, Comparator.comparing(ci -> ci.displayName));
        // Adjust positions:
        requestLayout();
    }

    @Override
    protected void layoutChildren()
    {
        // Super call autosizes children, which we still want:
        super.layoutChildren();
        
        // Layout all the classes, and use the final Y position as our preferred height:
        int finalY = redisplay(null, topLevel, 0, 0);
        // If our content height is different than before, we need to adjust our preferred height: 
        if (finalY != (int)getPrefHeight())
        {
            setPrefHeight(finalY);
            // Because we are within layout, we need an explicit call to notify parent of height change:
            getParent().requestLayout();
        }
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
    private int redisplay(InheritArrow arrowToSuper, List<ClassInfo> stratum, int x, int y)
    { 
        final int startY = y;
        List<Double> arrowArms = new ArrayList<>();
        
        for (ClassInfo classInfo : stratum)
        {
            y += VERTICAL_SPACING;
            
            // If no display make one (if there is display, no need to make again)
            if (classInfo.display == null)
            {
                classInfo.display = new ClassDisplay(classInfo.displayName, classInfo.image);
                getChildren().add(classInfo.display);
                // Often, the height is zero at this point, so we need to listen
                // for when it gets set right in order to re-layout:
                classInfo.display.heightProperty().addListener(this);
            }
            // The inherit arrow arm should point to the vertical midpoint of the class:
            double halfHeight = Math.floor(classInfo.display.getHeight() / 2.0);
            arrowArms.add(y + halfHeight - startY);
            
            classInfo.display.setLayoutX(x);
            // Update our preferred width if we've found a long class:
            if (x + classInfo.display.getWidth() > getPrefWidth())
            {
                setPrefWidth(x + classInfo.display.getWidth());
                // Because we are within layout, we need an explicit call to notify parent of width change:
                getParent().requestLayout();
            }
            classInfo.display.setLayoutY(y);
            // If height changes, we will layout again because of the listener added above:
            y += classInfo.display.getHeight();
            
            if (!classInfo.subClasses.isEmpty())
            {
                // If no existing arrow, make one and add to children:
                if (classInfo.arrowFromSub == null)
                {
                    classInfo.arrowFromSub = new InheritArrow();
                    getChildren().add(classInfo.arrowFromSub);
                }
                // Update the position.  Using 0.5 makes the lines lie exactly on a pixel and avoid anti-aliasing:
                classInfo.arrowFromSub.setLayoutX(x + 5 + 0.5);
                classInfo.arrowFromSub.setLayoutY(y + 0.5);

                // Now do the sub-classes of this class, indented to right:
                y = redisplay(classInfo.arrowFromSub, classInfo.subClasses, x + 20, y);
            }
            else
            {
                // If no longer have any subclasses, clean up any previous arrow:
                if (classInfo.arrowFromSub != null)
                {
                    getChildren().remove(classInfo.arrowFromSub);
                    classInfo.arrowFromSub = null;
                }
            }
        }
        
        if (arrowToSuper != null)
        {
            arrowToSuper.setArmLocations(15.0, arrowArms);
        }
        
        return y;
    }


    /**
     * Listener for when height changes on any of the classes.
     * All we want to do in that case is request a layout.  Note we don't want to
     * just use a lambda, because we also want to remove the listener later.
     */
    @Override
    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
    {
        requestLayout();
    }
}
