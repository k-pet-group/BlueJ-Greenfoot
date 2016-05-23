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
import java.util.Optional;
import java.util.stream.Stream;

import bluej.Config;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.PackageFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.InterfaceElement;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.DocumentedMultiCanvasFrame;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentItem;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameTypeCheck;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.ClassNameDefTextSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TriangleLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.DeepListBinding;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import bluej.editor.stride.BirdseyeManager;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import threadchecker.OnThread;
import threadchecker.Tag;

public class InterfaceFrame extends DocumentedMultiCanvasFrame
        implements TopLevelFrame<InterfaceElement>
{
    private final FrameContentRow importRow;
    private final FrameCanvas importCanvas;
    private final ObservableList<String> boundImports = FXCollections.observableArrayList();

    private TextSlot<NameDefSlotFragment> paramInterfaceName;
    private final InteractionManager editor;
    
               //private final ExtendsList extendsList;

    // can both be null in Greenfoot, where we don't show the package
    private final FrameContentRow packageRow;
    private final TextSlot<PackageFragment> packageSlot;
    private final BooleanProperty showingPackageSlot;
    // We have to keep a reference to negated version, to prevent it getting GCed:
    private final BooleanExpression notShowingPackageSlot;

    @OnThread(value = Tag.Any,requireSynchronized = true)
    private InterfaceElement element;
    private final EntityResolver projectResolver;

    private final FrameCanvas fieldsCanvas;
    private final FrameCanvas methodsCanvas;
    private final SlotLabel importsLabel = makeLabel("Imports");
    private final SlotLabel fieldsLabel = makeLabel("Fields");
    private final SlotLabel methodsLabel = makeLabel("Methods");
    private final FrameContentRow fieldsLabelRow;
    private final FrameContentRow methodsLabelRow;
    private final FrameContentItem endSpacer;
    private final TriangleLabel importTriangleLabel;

    private static SlotLabel makeLabel(String content)
    {
        SlotLabel l = new SlotLabel(content);
        JavaFXUtil.addStyleClass(l, "interface-section-label");
        return l;
    }

    public InterfaceFrame(InteractionManager editor, NameDefSlotFragment interfaceName, PackageFragment packageName,
              List<ImportElement> imports, List<TypeSlotFragment> extendsTypes, EntityResolver projectResolver,
              JavadocUnit documentation, boolean enabled)
    {
        super(editor, "interface", "interface-");
        this.editor = editor;
        this.projectResolver = projectResolver;

        // Spacer to make the class have a bit of space after last canvas;
        endSpacer = new FrameContentItem()
        {
            private Rectangle r = new Rectangle(1, 200, Color.TRANSPARENT);

            @Override
            public Stream<HeaderItem> getHeaderItemsDeep()
            {
                return Stream.empty();
            }

            @Override
            public Stream<HeaderItem> getHeaderItemsDirect()
            {
                return Stream.empty();
            }

            @Override
            public Bounds getSceneBounds()
            {
                return r.localToScene(r.getBoundsInLocal());
            }

            @Override
            public Optional<FrameCanvas> getCanvas()
            {
                return Optional.empty();
            }

            @Override
            public boolean focusLeftEndFromPrev()
            {
                return false;
            }

            @Override
            public boolean focusRightEndFromNext()
            {
                return false;
            }

            @Override
            public boolean focusTopEndFromPrev()
            {
                return false;
            }

            @Override
            public boolean focusBottomEndFromNext()
            {
                return false;
            }

            @Override
            public void setView(View oldView, View newView, SharedTransition animation)
            {

            }

            @Override
            public Node getNode()
            {
                return r;
            }
        };

        setDocumentation(documentation.toString());

        //Parameters
        paramInterfaceName = new ClassNameDefTextSlot(editor, this, getHeaderRow(), "interface-name-");
        paramInterfaceName.addValueListener(SlotTraversalChars.IDENTIFIER);
        paramInterfaceName.setPromptText("interface name");
        paramInterfaceName.setText(interfaceName);

/*
        extendsList = new ExtendsList(this, () -> {
            TypeTextSlot s = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor, Kind.INTERFACE), "interface-extends-");
            s.setPromptText("interface type");
            return s;
        }, () -> getCanvases().findFirst().ifPresent(c -> c.getFirstCursor().requestFocus()), editor);
        extendsTypes.forEach(t -> this.extendsList.addTypeSlotAtEnd(t.getContent(), false));

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(paramInterfaceName),
                extendsList.getHeaderItems()
        ));
*/
        documentationPromptTextProperty().bind(new SimpleStringProperty("Write a description of your ").concat(paramInterfaceName.textProperty()).concat(" interface here..."));

        this.fieldsCanvas = new FrameCanvas(editor, this, "interface-fields-");

        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(headerCaptionLabel),
                FXCollections.observableArrayList(paramInterfaceName)
        ));

        importCanvas = createImportsCanvas(imports);// TODO delete this and uncomment it in saved() if it cause NPE in future
        importCanvas.getShowingProperty().set(false);
        importTriangleLabel = new TriangleLabel(editor, t -> importCanvas.growUsing(t.getProgress()), t -> importCanvas.shrinkUsing(t.getOppositeProgress()), importCanvas.getShowingProperty());
        JavaFXUtil.addChangeListener(importTriangleLabel.expandedProperty(), b -> editor.updateErrorOverviewBar());
        importRow = new FrameContentRow(this, importsLabel, importTriangleLabel);

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


        fieldsLabelRow = new FrameContentRow(this, fieldsLabel);
        addCanvas(fieldsLabelRow, fieldsCanvas);

        this.methodsCanvas = new FrameCanvas(editor, this, "interface-");
        methodsLabelRow = new FrameContentRow(this, methodsLabel);
        addCanvas(methodsLabelRow, methodsCanvas);

        frameEnabledProperty.set(enabled);
    }

    public void checkForEmptySlot()
    {
        if ( packageSlot != null && packageSlot.isEmpty() ) {
            showingPackageSlot.set(packageSlot.isFocused());
        }
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

    // Can't drag Interface blocks:
    @Override
    public boolean canDrag()
    {
        return false;
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

    private List<CodeElement> getMembers(FrameCanvas frameCanvas)
    {
        List<CodeElement> members = new ArrayList<>();
        for (CodeFrame<?> c : frameCanvas.getBlocksSubtype(CodeFrame.class)) {
            c.regenerateCode();
            members.add(c.getCode());
        }
        return members;
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
        return ops;
//        return Collections.emptyList();
    }

    @Override
    protected List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
//        return null;
        return new ArrayList<>();
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        return new ArrayList<>();
/*
        // We deliberately don't include superclass extensions; we can't be disabled
        ExtensionDescription extendsExtension = null;
        if (fieldsCanvas.equals(canvas) || canvas == null) {
            extendsExtension = new ExtensionDescription(StrideDictionary.EXTENDS_EXTENSION_CHAR, "Add extends declaration",
                () -> extendsList.addTypeSlotAtEnd("", true), true, ExtensionSource.INSIDE_FIRST, ExtensionSource.MODIFIER);
        }

        return Utility.nonNulls(Arrays.asList(extendsExtension));
*/
    }

    @Override
    public void saved()
    {
        // TODO Auto-generated method stub
//        if (extendsInheritedCanvases.isEmpty()) {
//            updateInheritedItems();
//        }
    }

    private FrameCanvas createImportsCanvas(final List<ImportElement> imports)
    {
        FrameCanvas importCanvas = new FrameCanvas(editor, new CanvasParent() {

            @Override
            public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
            {
                return InterfaceFrame.this.importCanvas.findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend);
            }

            @Override
            public FrameTypeCheck check(FrameCanvas canvasBase)
            {
                return StrideDictionary.checkImport();
            }

            @Override
            public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursor)
            {
                return Collections.emptyList();
            }

            @Override
            public Frame getFrame()
            {
                return InterfaceFrame.this;
            }

            @Override
            public InteractionManager getEditor()
            {
                return editor;
            }

        }, "interface-import-");

        importCanvas.setAnimateLeftMarginScale(true);

        // Add available import frames:
        List<ImportElement> importsRev = new ArrayList<>(imports);
        Collections.reverse(importsRev);
        importsRev.forEach(item -> importCanvas.insertBlockBefore(item.createFrame(editor), importCanvas.getFirstCursor()));

        JavaFXUtil.onceInScene(importCanvas.getNode(), () -> importCanvas.shrinkUsing(new ReadOnlyDoubleWrapper(0.0)));

        new DeepListBinding<String>(boundImports) {
            private final ChangeListener<String> listener = (a, b, c) -> update();
            private final MultiListener<ObservableStringValue> stringListener
                    = new MultiListener<>(v -> { v.addListener(listener); return () -> v.removeListener(listener); });

            @Override
            protected Stream<ObservableList<?>> getListenTargets()
            {
                return Stream.of(importCanvas.getBlockContents());
            }

            @Override
            protected Stream<String> calculateValues()
            {
                return importCanvas.getBlockContents().stream().map(f -> (ImportFrame)f).map(ImportFrame::getImport);
            }

            @Override
            protected void update()
            {
                stringListener.listenOnlyTo(importCanvas.getBlockContents().stream().map(f -> (ImportFrame)f).map(ImportFrame::importProperty));
                super.update();
            }

        }.startListening();

        return importCanvas;
    }


    @Override
    public ObservableList<String> getImports()
    {
        return boundImports;
    }

    @Override
    public void addImport(String importSrc)
    {
        importCanvas.insertBlockAfter(new ImportFrame(editor, importSrc), importCanvas.getLastCursor());
    }

    public void addDefaultConstructor()
    {
        throw new IllegalAccessError();
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
    public List<MethodProtoFrame> getMethods()
    {
        return methodsCanvas.getBlocksSubtype(MethodProtoFrame.class);
    }

    @Override
    public List<ConstructorFrame> getConstructors()
    {
        return Collections.emptyList();
    }

    @Override
    public void insertAtEnd(Frame frame)
    {
        getLastCanvas().getLastCursor().insertBlockAfter(frame);
    }

    public FrameCanvas getfieldsCanvas()
    {
        return fieldsCanvas;
    }

    public FrameCanvas getMethodsCanvas()
    {
        return methodsCanvas;
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
    protected void modifyChildren(List<FrameContentItem> updatedChildren)
    {
        super.modifyChildren(updatedChildren);
        int n = 0;
        if (packageSlot != null)
        {
            updatedChildren.add(n, packageRow);
            n += 1;
        }
        updatedChildren.add(n, importRow);
        updatedChildren.add(n+1, importCanvas);
        updatedChildren.add(endSpacer);
    }

    @Override
    public void restore(InterfaceElement target)
    {
        paramInterfaceName.setText(target.getName());
//        restoreExtends(target);
        importCanvas.restore(target.getImports(), editor);
        fieldsCanvas.restore(target.getFields(), editor);
        methodsCanvas.restore(target.getMethods(), editor);
    }

    private void restoreExtends(InterfaceElement target)
    {
       // target.getExtendsType().forEach(ext -> extendsList.addTypeSlotAtEnd(ext.getContent(), true));
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
