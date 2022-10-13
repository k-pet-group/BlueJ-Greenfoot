/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package bluej.utility.javafx;

/*
 * This file contains source taken from Oracle JavaFX "FlowPane" class, available under the
 * GPL-with-classpath-exception license as per the original copyright notice above. Modifications
 * have been made for use in BlueJ/Greenfoot.
 * 
 * Modified: 2015, 2018.
 */

import javafx.css.SimpleStyleableDoubleProperty;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.EnumConverter;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.css.Styleable;

/**
 * FlowPane lays out its children in a flow that wraps at the flowpane's boundary.
 * <p>
 * A horizontal flowpane (the default) will layout nodes in rows, wrapping at the
 * flowpane's width.  A vertical flowpane lays out nodes in columns,
 * wrapping at the flowpane's height.  If the flowpane has a border and/or padding set,
 * the content will be flowed within those insets.
 * <p>
 * FlowPane's prefWrapLength property establishes it's preferred width
 * (for horizontal) or preferred height (for vertical). Applications should set
 * prefWrapLength if the default value (400) doesn't suffice.  Note that prefWrapLength
 * is used only for calculating the preferred size and may not reflect the actual
 * wrapping dimension, which tracks the actual size of the flowpane.
 * <p>
 * The alignment property controls how the rows and columns are aligned
 * within the bounds of the flowpane and defaults to Pos.TOP_LEFT.  It is also possible
 * to control the alignment of nodes within the rows and columns by setting
 * rowValignment for horizontal or columnHalignment for vertical.
 * <p>
 * Example of a horizontal flowpane:
 * <pre><code>     Image images[] = { ... };
 *     FlowPane flow = new FlowPane();
 *     flow.setVgap(8);
 *     flow.setHgap(4);
 *     flow.setPrefWrapLength(300); // preferred width = 300
 *     for (int i = 0; i < images.length; i++) {
 *         flow.getChildren().add(new ImageView(image[i]);
 *     }
 * </code></pre>
 *
 *<p>
 * Example of a vertical flowpane:
 * <pre><code>     FlowPane flow = new FlowPane(Orientation.VERTICAL);
 *     flow.setColumnHalignment(HPos.LEFT); // align labels on left
 *     flow.setPrefWrapLength(200); // preferred height = 200
 *     for (int i = 0; i < titles.size(); i++) {
 *         flow.getChildren().add(new Label(titles[i]);
 *     }
 * </code></pre>
 *
 * <p>
 * FlowPane lays out each managed child regardless of the child's visible property value;
 * unmanaged children are ignored for all layout calculations.</p>
 *
 * <p>
 * FlowPane may be styled with backgrounds and borders using CSS.  See
 * {@link javafx.scene.layout.Region Region} superclass for details.</p>
 *
 * <h4>Resizable Range</h4>
 *
 * A flowpane's parent will resize the flowpane within the flowpane's resizable range
 * during layout.   By default the flowpane computes this range based on its content
 * as outlined in the tables below.
 * <p>
 * horizontal:
 * <table border="1">
 * <tr><td></td><th>width</th><th>height</th></tr>
 * <tr><th>minimum</th>
 * <td>left/right insets plus largest of children's pref widths</td>
 * <td>top/bottom insets plus height required to display all children at their preferred heights when wrapped at a specified width</td></tr>
 * <tr><th>preferred</th>
 * <td>left/right insets plus prefWrapLength</td>
 * <td>top/bottom insets plus height required to display all children at their pref heights when wrapped at a specified width</td></tr>
 * <tr><th>maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * vertical:
 * <table border="1">
 * <tr><td></td><th>width</th><th>height</th></tr>
 * <tr><th>minimum</th>
 * <td>left/right insets plus width required to display all children at their preferred widths when wrapped at a specified height</td>
 * <td>top/bottom insets plus largest of children's pref heights</td><tr>
 * <tr><th>preferred</th>
 * <td>left/right insets plus width required to display all children at their pref widths when wrapped at the specified height</td>
 * <td>top/bottom insets plus prefWrapLength</td><tr>
 * <tr><th>maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * A flowpane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * FlowPane provides properties for setting the size range directly.  These
 * properties default to the sentinel value Region.USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>flowpane.setMaxWidth(500);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to Region.USE_COMPUTED_SIZE.
 * <p>
 * FlowPane does not clip its content by default, so it is possible that childrens'
 * bounds may extend outside its own bounds if a child's pref size is larger than
 * the space flowpane has to allocate for it.</p>
 *
 * @since JavaFX 2.0
 */
