/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

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

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.StringExpression;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * A graphical item that contains a margin (used for line numbers and/or breakpoint symbols, step marks, etc)
 * and a text line.
 */
@OnThread(Tag.FX)
public class MarginAndTextLine extends Region
{
    public static final int TEXT_LEFT_EDGE = 27;
    public static final double LINE_X = 24.5;
    public static final double MARGIN_BACKGROUND_WIDTH = 24;
    public static final double MARGIN_RIGHT = 23;
    private final Line dividerLine;
    private final int lineNumberToDisplay;
    private boolean hoveringMargin = false;

    // Does not include the hover icon, which is added dynamically:
    private final EnumSet<MarginDisplay> displayItems = EnumSet.noneOf(MarginDisplay.class);
    private final Tooltip breakpointHoverTooltip;
    private final Region backgroundNode;

    @OnThread(Tag.Any)
    public static enum MarginDisplay
    {
        // Important that step mark is after breakpoint, so that it appears in front:
        UNCOMPILED("bj-margin-uncompiled"), ERROR("bj-margin-error"), LINE_NUMBER, BREAKPOINT_HOVER, BREAKPOINT, STEP_MARK;

        public final String pseudoClass; // May be null

        MarginDisplay(String pseudoClass)
        {
            this.pseudoClass = pseudoClass;
        }
        
        MarginDisplay()
        {
            this(null);
        }
    }
    
    private final EnumMap<MarginDisplay, Node> cachedIcons = new EnumMap<MarginDisplay, Node>(MarginDisplay.class);
    final TextLine textLine;

