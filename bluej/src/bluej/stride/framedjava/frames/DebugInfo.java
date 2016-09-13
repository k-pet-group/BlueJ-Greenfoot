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
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.generic.FrameCursor;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

public class DebugInfo
{
    private final IdentityHashMap<FrameCursor, Display> displays = new IdentityHashMap<>();
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Map<String,DebugVarInfo> prevState, state;
    private final SimpleBooleanProperty showVars = new SimpleBooleanProperty(false);
    
    @OnThread(Tag.Any)
    public DebugInfo()
    {
        
    }
    
    @OnThread(Tag.Any)
    public synchronized void addVarState(Map<String,DebugVarInfo> state)
    {
        this.prevState = this.state;
        this.state = state;
    }
    
    @OnThread(Tag.FXPlatform)
    public synchronized Display getInfoDisplay(FrameCursor f, Node justExecuted, boolean isBeforeBreakpointFrame)
    {
        if (displays.containsKey(f))
        {
            displays.get(f).addState(prevState, state);
            return displays.get(f);
        }
        else
        {
            Display d = new Display(prevState, state, justExecuted, isBeforeBreakpointFrame);
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
        private final ArrayList<VBox> varDisplay = new ArrayList<>();
        private final Node node;
        private int curDisplay = -1;
        private Label curCounter;
        private final boolean isBreakpointFrame;

        @OnThread(Tag.FXPlatform)
        public Display(Map<String, DebugVarInfo> prevVars, Map<String, DebugVarInfo> vars, Node node, boolean isBreakpointFrame)
        {
            this.node = node;
            this.isBreakpointFrame = isBreakpointFrame;
            HBox controls = new HBox();
            curCounter = new Label("1/1");
            //controls.getChildren().addAll(new Label("<"), curCounter, new Label(">"));
            AnchorPane.setTopAnchor(controls, 5.0);
            AnchorPane.setRightAnchor(controls, 5.0);
            getChildren().add(controls);
            
            addState(prevVars, vars);
        }

        @OnThread(Tag.FXPlatform)
        private VBox makeDisplay(Map<String, DebugVarInfo> prevVars,
                Map<String, DebugVarInfo> vars) {
            VBox disp = new VBox();
            for (Map.Entry<String, DebugVarInfo> var : vars.entrySet())
            {
                DebugVarInfo prev = prevVars == null ? null : prevVars.get(var.getKey());
                HBox row = new HBox();
                Label k = new Label(var.getKey() + ": ");
                k.getStyleClass().add("debug-info-text");
                Node v = var.getValue().getDisplay(prev);
                v.getStyleClass().add("debug-info-text");
                row.getChildren().addAll(k, v);
                disp.getChildren().add(row);
                row.getStyleClass().add("debug-info");
                row.managedProperty().bind(showVars);
                row.visibleProperty().bind(showVars);
            }
            return disp;
        }
        
        @OnThread(Tag.FXPlatform)
        public void addState(Map<String, DebugVarInfo> prevVars, Map<String, DebugVarInfo> vars)
        {
            VBox disp = makeDisplay(prevVars, vars);
            varDisplay.add(disp);
            if (curDisplay >= 0)
            {
                getChildren().remove(varDisplay.get(curDisplay));
            }
            curDisplay = varDisplay.size() - 1;
            curCounter.setText((curDisplay + 1) + "/" + varDisplay.size());
            AnchorPane.setTopAnchor(disp, 2.0);
            AnchorPane.setLeftAnchor(disp, 2.0);
            getChildren().add(disp);
            JavaFXUtil.addStyleClass(this, "debug-info-highlight");
            pulse();
        }

        private void pulse()
        {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), this);
            st.setByX(0.3);
            st.setByY(0.3);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();            
        }

        @Override
        public void removeHighlight()
        {
            JavaFXUtil.removeStyleClass(this, "debug-info-highlight");
            JavaFXUtil.addStyleClass(this, "debug-info");
        }

        @Override
        public Node getNode()
        {
            return node;
        }

        @Override
        public @OnThread(Tag.FXPlatform) double getYOffset()
        {
            return 8;
        }

        @Override
        public @OnThread(Tag.FXPlatform) boolean isBreakpointFrame()
        {
            return isBreakpointFrame;
        }
    }
}