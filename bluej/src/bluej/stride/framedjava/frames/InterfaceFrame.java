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

import bluej.Config;
import bluej.editor.stride.BirdseyeManager;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.PackageFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.errors.CodeError;
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
import bluej.stride.generic.RecallableFocus;
import bluej.stride.generic.TopLevelDocumentMultiCanvasFrame;
import bluej.stride.operations.CustomFrameOperation;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.ClassNameDefTextSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.ExtendsList;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;

import threadchecker.OnThread;
import threadchecker.Tag;

public class InterfaceFrame extends TopLevelDocumentMultiCanvasFrame implements TopLevelFrame<InterfaceElement>
{
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private InterfaceElement element;

    private TextSlot<NameDefSlotFragment> paramInterfaceName;
    private final ExtendsList extendsList;

    public InterfaceFrame(InteractionManager editor, EntityResolver projectResolver, PackageFragment packageName,
                          List<ImportElement> imports, JavadocUnit documentation, NameDefSlotFragment interfaceName,
                          List<TypeSlotFragment> extendsTypes, boolean enabled)
    {
        super(editor, projectResolver, "interface", "interface-", imports, documentation, enabled);

        // Since we don't support packages in Greenfoot, we don't bother showing the package declaration:
        if (Config.isGreenfoot())
        {
            this.packageRow = null;
            this.packageSlot = null;
            this.showingPackageSlot = null;
            this.notShowingPackageSlot = null;
        }
        else
        {
            this.packageRow = new FrameContentRow(this);

            // Spacer to catch the mouse click
            SlotLabel spacer = new SlotLabel(" ");
            spacer.setOpacity(0.0);
            spacer.setCursor(Cursor.TEXT);

            this.packageSlot = new TextSlot<PackageFragment>(editor, this, this, this.packageRow, null, "package-slot-", Collections.emptyList())
            {
                @Override
                protected PackageFragment createFragment(String content)
                {
                    return new PackageFragment(content, this);
                }

                @Override
                public void valueChangedLostFocus(String oldValue, String newValue)
                {
                    // Nothing to do
                }

                @Override
                public List<? extends PossibleLink> findLinks()
                {
                    return Collections.emptyList();
                }

                @Override
                public int getStartOfCurWord()
                {
                    // Start of word is always start of slot; don't let the dots in package/class names break the word:
                    return 0;
                }
            };
            this.packageSlot.setPromptText("package name");
            boolean packageNameNotEmpty = packageName != null && !packageName.isEmpty();
            if (packageNameNotEmpty) {
                this.packageSlot.setText(packageName);
            }
            this.showingPackageSlot = new SimpleBooleanProperty(packageNameNotEmpty);
            this.notShowingPackageSlot = showingPackageSlot.not();
            JavaFXUtil.addChangeListener(showingPackageSlot, showing -> {
                if (!showing) {
                    packageSlot.setText("");
                    packageSlot.cleanup();
                }
                editor.modifiedFrame(this);
            });

            spacer.setOnMouseClicked(e -> {
                showingPackageSlot.set(true);
                packageSlot.requestFocus();
                e.consume();
            });

            this.packageRow.bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                    FXCollections.observableArrayList(new SlotLabel("package ")),
                    JavaFXUtil.listBool(notShowingPackageSlot, spacer),
                    JavaFXUtil.listBool(showingPackageSlot, this.packageSlot)
            ));

