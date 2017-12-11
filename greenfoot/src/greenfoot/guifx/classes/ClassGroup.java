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

        public ClassInfo(String fullyQualifiedName, String displayName, Image image, List<ClassInfo> subClasses)
        {
            this.fullyQualifiedName = fullyQualifiedName;
            this.displayName = displayName;
            this.image = image;
            this.subClasses.addAll(subClasses);
            Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.displayName));
        }
        
        public String getQualifiedName()
        {
            return fullyQualifiedName;
        }

        /**
         * Adds a subclass.  Don't forget to call updateAfterAdd() on the enclosing ClassGroup.
         */
        public void add(ClassInfo classInfo)
        {
            subClasses.add(classInfo);
            Collections.sort(this.subClasses, Comparator.comparing(ci -> ci.displayName));
        }

        public List<ClassInfo> getSubClasses()
        {
            return Collections.unmodifiableList(subClasses);
        }
    }
    // For Actor and World groups, just those base classes.  For other, can be many top-level:
    private final List<ClassInfo> topLevel = new ArrayList<>();
        
    public ClassGroup()
    {
        // Force preferred height:
        setMinHeight(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);
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
     * @return
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
        double finalY = redisplay(topLevel, 0.0, 0.0);
        setPrefHeight(finalY);
    }

    /**
     * Lay out the list of classes vertically, at the same indent.
     * Also lay out any subclasses.
     * @return The resulting Y position after doing the layout.
     */
    private double redisplay(List<ClassInfo> stratum, double x, double y)
    {
        for (ClassInfo classInfo : stratum)
        {
            // If no display make one (if there is display, no need to make again)
            if (classInfo.display == null)
            {
                classInfo.display = new ClassDisplay(classInfo.displayName, classInfo.image);
                getChildren().add(classInfo.display);
                // Often, the height is zero at this point, so we need to listen
                // for when it gets set right in order to re-layout:
                classInfo.display.heightProperty().addListener(this);
            }
            
            classInfo.display.setLayoutX(x);
            classInfo.display.setLayoutY(y);
            // If height changes, we will layout again because of the listener added above:
            y += classInfo.display.getHeight();
            
            // Now do any sub-classes of this class, indented to right:
            y = redisplay(classInfo.subClasses, x + 20.0, y);
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
