/*
 This file is part of the BlueJ program.
 Copyright (C) 2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg

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
package bluej.editor.stride;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import bluej.pkgmgr.target.role.Kind;
import bluej.utility.BackgroundConsumer;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.Observable;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;

import bluej.collect.StrideEditReason;
import bluej.parser.AssistContent;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.Loader;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.GreenfootFrameUtil;
import bluej.stride.framedjava.frames.StrideCategory;
import bluej.stride.framedjava.frames.StrideDictionary;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.parser.AssistContentThreadSafe;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameDictionary;
import bluej.stride.generic.FrameTypeCheck;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.LinkedIdentifier;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXSupplier;
import nu.xom.Element;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This is the GUI representation of the frame shelf, one per Stride editor window.
 */
@OnThread(Tag.FX)
public class FrameShelf implements InteractionManager, CanvasParent, FrameTypeCheck
{
    private final SimpleStringProperty fakeName = new SimpleStringProperty("...");
    private final ObjectProperty<Frame.View> viewHolder = new ReadOnlyObjectWrapper<>(Frame.View.NORMAL);
    
    private final FXTabbedEditor parent;
    private final BorderPaneWithHighlightColor shelfPane = new BorderPaneWithHighlightColor();
    private final FrameCanvas canvas = new FrameCanvas(this, this, "shelf-");
    private final FrameSelection selection = new FrameSelection(this);
    private final FrameShelfStorage centralStorage;
    private FrameCursor dragTarget;

