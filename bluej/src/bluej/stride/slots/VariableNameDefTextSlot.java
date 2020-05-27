/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import bluej.utility.javafx.AbstractOperation;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;

import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.editor.stride.CodeOverlayPane;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.FrameHelper;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.StructuredSlot.PlainVarReference;
import bluej.stride.generic.Frame;
import bluej.stride.generic.SuggestedFollowUpDisplay;
import bluej.utility.Utility;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;

public class VariableNameDefTextSlot extends TextSlot<NameDefSlotFragment>
{
    private FXSupplier<Boolean> shouldRename;
    private Canvas allUsesCanvas;
    private Button hideAllUsesButton;

    // We don't differentiate start and part, as that just gets too fiddly during editing:
    private SlotValueListener spaceCharactersListener = (slot, oldValue, newValue, parent) -> newValue.chars().noneMatch(Character::isSpaceChar);

    public <T extends Frame & CodeFrame<? extends CodeElement>>
    VariableNameDefTextSlot(InteractionManager editor, T frameParent, FrameContentRow row, String stylePrefix)
    {
        super(editor, frameParent, frameParent, row, null, stylePrefix, Collections.emptyList());
        this.shouldRename = () -> true;
        addValueListener(spaceCharactersListener);
    }
    
    public <T extends Frame & CodeFrame<? extends CodeElement>>
    VariableNameDefTextSlot(InteractionManager editor, T frameParent, FrameContentRow row, FXSupplier<Boolean> shouldRename, String stylePrefix)
    {
        super(editor, frameParent, frameParent, row, null, stylePrefix, Collections.emptyList());
        this.shouldRename = shouldRename;
        addValueListener(spaceCharactersListener);
    }
    
    public VariableNameDefTextSlot(InteractionManager editor, Frame frameParent,
            CodeFrame<? extends CodeElement> codeFrameParent, FrameContentRow row, String stylePrefix)
    {
        super(editor, frameParent, codeFrameParent, row, null, stylePrefix, Collections.emptyList());
        this.shouldRename = () -> true;
        addValueListener(spaceCharactersListener);
    }

