/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.generic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import bluej.utility.javafx.BetterVBox;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

// Package-visible
// A VBox that supports a -bj-left-margin CSS property
class CanvasVBox extends BetterVBox
{
    private final SimpleStyleableDoubleProperty leftMarginProperty = new SimpleStyleableDoubleProperty(LEFT_MARGIN_META_DATA);
    private final SimpleStyleableDoubleProperty bottomMarginProperty = new SimpleStyleableDoubleProperty(BOTTOM_MARGIN_META_DATA);
    private final SimpleStyleableDoubleProperty topMarginProperty = new SimpleStyleableDoubleProperty(TOP_MARGIN_META_DATA);
    private final SimpleStyleableDoubleProperty rightMarginProperty = new SimpleStyleableDoubleProperty(RIGHT_MARGIN_META_DATA);
    private final SimpleStyleableDoubleProperty cssMinHeightProperty = new SimpleStyleableDoubleProperty(MIN_HEIGHT_META_DATA);
    private final SimpleStyleableDoubleProperty frameMarginTopProperty = new SimpleStyleableDoubleProperty(TOP_FRAME_MARGIN_META_DATA);
    private final SimpleStyleableDoubleProperty frameMarginBottomProperty = new SimpleStyleableDoubleProperty(BOTTOM_FRAME_MARGIN_META_DATA);

    private final SimpleStyleableDoubleProperty curlyBracketHeight = new SimpleStyleableDoubleProperty(CURLY_BRACKET_HEIGHT_META_DATA);

    /*
     * To explain these properties: we want to be able to animate the insets for the border and background
     * as part of the transition to Java preview (primarily, to add space for brackets above and below the canvas,
     * without having to add another node).  We are setting custom insets anyway, because we use custom properties
     * for the margins, above.
     *
     * If any of the border or background properties are set in CSS, then when CSS is reapplied, any customisations
     * (e.g. during animation) to *any* border or background aspects are clobbered, because border and background
     * are each set as one complete object.  To avoid this, we don't set the -fx-{border,background}-* properties
     * in CSS, but instead set -bj-{border,background}-* equivalents.  We then manually merge these with the custom
     * margins to form the real border and background.
     */

    private final SimpleStyleableObjectProperty<Color> borderColorProperty = new SimpleStyleableObjectProperty<Color>(BORDER_COLOR_META_DATA);
    private final SimpleStyleableObjectProperty<Insets> borderInsetsProperty = new SimpleStyleableObjectProperty<>(BORDER_INSETS_META_DATA);
    private final SimpleStyleableObjectProperty<Color> backgroundColorProperty = new SimpleStyleableObjectProperty<Color>(BACKGROUND_COLOR_META_DATA);
    private final SimpleStyleableObjectProperty<Insets> backgroundInsetsProperty = new SimpleStyleableObjectProperty<>(BACKGROUND_INSETS_META_DATA);

    // None of the following are actually Insets, but Insets happens to have a pre-made CSS converter
    // which handles 1 or 4 values in a list nicely, so we are repurposing it to avoid writing our own:
    private final SimpleStyleableObjectProperty<Insets> borderWidthProperty = new SimpleStyleableObjectProperty<>(BORDER_WIDTH_META_DATA);
    private final SimpleStyleableObjectProperty<Insets> borderRadiusProperty = new SimpleStyleableObjectProperty<>(BORDER_RADIUS_META_DATA);
    private final SimpleStyleableObjectProperty<Insets> backgroundRadiusProperty = new SimpleStyleableObjectProperty<>(BACKGROUND_RADIUS_META_DATA);

    private static final CssMetaData<CanvasVBox, Number> LEFT_MARGIN_META_DATA =
            JavaFXUtil.cssSize("-bj-left-margin", v -> v.leftMarginProperty);
    private static final CssMetaData<CanvasVBox, Number> BOTTOM_MARGIN_META_DATA =
            JavaFXUtil.cssSize("-bj-bottom-margin", v -> v.bottomMarginProperty);
    private static final CssMetaData<CanvasVBox, Number> TOP_MARGIN_META_DATA =
            JavaFXUtil.cssSize("-bj-top-margin", v -> v.topMarginProperty);
    private static final CssMetaData<CanvasVBox, Number> RIGHT_MARGIN_META_DATA =
            JavaFXUtil.cssSize("-bj-right-margin", v -> v.rightMarginProperty);
    
    private static final CssMetaData<CanvasVBox, Number> MIN_HEIGHT_META_DATA =
            JavaFXUtil.cssSize("-bj-min-height", v -> v.cssMinHeightProperty);

    private static final CssMetaData<CanvasVBox, Number> CURLY_BRACKET_HEIGHT_META_DATA =
            JavaFXUtil.cssSize("-bj-curly-bracket-height", v -> v.curlyBracketHeight);

    private static final CssMetaData<CanvasVBox, Number> TOP_FRAME_MARGIN_META_DATA =
        JavaFXUtil.cssSize("-bj-frame-margin-top", v -> v.frameMarginTopProperty);
    private static final CssMetaData<CanvasVBox, Number> BOTTOM_FRAME_MARGIN_META_DATA =
        JavaFXUtil.cssSize("-bj-frame-margin-bottom", v -> v.frameMarginBottomProperty);