public class HangingFlowPane extends Pane {

    /********************************************************************
     *  BEGIN static methods
     ********************************************************************/
    private static final String MARGIN_CONSTRAINT = "flowpane-margin";

    /**
     * Sets the margin for the child when contained by a flowpane.
     * If set, the flowpane will layout it out with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a flowpane
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        FlowPane.setMargin(child, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param child the child node of a flowpane
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return FlowPane.getMargin(child);
    }

    /**
     * Removes all flowpane constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setMargin(child, null);
    }

    /********************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates a horizontal FlowPane layout with hgap/vgap = 0.
     */
    public HangingFlowPane() {
        super();
        getStyleClass().add("hanging-flow-pane");
    }

    /**
     * Creates a horizontal FlowPane layout with hgap/vgap = 0.
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public HangingFlowPane(Node... children) {
        this();
        getChildren().addAll(children);
    }

    private SimpleStyleableDoubleProperty hangingIndentProperty = new SimpleStyleableDoubleProperty(StyleableProperties.HANGING_INDENT, 0.0);

    /**
     * The amount that each row after the first is indented by.
     */
    public SimpleStyleableDoubleProperty hangingIndentProperty()
    {
        return hangingIndentProperty;
    }
    
    public void setHangingIndent(double pixels)
    {
        hangingIndentProperty.set(pixels);
    }

    /**
     * The gap between rows when there are multiple rows
     */
    private SimpleStyleableDoubleProperty rowSpacingProperty = new SimpleStyleableDoubleProperty(StyleableProperties.ROW_SPACING, 0.0);
    