    public MarginAndTextLine(int lineNumberToDisplay, TextLine textLine, FXPlatformSupplier<Boolean> onClick, FXPlatformSupplier<ContextMenu> getContextMenuToShow)
    {
        this.dividerLine = new Line(LINE_X, 0.5, LINE_X, 1);
        dividerLine.getStyleClass().add("flow-margin-line");
        this.lineNumberToDisplay = lineNumberToDisplay;
        this.backgroundNode = new Region();
        backgroundNode.getStyleClass().add("flow-margin-background");
        this.textLine = textLine;
        getChildren().setAll(backgroundNode, textLine, dividerLine);
        getStyleClass().add("margin-and-text-line");
        String breakpointHoverUsualText = Config.getString("editor.set.breakpoint.hint");
        String breakpointHoverFailText = Config.getString("editor.set.breakpoint.fail");
        breakpointHoverTooltip = new Tooltip(breakpointHoverUsualText);
        breakpointHoverTooltip.setShowDelay(Duration.seconds(1));
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getX() < LINE_X)
            {
                if (e.getButton() == MouseButton.PRIMARY && !e.isShiftDown())
                {
                    if (!onClick.get())
                    {
                        breakpointHoverTooltip.setText(breakpointHoverFailText);
                        breakpointHoverTooltip.setShowDelay(Duration.ZERO);
                    }
                    else
                    {
                        breakpointHoverTooltip.setText(breakpointHoverUsualText);
                        breakpointHoverTooltip.setShowDelay(Duration.seconds(1));
                    }
                    e.consume();
                }
            }
        });
        addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            hoveringMargin = e.getX() < LINE_X;
            setMarginGraphics(EnumSet.copyOf(displayItems));
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            hoveringMargin = false;
            breakpointHoverTooltip.setText(breakpointHoverUsualText);
            breakpointHoverTooltip.setShowDelay(Duration.seconds(1));
            setMarginGraphics(EnumSet.copyOf(displayItems));
        });

        // Context menu to show or hide line numbers
        ContextMenu contextMenu = new ContextMenu();
        // If they right-click on us, we show new-class and import-class actions:
        contextMenu.getItems().add(
            JavaFXUtil.makeMenuItem(
                Config.getString("prefmgr.edit.displaylinenumbers"),
                () -> {PrefMgr.setFlag(PrefMgr.LINENUMBERS, !PrefMgr.getFlag(PrefMgr.LINENUMBERS)); },
                null
            )
        );

        // Right-clicks/control-clicks in the left margin show this menu:
        backgroundNode.setOnContextMenuRequested(e -> {
            if (contextMenu.isShowing())
            {
                contextMenu.hide();
            }
            contextMenu.show(backgroundNode, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        // Right-clicks/control-clicks anywhere else in the line show this menu:
        this.setOnContextMenuRequested(e -> {
            getContextMenuToShow.get().show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void layoutChildren()
    {
        for (Node child : getChildren())
        {
            if (child == textLine)
            {
                textLine.resizeRelocate(TEXT_LEFT_EDGE, 0, getWidth() - TEXT_LEFT_EDGE, getHeight());
            }
            else if (child == dividerLine)
            {
                dividerLine.setEndY(getHeight() - 0.5);
            }
            else if (child == backgroundNode)
            {
                backgroundNode.resizeRelocate(0, 0, MARGIN_BACKGROUND_WIDTH, getHeight());
            }
            else
            {
                double height = child.prefHeight(-1);
                double width = child.prefWidth(-1);
                child.resizeRelocate(MARGIN_RIGHT - width, (getHeight() - height) / 2.0, width, height);
            }
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefWidth(double height)
    {
        return textLine.prefWidth(height) + TEXT_LEFT_EDGE;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computePrefHeight(double width)
    {
        return textLine.prefHeight(width - TEXT_LEFT_EDGE);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMinWidth(double height)
    {
        return textLine.minWidth(height) + TEXT_LEFT_EDGE;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMinHeight(double width)
    {
        return textLine.minHeight(width - TEXT_LEFT_EDGE);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMaxWidth(double height)
    {
        return textLine.maxWidth(height) + TEXT_LEFT_EDGE;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected double computeMaxHeight(double width)
    {
        return textLine.maxHeight(width - TEXT_LEFT_EDGE);
    }

    /**
     * Updates the line after a font change
     * @param fontCSS
     */
    public void fontSizeChanged(StringExpression fontCSS)
    {
        textLine.fontSizeChanged(fontCSS);
    }

    @OnThread(Tag.FX)
    public void setMarginGraphics(EnumSet<MarginDisplay> displayItems)
    {
        this.displayItems.clear();
        this.displayItems.addAll(displayItems);
        EnumSet<MarginDisplay> toAdd = EnumSet.copyOf(displayItems);
        if (hoveringMargin && !toAdd.contains(MarginDisplay.BREAKPOINT))
        {
            toAdd.add(MarginDisplay.BREAKPOINT_HOVER);
            toAdd.remove(MarginDisplay.LINE_NUMBER);
        }
        ArrayList<Node> content = new ArrayList<>();
        content.add(backgroundNode);
        content.add(textLine);
        content.add(dividerLine);

        for (MarginDisplay display : toAdd)
        {
            Node item = cachedIcons.computeIfAbsent(display, d -> {
                switch (d)
                {
                    case LINE_NUMBER:
                        Label label = new Label(Integer.toString(lineNumberToDisplay));
                        label.setEllipsisString("\u2026");
                        label.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
                        JavaFXUtil.addStyleClass(label, "flow-line-label");
                        label.setMouseTransparent(true);
                        return label;
                    case STEP_MARK:
                        return makeStepMarkIcon();
                    case BREAKPOINT:
                        return makeBreakpointIcon();
                    case BREAKPOINT_HOVER:
                        Node icon = makeBreakpointIcon();
                        icon.setOpacity(0.3);
                        Tooltip.install(icon, breakpointHoverTooltip);
                        return icon;
                    case ERROR:
                        return null; // Only sets pseudo-class
                    case UNCOMPILED:
                        return null; // Only sets pseudo-class
                    default: // Shouldn't happen:
                        return null;
                }
            });
            if (item != null)
            {
                content.add(item);
            }
        }
        // Don't set content if it's the same; this would cause listeners to fire
        // and an unnecessary layout pass to occur
        if (!content.equals(getChildren()))
        {
            getChildren().setAll(content);
        }

        for (MarginDisplay marginDisplay : MarginDisplay.values())
        {
            if (marginDisplay.pseudoClass != null)
            {
                JavaFXUtil.setPseudoclass(marginDisplay.pseudoClass, displayItems.contains(marginDisplay), this);
            }
        }
    }


    // Red octagon with white STOP on it.  By doing it as a shape rather than
    // image file, we get it looking good on all HiDPI displays.
    private static Node makeBreakpointIcon()
    {
        Node icon = Config.makeStopIcon(false);
        JavaFXUtil.addStyleClass(icon, "moe-breakpoint-icon");
        icon.setMouseTransparent(true);
        return icon;
    }

    private static Node makeStepMarkIcon()
    {
        Shape arrow = Config.makeArrowShape(false);
        JavaFXUtil.addStyleClass(arrow, "moe-step-mark-icon");
        arrow.setMouseTransparent(true);
        return arrow;
    }
}
