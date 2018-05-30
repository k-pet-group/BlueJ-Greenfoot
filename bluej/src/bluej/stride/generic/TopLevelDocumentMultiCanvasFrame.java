/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017 Michael Kölling and John Rosenberg
 
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
package bluej.stride.generic;

import bluej.Config;
import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.elements.TopLevelCodeElement;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.ImportFrame;
import bluej.stride.framedjava.frames.StrideDictionary;
import bluej.stride.framedjava.frames.TopLevelFrame;
import bluej.stride.slots.ClassNameDefTextSlot;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TriangleLabel;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.DeepListBinding;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * A top level class to represent common features in Class and Interface frames
 * @author Amjad Altadmri
 */
public abstract class TopLevelDocumentMultiCanvasFrame<ELEMENT extends CodeElement & TopLevelCodeElement> extends DocumentedMultiCanvasFrame implements TopLevelFrame<ELEMENT>
{
    protected final InteractionManager editor;
    protected final EntityResolver projectResolver;
    private final String stylePrefix;

    @OnThread(value = Tag.Any,requireSynchronized = true)
    protected ELEMENT element;

    // can both be null in Greenfoot, where we don't show the package
    protected FrameContentRow packageRow; // final - after moving initialization to this class
    protected SlotLabel packageNameLabel; // final ?

    protected final FrameContentRow importRow;
    protected final FrameCanvas importCanvas;
    protected final ObservableList<String> boundImports = FXCollections.observableArrayList();
    protected final SlotLabel importsLabel;
    protected final TriangleLabel importTriangleLabel;

    protected TextSlot<NameDefSlotFragment> paramName;

    protected final FrameCanvas fieldsCanvas;
    protected final FrameCanvas methodsCanvas;
    protected final SlotLabel fieldsLabel;
    protected final SlotLabel methodsLabel;
    protected final FrameContentRow fieldsLabelRow;
    protected final FrameContentRow methodsLabelRow;

    protected final FrameContentItem endSpacer;

    public TopLevelDocumentMultiCanvasFrame(InteractionManager editor, EntityResolver projectResolver, String caption,
                                        String stylePrefix, String packageName, List<ImportElement> imports,
                                        JavadocUnit documentation, NameDefSlotFragment topLevelFrameName, boolean enabled)
    {
        //Frame frameParent
        super(editor, caption, stylePrefix);
        this.editor = editor;
        this.projectResolver = projectResolver;
        this.stylePrefix = stylePrefix;

        // Spacer to make the class have a bit of space after last canvas;
        endSpacer = new FrameContentItem() {
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


        // Since we don't support packages in Greenfoot, we don't bother showing the package declaration:
        if (Config.isGreenfoot())
        {
            this.packageRow = null;
            this.packageNameLabel = null;
        }
        else
        {
            if (packageName != null && !packageName.isEmpty()) {
                this.packageRow = new FrameContentRow(this);
                this.packageNameLabel = new SlotLabel(packageName, "package-slot-");
                this.packageRow.bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                        FXCollections.observableArrayList(new SlotLabel("package "), this.packageNameLabel)
                ));
            }
        }


        importsLabel = makeLabel(Config.getString("frame.editor.label.imports"));
        fieldsLabel = makeLabel(Config.getString("frame.editor.label.fields"));
        methodsLabel = makeLabel(Config.getString("frame.editor.label.methods"));

        importCanvas = createImportsCanvas(imports);// TODO delete this and uncomment it in saved() if it cause NPE in future
        //importCanvas.addToLeftMargin(10.0);
        importCanvas.getShowingProperty().set(false);
        JavaFXUtil.addChangeListener(importCanvas.getShowingProperty(), showing -> {
            if (!showing && isCanvasHasFocus(importCanvas)) {
                getfieldsCanvas().getFirstCursor().requestFocus();
            }
        });

        importTriangleLabel = new TriangleLabel(editor, t -> importCanvas.growUsing(t.getProgress()),
                t -> importCanvas.shrinkUsing(t.getOppositeProgress()), importCanvas.getShowingProperty());
        JavaFXUtil.addChangeListenerPlatform(importTriangleLabel.expandedProperty(),
                b -> editor.updateErrorOverviewBar());
        importRow = new FrameContentRow(this, importsLabel, importTriangleLabel);
        //alterImports(editor.getImports());

        //Parameters
        Properties localProperties = new Properties();
        localProperties.put("CAPTION", caption);
        paramName = new ClassNameDefTextSlot(editor, this, getHeaderRow(), stylePrefix + "name-");
        paramName.addValueListener(SlotTraversalChars.IDENTIFIER);
        paramName.setPromptText(Config.getString("frame.editor.param.prompt", null, localProperties));
        paramName.setText(topLevelFrameName);

        //Documentation
        localProperties.put("CLASSNAME", paramName.textProperty().get());
        setDocumentation(documentation.toString());
        documentationPromptTextProperty().bind(new SimpleStringProperty(
                Config.getString("frame.editor.toplevel.doc.prompt", null, localProperties)));

        this.fieldsCanvas = new FrameCanvas(editor, this, stylePrefix + "fields-");
        fieldsLabelRow = new FrameContentRow(this, fieldsLabel);
        addCanvas(fieldsLabelRow, fieldsCanvas);

        this.methodsCanvas = new FrameCanvas(editor, this, stylePrefix);
        methodsLabelRow = new FrameContentRow(this, methodsLabel);
        addCanvas(methodsLabelRow, methodsCanvas);

