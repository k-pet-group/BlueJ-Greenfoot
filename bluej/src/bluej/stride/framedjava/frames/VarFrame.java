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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
//import javafx.scene.input.KeyCode;

import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.FilledExpressionSlot;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentItem;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.operations.FrameOperation;
import bluej.stride.operations.ToggleBooleanProperty;
import bluej.stride.slots.AccessPermissionSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.ChoiceSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.SlotValueListener;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.stride.slots.TypeTextSlot;
import bluej.stride.slots.VariableNameDefTextSlot;

import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * A variable/object declaration block (with optional init)
 * @author Fraser McKay
 */
public class VarFrame extends SingleLineFrame
  implements CodeFrame<VarElement>, DebuggableFrame
{
    //TODO make them private after refactoring
    public static final String STATIC_NAME = "static";
    public static final String FINAL_NAME = "final";
    public static final String TOGGLE_STATIC_VAR = "toggleStaticVar";
    public static final String TOGGLE_FINAL_VAR = "toggleFinalVar";

    private final BooleanProperty accessModifier = new SimpleBooleanProperty(false);
    private final ChoiceSlot<AccessPermission> access; // present only when it is a class field
    private final SlotLabel staticLabel = new SlotLabel(STATIC_NAME + " ");
    private final BooleanProperty staticModifier = new SimpleBooleanProperty(false);
    private final SlotLabel finalLabel = new SlotLabel(FINAL_NAME + " ");
    public final BooleanProperty finalModifier = new SimpleBooleanProperty(false);
    private final TypeTextSlot slotType;
    private final VariableNameDefTextSlot slotName;
    private final BooleanProperty showingValue = new SimpleBooleanProperty(false);
    private final SlotLabel equalLabel = new SlotLabel("=");
    private final ExpressionSlot<FilledExpressionSlotFragment> slotValue; // not always valid
    private VarElement element;

    /**
     * Default constructor.
     * @param editor 
     */
    VarFrame(final InteractionManager editor, boolean isFinal, boolean isStatic)
    {
        super(editor, "var ", "var-");
        //Parameters

        staticModifier.set(isStatic);
        finalModifier.set(isFinal);
        // If it's final, show initialisation value from outset:
        showingValue.set(isFinal);

        modifiers.put(STATIC_NAME, staticModifier);
        modifiers.put(FINAL_NAME, finalModifier);
        
        headerCaptionLabel.setAnimateCaption(false);

        // Renaming fields is more difficult (could be accesses in other classes)
        // so for now we stick to renaming local vars:
        slotName = new VariableNameDefTextSlot(editor, this, getHeaderRow(), () -> isField(getParentCanvas()), "var-name-");
        
        slotName.addValueListener(new SlotValueListener()
        {

            @Override
            public boolean valueChanged(HeaderItem slot, String oldValue,
                                        String newValue, FocusParent<HeaderItem> parent)
            {
                return true;
            }

            @Override
            public void deletePressedAtEnd(HeaderItem slot)
            {
                deleteAtEnd(getHeaderRow(), slot);
            }

            @Override
            public void backSpacePressedAtStart(HeaderItem slot)
            {
                backspaceAtStart(getHeaderRow(), slot);
            }
        });
        
        slotName.setPromptText("name");
        
        slotType = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor), "var-type-");
        slotType.addValueListener(SlotTraversalChars.IDENTIFIER);
        slotType.setPromptText("type");
        slotType.addValueListener(new SlotTraversalChars() {
            @Override
            public void backSpacePressedAtStart(HeaderItem slot)
            {
                getHeaderRow().backspaceAtStart(slot);
            }
        });

        access = new AccessPermissionSlot(editor, this, getHeaderRow(), "var-access-");
        access.setValue(AccessPermission.PRIVATE);

        slotValue = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "var-value-");
        slotValue.bindTargetType(slotType.textProperty());
        slotValue.setSimplePromptText("value");
        slotValue.onLostFocus(this::checkForEmptySlot);
        Platform.runLater(() -> {if (isFresh()) onNonFresh(this::checkForEmptySlot);});

        JavaFXUtil.addChangeListener(showingValue, showing -> {
            if (!showing) {
                slotValue.setText("");
                slotValue.clearErrorMarkers();
            }
            editor.modifiedFrame(this);
        });

        FXRunnable runAddValSlot = () -> {
                showingValue.set(true);
                // And move focus in:
                getHeaderRow().focusRight(slotName);
        };
        slotName.addValueListener(new SlotTraversalChars(runAddValSlot, SlotTraversalChars.ASSIGN_LHS.getChars()));

        

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(headerCaptionLabel),
                JavaFXUtil.listBool(accessModifier, access),
                JavaFXUtil.listBool(staticModifier, staticLabel),
                JavaFXUtil.listBool(finalModifier, finalLabel),
                FXCollections.observableArrayList(slotType, slotName),
                JavaFXUtil.listBool(showingValue, equalLabel, slotValue),
                FXCollections.observableArrayList(previewSemi)
        ));

        JavaFXUtil.addChangeListener(staticModifier, b -> editor.modifiedFrame(this));
        JavaFXUtil.addChangeListener(finalModifier, b -> editor.modifiedFrame(this));
    }
    
    // If varValue is null, that means the slot is not shown
    // If accessValue is null, that means the slot is not shown
    public VarFrame(InteractionManager editor, AccessPermissionFragment accessValue, boolean staticModifier, boolean finalModifier,
            TypeSlotFragment varType, NameDefSlotFragment varName, FilledExpressionSlotFragment varValue, boolean enabled)
    {
        this(editor, finalModifier, staticModifier);
        accessModifier.set(accessValue != null);
        if (accessValue != null) {
            access.setValue(accessValue.getValue());
        }
        slotType.setText(varType);
        slotName.setText(varName);
        showingValue.set(varValue != null);
        if (varValue != null) {
            slotValue.setText(varValue);
        }
        frameEnabledProperty.set(enabled);
    }

    @Override
    public void regenerateCode()
    {
        element = new VarElement(this, accessModifier.get() ? new AccessPermissionFragment(access.getValue(AccessPermission.EMPTY)) : null,
                staticModifier.get(), finalModifier.get(), slotType.getSlotElement(), slotName.getSlotElement(), 
                showingValue.get() ? slotValue.getSlotElement() : null, frameEnabledProperty.get());
    }
    
    @Override
    public VarElement getCode()
    {
        return element;
    }
    
    public static FrameFactory<VarFrame> getFactory()
    {
        return new FrameFactory<VarFrame>() {
            @Override
            public VarFrame createBlock(InteractionManager editor)
            {
                return new VarFrame(editor, false, false);
            }
                        
            @Override
            public Class<VarFrame> getBlockClass()
            {
                return VarFrame.class;
            }
        };
    }

    public static FrameFactory<VarFrame> getLocalConstantFactory()
    {
        return new FrameFactory<VarFrame>() {
            @Override
            public VarFrame createBlock(InteractionManager editor)
            {
                return new VarFrame(editor, true, false);
            }

            @Override
            public Class<VarFrame> getBlockClass()
            {
                return VarFrame.class;
            }
        };
    }

    public static FrameFactory<VarFrame> getClassConstantFactory()
    {
        return new FrameFactory<VarFrame>() {
            @Override
            public VarFrame createBlock(InteractionManager editor)
            {
                return new VarFrame(editor, true, true);
            }

            @Override
            public Class<VarFrame> getBlockClass()
            {
                return VarFrame.class;
            }
        };
    }
    
    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        if (parentCanvas == null) {
            // When deleting the frame or remove old copy due to drag.
            return;
        }
        
        if (isField(parentCanvas)) {
            // TODO maybe we have to change this to:
            // fields in classes have different behavior from ones in interfaces.
            accessModifier.set(true);
            addStyleClass("instance-var-frame");
            headerCaptionLabel.setText("");
        }
        else {
            staticModifier.set(false);
            accessModifier.set(false);
            removeStyleClass("instance-var-frame");
            // We must use transparency, not adding/removing, to maintain the same indentation in each frame
            headerCaptionLabel.setText("var ");
            JavaFXUtil.setPseudoclass("bj-transparent", isAfterVarFrame(parentCanvas), headerCaptionLabel.getNode());
        }
    }

    private boolean isAfterVarFrame(FrameCanvas parentCanvas)
    {
        Frame frameBefore = parentCanvas.getFrameBefore(getCursorBefore());
        int counter = 0;
        while ( frameBefore != null && !frameBefore.isEffectiveFrame() && counter < 2) {
            counter++;
            frameBefore = parentCanvas.getFrameBefore(frameBefore.getCursorBefore());
        }
        return frameBefore instanceof VarFrame;
    }
    
    public boolean isField(FrameCanvas parentCanvas)
    {
        if (parentCanvas == null) {
            bluej.utility.Debug.printCallStack("parentCanvas shouldn't be null");
            return false;
        }
        return parentCanvas.getParent().getChildKind(parentCanvas) == CanvasParent.CanvasKind.FIELDS;
    }
    
    @Override
    public List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
        return GreenfootFrameUtil.cutCopyPasteOperations(editor);
    }
    
    @Override
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return ((JavaCanvas)getParentCanvas()).showDebugBefore(this, debug);        
    }

    @Override
    public void checkForEmptySlot()
    {
        showingValue.set((isFresh() && finalModifier.get()) || !slotValue.isEmpty() || slotValue.isShowingSuggestions());
    }

    @Override
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> r = new ArrayList<>(super.getContextOperations());
        // is in class?
        if (isField(getParentCanvas())) {
            r.add(new ToggleBooleanProperty(getEditor(), TOGGLE_STATIC_VAR, STATIC_NAME));
        }
        r.add(new ToggleBooleanProperty(getEditor(), TOGGLE_FINAL_VAR, FINAL_NAME));
        return r;
    }

    @Override
    public List<String> getDeclaredVariablesAfter()
    {
        String name = slotName.getText();
        if (name.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(name);
    }

    @Override
    public boolean backspaceAtStart(FrameContentItem row, HeaderItem src)
    {
        if (src == slotValue) {
            showingValue.set(false);
            slotName.requestFocus(Focus.RIGHT);
            return true;
        }
        else
            return super.backspaceAtStart(row, src);
    }

    @Override
    public boolean deleteAtEnd(FrameContentItem row, HeaderItem src)
    {
        if (src == slotName)
        {
            showingValue.set(false);
            return false; // No need to move focus
        }
        return false;
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return slotName;
    }

    @Override
    public void focusName() {
        slotName.requestFocus(Focus.LEFT);
    }

    @Override
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setViewNoOverride(oldView, newView, animateProgress);
        if (newView == View.JAVA_PREVIEW)
            headerCaptionLabel.shrinkHorizontally(animateProgress);
        else
            headerCaptionLabel.growHorizontally(animateProgress);
    }

    @Override
    public boolean tryRestoreTo(CodeElement codeElement)
    {
        // instanceof bit hacky, but easiest way to do it:
        if (codeElement instanceof VarElement)
        {
            VarElement nme = (VarElement)codeElement;
            staticModifier.set(nme.isStatic());
            finalModifier.set(nme.isFinal());
            slotType.setText(nme.getType());
            slotName.setText(nme.getName());
            if (nme.getValue() != null) {
                slotValue.setText(nme.getValue());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean focusWhenJustAdded()
    {
        slotType.requestFocus();
        return true;
    }

    public List<ExtensionDescription> getAvailablePrefixes()
    {
        List<ExtensionDescription> prefixes = new ArrayList<>(super.getAvailablePrefixes());
        addStaticFinalToList(prefixes);
        return prefixes;
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions()
    {
        final List<ExtensionDescription> extensions = new ArrayList<>(super.getAvailableExtensions());
        addStaticFinalToList(extensions);
        return extensions;
    }

    @Override
    public List<ExtensionDescription> getAvailableSelectionModifiers()
    {
        final List<ExtensionDescription> modifiers = new ArrayList<>(super.getAvailableSelectionModifiers());
        addStaticFinalToList(modifiers);
        return modifiers;
    }

    private void addStaticFinalToList(List<ExtensionDescription> actions)
    {
        actions.add(new ExtensionDescription('n', "Add/Remove final", () ->
                //, KeyCode.N
                new ToggleBooleanProperty(getEditor(), TOGGLE_FINAL_VAR, FINAL_NAME).activate(this), false, true));
        if (isField(getParentCanvas())) {
            actions.add(new ExtensionDescription('s', "Add/Remove static", () ->
                    // , KeyCode.S
                    new ToggleBooleanProperty(getEditor(), TOGGLE_STATIC_VAR, STATIC_NAME).activate(this), false, true));
        }
    }
}
