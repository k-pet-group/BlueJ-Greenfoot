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
package bluej.stride.framedjava.frames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleKnownMethodLink;
import bluej.stride.framedjava.slots.UnderlineContainer;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.WrappableSlotLabel;
import bluej.utility.javafx.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import bluej.parser.AssistContent.ParamInfo;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.slots.TextOverlayPosition;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.utility.javafx.AbstractOperation.Combine;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A frame to show the details of an inherited method in the inherited canvas
 */
public class InheritedMethodFrame extends SingleLineFrame
{
    private final InteractionManager editor;
    // The class frame which our inherited canvas is in:
    private final ClassFrame container;
    // The class which this method is originally declared in (e.g. "java.lang.Object" for "toString")
    private final String originatingClass;
    private final AccessPermission access;
    private final String returnType;
    private final String methodName;
    private final List<ParamInfo> params;
    private final InheritedMethodSlot slot;
    private final WrappableSlotLabel overrideLabel = new WrappableSlotLabel("");
    private NormalMethodFrame override;

    public InheritedMethodFrame(InteractionManager editor, ClassFrame container, String originatingClass, AccessPermission access,
            String returnType, String methodName, List<ParamInfo> params)
    {
        super(editor, "", "inherited-method-");
        
        String preName = access + " \t" + returnType + " ";
        String postName = "(" +
                params.stream().map(p -> p.getUnqualifiedType() + (p.getFormalName() == null ? "" : " " + p.getFormalName())).
                collect(Collectors.joining(", ")) + ")";
        
        this.editor = editor;
        this.container = container;
        this.originatingClass = originatingClass;
        this.access = access;
        this.returnType = returnType;
        this.methodName = methodName;
        this.params = params;
        
        slot = new InheritedMethodSlot(preName, methodName, postName);

        overrideLabel.addStyleClass("inherited-method-override-label");
        overrideLabel.setAlignment(HangingFlowPane.FlowAlignment.RIGHT);

        checkForOverride();

        setHeaderRow(slot, overrideLabel);
    }

    private void checkForOverride()
    {
        container.findMethod(methodName, params, override -> {
            this.override = override;
            if (override != null)
            {
                overrideLabel.setText("overridden in this class");
            }
        });
    }

    @Override
    public boolean canDrag()
    {
        // We cannot be dragged:
        return false;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> operations = new ArrayList<>();
        
        operations.add(new FrameOperation(getEditor(), "GO TO " + originatingClass + "." + methodName, Combine.ONE) {
            {
                this.enabled = false;
            }

            // Spaces make sure menu is wide enough:
            private StringProperty text = new SimpleStringProperty("Scanning...                   ");
            
            @Override
            public List<ItemLabel> getLabels()
            {
                return Collections.singletonList(new ItemLabel(text, MenuItemOrder.GOTO_DEFINITION));
            }

            @Override
            protected void execute(List<Frame> frames) {}

            @Override
            @OnThread(Tag.FXPlatform)
            public void onMenuShowing(CustomMenuItem item)
            {
                InheritedMethodFrame.this.editor.searchLink(new PossibleKnownMethodLink(originatingClass, methodName, Utility.mapList(params, pi -> pi.getQualifiedType()), 0, 1, slot),
                    optLink -> {
                        if (optLink.isPresent())
                        {
                            text.set("Show original");
                            item.setDisable(false);
                            item.onActionProperty().set(e -> optLink.get().getOnClick().run());
                        }
                    });
            }

            @Override
            public void onMenuHidden(CustomMenuItem item)
            {
                item.setDisable(true);
                text.set("Scanning...");
            }

            @Override
            public boolean onlyOnContextMenu()
            {
                return true;
            }
        });
        

        if (override == null)
        {
            operations.add(new CustomFrameOperation(editor, "OVERRIDE", Arrays.asList("Override"), AbstractOperation.MenuItemOrder.OVERRIDE, this, () ->
            {
                NormalMethodFrame methodFrame = new NormalMethodFrame(editor, new AccessPermissionFragment(access), false, false, returnType, methodName, "", true);
                params.forEach(p -> methodFrame.paramsPane.addFormal(new TypeSlotFragment(p.getUnqualifiedType(), p.getUnqualifiedType()), new NameDefSlotFragment((p.getFormalName() == null ? "" : p.getFormalName()))));
                container.getLastInternalCursor().insertBlockAfter(methodFrame);
                methodFrame.getFirstInternalCursor().requestFocus();
                methodFrame.markFresh();
            }));
        }
        else
        {
            operations.add(new CustomFrameOperation(editor, "GO TO " + methodName, Arrays.asList("Show override"), AbstractOperation.MenuItemOrder.GOTO_OVERRIDE, this, () -> {
                override.focusName();
            }));
        }

        return operations;
    }

    // This is a slot so that it can have a hyperlink:
    private class InheritedMethodSlot implements HeaderItem, ErrorUnderlineCanvas.UnderlineInfo, UnderlineContainer
    {
        private final ObservableList<Label> labels = FXCollections.observableArrayList();
        private final int methodNameIndex;
        
        public InheritedMethodSlot(String preName, String methodName, String postName)
        {
            Arrays.stream(preName.split("\\s+")).forEach(s -> labels.add(new Label(s + " ")));
            methodNameIndex = labels.size();
            final Label methodNameLabel = new Label(methodName);
            JavaFXUtil.addStyleClass(methodNameLabel, "inherited-method-name");
            labels.add(methodNameLabel);
            Arrays.stream(postName.split("\\s+")).forEach(s -> labels.add(new Label(s + " ")));
            for (Label label : labels)
            {
                JavaFXUtil.addStyleClass(label, "inherited-method-slot");
            
                label.setOnMouseMoved(e -> {
                    JavaFXUtil.setPseudoclass("bj-hyperlink", getHeaderRow().getOverlay().linkFromX(e.getSceneX()) != null, label);
                });
                label.setOnMouseClicked(e -> {
                    // check for click on underlined region
                    Utility.ifNotNull(getHeaderRow().getOverlay().linkFromX(e.getSceneX()), FXPlatformRunnable::run);
                });
            } 
        }

        @Override
        public TextOverlayPosition getOverlayLocation(int caretPos,
                                                      boolean javaPos)
        {
            Label label = labels.get(methodNameIndex);
            return TextOverlayPosition.nodeToOverlay(label, JavaFXUtil.measureString(label, label.getText().substring(0, caretPos)),
                    0, label.getBaselineOffset(), label.getHeight());
        }

        @Override
        public ObservableList<? extends Node> getComponents()
        {
            return labels;
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void addUnderline(EditableSlot.Underline u)
        {
            getHeaderRow().getOverlay().addUnderline(this, 0, labels.get(methodNameIndex).getText().length(), u.getOnClick());
        }

        @Override
        @OnThread(Tag.FXPlatform)
        public void removeAllUnderlines()
        {
            getHeaderRow().getOverlay().clearUnderlines();
        }

        @Override
        public List<? extends PossibleLink> findLinks()
        {
            return Collections.singletonList(new PossibleKnownMethodLink(originatingClass, methodName, Utility.mapList(params, ParamInfo::getQualifiedType), 0, 1, this));
        }

        @Override
        public EditableSlot asEditable()
        {
            return null;
        }

        @Override
        public void setView(Frame.View oldView, Frame.View newView, SharedTransition animate)
        {
        }
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void compiled()
    {
        super.compiled();
        checkForOverride();
    }
}