        frameEnabledProperty.set(enabled);
    }

    /**
     * Returns true if the focus inside a canvas, this is one of two cases:
     *      1- One of the FrameCursors inside the canvas is focused
     *      2- One of the focusable slots in one of the frames inside the canvas is focused
     *
     * @param canvas the FrameCanvas we are looking into
     * @return True only if one of focuasble targets inside the canvas is focused.
     */

    protected boolean isCanvasHasFocus(FrameCanvas canvas)
    {
        if (canvas.getFocusableCursors().stream().anyMatch(c -> c.isFocused()) ) {
            // a FrameCursor inside 'canvas' is focused
            return true;
        }

        if (canvas.getBlockContents().stream().anyMatch(b -> b.getFocusablesInclContained().anyMatch(s -> s.isFocused()))) {
            // a slot in a Frame inside 'canvas' is focused
            return true;
        }
        return false;
    }

    protected SlotLabel makeLabel(String content)
    {
        SlotLabel l = new SlotLabel(content);
        JavaFXUtil.addStyleClass(l, stylePrefix + "section-label");
        return l;
    }

    // Can't drag class/interface blocks:
    @Override
    public boolean canDrag()
    {
        return false;
    }

    protected List<CodeElement> getMembers(FrameCanvas frameCanvas)
    {
        List<CodeElement> members = new ArrayList<>();
        for (CodeFrame<?> c : frameCanvas.getBlocksSubtype(CodeFrame.class)) {
            c.regenerateCode();
            members.add(c.getCode());
        }
        return members;
    }

    private FrameCanvas createImportsCanvas(final List<ImportElement> imports)
    {
        FrameCanvas importCanvas = new FrameCanvas(editor, new CanvasParent() {

            @Override
            public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
            {
                return TopLevelDocumentMultiCanvasFrame.this.importCanvas.findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend);
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
                return TopLevelDocumentMultiCanvasFrame.this;
            }

            @Override
            public InteractionManager getEditor()
            {
                return editor;
            }

            @Override
            public CanvasKind getChildKind(FrameCanvas c)
            {
                return CanvasKind.IMPORTS;
            }
        }, stylePrefix + "import-");

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

    public ObservableList<String> getImports()
    {
        return boundImports;
    }

    public void addImport(String importSrc)
    {
        importCanvas.insertBlockAfter(new ImportFrame(editor, importSrc), importCanvas.getLastCursor());
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
    protected void modifyChildren(List<FrameContentItem> updatedChildren)
    {
        super.modifyChildren(updatedChildren);
        int n = 0;
        if (packageNameLabel != null)
        {
            updatedChildren.add(n, packageRow);
            n += 1;
        }
        updatedChildren.add(n, importRow);
        updatedChildren.add(n+1, importCanvas);
        updatedChildren.add(endSpacer);
    }

    @Override
    public void bindMinHeight(DoubleBinding prop)
    {
        getRegion().minHeightProperty().bind(prop);
    }

    @Override
    public void insertAtEnd(Frame frame)
    {
        getLastCanvas().getLastCursor().insertBlockAfter(frame);
    }

    @Override
    public ObservableStringValue nameProperty()
    {
        return paramName.textProperty();
    }

    @Override
    public FrameCanvas getImportCanvas()
    {
        return importCanvas;
    }

    @Override
    public void ensureImportCanvasShowing()
    {
        importCanvas.getShowingProperty().set(true);
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return paramName;
    }

    @Override
    public void focusName()
    {
        paramName.requestFocus(Focus.LEFT);
    }

    @Override
    public Stream<RecallableFocus> getFocusables()
    {
        // All slots, and all cursors:
        return getFocusablesInclContained();
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

            // Look for a special method:
            Frame specialMethod = findASpecialMethod();
            if (specialMethod != null)
            {
                c = specialMethod.getFirstInternalCursor();
            }
            else
            {
                // Go to top of methods:
                c = methodsCanvas.getFirstCursor();
            }
        }
        c.requestFocus();
        editor.scrollTo(c.getNode(), -100);
    }

    abstract protected Frame findASpecialMethod();

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setView(oldView, newView, animateProgress);
        boolean java = newView == View.JAVA_PREVIEW;
        if (java || oldView == View.JAVA_PREVIEW) {
            fieldsCanvas.previewCurly(java, true, false, header.getLeftFirstItem(), null, animateProgress);
            methodsCanvas.previewCurly(java, false, true, header.getLeftFirstItem(), null, animateProgress);
        }

        getCanvases().forEach(canvas -> {
            canvas.setView(oldView, newView, animateProgress);
            canvas.getCursors().forEach(c -> c.setView(newView, animateProgress));
        });

        animateLabelRows(newView, animateProgress);
        // Always show imports in Java preview:
        if (java)
            importTriangleLabel.expandedProperty().set(true);
            // And don't show in bird's eye:
        else if (newView.isBirdseye())
            importTriangleLabel.expandedProperty().set(false);

        animateCanvasLabels(oldView, newView, animateProgress);

        // TODO if extendsInheritedCanvases is added to Interfaces, use the code of setView in ClassFrame.
    }

    private void animateLabelRows(View newView, SharedTransition animateProgress)
    {
        final List<FrameContentRow> labelRows = getLabelRows();
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
    }

    protected abstract List<FrameContentRow> getLabelRows();


    private void animateCanvasLabels(View oldView, View newView, SharedTransition animateProgress)
    {
        List<SlotLabel> animateLabels = getCanvasLabels();
        if (newView == View.JAVA_PREVIEW)
        {
            animateLabels.forEach(l -> l.shrinkVertically(animateProgress));
        }
        else if (oldView == View.JAVA_PREVIEW)
        {
            animateLabels.forEach(l -> l.growVertically(animateProgress));
        }
    }

    protected abstract List<SlotLabel> getCanvasLabels();

}