    /**
     * The preferred width where content should wrap in a horizontal flowpane or
     * the preferred height where content should wrap in a vertical flowpane.
     * <p>
     * This value is used only to compute the preferred size of the flowpane and may
     * not reflect the actual width or height, which may change if the flowpane is
     * resized to something other than its preferred size.
     * <p>
     * Applications should initialize this value to define a reasonable span
     * for wrapping the content.
     *
     */
    public final DoubleProperty prefWrapLengthProperty() {
        if (prefWrapLength == null) {
            prefWrapLength = new DoublePropertyBase(400) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return HangingFlowPane.this;
                }

                @Override
                public String getName() {
                    return "prefWrapLength";
                }
            };
        }
        return prefWrapLength;
    }
    private DoubleProperty prefWrapLength;
    public final void setPrefWrapLength(double value) { prefWrapLengthProperty().set(value); }
    public final double getPrefWrapLength() { return prefWrapLength == null ? 400 : prefWrapLength.get(); }


    /**
     * The overall alignment of the flowpane's content within its width and height.
     * <p>For a horizontal flowpane, each row will be aligned within the flowpane's width
     * using the alignment's hpos value, and the rows will be aligned within the
     * flowpane's height using the alignment's vpos value.
     * <p>For a vertical flowpane, each column will be aligned within the flowpane's height
     * using the alignment's vpos value, and the columns will be aligned within the
     * flowpane's width using the alignment's hpos value.
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {

                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<HangingFlowPane, Pos> getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return HangingFlowPane.this;
                }

                @Override
                public String getName() {
                    return "alignment";
                }
            };
        }
        return alignment;
    }

    private ObjectProperty<Pos> alignment;
    public final void setAlignment(Pos value) { alignmentProperty().set(value); }
    public final Pos getAlignment() { return alignment == null ? Pos.TOP_LEFT : alignment.get(); }
    private Pos getAlignmentInternal() {
        Pos localPos = getAlignment();
        return localPos == null ? Pos.TOP_LEFT : localPos;
    }

    /**
     * The vertical alignment of nodes within each row of a horizontal flowpane.
     * If this property is set to VPos.BASELINE, then the flowpane will always
     * resize children to their preferred heights, rather than expanding heights
     * to fill the row height.
     * The property is ignored for vertical flowpanes.
     */
    public final ObjectProperty<VPos> rowValignmentProperty() {
        if (rowValignment == null) {
            rowValignment = new StyleableObjectProperty<VPos>(VPos.CENTER) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<HangingFlowPane, VPos> getCssMetaData() {
                    return StyleableProperties.ROW_VALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return HangingFlowPane.this;
                }

                @Override
                public String getName() {
                    return "rowValignment";
                }
            };
        }
        return rowValignment;
    }

    private ObjectProperty<VPos> rowValignment;
    public final void setRowValignment(VPos value) { rowValignmentProperty().set(value); }
    public final VPos getRowValignment() { return rowValignment == null ? VPos.CENTER : rowValignment.get(); }
    private VPos getRowValignmentInternal() {
        VPos localPos =  getRowValignment();
        return localPos == null ? VPos.CENTER : localPos;
    }

    @Override public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override protected double computeMinWidth(double height)
    {
        double maxPref = 0;
        final List<Node> children = getChildren();
        for (int i=0, size=children.size(); i<size; i++) {
            Node child = children.get(i);
            if (child.isManaged()) {
                maxPref = Math.max(maxPref, child.prefWidth(-1));
            }
        }
        final Insets insets = getInsets();
        return insets.getLeft() + snapSize(maxPref) + insets.getRight();
    }

    @Override protected double computeMinHeight(double width)
    {
        return computePrefHeight(width);
    }

    @Override protected double computePrefWidth(double forHeight) {
        final Insets insets = getInsets();
        // horizontal
        double maxRunWidth = getPrefWrapLength();
        List<Run> hruns = getRuns(maxRunWidth);
        double w = computeContentWidth(hruns);
        w = getPrefWrapLength() > w ? getPrefWrapLength() : w;
        return insets.getLeft() + snapSize(w) + insets.getRight();
    }

    @Override protected double computePrefHeight(double forWidth) {
        final Insets insets = getInsets();
        // horizontal
        double maxRunWidth = forWidth != -1?
            forWidth - insets.getLeft() - insets.getRight() : getPrefWrapLength();
        List<Run> hruns = getRuns(maxRunWidth);
        return insets.getTop() + computeContentHeight(hruns) + insets.getBottom();
    }

    @Override public void requestLayout() {
        if (!computingRuns) {
            runs = null;
        }
        super.requestLayout();
    }

    private List<Run> runs = null;
    private double lastMaxRunLength = -1;
    boolean computingRuns = false;

    private List<Run> getRuns(double maxRunLength) {
        if (runs == null || maxRunLength != lastMaxRunLength) {
            computingRuns = true;
            lastMaxRunLength = maxRunLength;
            runs = new ArrayList();
            double runLength = 0;
            double runOffset = 0;
            Run run = new Run();
            double vgap = rowSpacingProperty.get();
            double hgap = 0;

            final List<Node> children = getChildren();
            boolean goingBackwards = false;
            int furthestReached = 0;
            for (int i=0, size=children.size(); i<size; i++) {
                Node child = children.get(i);
                if (child.isManaged()) {
                    LayoutRect nodeRect = new LayoutRect();
                    nodeRect.node = child;
                    Insets margin = getMargin(child);
                    nodeRect.width = computeChildPrefAreaWidth(child, margin);
                    nodeRect.height = computeChildPrefAreaHeight(child, margin);
                    nodeRect.alignment = getAlignment(child);
                    double nodeLength = nodeRect.width;
                    // We only need to do something special if either:
                    //  - our run is too long (thus needs breaking), and the run has multiple items (this one, plus at least one already)
                    //  - we are going backwards removing items to find a suitable break point
                    if (goingBackwards || (runLength + nodeLength > maxRunLength && run.rects.size() >= 1))
                    {
                        // If we are already going backwards, remove from current run:
                        if (goingBackwards)
                        {
                            runLength -= run.rects.get(run.rects.size() - 1).width + hgap;
                            run.rects.remove(run.rects.size() - 1);
                        }
                        
                        // Make sure we can break here.  If not, and the run is not empty,
                        // go backwards (or keep going backwards)
                        // until we find a suitable break point.  However, if we have been
                        // here before (i <= furthestReached), we're not going to manage to find a
                        // good break point
                        // so we need to not try, to avoid going into an infinite loop.
                        if (!canBreakBefore(child) && run.rects.size() > 0 && (goingBackwards || i > furthestReached))
                        {
                            furthestReached = Math.max(i, furthestReached);
                            goingBackwards = true;
                            i -= 2; // We only really want to subtract one, but continue
                                    // still executes the i++ at the end of the loop.
                            continue;
                        }
                        // If we reach here, we will perform a break, even if it is not allowed here.
                        // It may be that the break is redundant (the run is now empty), because
                        // we were going backwards, removed whole run, and are now going to start forwards again.
                        if (run.rects.size() > 0)
                        {
                            normalizeRun(run, runOffset);
                            // horizontal
                            runOffset += run.height + vgap;
                            runs.add(run);
                            runLength = hangingIndentProperty.get();
                            run = new Run();
                        }
                    }
                    // If we reach here, we're no longer going backwards:
                    goingBackwards = false;
                    // horizontal
                    nodeRect.x = runLength;
                    runLength += nodeRect.width + hgap;
                    run.rects.add(nodeRect);
                }

            }
            // insert last run
            normalizeRun(run, runOffset);
            runs.add(run);
            computingRuns = false;
        }
        return runs;
    }

    private void normalizeRun(final Run run, double runOffset) {
        // horizontal
        ArrayList<Node> rownodes = new ArrayList();
        double hgap = 0;
        run.width = (run.rects.size()-1)*snapSpace(hgap);
        for (int i=0, max=run.rects.size(); i<max; i++) {
            LayoutRect lrect = run.rects.get(i);
            rownodes.add(lrect.node);
            run.width += lrect.width;
            lrect.y = runOffset;
        }
        run.height = computeMaxPrefAreaHeight(rownodes, getRowValignment());
        run.baselineOffset = getRowValignment() == VPos.BASELINE?
            getAreaBaselineOffset(rownodes, run.rects, run.height, true) : 0;
    }

    private double computeContentWidth(List<Run> runs) {
        double cwidth = 0;
        for (int i=0, max=runs.size(); i<max; i++) {
            Run run = runs.get(i);
            cwidth = Math.max(cwidth, run.width);
        }
        return cwidth;
    }

    private double computeContentHeight(List<Run> runs) {
        double vgap = rowSpacingProperty.get();
        double cheight = (runs.size()-1)*snapSpace(vgap);
        for (int i=0, max=runs.size(); i<max; i++) {
            Run run = runs.get(i);
            // horizontal
            cheight += run.height;
        }
        return cheight;
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final double width = getWidth();
        final double height = getHeight();
        final double top = insets.getTop();
        final double left = insets.getLeft();
        final double bottom = insets.getBottom();
        final double right = insets.getRight();
        final double insideWidth = width - left - right;
        final double insideHeight = height - top - bottom;

        //REMIND(aim): need to figure out how to cache the runs to avoid over-calculation
        final List<Run> runs = getRuns(insideWidth);

        // Now that the nodes are broken into runs, figure out alignments
        for (int i=0, max=runs.size(); i<max; i++) {
            final Run run = runs.get(i);
            final double xoffset = left + computeXOffset(insideWidth,
                run.width,
                getAlignmentInternal().getHpos());
            final double yoffset = top + computeYOffset(insideHeight,
                computeContentHeight(runs),
                getAlignmentInternal().getVpos());

            // First, do all the left-aligned nodes in the run:
            int leftNode = 0;
            for (; leftNode < run.rects.size() && run.rects.get(leftNode).alignment == FlowAlignment.LEFT; leftNode++) {
                final LayoutRect lrect = run.rects.get(leftNode);
//              System.out.println("flowpane.layout: run="+i+" "+run.width+"x"+run.height+" xoffset="+xoffset+" yoffset="+yoffset+" lrect="+lrect);
                final double x = xoffset + lrect.x;
                final double y = yoffset + lrect.y;
                layoutInArea(lrect.node, x, y,
                    lrect.width,
                    run.height,
                    run.baselineOffset, getMargin(lrect.node),
                    HPos.LEFT, getRowValignmentInternal());
            }
            // Now do all the right-aligned nodes, if there are any:
            if (leftNode < run.rects.size())
            {
                double rightOffset = computeXOffset(insideWidth, 0, HPos.RIGHT) - (run.rects.get(run.rects.size() - 1).x + run.rects.get(run.rects.size() - 1).width);
                for (int rightNode = leftNode; rightNode < run.rects.size(); rightNode++)
                {
                    final LayoutRect lrect = run.rects.get(rightNode);
                    final double x = xoffset + rightOffset + lrect.x;
                    final double y = yoffset + lrect.y;
                    layoutInArea(lrect.node, x, y,
                            lrect.width,
                            run.height,
                            run.baselineOffset, getMargin(lrect.node),
                            HPos.LEFT, getRowValignmentInternal());
                }
            }
        }
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/


    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     * @treatAsPrivate implementation detail
     */
    private static class StyleableProperties {

        private static final CssMetaData<HangingFlowPane,Pos> ALIGNMENT =
            new CssMetaData<HangingFlowPane,Pos>("-fx-alignment",
                new EnumConverter<Pos>(Pos.class), Pos.TOP_LEFT) {

                @Override
                public boolean isSettable(HangingFlowPane node) {
                    return node.alignment == null || !node.alignment.isBound();
                }

                @Override
                public StyleableProperty<Pos> getStyleableProperty(HangingFlowPane node) {
                    return (StyleableProperty<Pos>)node.alignmentProperty();
                }

            };
        private static final CssMetaData<HangingFlowPane, Number> HANGING_INDENT = JavaFXUtil.cssSize("-bj-hanging-indent", hfp -> hfp.hangingIndentProperty);

        private static final CssMetaData<HangingFlowPane, Number> ROW_SPACING = JavaFXUtil.cssSize("-bj-row-spacing", hfp -> hfp.rowSpacingProperty);

        private static final CssMetaData<HangingFlowPane,VPos> ROW_VALIGNMENT =
            new CssMetaData<HangingFlowPane,VPos>("-fx-row-valignment",
                new EnumConverter<VPos>(VPos.class), VPos.CENTER) {

                @Override
                public boolean isSettable(HangingFlowPane node) {
                    return node.rowValignment == null || !node.rowValignment.isBound();
                }

                @Override
                public StyleableProperty<VPos> getStyleableProperty(HangingFlowPane node) {
                    return (StyleableProperty<VPos>)node.rowValignmentProperty();
                }

            };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(ALIGNMENT);
            styleables.add(ROW_VALIGNMENT);
            styleables.add(HANGING_INDENT);
            styleables.add(ROW_SPACING);

            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }


    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8.0
     */


    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    //REMIND(aim); replace when we get mutable rects
    private static class LayoutRect {
        public Node node;
        double x;
        double y;
        double width;
        double height;
        FlowAlignment alignment;

        @Override public String toString() {
            return "LayoutRect node id="+node.getId()+" "+x+","+y+" "+width+"x"+height;
        }
    }

    private static class Run {
        ArrayList<LayoutRect> rects = new ArrayList();
        double width;
        double height;
        double baselineOffset;
    }


    // From Region:

    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch(hpos) {
            case LEFT:
                return 0;
            case CENTER:
                return (width - contentWidth) / 2;
            case RIGHT:
                return width - contentWidth;
            default:
                throw new AssertionError("Unhandled hPos");
        }
    }

    static double computeYOffset(double height, double contentHeight, VPos vpos) {
        switch(vpos) {
            case BASELINE:
            case TOP:
                return 0;
            case CENTER:
                return (height - contentHeight) / 2;
            case BOTTOM:
                return height - contentHeight;
            default:
                throw new AssertionError("Unhandled vPos");
        }
    }

    double getAreaBaselineOffset(List<Node> children,
                                 ArrayList<LayoutRect> positionToWidth,
                                 double areaHeight, boolean fillHeight) {
        return getAreaBaselineOffset(children, positionToWidth, areaHeight, fillHeight, isSnapToPixel());
    }

    static double getAreaBaselineOffset(List<Node> children,
                                        ArrayList<LayoutRect> positionToWidth,
                                        double areaHeight, boolean fillHeight, boolean snapToPixel) {
        return getAreaBaselineOffset(children, positionToWidth, areaHeight, fillHeight,
            getMinBaselineComplement(children), snapToPixel);
    }

    /**
     * Returns the baseline offset of provided children, with respect to the minimum complement, computed
     * by {@link #getMinBaselineComplement(java.util.List)} from the same set of children.
     * @param children the children with baseline alignment
     * @param margins their margins (callback)
     * @param positionToWidth callback for children widths (can return -1 if no bias is used)
     * @param areaHeight height of the area to layout in
     * @param fillHeight callback to specify children that has fillHeight constraint
     * @param minComplement minimum complement
     */
    static double getAreaBaselineOffset(List<Node> children,
                                        ArrayList<LayoutRect> positionToWidth,
                                        double areaHeight, boolean fillHeight, double minComplement, boolean snapToPixel) {
        double b = 0;
        for (int i = 0;i < children.size(); ++i) {
            Node n = children.get(i);
            Insets margin = getMargin(n);
            double top = margin != null? snapSpace(margin.getTop(), snapToPixel) : 0;
            double bottom = (margin != null? snapSpace(margin.getBottom(), snapToPixel) : 0);
            final double bo = n.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                double alt = -1;
                if (n.getContentBias() == Orientation.HORIZONTAL) {
                    alt = positionToWidth.get(i).width;
                }
                if (fillHeight) {
                    // If the children fills it's height, than it's "preferred" height is the area without the complement and insets
                    b = Math.max(b, top + boundedSize(n.minHeight(alt), areaHeight - minComplement - top - bottom,
                        n.maxHeight(alt)));
                } else {
                    // Otherwise, we must use the area without complement and insets as a maximum for the Node
                    b = Math.max(b, top + boundedSize(n.minHeight(alt), n.prefHeight(alt),
                        Math.min(n.maxHeight(alt), areaHeight - minComplement - top - bottom)));
                }
            } else {
                b = Math.max(b, top + bo);
            }
        }
        return b;
    }

    /**
     * Return the minimum complement of baseline
     * @param children
     * @return
     */
    static double getMinBaselineComplement(List<Node> children) {
        return getBaselineComplement(children, true, false);
    }

    private static double getBaselineComplement(List<Node> children, boolean min, boolean max) {
        double bc = 0;
        for (Node n : children) {
            final double bo = n.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                continue;
            }
            if (n.isResizable()) {
                bc = Math.max(bc, (min ? n.minHeight(-1) : max ? n.maxHeight(-1) : n.prefHeight(-1)) - bo);
            } else {
                bc = Math.max(bc, n.getLayoutBounds().getHeight() - bo);
            }
        }
        return bc;
    }

    static double boundedSize(double min, double pref, double max) {
        double a = pref >= min ? pref : min;
        double b = min >= max ? min : max;
        return a <= b ? a : b;
    }

    /**
     * If snapToPixel is true, then the value is rounded using Math.round. Otherwise,
     * the value is simply returned. This method will surely be JIT'd under normal
     * circumstances, however on an interpreter it would be better to inline this
     * method. However the use of Math.round here, and Math.ceil in snapSize is
     * not obvious, and so for code maintenance this logic is pulled out into
     * a separate method.
     *
     * @param value The value that needs to be snapped
     * @param snapToPixel Whether to snap to pixel
     * @return value either as passed in or rounded based on snapToPixel
     */
    private static double snapSpace(double value, boolean snapToPixel) {
        return snapToPixel ? Math.round(value) : value;
    }

    double computeChildPrefAreaWidth(Node child, Insets margin) {
        return computeChildPrefAreaWidth(child, -1, margin, -1, false);
    }

    double computeChildPrefAreaWidth(Node child, double baselineComplement, Insets margin, double height, boolean fillHeight) {
        final boolean snap = isSnapToPixel();
        double left = margin != null? snapSpace(margin.getLeft(), snap) : 0;
        double right = margin != null? snapSpace(margin.getRight(), snap) : 0;
        double alt = -1;
        if (height != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            double top = margin != null? snapSpace(margin.getTop(), snap) : 0;
            double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                height - top - bottom - baselineComplement :
                height - top - bottom;
            if (fillHeight) {
                alt = snapSize(boundedSize(
                    child.minHeight(-1), contentHeight,
                    child.maxHeight(-1)));
            } else {
                alt = snapSize(boundedSize(
                    child.minHeight(-1),
                    child.prefHeight(-1),
                    Math.min(child.maxHeight(-1), contentHeight)));
            }
        }
        return left + snapSize(boundedSize(child.minWidth(alt), child.prefWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildPrefAreaHeight(Node child, Insets margin) {
        return computeChildPrefAreaHeight(child, -1, margin, -1);
    }

    double computeChildPrefAreaHeight(Node child, double prefBaselineComplement, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top = margin != null? snapSpace(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;

        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null ? snapSpace(margin.getLeft(), snap) : 0;
            double right = margin != null ? snapSpace(margin.getRight(), snap) : 0;
            alt = snapSize(boundedSize(
                child.minWidth(-1), width != -1 ? width - left - right
                    : child.prefWidth(-1), child.maxWidth(-1)));
        }

        if (prefBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                // When baseline is same as height, the preferred height of the node will be above the baseline, so we need to add
                // the preferred complement to it
                return top + snapSize(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom
                    + prefBaselineComplement;
            } else {
                // For all other Nodes, it's just their baseline and the complement.
                // Note that the complement already contain the Node's preferred (or fixed) height
                return top + baseline + prefBaselineComplement + bottom;
            }
        } else {
            return top + snapSize(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom;
        }
    }

    double computeMaxPrefAreaHeight(List<Node>children, VPos valignment) {
        return getMaxAreaHeight(children, null, valignment, false);
    }

    /* utility method for computing the max of children's min or pref heights, taking into account baseline alignment */
    private double getMaxAreaHeight(List<Node> children, double childWidths[], VPos valignment, boolean minimum) {
        final double singleChildWidth = childWidths == null ? -1 : childWidths.length == 1 ? childWidths[0] : Double.NaN;
        if (valignment == VPos.BASELINE) {
            double maxAbove = 0;
            double maxBelow = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Node child = children.get(i);
                final double childWidth = Double.isNaN(singleChildWidth) ? childWidths[i] : singleChildWidth;
                Insets margin = getMargin(child);
                final double top = margin != null? snapSpace(margin.getTop()) : 0;
                final double bottom = margin != null? snapSpace(margin.getBottom()) : 0;
                final double baseline = child.getBaselineOffset();

                final double childHeight = minimum? snapSize(child.minHeight(childWidth)) : snapSize(child.prefHeight(childWidth));
                if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                    maxAbove = Math.max(maxAbove, childHeight + top);
                } else {
                    maxAbove = Math.max(maxAbove, baseline + top);
                    maxBelow = Math.max(maxBelow,
                        snapSpace(minimum?snapSize(child.minHeight(childWidth)) : snapSize(child.prefHeight(childWidth))) -
                            baseline + bottom);
                }
            }
            return maxAbove + maxBelow; //remind(aim): ceil this value?
        } else {
            double max = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Node child = children.get(i);
                Insets margin = getMargin(child);
                final double childWidth = Double.isNaN(singleChildWidth) ? childWidths[i] : singleChildWidth;
                max = Math.max(max, minimum?
                    computeChildMinAreaHeight(child, -1, margin, childWidth) :
                    computeChildPrefAreaHeight(child, -1, margin, childWidth));
            }
            return max;
        }
    }

    double computeChildMinAreaHeight(Node child, double minBaselineComplement, Insets margin, double width) {
        final boolean snap = isSnapToPixel();
        double top =margin != null? snapSpace(margin.getTop(), snap) : 0;
        double bottom = margin != null? snapSpace(margin.getBottom(), snap) : 0;

        double alt = -1;
        if (child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double left = margin != null? snapSpace(margin.getLeft(), snap) : 0;
            double right = margin != null? snapSpace(margin.getRight(), snap) : 0;
            alt = snapSize(width != -1? boundedSize(child.minWidth(-1), width - left - right, child.maxWidth(-1)) :
                child.maxWidth(-1));
        }

        // For explanation, see computeChildPrefAreaHeight
        if (minBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                return top + snapSize(child.minHeight(alt)) + bottom
                    + minBaselineComplement;
            } else {
                return baseline + minBaselineComplement;
            }
        } else {
            return top + snapSize(child.minHeight(alt)) + bottom;
        }
    }

    public static enum FlowAlignment { LEFT, RIGHT }
    private static final String ALIGNMENT = "hangingflowpane-alignment";

    /**
     * Sets the alignment for the child when contained by a border pane.
     * If set, will override the border pane's default alignment for the child's position.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a border pane
     * @param value the alignment position for the child
     */
    public static void setAlignment(Node child, FlowAlignment value) {
        setConstraint(child, ALIGNMENT, value);
    }

    private static FlowAlignment getAlignment(Node child) {
        FlowAlignment a = (FlowAlignment)getConstraint(child, ALIGNMENT);
        if (a != null)
            return a;
        else
            return FlowAlignment.LEFT;
    }
    
    private static final String BREAK_BEFORE = "hangingflowpane-breakbefore";
    
    public static void setBreakBefore(Node child, Boolean canBreakBefore)
    {
        setConstraint(child, BREAK_BEFORE, canBreakBefore);
    }
    
    private static boolean canBreakBefore(Node child)
    {
        Boolean b = (Boolean)getConstraint(child, BREAK_BEFORE);
        if (b == null)
            return true; // Default is true
        else
            return b;
    }

    // From Pane:
    private static void setConstraint(Node node, Object key, Object value) {
        if (value == null) {
            node.getProperties().remove(key);
        } else {
            node.getProperties().put(key, value);
        }
        if (node.getParent() != null) {
            node.getParent().requestLayout();
        }
    }

    private static Object getConstraint(Node node, Object key) {
        if (node.hasProperties()) {
            return node.getProperties().get(key);
        }
        return null;
    }
}
