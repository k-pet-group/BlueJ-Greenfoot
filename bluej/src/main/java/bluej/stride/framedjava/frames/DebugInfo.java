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
package bluej.stride.framedjava.frames;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

public class DebugInfo
{
    private final IdentityHashMap<FrameCursor, Display> displays = new IdentityHashMap<>();
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Map<String,DebugVarInfo> prevState, state;
    private final SimpleBooleanProperty showVars = new SimpleBooleanProperty(false);
    private int stateIndex;

    @OnThread(Tag.Any)
    public DebugInfo()
    {
        
    }
    
    @OnThread(Tag.Any)
    public synchronized void addVarState(Map<String,DebugVarInfo> state, int index)
    {
        this.prevState = this.state;
        this.state = state;
        this.stateIndex = index;
    }
    
    @OnThread(Tag.FXPlatform)
    public synchronized Display getInfoDisplay(FrameCursor f, Node frameNode, String stylePrefix, boolean isBeforeBreakpointFrame)
    {
        if (displays.containsKey(f))
        {
            displays.get(f).addState(prevState, state, stateIndex);
            return displays.get(f);
        }
        else
        {
            Display d = new Display(prevState, state, stateIndex, frameNode, stylePrefix, isBeforeBreakpointFrame);
            // Notify any parents:
            Frame frame = f.getFrameAfter();
            while (frame != null)
            {
                frame = Optional.ofNullable(frame.getParentCanvas()).map(FrameCanvas::getParent).map(CanvasParent::getFrame).orElse(null);
                if (frame != null && frame instanceof WhileFrame) // TODO: generalise to other loops
                {
                    FrameCursor cursor = frame.getCursorBefore();
                    if (displays.containsKey(cursor))
                        displays.get(cursor).addChild(d);
                }
            }
            displays.put(f, d);
            return d;
        }
    }
    
    public void removeAllDisplays(List<Node> disps)
    {
        Iterator<Entry<FrameCursor, Display>> it = displays.entrySet().iterator();
        while (it.hasNext())
        {
            if (disps.contains(it.next().getValue()))
            {
                it.remove();
            }
        }
    }

    public void hideAllDisplays()
    {
        displays.forEach((cursor, display) -> {
            cursor.getParentCanvas().getSpecialBefore(cursor).getChildren().remove(display);
        });
        displays.clear();
    }

    public void bindVarVisible(ObservableBooleanValue showVars)
    {
        this.showVars.bind(showVars);
    }

    public class Display extends AnchorPane implements HighlightedBreakpoint
    {
        private final ObservableList<Pane> varDisplay = FXCollections.observableArrayList();
        private final ArrayList<Integer> varIndexes = new ArrayList<>();
        private final Node frameNode;
        private final SimpleIntegerProperty curDisplay = new SimpleIntegerProperty(-1);
        private final Label curCounter;
        private final boolean isBreakpointFrame;
        private final ObservableList<Display> children = FXCollections.observableArrayList();
        private final BooleanBinding showControls;
        private Display parent = null;

        @OnThread(Tag.FXPlatform)
        public Display(Map<String, DebugVarInfo> prevVars, Map<String, DebugVarInfo> vars, int varIndex, Node frameNode, String stylePrefix, boolean isBreakpointFrame)
        {
            this.isBreakpointFrame = isBreakpointFrame;
            this.frameNode = frameNode;
            HBox controls = new HBox();
            curCounter = new Label("1/1");
            curCounter.textProperty().bind(curDisplay.add(1).asString().concat("/").concat(Bindings.size(varDisplay).asString()));
            Label leftArrow = new Label("<");
            Label rightArrow = new Label(">");
            JavaFXUtil.addStyleClass(curCounter, "debug-info-number");
            JavaFXUtil.addStyleClass(leftArrow, "debug-info-arrow");
            JavaFXUtil.addStyleClass(rightArrow, "debug-info-arrow");
            leftArrow.setOnMouseClicked(e -> {left(); e.consume();});
            rightArrow.setOnMouseClicked(e -> {right(); e.consume();});
            controls.getChildren().addAll(leftArrow, curCounter, rightArrow);
            AnchorPane.setTopAnchor(controls, 2.0);
            AnchorPane.setRightAnchor(controls, 5.0);
            showControls = Bindings.size(varDisplay).greaterThan(1).and(Bindings.isNotEmpty(children));
            controls.managedProperty().bind(showControls);
            controls.visibleProperty().bind(showControls);
            getChildren().add(controls);
            JavaFXUtil.addStyleClass(this, "debug-info-surround");
            if (stylePrefix != null && !stylePrefix.isEmpty())
                JavaFXUtil.setPseudoclass("bj-" + stylePrefix + "debug", true, this);
            
            curDisplay.addListener((prop, prev, now) -> {
                if (prev.intValue() >= 0 && prev.intValue() < varDisplay.size())
                    getChildren().remove(varDisplay.get(prev.intValue()));
                if (now.intValue() >= 0 && now.intValue() < varDisplay.size())
                    getChildren().add(0, varDisplay.get(now.intValue()));
                updateChildren();
            });
            varDisplay.addListener((ListChangeListener<? super Pane>)c -> {
                // Currently only additions happen, so we just check if 
                // we trying to display one past the end:
                if (parent == null && curDisplay.get() == varDisplay.size() - 1)
                    getChildren().add(0, varDisplay.get(curDisplay.get()));
            });
            
            addState(prevVars, vars, varIndex);
        }

