/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg 

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
package bluej.editor.flow;

import bluej.utility.javafx.JavaFXUtil;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * Created by neil on 29/06/2016.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class ScopeColorsBorderPane extends BorderPane implements ScopeColors
{
    private final CssMetaData<ScopeColorsBorderPane, Color> BACKGROUND_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-background-color", ScopeColorsBorderPane::scopeBackgroundColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> CLASS_COLOR_META_DATA =
        JavaFXUtil.cssColor("-bj-class-color", ScopeColorsBorderPane::scopeClassColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> CLASS_OUTER_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-class-outer-color", ScopeColorsBorderPane::scopeClassOuterColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> CLASS_INNER_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-class-inner-color", ScopeColorsBorderPane::scopeClassInnerColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> METHOD_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-method-color", ScopeColorsBorderPane::scopeMethodColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> METHOD_OUTER_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-method-outer-color", ScopeColorsBorderPane::scopeMethodOuterColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> SELECTION_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-selection-color", ScopeColorsBorderPane::scopeSelectionColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> SELECTION_OUTER_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-selection-outer-color", ScopeColorsBorderPane::scopeSelectionOuterColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> ITERATION_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-iteration-color", ScopeColorsBorderPane::scopeIterationColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> ITERATION_OUTER_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-iteration-outer-color", ScopeColorsBorderPane::scopeIterationOuterColorProperty);

    private final CssMetaData<ScopeColorsBorderPane, Color> BREAKPOINT_OVERLAY_META_DATA =
        JavaFXUtil.cssColor("-bj-breakpoint-overlay-color", ScopeColorsBorderPane::breakpointOverlayColorProperty);
    private final CssMetaData<ScopeColorsBorderPane, Color> STEP_OVERLAY_META_DATA =
        JavaFXUtil.cssColor("-bj-step-overlay-color", ScopeColorsBorderPane::stepMarkOverlayColorProperty);


    private final SimpleStyleableObjectProperty<Color> scopeBackgroundColorProperty = new SimpleStyleableObjectProperty<Color>(BACKGROUND_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeClassColorProperty = new SimpleStyleableObjectProperty<Color>(CLASS_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeClassOuterColorProperty = new SimpleStyleableObjectProperty<Color>(CLASS_OUTER_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeClassInnerColorProperty = new SimpleStyleableObjectProperty<Color>(CLASS_INNER_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeMethodColorProperty = new SimpleStyleableObjectProperty<Color>(METHOD_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeMethodOuterColorProperty = new SimpleStyleableObjectProperty<Color>(METHOD_OUTER_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeSelectionColorProperty = new SimpleStyleableObjectProperty<Color>(SELECTION_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeSelectionOuterColorProperty = new SimpleStyleableObjectProperty<Color>(SELECTION_OUTER_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeIterationColorProperty = new SimpleStyleableObjectProperty<Color>(ITERATION_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> scopeIterationOuterColorProperty = new SimpleStyleableObjectProperty<Color>(ITERATION_OUTER_COLOR_META_DATA, Color.WHITE);
    private final SimpleStyleableObjectProperty<Color> breakpointOverlayColorProperty = new SimpleStyleableObjectProperty<Color>(BREAKPOINT_OVERLAY_META_DATA, Color.RED);
    private final SimpleStyleableObjectProperty<Color> stepMarkOverlayColorProperty = new SimpleStyleableObjectProperty<Color>(STEP_OVERLAY_META_DATA, Color.GREEN);

    private final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList =
        JavaFXUtil.extendCss(BorderPane.getClassCssMetaData())
            .add(BACKGROUND_COLOR_META_DATA)
            .add(CLASS_COLOR_META_DATA)
            .add(CLASS_OUTER_COLOR_META_DATA)
            .add(CLASS_INNER_COLOR_META_DATA)
            .add(METHOD_COLOR_META_DATA)
            .add(METHOD_OUTER_COLOR_META_DATA)
            .add(SELECTION_COLOR_META_DATA)
            .add(SELECTION_OUTER_COLOR_META_DATA)
            .add(ITERATION_COLOR_META_DATA)
            .add(ITERATION_OUTER_COLOR_META_DATA)
            .add(BREAKPOINT_OVERLAY_META_DATA)
            .add(STEP_OVERLAY_META_DATA)
            .build();

    public final SimpleStyleableObjectProperty<Color> scopeBackgroundColorProperty() { return scopeBackgroundColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeClassColorProperty() { return scopeClassColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeClassOuterColorProperty() { return scopeClassOuterColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeClassInnerColorProperty() { return scopeClassInnerColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeMethodColorProperty() { return scopeMethodColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeMethodOuterColorProperty() { return scopeMethodOuterColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeSelectionColorProperty() { return scopeSelectionColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeSelectionOuterColorProperty() { return scopeSelectionOuterColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeIterationColorProperty() { return scopeIterationColorProperty; }
    public final SimpleStyleableObjectProperty<Color> scopeIterationOuterColorProperty() { return scopeIterationOuterColorProperty; }
    public final SimpleStyleableObjectProperty<Color> breakpointOverlayColorProperty() { return breakpointOverlayColorProperty; }
    public final SimpleStyleableObjectProperty<Color> stepMarkOverlayColorProperty() { return stepMarkOverlayColorProperty; }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData()
    {
        return cssMetaDataList;
    }

    public ScopeColorsBorderPane()
    {
        JavaFXUtil.addStyleClass(this, "scope-colors");
    }
}