            packageSlot.addFocusListener(this);
        }

        //Parameters
        paramInterfaceName = new ClassNameDefTextSlot(editor, this, getHeaderRow(), "interface-name-");
        paramInterfaceName.addValueListener(SlotTraversalChars.IDENTIFIER);
        paramInterfaceName.setPromptText("interface name");
        paramInterfaceName.setText(interfaceName);

        documentationPromptTextProperty().bind(new SimpleStringProperty("Write a description of your ").concat(paramInterfaceName.textProperty()).concat(" interface here..."));

        extendsList = new ExtendsList(this, () -> {
            TypeSlot s = new TypeSlot(editor, this, this, getHeaderRow(), new TypeCompletionCalculator(editor, InteractionManager.Kind.INTERFACE), "interface-");
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

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(headerCaptionLabel),
                FXCollections.observableArrayList(paramInterfaceName),
                extendsList.getHeaderItems()
        ));
    }

    @Override
    public void focusOnBody(BodyFocus on)
    {
        FrameCursor c;
        if (on == BodyFocus.TOP)
        {
            c = fieldsCanvas.getFirstCursor();
        }
        else if (on == BodyFocus.BOTTOM)
        {
            c = methodsCanvas.getLastCursor();
        }
        else
        {
            // If we have any errors, focus on them
            Optional<CodeError> error = getCurrentErrors().findFirst();
            if (error.isPresent())
            {
                error.get().jumpTo(editor);
                return;
            }

            // Go to top of methods:
            c = methodsCanvas.getFirstCursor();
        }
        c.requestFocus();
        editor.scrollTo(c.getNode(), -100);
    }

    @Override
    public void bindMinHeight(DoubleBinding prop)
    {
        getRegion().minHeightProperty().bind(prop);
    }

    @Override
    public synchronized void regenerateCode()
    {
        List<CodeElement> fields = getMembers(fieldsCanvas);
        List<CodeElement> methods = getMembers(methodsCanvas);
        List<ImportElement> imports = Utility.mapList(getMembers(importCanvas), e -> (ImportElement)e);
        element = new InterfaceElement(this, projectResolver, paramInterfaceName.getSlotElement(),
                null, //extendsList.getTypes(),
                fields, methods, new JavadocUnit(getDocumentation()), packageSlot == null ? null : packageSlot.getSlotElement(),
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

        ops.add(new CustomFrameOperation(getEditor(), "addExtends", Arrays.asList("Add 'extends'"),
                EditableSlot.MenuItemOrder.TOGGLE_EXTENDS, this, () -> extendsList.addTypeSlotAtEnd("", true)));

        final List<TypeSlotFragment> types = extendsList.getTypes();
        for (int i = 0; i < types.size(); i++)
        {
            final int index = i;
            TypeSlotFragment type = types.get(i);
            CustomFrameOperation removeOp = new CustomFrameOperation(getEditor(), "removeExtends",
                    Arrays.asList("Remove 'extends " + type.getContent() + "'"), EditableSlot.MenuItemOrder.TOGGLE_EXTENDS,
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
            extendsExtension = new ExtensionDescription(StrideDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration",
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
    public void insertAtEnd(Frame frame)
    {
        getLastCanvas().getLastCursor().insertBlockAfter(frame);
    }

    @Override
    public ObservableStringValue nameProperty()
    {
        return paramInterfaceName.textProperty();
    }

    @Override
    public Stream<RecallableFocus> getFocusables()
    {
        // All slots, and all cursors:
        return getFocusablesInclContained();
    }

    @Override
    public FrameCanvas getImportCanvas() {
        return importCanvas;
    }

    @Override
    public void ensureImportCanvasShowing()
    {
        importCanvas.getShowingProperty().set(true);
    }

    @Override
    public Stream<FrameCanvas> getPersistentCanvases()
    {
        return getCanvases();
//        return getCanvases().filter(canvas -> !extendsInheritedCanvases.contains(canvas));
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return paramInterfaceName;
    }

    @Override
    public void focusName()
    {
        paramInterfaceName.requestFocus(Focus.LEFT);
    }

    @Override
    public FrameTypeCheck check(FrameCanvas canvas)
    {
        if (canvas == fieldsCanvas)
            return StrideDictionary.checkField();
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
        paramInterfaceName.setText(target.getName());
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
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        setViewNoOverride(oldView, newView, animateProgress);
        boolean java = newView == View.JAVA_PREVIEW;
        if (oldView == View.JAVA_PREVIEW || newView == View.JAVA_PREVIEW)
        {
            fieldsCanvas.previewCurly(java, true, false, header.getLeftFirstItem(), null, animateProgress);
            methodsCanvas.previewCurly(java, false, true, header.getLeftFirstItem(), null, animateProgress);
        }

        getCanvases().forEach(canvas -> {
            canvas.setView(oldView, newView, animateProgress);
            canvas.getCursors().forEach(c -> c.setView(newView, animateProgress));
        });

        final List<FrameContentRow> labelRows = Arrays.asList(importRow, fieldsLabelRow, methodsLabelRow);
        if (newView == View.NORMAL)
        {
            animateProgress.addOnStopped(() -> {
                importTriangleLabel.setVisible(true);
                importTriangleLabel.setManaged(true);
                labelRows.forEach(r -> r.setSnapToPixel(true));
            });
        }
        else
        {
            labelRows.forEach(r -> r.setSnapToPixel(false));
            importTriangleLabel.setVisible(false);
            importTriangleLabel.setManaged(false);
        }
        // Always show imports in Java preview:
        if (java)
            importTriangleLabel.expandedProperty().set(true);
            // And don't show in bird's eye:
        else if (newView.isBirdseye())
            importTriangleLabel.expandedProperty().set(false);

        List<SlotLabel> animateLabels = Arrays.asList(importsLabel, fieldsLabel, methodsLabel);
        if (java)
        {
            animateLabels.forEach(l -> l.shrinkVertically(animateProgress));
        }
        else if (oldView == View.JAVA_PREVIEW)
        {
            animateLabels.forEach(l -> l.growVertically(animateProgress));
        }
    }
}
