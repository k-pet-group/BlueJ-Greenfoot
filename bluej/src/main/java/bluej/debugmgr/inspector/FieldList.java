/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2016,2017,2019,2020,2021  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr.inspector;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;

import static bluej.debugger.DebuggerObject.OBJECT_REFERENCE;

/**
 * A graphical representation of a list of fields from a class or object or method result, for use in an inspector.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 *  
 */
@OnThread(Tag.FXPlatform)
public class FieldList extends ScrollPane 
{
    private final static Image objectrefIcon = Config.getImageAsFXImage("image.inspector.objectref");
    private static final double ROW_HEIGHT = 30;
    
    // The actual list of fields, inside our ScrollPane:
    private final ContentPane content = new ContentPane();
    // The latest data:
    private final List<FieldInfo> curData = new ArrayList<>();
    // The currently selected row index:
    private final IntegerProperty selectedRow = new SimpleIntegerProperty(-1);
    // A placeholder shown where are no fields:
    private final Label placeholderLabel = new Label();

    public FieldList()
    {
        getStyleClass().add("field-list");
        setContent(new StackPane(content, placeholderLabel));
        content.managedProperty().bind(content.visibleProperty());
        placeholderLabel.managedProperty().bind(placeholderLabel.visibleProperty());
        // With no content at the beginning, only the placeholder label is visible:
        placeholderLabel.setVisible(true);
        content.setVisible(false);
        StackPane.setAlignment(placeholderLabel, Pos.CENTER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setFitToWidth(true);
    }

    /**
     * Select the next item down, if possible
     */
    public void down()
    {
        select(Math.min(curData.size() - 1, selectedRow.get() + 1));
    }

    /**
     * Select the next item up, if possible
     */
    public void up()
    {
        select(Math.max(0, selectedRow.get() - 1));
    }

    /**
     * Sets the new fields and values.  If this is identical, the update is skipped.
     */
    public void setData(List<FieldInfo> listData)
    {
        if (listData.equals(curData))
            return;
        
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < listData.size(); i++)
        {
            FieldInfo field = listData.get(i);
            Label valueLabel = new Label(field.getValue());
            if (OBJECT_REFERENCE.equals(valueLabel.getText()))
            {
                valueLabel.setGraphic(new ImageView(objectrefIcon));
                valueLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            Pane wrapper = new BorderPane(valueLabel);
            JavaFXUtil.addStyleClass(wrapper, "inspector-field-value-wrapper");
            JavaFXUtil.addStyleClass(valueLabel, "inspector-field-value-label");
            Label descriptionLabel = new Label(field.getDescription());
            JavaFXUtil.addStyleClass(descriptionLabel, "inspector-field-description");
            descriptionLabel.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
            children.add(descriptionLabel);
            children.add(wrapper);

            int iFinal = i;
            descriptionLabel.setOnMouseClicked(e -> select(iFinal));
            wrapper.setOnMouseClicked(e -> select(iFinal));
        }
        content.getChildren().setAll(children);
        content.setVisible(!children.isEmpty());
        placeholderLabel.setVisible(children.isEmpty());
        curData.clear();
        curData.addAll(listData);
        // Make sure graphics are refreshed by changing the selection back and forth:
        int sel = selectedRow.get();
        select(-1);
        select(sel);
        requestLayout();
    }
    
    /**
     * Sets the text to show when the list is empty
     */
    public void setPlaceHolderText(String text)
    {
        placeholderLabel.setText(text);
    }

    /**
     * Gets the selected row index, to be listened to.
     */
    public IntegerExpression selectedIndexProperty()
    {
        return selectedRow;
    }

    /**
     * Selects the given row (first is zero)
     */
    public void select(int index)
    {
        if (index == selectedRow.get())
            return;
        
        selectedRow.set(index);
        ObservableList<Node> children = content.getChildren();
        for (int i = 0; i < children.size(); i += 2)
        {
            boolean selected = i / 2 == index;
            JavaFXUtil.setPseudoclass("bj-selected", selected, children.get(i), children.get(i + 1));
            if (selected && (children.get(i).localToScene(0, 0).getY() < localToScene(0, 0).getY() || children.get(i).localToScene(0, ROW_HEIGHT).getY() > localToScene(0, getHeight()).getY()))
            {
                JavaFXUtil.scrollTo(this, children.get(i));
            }
        }
    }

    /**
     * The content of the ScrollPane; the actual field descriptions and values.
     * The children are always paired: description then value, description then value.
     * So the number of children is exactly double the number of fields (rows).
     */
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    private static class ContentPane extends Region
    {
        private static final int MIN_LEFT = 50;
        private static final int LARGEST_SENSIBLE_RIGHT = 500;

        public ContentPane()
        {
            getStyleClass().add("field-list-content");
        }
        
        @Override
        protected void layoutChildren()
        {
            // If you have a large label and large value, and resize it from minimum to maximum, the space gets gradually allocated as follows:
            // - The label has a minimum size of 50 pixels
            // - The rest is allocated to the value, up until it has taken up 500 pixels
            // - Then the space is allocated to the label until the label fully fits
            // - Then the space is allocated to the value until it fully fits
            // - After that, any remaining space is allocated 25% to the label and 75% to the value
            
            Insets outerPadding = getInsets();
            double widthMinusPadding = getWidth() - (outerPadding.getLeft() + outerPadding.getRight());
            List<Node> children = getChildren();
            // We make the left labels at least 50 pixels wide so they don't shrink to just "..."
            // if there are large values on the right
            double largestMinLeft = MIN_LEFT;
            double largestPrefLeft = largestMinLeft;
            double largestRight = 0;
            
            for (int i = 0; i < children.size(); i += 2)
            {
                largestMinLeft = Math.max(largestMinLeft, children.get(i).minWidth(ROW_HEIGHT));
                largestPrefLeft = Math.max(largestPrefLeft, children.get(i).prefWidth(ROW_HEIGHT));
                largestRight = Math.max(largestRight, children.get(i + 1).prefWidth(ROW_HEIGHT));
            }
            
            double leftWidth;
            if (largestPrefLeft + largestRight <= widthMinusPadding)
            {
                // We are bigger than we need to be.
                // Share any spare width such that the right-hand side gets three-quarters of it:
                leftWidth = largestPrefLeft + (widthMinusPadding - largestPrefLeft - largestRight) * 0.25f;
            }
            else
            {
                // We'll have to truncate.  The decision on how to truncate is a bit difficult because our
                // lack of sufficient width could be because:
                //  - the user has resized the inspector window to be unreasonably small
                //  - the variable name and/or type is very long
                //  - the value (especially String values) is very long
                // In general we want to give available space to the right-hand side, but not if it wants to be huge
                // So we restrict its asked for space to 500 pixels
                // (Don't forget this is only if there isn't room for everything; if there is we won't reach here)
                largestRight = Math.min(Math.max(widthMinusPadding - largestPrefLeft, LARGEST_SENSIBLE_RIGHT), largestRight);
                
                // Then we set the left-width to be what's left-over from the largestRight, but no smaller than its minimum space:
                leftWidth = Math.max(widthMinusPadding - largestRight, largestMinLeft);
            }
            
            double rightWidth = widthMinusPadding - leftWidth;
            
            double y = outerPadding.getTop();
            for (int i = 0; i < children.size(); i += 2)
            {
                children.get(i).resizeRelocate(outerPadding.getLeft(), y, leftWidth, ROW_HEIGHT);
                children.get(i + 1).resizeRelocate(outerPadding.getLeft() + leftWidth, y, rightWidth, ROW_HEIGHT);
                y += ROW_HEIGHT;
            }
        }

        // Make parent method public:
        @Override
        public ObservableList<Node> getChildren()
        {
            return super.getChildren();
        }

        @Override
        protected double computePrefWidth(double height)
        {
            List<Node> children = getChildren();
            double largestLeft = MIN_LEFT;
            double largestRight = 0;
            for (int i = 0; i < children.size(); i += 2)
            {
                largestLeft = Math.max(largestLeft, children.get(i).prefWidth(height));
                largestRight = Math.max(largestRight, children.get(i + 1).prefWidth(height));
            }
            // We don't make our preferred width to be the actual sum of 
            // the largest preferred left + largest preferred right
            // because this may be massive and cause very wide inspector
            // windows.  So we cap the left at 150 and the right at
            // LARGEST_SENSIBLE_RIGHT (currently 500):
            return Math.min(150, largestLeft) + Math.min(LARGEST_SENSIBLE_RIGHT, largestRight) + getPadding().getLeft() + getPadding().getRight();
        }
    }
}        