    @Override
    public NameDefSlotFragment createFragment(String content)
    {
        return new NameDefSlotFragment(content, this);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void valueChangedLostFocus(String oldVal, String newVal)
    {
        if (!oldVal.equals(newVal) && !oldVal.isEmpty() && !newVal.isEmpty() && !shouldRename.get())
        {
            // Since we will be cancelled if any further modification takes place, it is ok
            // to calculate the actions once and cache them.  This way, we avoid doing the calculation
            // twice (once to decide if there is anything to do, once to actually do it).
            
            VariableRefFinder renamer = findVarReferences(oldVal);
            
            if (!renamer.refs.isEmpty())
            {
                SuggestedFollowUpDisplay disp = new SuggestedFollowUpDisplay(editor, "Do you want to rename all uses of old variable \"" + oldVal + "\" to use \"" + newVal + "\" instead?", () -> renamer.refs.forEach(r -> r.rename.accept(newVal)));
                disp.showBefore(getNode());
            }
        }
        
    }
    
    private class VariableRefFinder implements BiConsumer<Map<String, List<Frame>>, Frame>
    {
        private final String name;
        private final ArrayList<PlainVarReference> refs = new ArrayList<>();
        
        VariableRefFinder(String name)
        {
            this.name = name; 
        }

        @Override
        @OnThread(value = Tag.FX, ignoreParent = true)
        public void accept(Map<String, List<Frame>> scopes, Frame f)
        {
            for (ExpressionSlot e : Utility.iterableStream(f.getEditableSlotsDirect().map(EditableSlot::asExpressionSlot).filter(x -> x != null)))
            {
                List<Frame> oldScope = scopes.get(name);
                // Only rename if the variable is from our scope, which shows up as no scope here.
                // (Technically, name shadowing for plain vars is invalid, but the user may get
                // into that state, in which case when they rename the outer one, they almost certainly
                // don't want to rename uses of the inner shadowing var)
                if (oldScope == null || oldScope.size() == 0)
                {
                    refs.addAll(e.findPlainVarReferences(name));
                }
            }
        }
        
    }

    private VariableRefFinder findVarReferences(String name)
    {
        VariableRefFinder renamer = new VariableRefFinder(name);
        FrameHelper.processVarScopesAfter(frameParent.getParentCanvas(), frameParent, renamer);
        return renamer;
    }

    @Override
    protected Map<TopLevelMenu, AbstractOperation.MenuItems> getExtraContextMenuItems()
    {
        return Collections.singletonMap(TopLevelMenu.VIEW, new AbstractOperation.MenuItems(FXCollections.observableArrayList()) {
            public void onShowing()
            {
                if (allUsesCanvas != null)
                {
                    items.setAll(AbstractOperation.MenuItemOrder.SHOW_HIDE_USES.item(JavaFXUtil.makeMenuItem("Hide uses of \"" + getText() + "\"", () -> hideUsesOverlay(), null)));
                }
                else
                {
                    VariableRefFinder refFinder = findVarReferences(getText());
                    
                    if (!refFinder.refs.isEmpty())
                    {
                        items.setAll(AbstractOperation.MenuItemOrder.SHOW_HIDE_USES.item(JavaFXUtil.makeMenuItem("See uses of \"" + getText() + "\"", () -> showUsesOverlay(refFinder.refs), null)));
                    }
                    else
                        items.clear();
                }
            }
        });
    }

    @OnThread(Tag.FXPlatform)
    private void showUsesOverlay(List<PlainVarReference> refs)
    {
        hideUsesOverlay();
        CodeOverlayPane overlay = editor.getCodeOverlayPane();
        allUsesCanvas = overlay.addFullSizeCanvas();
        
        GraphicsContext g = allUsesCanvas.getGraphicsContext2D();
        g.setStroke(Color.BLUE);
        g.setLineWidth(2.0);
        
        final double LEFT = 10.0;
        
        List<Bounds> boundsList = Utility.mapList(refs, ref -> ref.refNode.localToScene(ref.refNode.getBoundsInLocal()));
        Bounds ourSceneBounds = getNode().localToScene(getNode().getBoundsInLocal());
        boundsList.add(ourSceneBounds);
        
        // Draw them right to left, so that we overdraw the horizontal connecting lines correctly:
        boundsList.sort((a, b) -> Double.compare(b.getMinX(), a.getMinX()));
        
        List<Double> yCoords = Utility.mapList(boundsList, sceneBounds -> {
            double y = overlay.sceneYToCodeOverlayY(sceneBounds.getMinY());
            g.clearRect(sceneBounds.getMinX(), y, sceneBounds.getWidth(), sceneBounds.getHeight());
            g.strokeRect(sceneBounds.getMinX(), y, sceneBounds.getWidth(), sceneBounds.getHeight());
            double midY = y + (sceneBounds.getHeight() / 2.0);
            g.strokeLine(LEFT, midY, sceneBounds.getMinX(), midY);
            return midY;
        });
        
        // Vertical line in left margin:
        g.strokeLine(LEFT, yCoords.stream().min(Double::compare).get(), LEFT, yCoords.stream().max(Double::compare).get());
        
        Canvas hide = new Canvas(10, 10);
        GraphicsContext g2 = hide.getGraphicsContext2D();
        g2.setStroke(Color.RED);
        g2.strokeLine(1, 1, hide.getWidth() - 2.0, hide.getHeight() - 2.0);
        g2.strokeLine(hide.getWidth() - 2.0, 1, 1, hide.getHeight() - 2.0);
        hideAllUsesButton = new Button("", hide);
        JavaFXUtil.addStyleClass(hideAllUsesButton, "hide-all-uses-button");
        hideAllUsesButton.setOnAction(e -> hideUsesOverlay());
        
        overlay.addOverlay(hideAllUsesButton, getNode(), new ReadOnlyDoubleWrapper(- ourSceneBounds.getMinX() + LEFT - hide.getWidth()/2.0), null);
    }

    @OnThread(Tag.FXPlatform)
    private void hideUsesOverlay()
    {
        if (allUsesCanvas != null)
        {
            CodeOverlayPane overlayPane = editor.getCodeOverlayPane();
            overlayPane.removeOverlay(allUsesCanvas);
            overlayPane.removeOverlay(hideAllUsesButton);
            allUsesCanvas = null;
            hideAllUsesButton = null;
        }
    }


    @Override
    public List<? extends PossibleLink> findLinks()
    {
        // The name is defined here, so there's never any links to go somewhere else
        return Collections.emptyList();
    }
}