        @OnThread(Tag.FXPlatform)
        private Pane makeDisplay(Map<String, DebugVarInfo> prevVars,
                                 Map<String, DebugVarInfo> vars) {
            GridPane disp = new GridPane();
            disp.setHgap(20);
            JavaFXUtil.addStyleClass(disp, "debug-info-rows");
            int index = 0;
            for (Map.Entry<String, DebugVarInfo> var : vars.entrySet())
            {
                DebugVarInfo prev = prevVars == null ? null : prevVars.get(var.getKey());
                HBox row = new HBox();
                Label k = new Label(var.getKey() + ": ");
                k.getStyleClass().add("debug-info-text");
                Node v = var.getValue().getDisplay(prev);
                v.getStyleClass().add("debug-info-text");
                row.getChildren().addAll(k, v);
                disp.add(row, index % 3, index / 3);
                row.getStyleClass().add("debug-info");
                row.managedProperty().bind(showVars);
                row.visibleProperty().bind(showVars);
                index += 1;
            }
            return disp;
        }
        
        @OnThread(Tag.FXPlatform)
        public void addState(Map<String, DebugVarInfo> prevVars, Map<String, DebugVarInfo> vars, int varIndex)
        {
            Pane disp = makeDisplay(prevVars, vars);
            varIndexes.add(varIndex);
            varDisplay.add(disp);
            if (parent == null)
                curDisplay.set(varDisplay.size() - 1);
            AnchorPane.setTopAnchor(disp, 1.0);
            AnchorPane.setLeftAnchor(disp, 1.0);
            AnchorPane.setBottomAnchor(disp, 1.0);
            AnchorPane.setRightAnchor(disp, 1.0);
            JavaFXUtil.setPseudoclass("bj-highlight", false, displays.values().toArray(new Node[0]));
            JavaFXUtil.setPseudoclass("bj-highlight", true, this);
            pulse();
        }
        
        private void left()
        {
            if (curDisplay.get() > 0)
            {
                curDisplay.set(curDisplay.get() - 1);
            }
        }
        
        private void right()
        {
            if (curDisplay.get() < varDisplay.size() - 1)
            {
                curDisplay.set(curDisplay.get() + 1);
            }
        }

        private void pulse()
        {
            /*
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setByX(0.3);
            st.setByY(0.3);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();            
            */
        }

        @Override
        public void removeHighlight()
        {
            //JavaFXUtil.removeStyleClass(this, "debug-info-highlight");
            //JavaFXUtil.addStyleClass(this, "debug-info");
        }

        @Override
        public Node getNode()
        {
            return this;
        }

        @Override
        public @OnThread(Tag.FXPlatform) double getYOffset()
        {
            return 5;
        }

        @Override
        public @OnThread(Tag.FXPlatform) double getYOffsetOfTurnBack()
        {
            return frameNode.localToScene(frameNode.getBoundsInLocal()).getMaxY() - 5 - localToScene(getBoundsInLocal()).getMinY();
        }

        @Override
        public @OnThread(Tag.FXPlatform) boolean isBreakpointFrame()
        {
            return isBreakpointFrame;
        }

        public void addChild(Display child)
        {
            if (!children.contains(child))
            {
                children.add(child);
                child.parent = this;
                child.varDisplay.addListener((ListChangeListener<? super Pane>)c -> updateChildren());
            }
        }
        
        private void updateChildren()
        {
            for (Display child : children)
            {
                int lowerBound = curDisplay.get() >= 0 ? (isLatest() && curDisplay.get() >= 1 ? varIndexes.get(curDisplay.get() - 1) : varIndexes.get(curDisplay.get())) : -1;
                int upperBound = curDisplay.get() + 1 < varIndexes.size() ? varIndexes.get(curDisplay.get() + 1) : Integer.MAX_VALUE;

                // Find the child iteration that is within the bounds, if any:
                child.curDisplay.set(child.varIndexes.indexOf(child.varIndexes.stream().filter(varIndex -> varIndex >= lowerBound && varIndex <= upperBound).findFirst().orElse(-1)));
            }
        }

        @Override
        public @OnThread(Tag.FXPlatform) boolean showExec(int index)
        {
            if (curDisplay.get() < 0)
                return false; // Not reached here yet
            if (curDisplay.get() < varDisplay.size())
            {
                // Trying to show an arrow arriving at the first display and we are a parent:
                if (!varIndexes.isEmpty() && varIndexes.get(0) == index && !children.isEmpty())
                    return true;
                // Trying to show an arrow arriving at the current display and we are not parent or we are parent but are latest:
                if (varIndexes.get(curDisplay.get()) == index && (children.isEmpty() || isLatest()))
                    return true;
                // Trying to show an arrow arriving at the next display and we are a parent:
                if (curDisplay.get() + 1 < varIndexes.size() && varIndexes.get(curDisplay.get() + 1) == index && !children.isEmpty())
                    return true;
            }
            return false;
            /*
            
            return curDisplay.get() >= 0 && 
                ((curDisplay.get() < varDisplay.size() && 
                    (varIndexes.get(curDisplay.get()) == index || 
                    (!children.isEmpty() && curDisplay.get() > 0 && varIndexes.get(curDisplay.get() - 1) == index)))
                || (parent != null && parent.curDisplay.get() > 0 && parent.curDisplay.get() < parent.varDisplay.size() && parent.varIndexes.get(parent.curDisplay.get() - 1) == index - 1));
                */
        }

        /**
         * Is the latest index on this later than all children?
         * @return
         */
        private boolean isLatest()
        {
            return varIndexes.get(curDisplay.get()) >= children.stream().mapToInt(c -> c.varIndexes.isEmpty() ? -1 : c.varIndexes.get(c.varIndexes.size() - 1)).max().orElse(-1);
        }
    }
}