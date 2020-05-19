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

import bluej.Config;
import bluej.editor.stride.BirdseyeManager;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.InterfaceElement;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameTypeCheck;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.TopLevelDocumentMultiCanvasFrame;
import bluej.stride.operations.CopyFrameAsImageOperation;
import bluej.stride.operations.CopyFrameAsJavaOperation;
import bluej.stride.operations.CopyFrameAsStrideOperation;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.ExtendsList;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.SharedTransition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import threadchecker.OnThread;
import threadchecker.Tag;

public class InterfaceFrame extends TopLevelDocumentMultiCanvasFrame<InterfaceElement>
{
    private final ExtendsList extendsList;

    public InterfaceFrame(InteractionManager editor, EntityResolver projectResolver, String packageName,
                          List<ImportElement> imports, JavadocUnit documentation, NameDefSlotFragment interfaceName,
                          List<TypeSlotFragment> extendsTypes, boolean enabled)
    {
        super(editor, projectResolver, "interface", "interface-", packageName, imports, documentation, interfaceName, enabled);

        extendsList = new ExtendsList(this, () -> {
            TypeSlot s = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.INTERFACE, "interface-");
            s.setSimplePromptText("interface type");
            return s;
        }, () -> getCanvases().findFirst().ifPresent(c -> c.getFirstCursor().requestFocus()), editor);

