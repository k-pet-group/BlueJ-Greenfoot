/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019 Michael Kölling and John Rosenberg
 
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
import java.util.stream.Stream;

import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.FrameCursor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.VarElement;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.FilledExpressionSlot;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
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
import bluej.stride.slots.VariableNameDefTextSlot;

import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A variable/object declaration block (with optional init)
 * @author Fraser McKay
 */
public class VarFrame extends SingleLineFrame
  implements CodeFrame<VarElement>, DebuggableFrame
{
    private static final String STATIC_NAME = "static";
    private static final String FINAL_NAME = "final";
    private static final String TOGGLE_STATIC_VAR = "toggleStaticVar";
    private static final String TOGGLE_FINAL_VAR = "toggleFinalVar";

    private final BooleanProperty accessModifier = new SimpleBooleanProperty(false);
    private final ChoiceSlot<AccessPermission> access; // present only when it is a class field
    private final SlotLabel staticLabel = new SlotLabel(STATIC_NAME + " ");
    private final BooleanProperty staticModifier = new SimpleBooleanProperty(false);
    private final SlotLabel finalLabel = new SlotLabel(FINAL_NAME + " ");
    private final BooleanProperty finalModifier = new SimpleBooleanProperty(false);
    private final TypeSlot slotType;
    private final VariableNameDefTextSlot slotName;
    private final BooleanProperty showingValue = new SimpleBooleanProperty(false);
    private final SlotLabel assignLabel = new SlotLabel(AssignFrame.ASSIGN_SYMBOL);
    private final ExpressionSlot<FilledExpressionSlotFragment> slotValue; // not always valid
    private final BooleanProperty slotValueBlank = new SimpleBooleanProperty(true);
    private VarElement element;

    private final BooleanProperty hasKeyboardFocus = new SimpleBooleanProperty(false);
    private final BooleanProperty inInterfaceProperty = new SimpleBooleanProperty(false);

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
            @OnThread(Tag.FXPlatform)
            public void deletePressedAtEnd(HeaderItem slot)
            {
                deleteAtEnd(getHeaderRow(), slot);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void backSpacePressedAtStart(HeaderItem slot)
            {
                backspaceAtStart(getHeaderRow(), slot);
            }
        });
        
        slotName.setPromptText("name");
        
        slotType = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.DECLARATION, "var-type-");
        slotType.setSimplePromptText("type");
        slotType.addClosingChar(' ');

        access = new AccessPermissionSlot(editor, this, getHeaderRow(), "var-access-");
        access.setValue(AccessPermission.PRIVATE);

        slotValue = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "var-value-");
        slotValue.bindTargetType(slotType.javaProperty());
        slotValue.setSimplePromptText("value");
        slotValue.onLostFocus(this::checkForEmptySlot);
        //onNonFresh(this::checkForEmptySlot);

        JavaFXUtil.addChangeListener(showingValue, showing -> {
            if (!showing) {
                slotValue.cleanup();
            }
        });

        FXRunnable runAddValSlot = () -> {
                // And move focus in:
                getHeaderRow().focusRight(slotName);
        };
        slotName.addValueListener(new SlotTraversalChars(runAddValSlot, SlotTraversalChars.ASSIGN_LHS.getChars()));

        

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(headerCaptionLabel),
                JavaFXUtil.listBool(accessModifier, access),
                JavaFXUtil.listBool(staticModifier, staticLabel),
                JavaFXUtil.listBool(finalModifier, finalLabel),
                FXCollections.observableArrayList(slotType, slotName),
                JavaFXUtil.listBool(showingValue, List.of(assignLabel, slotValue)),
                FXCollections.observableArrayList(previewSemi)
        ));

        JavaFXUtil.addChangeListener(staticModifier, b -> editor.modifiedFrame(this, false));
        JavaFXUtil.addChangeListener(finalModifier, b -> editor.modifiedFrame(this, false));

        hasKeyboardFocus.bind(
                (accessModifier.and(access.effectivelyFocusedProperty()))
                .or(slotType.effectivelyFocusedProperty())
                .or(slotName.effectivelyFocusedProperty())
                .or(slotValue.effectivelyFocusedProperty())
                );

        slotValue.onTextPropertyChange(s -> slotValueBlank.set(s.isEmpty()));
        // We must make the showing immediate when you get keyboard focus, as otherwise there
        // are problems with focusing the slot and then it disappears:
        ReadOnlyBooleanProperty keyFocusDelayed = JavaFXUtil.delay(hasKeyboardFocus, Duration.ZERO, Duration.millis(100));
        showingValue.bind(inInterfaceProperty.or(keyFocusDelayed).or(slotValueBlank.not()));
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
        if (varValue != null) {
            slotValue.setText(varValue);
        }
        frameEnabledProperty.set(enabled);
    }

    @Override
    public void regenerateCode()
    {
        // We generate the return value iff:
        //   - The value is currently visible, AND
        //     - the text is non-empty, OR
        //     - we have triggered code completion in the slot
        final boolean generateValue = showingValue.get() && (!slotValue.getText().isEmpty() || slotValue.isCurrentlyCompleting());
        element = new VarElement(this, accessModifier.get() ? new AccessPermissionFragment(access.getValue(AccessPermission.EMPTY)) : null,
                staticModifier.get(), finalModifier.get(), slotType.getSlotElement(), slotName.getSlotElement(), 
                generateValue ? slotValue.getSlotElement() : null, frameEnabledProperty.get());
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

    public static FrameFactory<VarFrame> getInterfaceConstantFactory()
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
    
    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        if (parentCanvas == null) {
            // When deleting the frame or remove old copy due to drag.
            return;
        }

        inInterfaceProperty.setValue(isInInterface(parentCanvas));
        if (isField(parentCanvas)) {
            if (inInterfaceProperty.getValue()) {
                addStyleClass("interface-var-frame");
            }
            else {
                // No need for accessModifier in interfaces.
                accessModifier.set(true);
                addStyleClass("class-var-frame");
            }
            headerCaptionLabel.setText("");
        }
        else {
            staticModifier.set(false);
            accessModifier.set(false);
            removeStyleClass(isInInterface(parentCanvas) ? "interface-var-frame" : "class-var-frame");
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
    
    private boolean isField(FrameCanvas parentCanvas)
    {
        if (parentCanvas == null) {
            // This means that the frame is being deleted, so it has no parent.
            // This can happen if we arrive here in the middle of an undo operation while updating the cheat sheet,
            // but in that case just return arbitrary value, and cheat sheet will be updated again later:
            return false;
        }
        return parentCanvas.getParent().getChildKind(parentCanvas) == CanvasParent.CanvasKind.FIELDS;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        //final
        List<FrameOperation> operations = new ArrayList<>(super.getContextOperations());
        operations.addAll(getStaticFinalOperations());
        return operations;
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
        assignLabel.setText(newView == View.JAVA_PREVIEW ? "=" : AssignFrame.ASSIGN_SYMBOL);
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
            // This case where there was a value in the old state
            // but should be removed
            else if (slotValue != null) {
                slotValue.setText("");
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

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas innerCanvas, FrameCursor cursorInCanvas)
    {
        final List<ExtensionDescription> extensions = new ArrayList<>(super.getAvailableExtensions(innerCanvas, cursorInCanvas));
        if ( !inInterfaceProperty.getValue() ) {
            getStaticFinalOperations().forEach(op -> extensions.add(new ExtensionDescription(op, this, true,
                    ExtensionSource.BEFORE, ExtensionSource.AFTER, ExtensionSource.MODIFIER, ExtensionSource.SELECTION)));
        }
        return extensions;
    }

    private List<ToggleBooleanProperty> getStaticFinalOperations()
    {
        List<ToggleBooleanProperty> operations = new ArrayList<>();
        operations.add(new ToggleBooleanProperty(getEditor(), TOGGLE_FINAL_VAR, FINAL_NAME, 'n'));
        // is in class?
        if (isField(getParentCanvas())) {
            operations.add(new ToggleBooleanProperty(getEditor(), TOGGLE_STATIC_VAR, STATIC_NAME, 's'));
        }
        return operations;
    }

    @Override
    public Stream<EditableSlot> getPossiblyHiddenSlotsDirect()
    {
        return Stream.of(access, slotValue);
    }
}