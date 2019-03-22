/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2016,2017,2019  Michael Kolling and John Rosenberg
 
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

import static bluej.debugger.DebuggerObject.OBJECT_REFERENCE;

/**
 * A graphical representation of a list of fields from a class or object.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 *  
 */
@OnThread(Tag.FXPlatform)
public class FieldList extends TableView<FieldInfo>
{
    final static private Image objectrefIcon = Config.getImageAsFXImage("image.inspector.objectref");
    private static class StringOrRef
    {
        private String string; // null if reference

        public StringOrRef(String string)
        {
            this.string = OBJECT_REFERENCE.equals(string) ? null : string;
        }
    }

    /**
     * Creates a new fieldlist with no data.
     */
    public FieldList()
    {
        this.getSelectionModel().setCellSelectionEnabled(false);
        this.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        int rowHeight = 30;
        this.setFixedCellSize(rowHeight);
        prefHeightProperty().bind(Bindings.min(400.0, fixedCellSizeProperty().multiply(Bindings.size(getItems())).add(JavaFXUtil.ofD(paddingProperty(), Insets::getTop)).add(JavaFXUtil.ofD(paddingProperty(), Insets::getBottom))));
        setMinHeight(3.5 * (double)rowHeight);
        JavaFXUtil.addStyleClass(this, "field-list");

        javafx.scene.control.TableColumn<FieldInfo, String> description = new javafx.scene.control.TableColumn<FieldInfo, String>();
        JavaFXUtil.addStyleClass(description, "inspector-field-description");
        description.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().getDescription()));
        javafx.scene.control.TableColumn<FieldInfo, StringOrRef> value = new javafx.scene.control.TableColumn<>();
        JavaFXUtil.addStyleClass(value, "inspector-field-value");
        value.setCellValueFactory(v -> new ReadOnlyObjectWrapper(new StringOrRef(v.getValue().getValue())));
        value.setCellFactory(col -> new ValueCell());
        getColumns().setAll(description, value);

        // Apply auto fit size to the fieldList Columns by setting the 
        // MinWidth and MaxWidth of the description column since it is first column on the left, 
        // and setting the MinWidth of the value column
        JavaFXUtil.addChangeListener(widthProperty(), s -> {
            double descriptionWidth =0;
            for (int i=0; i < getItems().size();i++) 
            {
                Text textDescription = new Text(description.getCellData(i));
                if (descriptionWidth < textDescription.getLayoutBounds().getWidth()) 
                {
                    descriptionWidth = textDescription.getLayoutBounds().getWidth();
                }
            }
            description.setMinWidth(descriptionWidth * 1.5);
            description.setMaxWidth(descriptionWidth * 2);

            double valueWidth =0;
            for (int i=0; i< getItems().size();i++) 
            {
                if (value.getText() != null)
                {
                    Text textValue = new Text(value.getCellData(i).string);
                    if (valueWidth < textValue.getLayoutBounds().getWidth()) 
                    {
                        valueWidth = textValue.getLayoutBounds().getWidth();
                    }
                }
            }
            value.setMinWidth(valueWidth);
        });
        
        // Turn off header, from https://community.oracle.com/thread/2321823
        JavaFXUtil.addChangeListener(widthProperty(), ignore -> {
            //Don't show header
            Pane header = (Pane) lookup("TableHeaderRow");
            if (header.isVisible()){
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
            }
        });
    }

    /**
     * A list of fields that should be shown in this list.
     * 
     * @param listData
     *            The list of fields
     */
    public void setData(List<FieldInfo> listData)
    {
        getItems().setAll(listData);        
    }

    /**
     * A TableCell which either shows a label or a graphic.  They are wrapped
     * in a container to allow a border with padding to be applied.
     */
    private static class ValueCell extends TableCell<FieldInfo, StringOrRef>
    {
        private HBox container = new HBox(); // HBox so that we can use baseline-alignment
        private Label label = new Label();
        private ImageView objRefPic;
        private SimpleBooleanProperty showingLabel = new SimpleBooleanProperty(true);
        private SimpleBooleanProperty occupied = new SimpleBooleanProperty(true);

        public ValueCell()
        {
            objRefPic = new ImageView(objectrefIcon);
            container.getChildren().addAll(label, objRefPic);
            JavaFXUtil.addStyleClass(container, "inspector-field-value-wrapper");
            JavaFXUtil.addStyleClass(label, "inspector-field-value-label");
            objRefPic.managedProperty().bind(showingLabel.not());
            objRefPic.visibleProperty().bind(showingLabel.not());
            label.managedProperty().bind(showingLabel);
            label.visibleProperty().bind(showingLabel);
            container.visibleProperty().bind(occupied);
            setText("");
            setGraphic(container);
        }

        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        protected void updateItem(StringOrRef v, boolean empty)
        {
            super.updateItem(v, empty);
            occupied.set(!empty);
            if (v != null && v.string == null)
            {
                showingLabel.set(false);
            }
            else
            {
                label.setText(v == null || empty ? "" : v.string);
                showingLabel.set(true);
            }
        }
    }
}