    private static final CssMetaData<CanvasVBox, Color> BORDER_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-border-color", v -> v.borderColorProperty);
    private static final CssMetaData<CanvasVBox, Color> BACKGROUND_COLOR_META_DATA =
            JavaFXUtil.cssColor("-bj-background-color", v -> v.backgroundColorProperty);

    private static final CssMetaData<CanvasVBox, Insets> BORDER_INSETS_META_DATA =
            JavaFXUtil.cssInsets("-bj-border-insets", v -> v.borderInsetsProperty);
    private static final CssMetaData<CanvasVBox, Insets> BACKGROUND_INSETS_META_DATA =
            JavaFXUtil.cssInsets("-bj-background-insets", v -> v.backgroundInsetsProperty);

    // See above comment on the properties: not really Insets
    private static final CssMetaData<CanvasVBox, Insets> BORDER_RADIUS_META_DATA =
            JavaFXUtil.cssInsets("-bj-border-radius", v -> v.borderRadiusProperty);
    private static final CssMetaData<CanvasVBox, Insets> BORDER_WIDTH_META_DATA =
            JavaFXUtil.cssInsets("-bj-border-width", v -> v.borderWidthProperty);
    private static final CssMetaData<CanvasVBox, Insets> BACKGROUND_RADIUS_META_DATA =
            JavaFXUtil.cssInsets("-bj-background-radius", v -> v.backgroundRadiusProperty);

    private static final List <CssMetaData <? extends Styleable, ? > > cssMetaDataList =
            JavaFXUtil.extendCss(Pane.getClassCssMetaData())
                    .add(LEFT_MARGIN_META_DATA)
                    .add(BOTTOM_MARGIN_META_DATA)
                    .add(TOP_MARGIN_META_DATA)
                    .add(RIGHT_MARGIN_META_DATA)
                    .add(MIN_HEIGHT_META_DATA)
                    .add(TOP_FRAME_MARGIN_META_DATA)
                    .add(BOTTOM_FRAME_MARGIN_META_DATA)
                    .add(CURLY_BRACKET_HEIGHT_META_DATA)
                    .add(BORDER_COLOR_META_DATA)
                    .add(BACKGROUND_COLOR_META_DATA)
                    .add(BORDER_INSETS_META_DATA)
                    .add(BORDER_RADIUS_META_DATA)
                    .add(BORDER_WIDTH_META_DATA)
                    .add(BACKGROUND_RADIUS_META_DATA)
              .build();
         
    public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() { return cssMetaDataList; }
    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() { return getClassCssMetaData(); }

    private DoubleProperty animateExtraSpace = new SimpleDoubleProperty(0.0);
    // This one is only used by imports canvas at the moment, to animate left border:
    private final DoubleProperty leftMarginScale = new SimpleDoubleProperty(1.0);
    
    private final HashSet<Node> frameNodes = new HashSet<>(); // read-only, for doing margins

    public CanvasVBox(double minWidth, ObservableList<Frame> frames)
    {
        super(minWidth);
        frames.addListener((ListChangeListener<? super Frame>) change -> {
            frameNodes.clear();
            for (Frame f : frames)
                frameNodes.add(f.getNode());
        });
        
        minHeightProperty().bind(cssMinHeightProperty);
        
        // This is a bit hacky; we borrow our CSS properties and use them to set
        // other CSS properties as the implementation.  But the implementation
        // may change again in future, so it's not a bad idea to decouple our CSS
        // margins from how exactly we make them take effect:
        ChangeListener l = (a, b, c) -> {
            Insets insets = calculateMargins(Insets.EMPTY);
            setPadding(insets);
            updateBackground();
            updateBorder();
        };
        leftMarginProperty.addListener(l);
        leftMarginScale.addListener(l);
        rightMarginProperty.addListener(l);
        topMarginProperty.addListener(l);
        bottomMarginProperty.addListener(l);
        animateExtraSpace.addListener(l);

        ChangeListener border = (a, b, c) -> updateBorder();
        borderWidthProperty.addListener(border);
        borderColorProperty.addListener(border);
        borderRadiusProperty.addListener(border);
        borderInsetsProperty.addListener(border);

        ChangeListener bk = (a, b, c) -> updateBackground();
        backgroundColorProperty.addListener(bk);
        backgroundRadiusProperty.addListener(bk);
    }

    private void updateBackground()
    {
        setBackground(new Background(new BackgroundFill(backgroundColorProperty.get(), makeCornerRadii(backgroundRadiusProperty.get()), calculateMargins(backgroundInsetsProperty.get()))));
    }

    private static CornerRadii makeCornerRadii(Insets xs)
    {
        if (xs == null)
            return CornerRadii.EMPTY;
        else
            // Here, top/right/bottom/left are really just the first, second, third, fourth items from the CSS,
            // they are not actually a top item.
            return new CornerRadii(xs.getTop(), xs.getRight(), xs.getBottom(), xs.getLeft(), false);
    }

