/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg
 
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

import bluej.parser.entity.EntityResolver;
import bluej.stride.framedjava.ast.JavadocUnit;
import bluej.stride.framedjava.ast.PackageFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ImportElement;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.ImportFrame;
import bluej.stride.framedjava.frames.StrideDictionary;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.TriangleLabel;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.MultiListener;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.DeepListBinding;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 *
 */
public abstract class TopLevelDocumentMultiCanvasFrame extends DocumentedMultiCanvasFrame
{
    protected final InteractionManager editor;
    protected final EntityResolver projectResolver;
    private final String stylePrefix;

    // can both be null in Greenfoot, where we don't show the package
    protected FrameContentRow packageRow; // final - after moving initialization to this class
    protected TextSlot<PackageFragment> packageSlot; // final - after moving initialization to this class
    protected BooleanProperty showingPackageSlot; // final - after moving initialization to this class
    // We have to keep a reference to negated version, to prevent it getting GCed:
    protected BooleanExpression notShowingPackageSlot; // final - after moving initialization to this class

    protected final FrameContentRow importRow;
    protected final FrameCanvas importCanvas;
    protected final ObservableList<String> boundImports = FXCollections.observableArrayList();
    protected final SlotLabel importsLabel;
    protected final TriangleLabel importTriangleLabel;

    protected final FrameCanvas fieldsCanvas;
    protected final FrameCanvas methodsCanvas;
    protected final SlotLabel fieldsLabel;
    protected final SlotLabel methodsLabel;
    protected final FrameContentRow fieldsLabelRow;
    protected final FrameContentRow methodsLabelRow;

    protected final FrameContentItem endSpacer;

    public TopLevelDocumentMultiCanvasFrame(InteractionManager editor, EntityResolver projectResolver, String caption,
                            String stylePrefix, List<ImportElement> imports, JavadocUnit documentation, boolean enabled)
    {
        //Frame frameParent
        super(editor, caption, stylePrefix);
        this.editor = editor;
        this.projectResolver = projectResolver;
        this.stylePrefix = stylePrefix;

        importsLabel = makeLabel("Imports");
        fieldsLabel = makeLabel("Fields");
        methodsLabel = makeLabel("Methods");

        setDocumentation(documentation.toString());

        importCanvas = createImportsCanvas(imports);// TODO delete this and uncomment it in saved() if it cause NPE in future
        //importCanvas.addToLeftMargin(10.0);
        importCanvas.getShowingProperty().set(false);
        importTriangleLabel = new TriangleLabel(editor, t -> importCanvas.growUsing(t.getProgress()), t -> importCanvas.shrinkUsing(t.getOppositeProgress()), importCanvas.getShowingProperty());
        JavaFXUtil.addChangeListener(importTriangleLabel.expandedProperty(), b -> editor.updateErrorOverviewBar());
        importRow = new FrameContentRow(this, importsLabel, importTriangleLabel);
        //alterImports(editor.getImports());

        this.fieldsCanvas = new FrameCanvas(editor, this, stylePrefix + "fields-");
        fieldsLabelRow = new FrameContentRow(this, fieldsLabel);
        addCanvas(fieldsLabelRow, fieldsCanvas);

        this.methodsCanvas = new FrameCanvas(editor, this, stylePrefix);
        methodsLabelRow = new FrameContentRow(this, methodsLabel);
        addCanvas(methodsLabelRow, methodsCanvas);

        frameEnabledProperty.set(enabled);

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
    }

    protected SlotLabel makeLabel(String content)
    {
        SlotLabel l = new SlotLabel(content);
        JavaFXUtil.addStyleClass(l, stylePrefix + "section-label");
        return l;
    }

    public void checkForEmptySlot()
    {
        if ( packageSlot != null && packageSlot.isEmpty() ) {
            showingPackageSlot.set(packageSlot.isFocused());
        }
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

    @Override
    protected List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
        return new ArrayList<>();
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
        if (packageSlot != null)
        {
            updatedChildren.add(n, packageRow);
            n += 1;
        }
        updatedChildren.add(n, importRow);
        updatedChildren.add(n+1, importCanvas);
        updatedChildren.add(endSpacer);
    }
}
