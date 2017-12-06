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
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import bluej.Config;
import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.framedjava.ast.AccessPermissionFragment;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.ParamFragment;
import bluej.stride.framedjava.ast.ThrowsTypeFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.MethodProtoElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.DocumentedSingleLineFrame;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.EditableSlot.MenuItemOrder;
import bluej.stride.slots.FormalParameters;
import bluej.stride.slots.MethodNameDefTextSlot;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.Throws;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

public class MethodProtoFrame extends DocumentedSingleLineFrame implements CodeFrame<MethodProtoElement>
{
    private final SlotLabel abstractLabel = new SlotLabel("abstract");
    private final BooleanProperty parentIsClass = new SimpleBooleanProperty(false);
    private final TypeSlot returnType;
    private final MethodNameDefTextSlot methodName;
    private final FormalParameters paramsPane;
    private final Throws throwsPane;
    private MethodProtoElement element;    

    public MethodProtoFrame(final InteractionManager editor)
    {
        super(editor, "", "method-"); //TODO AA or should we use methodproto- ?

        methodName = new MethodNameDefTextSlot(editor, this, getHeaderRow(), null, "method-name-");
        methodName.setPromptText("name");
        methodName.addValueListener(SlotTraversalChars.METHOD_NAME);
        
        returnType = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.RETURN, "method-return-type-");
        returnType.setSimplePromptText("type");
        returnType.addClosingChar(' ');

        paramsPane = new FormalParameters(editor, this, this, getHeaderRow(), "method-param-");
        setDocumentationPromptText("Describe your method here...");
        
        throwsPane = new Throws(this, () -> {
            TypeSlot s = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.THROWS_CATCH, "method-");
            s.setSimplePromptText("thrown type");
            return s;
        }, () -> getCursorAfter().requestFocus(), editor);

        getHeaderRow().getNode().getStyleClass().add("method-header");

        bindHeader();
    }
    
    public MethodProtoFrame(final InteractionManager editor, TypeSlotFragment returnType,
            NameDefSlotFragment methodName, List<ParamFragment> params, List<ThrowsTypeFragment> throwsTypes, String documentation, boolean enabled)
    {
        this(editor);
        this.returnType.setText(returnType);
        this.methodName.setText(methodName);
        params.forEach(item -> paramsPane.addFormal(item.getParamType(), item.getParamName()));
        throwsTypes.forEach(t -> throwsPane.addTypeSlotAtEnd(t.getType(), false));
        setDocumentation(documentation);
        frameEnabledProperty.set(enabled);
    }
    
    private void bindHeader()
    {
        getHeaderRow().bindContentsConcat(FXCollections.observableArrayList(
                JavaFXUtil.listBool(parentIsClass, abstractLabel),
                FXCollections.observableArrayList(returnType, methodName),
                paramsPane.getSlots(),
                throwsPane.getHeaderItems(),
                FXCollections.observableArrayList(previewSemi)
                ));
    }
    
    public static FrameFactory<MethodProtoFrame> getFactory()
    {
        return new FrameFactory<MethodProtoFrame>() {
            @Override
            public MethodProtoFrame createBlock(InteractionManager editor)
            {
                return new MethodProtoFrame(editor);
            }

            @Override 
            public Class<MethodProtoFrame> getBlockClass()
            { 
                return MethodProtoFrame.class;
            }
        };
    }    

    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> r = new ArrayList<>(super.getContextOperations());
        
        if (parentIsClass.get()) 
        {
            r.add(new CustomFrameOperation(getEditor(), "abstract->concrete",
                    Arrays.asList("Change", "to Concrete"), MenuItemOrder.TRANSFORM, this, () -> {
                        FrameCursor c = getCursorBefore();

                        NormalMethodElement el = new NormalMethodElement(null, new AccessPermissionFragment(AccessPermission.PUBLIC),
                                false, false, returnType.getSlotElement(), methodName.getSlotElement(), paramsPane.getSlotElement(), throwsPane.getTypes(),
                                new ArrayList<>(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
                        c.insertBlockAfter(el.createFrame(getEditor()));
                        c.getParentCanvas().removeBlock(this);
                    }));
        }
        
        return r;
    }

    @Override
    public void regenerateCode()
    {
        element = new MethodProtoElement(this, returnType.getSlotElement(), methodName.getSlotElement(),
                paramsPane.getSlotElement(), throwsPane.getTypes(), new JavadocUnit(getDocumentation()), frameEnabledProperty.get());
    }

    @Override
    public boolean focusWhenJustAdded()
    {
        returnType.requestFocus();
        return true;
    }

    @Override
    public MethodProtoElement getCode()
    {
        return element;
    }
    
    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas innerCanvas, FrameCursor cursorInCanvas)
    {
        ArrayList<ExtensionDescription> extensions = new ArrayList<>(super.getAvailableExtensions(innerCanvas, cursorInCanvas));
        extensions.add(new ExtensionDescription('t', Config.getString("frame.class.add.throw"),
                () -> throwsPane.addTypeSlotAtEnd("", true), true, ExtensionSource.BEFORE, ExtensionSource.AFTER, ExtensionSource.MODIFIER));
        return extensions;
    }

    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        parentIsClass.set(parentCanvas.getParent().getFrame() instanceof ClassFrame);
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return methodName;
    }
}