        extendsTypes.forEach(t -> this.extendsList.addTypeSlotAtEnd(t.getContent(), false));
        extendsList.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal)
                extendsList.ensureAtLeastOneSlot();
            else
                extendsList.clearIfSingleEmpty();
        });

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(headerCaptionLabel),
                FXCollections.observableArrayList(paramName),
                extendsList.getHeaderItems()
        ));
    }

    protected Frame findASpecialMethod()
    {
        return null;
    }

    @Override
    public synchronized void regenerateCode()
    {
        List<CodeElement> fields = getMembers(fieldsCanvas);
        List<CodeElement> methods = getMembers(methodsCanvas);
        List<ImportElement> imports = Utility.mapList(getMembers(importCanvas), e -> (ImportElement)e);
        element = new InterfaceElement(this, projectResolver, paramName.getSlotElement(), extendsList.getTypes(),
                fields, methods, new JavadocUnit(getDocumentation()), packageNameLabel == null ? null : packageNameLabel.getText(),
                imports, frameEnabledProperty.get());
    }

    @Override
    @OnThread(value = Tag.Any, ignoreParent = true)
    public synchronized InterfaceElement getCode()
    {
        return element;
    }

    @Override
    public List<FrameOperation> getContextOperations()
    {
        ArrayList<FrameOperation> ops = new ArrayList<>();
        ops.add(new CopyFrameAsStrideOperation(editor));
        ops.add(new CopyFrameAsImageOperation(editor));
        ops.add(new CopyFrameAsJavaOperation(editor));
        ops.add(new CustomFrameOperation(getEditor(), "addExtends", Arrays.asList(Config.getString("frame.class.add.extends")),
                AbstractOperation.MenuItemOrder.TOGGLE_EXTENDS, this, () -> extendsList.addTypeSlotAtEnd("", true)));

        final List<TypeSlotFragment> types = extendsList.getTypes();
        for (int i = 0; i < types.size(); i++)
        {
            final int index = i;
            TypeSlotFragment type = types.get(i);
            CustomFrameOperation removeOp = new CustomFrameOperation(getEditor(), "removeExtends",
                    Arrays.asList(Config.getString("frame.class.remove.extends.from").replace("$", type.getContent())), AbstractOperation.MenuItemOrder.TOGGLE_EXTENDS,
                    this, () -> extendsList.removeIndex(index));
            removeOp.setWideCustomItem(true);
            ops.add(removeOp);
        }

        return ops;
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        // We deliberately don't include super.getAvailableExtensions; we can't be disabled
        ExtensionDescription extendsExtension = null;
        if (fieldsCanvas.equals(canvas) || canvas == null) {
            extendsExtension = new ExtensionDescription(StrideDictionary.EXTENDS_EXTENSION_CHAR,  Config.getString("frame.class.add.extends.declaration"),
                    () -> extendsList.addTypeSlotAtEnd("", true), true, ExtensionDescription.ExtensionSource.INSIDE_FIRST,
                    ExtensionDescription.ExtensionSource.MODIFIER);
        }
        return Utility.nonNulls(Arrays.asList(extendsExtension));
    }

    @Override
    public void saved()
    {
        // TODO Auto-generated method stub
//        if (extendsInheritedCanvases.isEmpty()) {
//            updateInheritedItems();
//        }
    }

    @Override
    public BirdseyeManager prepareBirdsEyeView(SharedTransition animate)
    {
        // Birdseye view is not available in Interfaces
        return null;
    }

    @Override
    public void addExtendsClassOrInterface(String className)
    {
        extendsList.addTypeSlotAtEnd(className, false);
    }

    @Override
    public void addImplements(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeExtendsClass()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeExtendsOrImplementsInterface(String interfaceName)
    {
        List<TypeSlotFragment> extendsTypes = extendsList.getTypes();
        for (int i = 0; i < extendsTypes.size(); i++)
        {
            if (extendsTypes.get(i).getContent().equals(interfaceName))
            {
                extendsList.removeIndex(i);
                return;
            }
        }
    }
    
    @Override
    public boolean canDoBirdseye()
    {
        // No point, since we only have prototypes in
        return false;
    }

    @Override
    public void addDefaultConstructor()
    {
        throw new IllegalAccessError();
    }

    @Override
    public List<ConstructorFrame> getConstructors()
    {
        return Collections.emptyList();
    }

    @Override
    public List<MethodProtoFrame> getMethods()
    {
        return methodsCanvas.getBlocksSubtype(MethodProtoFrame.class);
    }

    @Override
    public Stream<FrameCanvas> getPersistentCanvases()
    {
        return getCanvases();
//        return getCanvases().filter(canvas -> !extendsInheritedCanvases.contains(canvas));
    }

    @Override
    public FrameTypeCheck check(FrameCanvas canvas)
    {
        if (canvas == fieldsCanvas)
            return StrideDictionary.checkInterfaceField();
        else if (canvas == methodsCanvas)
            return StrideDictionary.checkInterfaceMethod();
        else
            throw new IllegalStateException("Asking about canvas unknown to InterfaceFrame");
    }

    @Override
    public CanvasKind getChildKind(FrameCanvas c)
    {
        if (c == fieldsCanvas)
            return CanvasKind.FIELDS;
        else if (c == methodsCanvas)
            return CanvasKind.METHODS;
        else
            return CanvasKind.STATEMENTS; // Not true, but it's our default for now
    }

    @Override
    public void restore(InterfaceElement target)
    {
        paramName.setText(target.getName());
        extendsList.setTypes(target.getExtends());
        importCanvas.restore(target.getImports(), editor);
        fieldsCanvas.restore(target.getFields(), editor);
        methodsCanvas.restore(target.getMethods(), editor);
    }

    @Override
    protected FrameContentRow makeHeader(String stylePrefix)
    {
        return new FrameContentRow(this, stylePrefix) {
            @Override
            public boolean focusRightEndFromNext()
            {
                extendsList.ensureAtLeastOneSlot();
                Utility.findLast(extendsList.getTypeSlots()).get().requestFocus(Focus.RIGHT);
                return true;
            }
        };
    }

    @Override
    protected List<FrameContentRow> getLabelRows()
    {
        return Arrays.asList(importRow, fieldsLabelRow, methodsLabelRow);
    }

    @Override
    protected List<SlotLabel> getCanvasLabels()
    {
        return Arrays.asList(importsLabel, fieldsLabel, methodsLabel);
    }
}