    private static BorderWidths makeBorderWidths(Insets xs)
    {
        if (xs == null)
            return BorderWidths.EMPTY;
        else
            // Here, top/right/bottom/left are really just the first, second, third, fourth items from the CSS,
            // they are not actually necessarily a top inset.
            return new BorderWidths(xs.getTop(), xs.getRight(), xs.getBottom(), xs.getLeft());
    }

    private void updateBorder()
    {
        final BorderStrokeStyle style = new BorderStrokeStyle(StrokeType.OUTSIDE, StrokeLineJoin.ROUND, StrokeLineCap.SQUARE, 10.0f, 0.0f, Collections.emptyList());

        setBorder(new Border(new BorderStroke(borderColorProperty.get(), style, makeCornerRadii(borderRadiusProperty.get()), makeBorderWidths(borderWidthProperty.get()), calculateMargins(borderInsetsProperty.get()))));
    }

    private Insets calculateMargins(Insets including)
    {
        if (including == null)
            including = Insets.EMPTY;

        // Since 8u60, it seems that we don't need to allow space for the border, it is now
        // drawn inside the child's margins, not outside, even though our style is outside?
        // Anyway, not adding anything for it with 8u60, not sure if it is bug or a fix!
        final int top = (int) topMarginProperty.add(animateExtraSpace.multiply(curlyBracketHeight)).get() + (int) including.getTop();
        final int right = (int) rightMarginProperty.get() + (int)including.getRight();
        final int bottom = (int) bottomMarginProperty.add(animateExtraSpace.multiply(curlyBracketHeight)).get() + (int) including.getBottom();
        final int left = (int) leftMarginProperty.multiply(leftMarginScale).get() + (int)including.getLeft();
        return new Insets(top, right, bottom, left);
    }

    public void addSpace(SharedTransition animate)
    {
        animateExtraSpace.bind(animate.getProgress());
        animate.addOnStopped(animateExtraSpace::unbind);
    }

    public void removeSpace(SharedTransition animate)
    {
        animateExtraSpace.bind(animate.getProgress().negate().add(1.0));
        animate.addOnStopped(animateExtraSpace::unbind);
    }

    public double getBottomMargin()
    {
        return (int)bottomMarginProperty.get();
    }

    // Public, read-only margins:
    public DoubleBinding leftMarginProperty()
    {
        return new DoubleBinding()
        {
            { super.bind(leftMarginProperty); super.bind(leftMarginScale);}
            
            @Override
            protected double computeValue()
            {
                return (int)(leftMarginProperty.get() * leftMarginScale.get());
            }
        };
    }

    public DoubleBinding rightMarginProperty()
    {
        return new DoubleBinding()
        {
            { super.bind(rightMarginProperty); }

            @Override
            protected double computeValue()
            {
                return (int)rightMarginProperty.get();
            }
        };
    }

    @Override
    public double getTopMarginFor(Node n)
    {
        if (frameNodes.contains(n))
            return frameMarginTopProperty.get();
        return super.getTopMarginFor(n);
    }

    @Override
    public double getBottomMarginFor(Node n)
    {
        if (frameNodes.contains(n))
            return frameMarginBottomProperty.get();
        return super.getBottomMarginFor(n);
    }

    /**
     * Gets the bounds, in scene coordinates, of the contents of the canvas.
     *
     * The bounds of getNode() includes the margins of the canvas.  This utility method
     * excludes those margins and just gets the bounds of the actual visible canvas area
     * (which generally has the rounded rectangle around it)
     */
    public Bounds getContentSceneBounds()
    {
        Bounds b = localToScene(getBoundsInLocal());
        return new BoundingBox(
                b.getMinX() + leftMarginProperty.get(),
                b.getMinY() + topMarginProperty.get(),
                b.getWidth() - leftMarginProperty.get() - rightMarginProperty.get(),
                b.getHeight() - topMarginProperty.get() - bottomMarginProperty.get()
                );
    }

    public void animateColorsToPseudoClass(String pseudo, boolean on, SharedTransition animation)
    {
        final Color startBackground = backgroundColorProperty.get();
        final Color startBorder = borderColorProperty.get();
        JavaFXUtil.setPseudoclass(pseudo, on, this);
        applyCss();
        final Color endBackground = backgroundColorProperty.get();
        final Color endBorder = borderColorProperty.get();
        if (!startBackground.equals(endBackground) || !startBorder.equals(endBorder))
        {
            JavaFXUtil.addChangeListener(animation.getProgress(), t -> {
                Color background = startBackground.interpolate(endBackground, t.doubleValue());
                Color border = startBorder.interpolate(endBorder, t.doubleValue());
                backgroundColorProperty.set(background);
                borderColorProperty.set(border);
                updateBackground();
                updateBorder();
            });
        }
    }

    public DoubleProperty leftMarginScaleProperty()
    {
        return leftMarginScale;
    }

    public double getCurlyBracketHeight()
    {
        return curlyBracketHeight.get();
    }
}