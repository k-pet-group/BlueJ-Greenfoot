/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.slots;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class for rotatable triangle to be used with hideable canvases.
 * In the default JavaFX font, the right-pointing triangle is much smaller than
 * the downward-pointing triangle, so we can't use the two triangles to toggle 
 * the selection.  Instead, we always use the downward pointing one, but rotate it.
 * 
 * @author NCCB
 */
public class TriangleLabel extends HBox implements HeaderItem
{
    private final SimpleBooleanProperty expanded;
    private final Canvas canvas = new Canvas(8, 10);
    
    private final SimpleStyleableObjectProperty<Color> cssColorProperty = new SimpleStyleableObjectProperty<>(COLOR_META_DATA);
    public final SimpleStyleableObjectProperty<Color> cssColorProperty() { return cssColorProperty; }
    
    private static final CssMetaData<TriangleLabel, Color> COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-fill-color", TriangleLabel::cssColorProperty);
    
    private static final List <CssMetaData <? extends Styleable, ? > > cssMetaDataList =
            JavaFXUtil.extendCss(HBox.getClassCssMetaData())
              .add(COLOR_META_DATA)
              .build();
         
    public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() { return cssMetaDataList; }
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() { return getClassCssMetaData(); }    

    public TriangleLabel(InteractionManager editor, FXConsumer<SharedTransition> expand, FXConsumer<SharedTransition> shrink, SimpleBooleanProperty showing)
    {        
        this.expanded = showing;
        JavaFXUtil.addChangeListener(cssColorProperty, c -> {
            GraphicsContext g = this.canvas.getGraphicsContext2D();
            g.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
            g.setFill(c);
            g.fillPolygon(new double[] {1, 7, 1}, new double[] {1, 5, 9}, 3);
        });
        
        JavaFXUtil.addStyleClass(this, "triangle-label");
        
        getChildren().add(this.canvas);
        setAlignment(Pos.BASELINE_LEFT);
        setFillHeight(false);

        addEventFilter(MouseEvent.MOUSE_CLICKED, e -> { expanded.set(!expanded.get()); e.consume(); });
        JavaFXUtil.addChangeListener(expanded, new FXConsumer<Boolean>() {
            private SharedTransition transition = null;

            @Override
            public void accept(Boolean nowExpanded)
            {
                if (nowExpanded)
                {
                    JavaFXUtil.runNowOrLater(() -> animate(true));
                }
                else
                {
                    JavaFXUtil.runNowOrLater(() -> {
                        animate(false);
                        editor.getSelection().clear();
                    });
                }
            }

            @OnThread(Tag.FXPlatform)
            private void animate(boolean expandAnim)
            {
                if (transition != null)
                {
                    transition.stop();
                    transition = null;
                }
                transition = new SharedTransition();
                if (expandAnim)
                    expand.accept(transition);
                else
                    shrink.accept(transition);
                canvas.rotateProperty().bind((expandAnim ? transition.getProgress() : transition.getOppositeProgress()).multiply(90.0));
                transition.addOnStopped(canvas.rotateProperty()::unbind);
                transition.animateOver(Duration.millis(200));

            }
        });
    }
    @Override
    public ObservableList<Node> getComponents()
    {
        return FXCollections.observableArrayList(this);
    }
    @Override
    public EditableSlot asEditable()
    {
        return null;
    }
    
    public SimpleBooleanProperty expandedProperty() { return expanded; }

    @Override
    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animate)
    {
    }
}