    public FrameShelf(FXTabbedEditor parent, FrameShelfStorage storage)
    {
        this.parent = parent;
        shelfPane.setCenter(canvas.getNode());
        shelfPane.setStyle(getFontCSS().get());
        this.centralStorage = storage;
        storage.registerShelf(this);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withCompletions(JavaFragment.PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, FXPlatformConsumer<List<AssistContentThreadSafe>> handler)
    {
        JavaFXUtil.runAfterCurrent(() -> handler.accept(Collections.emptyList()));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withAccessibleMembers(JavaFragment.PosInSourceDoc pos, Set<AssistContent.CompletionKind> kinds, boolean includeOverridden, FXPlatformConsumer<List<AssistContentThreadSafe>> handler)
    {
        JavaFXUtil.runAfterCurrent(() -> handler.accept(Collections.emptyList()));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withSuperConstructors(FXPlatformConsumer<List<AssistContentThreadSafe>> handler)
    {
        JavaFXUtil.runAfterCurrent(() -> handler.accept(Collections.emptyList()));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withTypes(BackgroundConsumer<Map<String, AssistContentThreadSafe>> handler)
    {
        Utility.runBackground(() -> handler.accept(Collections.emptyMap()));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void withTypes(Class<?> superType, boolean includeSelf, Set<Kind> kinds, BackgroundConsumer<Map<String, AssistContentThreadSafe>> handler)
    {
        Utility.runBackground(() -> handler.accept(Collections.emptyMap()));
    }

    @Override
    public FrameCursor getFocusedCursor()
    {
        return null; // TODO
    }

    @Override
    public List<FileCompletion> getAvailableFilenames()
    {
        return Collections.emptyList();
    }

    @Override
    public ObservableStringValue nameProperty()
    {
        return fakeName;
    }

    @Override
    public FrameDictionary<StrideCategory> getDictionary()
    {
        return StrideDictionary.getDictionary();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void searchLink(PossibleLink link, FXPlatformConsumer<Optional<LinkedIdentifier>> callback)
    {
        JavaFXUtil.runAfterCurrent(() -> callback.accept(Optional.empty()));
    }

    @Override
    public Pane getDragTargetCursorPane()
    {
        return parent.getDragCursorPane();
    }

    @Override
    public void ensureImportsVisible()
    {
        // Not applicable
    }

    @Override
    public void updateCatalog(FrameCursor f)
    {
        // TODO
    }

    @Override
    public void updateErrorOverviewBar()
    {
        // Not applicable
    }

    @Override
    public Paint getHighlightColor()
    {
        return shelfPane.cssHighlightColorProperty().get();
    }

    @Override
    public List<AssistContentThreadSafe> getThisConstructors()
    {
        return Collections.emptyList();
    }

    @Override
    @OnThread(Tag.Any)
    public FrameEditor getFrameEditor()
    {
        return null;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public Class loadClass(String className)
    {
        return null;
    }

    @Override
    public void recordCodeCompletionStarted(SlotFragment position, int index, String stem, int codeCompletionId)
    {
        // Not applicable
    }

    @Override
    public void recordCodeCompletionEnded(SlotFragment position, int index, String stem, String completion, int codeCompletionId)
    {
        // Not applicable
    }

    @Override
    public void recordErrorIndicatorShown(int identifier)
    {
        // Not applicable
    }

    @Override
    public void setupFrame(Frame f)
    {
        FXTabbedEditor.setupFrameDrag(f, true, () -> parent, () -> true, () -> selection);
    }

    @Override
    public void setupFrameCursor(FrameCursor c)
    {
        // TODO ?
    }

    @Override
    public void setupFocusableSlotComponent(EditableSlot parent, Node focusableComponent, boolean canCodeComplete, FXSupplier<List<ExtensionDescription>> getExtensions, List<FrameCatalogue.Hint> hints)
    {
        // TODO ?
    }

    @Override
    public void setupSuggestionWindow(Stage window)
    {
        // TODO ?
    }

    @Override
    public void clickNearestCursor(double sceneX, double sceneY, boolean shiftDown)
    {
        FrameCursor c = canvas.findClosestCursor(sceneX, sceneY, Collections.emptyList(), false, true);
        if (c != null)
            c.requestFocus();
    }

    @Override
    public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend)
    {
        return canvas.findClosestCursor(sceneX, sceneY, exclude, isDrag, canDescend);
    }

    @Override
    public FrameCursor createCursor(FrameCanvas parent)
    {
        return new FrameCursor(this, parent);
    }

    @Override
    public Observable getObservableScroll()
    {
        return new ReadOnlyDoubleWrapper(0.0); // Not applicable
    }

    @Override
    public DoubleExpression getObservableViewportHeight()
    {
        return new ReadOnlyDoubleWrapper(0.0); // Not applicable
    }

    @Override
    public WindowOverlayPane getWindowOverlayPane()
    {
        return null; // TODO
    }

    @Override
    public CodeOverlayPane getCodeOverlayPane()
    {
        return null; // TODO
    }

    @Override
    public void modifiedFrame(Frame f, boolean force)
    {
        // Not applicable
    }

    @Override
    public void recordEdits(StrideEditReason reason)
    {
         // Not applicable
    }

    @Override
    public void afterRegenerateAndReparse(FXPlatformRunnable action)
    {
        // TODO
    }

    @Override
    public void beginRecordingState(RecallableFocus f)
    {
        // TODO ?
    }

    @Override
    public void endRecordingState(RecallableFocus f)
    {
        // TODO ?
    }

    @Override
    public void scrollTo(Node n, double yOffsetFromTop, Duration duration)
    {
        // TODO ?
    }

    @Override
    public void ensureNodeVisible(Node node)
    {
        // TODO ?
    }

    @Override
    public FrameSelection getSelection()
    {
        return selection;
    }

    @Override
    public void registerStackHighlight(Frame frame)
    {
        // Not applicable
    }

    @Override
    public boolean isLoading()
    {
        return false; // TODO
    }

    @Override
    public ReadOnlyObjectProperty<Frame.View> viewProperty()
    {
        return viewHolder;
    }

    @Override
    public void showUndoDeleteBanner(int totalEffort)
    {
        // Not applicable ?
    }

    @Override
    public boolean isEditable()
    {
        return true;
    }

    @Override
    public BooleanProperty cheatSheetShowingProperty()
    {
        return null; // TODO
    }

    @Override
    public void recordUnknownCommandKey(Frame enclosingFrame, int index, char key)
    {
        // Not applicable
    }

    @Override
    public void recordShowHideFrameCatalogue(boolean show, FrameCatalogue.ShowReason reason)
    {
        // Not applicable
    }

    @Override
    @OnThread(Tag.FX)
    public ImageView makeClassImageView()
    {
        // Not applicable
        return null;
    }

    @Override
    public FrameTypeCheck check(FrameCanvas childCanvas)
    {
        return this;
    }

    @Override
    public FrameCursor getCursorBefore(FrameCanvas c)
    {
        return null;
    }

    @Override
    public FrameCursor getCursorAfter(FrameCanvas c)
    {
        return null;
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        return Collections.emptyList();
    }

    @Override
    public InteractionManager getEditor()
    {
        return this;
    }

    @Override
    public void modifiedCanvasContent()
    {
        // Not applicable
    }

    @Override
    public Frame getFrame()
    {
        return null;
    }

    @Override
    public CanvasKind getChildKind(FrameCanvas c)
    {
        return null; // TODO
    }

    @Override
    public boolean canInsert(StrideCategory category)
    {
        // Anything goes:
        return true;
    }

    @Override
    public boolean canPlace(Class<? extends Frame> type)
    {
        // Anything goes:
        return true;
    }

    @Override
    public StringExpression getFontCSS()
    {
        return new ReadOnlyStringWrapper("-fx-font-size:10pt;");
    }

    @Override
    public double getFontSize()
    {
        return 10;
    }

    public Node getNode()
    {
        return shelfPane;
    }

    @OnThread(Tag.FXPlatform)
    public void draggedTo(List<Frame> dragSourceFrames, double sceneX, double sceneY, boolean copying)
    {
        Bounds shelfBounds = shelfPane.localToScene(shelfPane.getBoundsInLocal());
        if (sceneX < shelfBounds.getMinX() || sceneX > shelfBounds.getMaxX())
        {
            // Drag has moved out of editor section, don't show any drag target for now:
            if (dragTarget != null) {
                dragTarget.stopShowAsDropTarget();
                dragTarget = null;
            }
        }
        else
        {
            FrameCursor newDragTarget = canvas.findClosestCursor(sceneX, sceneY, dragSourceFrames, true, true);
            if (newDragTarget != null && dragTarget != newDragTarget)
            {
                if (dragTarget != null) {
                    dragTarget.stopShowAsDropTarget();
                    dragTarget = null;
                }
                boolean src = FXTabbedEditor.isUselessDrag(newDragTarget, dragSourceFrames, copying);
                boolean acceptsAll = true;
                for (Frame srcFrame : dragSourceFrames) {
                    acceptsAll &= newDragTarget.getParentCanvas().acceptsType(srcFrame);
                }
                newDragTarget.showAsDropTarget(src, acceptsAll, copying);
                dragTarget = newDragTarget;
            }
        }

        if (dragTarget != null)
        {
            dragTarget.updateDragCopyState(copying);
        }
    }

    @OnThread(Tag.FXPlatform)
    public void dragEnd(ArrayList<Frame> dragSourceFrames, boolean fromShelf, boolean copying)
    {
        // First, move the blocks:
        if (dragSourceFrames != null && !dragSourceFrames.isEmpty())
        {
            if (dragTarget != null)
            {
                // Check all of them can be dragged to new location:
                boolean canMove = dragSourceFrames.stream().allMatch(src -> dragTarget.getParentCanvas().acceptsType(src));

                if (canMove && !FXTabbedEditor.isUselessDrag(dragTarget, dragSourceFrames, copying))
                {
                    beginRecordingState(dragTarget);
                    performDrag(dragSourceFrames, fromShelf, copying);
                    endRecordingState(dragTarget);
                }
                selection.clear();

                // Then stop showing cursor as drag target:
                dragTarget.stopShowAsDropTarget();
                dragTarget = null;
            }
        }
    }

    @OnThread(Tag.FXPlatform)
    private void performDrag(List<Frame> dragSourceFrames, boolean fromShelf, boolean copying)
    {
        Frame parentFrame = dragTarget.getParentCanvas().getParent().getFrame();
        boolean shouldDisable = parentFrame != null && !parentFrame.isFrameEnabled();

        InteractionManager editor = dragSourceFrames.get(0).getEditor();
        
        // We only record if we are moving from an editor.
        // Copying from editor, or coming from shelf, doesn't change code.
        if (!fromShelf && !copying)
            editor.recordEdits(StrideEditReason.FLUSH);

        // We must add blocks in reverse order after cursor:
        Collections.reverse(dragSourceFrames);
        List<CodeElement> elements = GreenfootFrameUtil.getElementsForMultipleFrames(dragSourceFrames);
        for (CodeElement codeElement : elements) {
            final Frame frame = codeElement.createFrame(this);
            dragTarget.insertBlockAfter(frame);
            if (shouldDisable)
                frame.setFrameEnabled(false);
        }
        if (!copying)
            dragSourceFrames.forEach(src -> src.getParentCanvas().removeBlock(src));

        if (!fromShelf && !copying)
            editor.recordEdits(StrideEditReason.FRAMES_DRAG_SHELF);
    }

    public void cleanup()
    {
        centralStorage.deregisterShelf(this);
    }

    /**
     * Gets the observable list of frames on this graphical shelf interface.
     *
     * Do not modify the contents directly under any circumstances!  Only use it for listening.
     */
    public ObservableList<Frame> getContent()
    {
        return canvas.getBlockContents();
    }

    public void setContent(Element framesElement)
    {
        canvas.clear();
        for (int i = 0; i < framesElement.getChildElements().size(); i++) {
            canvas.insertBlockAfter(Loader.loadElement(framesElement.getChildElements().get(i)).createFrame(this), null);
        }
    }
}
