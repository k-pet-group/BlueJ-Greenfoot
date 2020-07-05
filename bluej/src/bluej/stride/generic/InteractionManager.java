/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019,2020 Michael Kölling and John Rosenberg
 
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

import bluej.collect.StrideEditReason;
import bluej.parser.AssistContentThreadSafe;
import bluej.editor.stride.FrameCatalogue;
import bluej.editor.stride.FrameEditor;
import bluej.pkgmgr.target.role.Kind;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.slots.LinkedIdentifier;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.editor.fixes.SuggestionList.SuggestionListParent;
import bluej.utility.BackgroundConsumer;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXSupplier;
import javafx.beans.Observable;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import bluej.editor.stride.CodeOverlayPane;
import bluej.editor.stride.FrameSelection;
import bluej.editor.stride.WindowOverlayPane;
import bluej.parser.AssistContent;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.StrideCategory;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.slots.EditableSlot;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@OnThread(Tag.FX)
public interface InteractionManager extends SuggestionListParent
{
    /**
     * Gets completions at that point in the file
     */
    @OnThread(Tag.FXPlatform)
    void withCompletions(JavaFragment.PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, FXPlatformConsumer<List<AssistContentThreadSafe>> handler);

    /**
     * Gets fields for the class.  posInfile is a bit of a workaround to make sure
     * we are in a method in the class.
     */
    @OnThread(Tag.FXPlatform)
    void withAccessibleMembers(JavaFragment.PosInSourceDoc pos, Set<AssistContent.CompletionKind> kinds, boolean includeOverridden, FXPlatformConsumer<List<AssistContentThreadSafe>> handler);

    @OnThread(Tag.FXPlatform)
    void withSuperConstructors(FXPlatformConsumer<List<AssistContentThreadSafe>> handler);

    /**
     * Gets a list of available types
     */
    @OnThread(Tag.FXPlatform) void withTypes(BackgroundConsumer<Map<String, AssistContentThreadSafe>> handler);

    /**
     * Gets a list of available types that have the given type as a super type (direct or indirect)
     */
    @OnThread(Tag.FXPlatform) void withTypes(Class<?> superType, boolean includeSelf, Set<Kind> kinds, BackgroundConsumer<Map<String, AssistContentThreadSafe>> handler);


    FrameCursor getFocusedCursor();

    /**
     * A list of available file names (from images or sounds directory in Greenfoot)
     */
    List<FileCompletion> getAvailableFilenames();

    // Used by constructors to set their name to the class name
    ObservableStringValue nameProperty();

    FrameDictionary<StrideCategory> getDictionary();
    
    @OnThread(Tag.FXPlatform)
    public void searchLink(PossibleLink link, FXPlatformConsumer<Optional<LinkedIdentifier>> callback);

    @OnThread(Tag.FXPlatform)
    Pane getDragTargetCursorPane();

    void ensureImportsVisible();

    @OnThread(Tag.FXPlatform)
    void updateCatalog(FrameCursor f);

    @OnThread(Tag.FXPlatform)
    void updateErrorOverviewBar();

    Paint getHighlightColor();

    @OnThread(Tag.FXPlatform)
    List<AssistContentThreadSafe> getThisConstructors();

    @OnThread(Tag.Any)
    FrameEditor getFrameEditor();

    @OnThread(Tag.FXPlatform)
    Class loadClass(String className);

    // See corresponding DataCollector methods for more parameter info.
    @OnThread(Tag.FXPlatform)
    void recordCodeCompletionStarted(SlotFragment position, int index, String stem, int codeCompletionId);

    // See corresponding DataCollector methods for more parameter info.
    @OnThread(Tag.FXPlatform)
    void recordCodeCompletionEnded(SlotFragment position, int index, String stem, String completion, int codeCompletionId);

    @OnThread(Tag.FXPlatform)
    void recordErrorIndicatorShown(int identifier);

    boolean isEditable();

    @OnThread(Tag.FXPlatform)
    BooleanProperty cheatSheetShowingProperty();

    @OnThread(Tag.FXPlatform)
    void recordUnknownCommandKey(Frame enclosingFrame, int index, char key);

    /**
     * Records the reason and the focused cursor info, if any, when showing or hiding the FrameCatalogue of this editor.
     *
     * @param show                 true for showing and false for hiding
     * @param reason               The event which triggers the change.
     *                             It is one of the values in the FrameCatalogue.ShowReason enum.
     */
    @OnThread(Tag.FXPlatform)
    void recordShowHideFrameCatalogue(boolean show, FrameCatalogue.ShowReason reason);

    /**
     * Gets an image for the class, suitable for displaying as an overlay in the class
     * body.  Return null if there's no such available image.
     */
    @OnThread(Tag.FX)
    ImageView makeClassImageView();

    public static enum ShortcutKey
    {
        YES_ANYWHERE,
        NO_ANYWHERE;
    }
    
    /**
     * Adds mouse-drag handlers to the given Frame, so that if a drag starts on that
     * frame, it will be handled properly.  Also adds click listeners for showing
     * popup menu, and any other listeners needed.
     */
    public void setupFrame(Frame f);
    
    /**
     * Adds mouse-drag handlers to the given FrameCursor, so that if a drag starts on that
     * cursor, it will be handled properly, and any other listeners needed
     */
    public void setupFrameCursor(FrameCursor c);
    
    /**
     * Adds any necessary listeners to the given Node that will be a focusable part of the given EditableSlot,
     * for example making sure that the node is always in the visible viewport when focused.  If not (sufficiently) visible, it will be scrolled to.
     */
    public void setupFocusableSlotComponent(EditableSlot parent, Node focusableComponent, boolean canCodeComplete, FXSupplier<List<ExtensionDescription>> getExtensions, List<FrameCatalogue.Hint> hints);

    /**
     * Focuses the nearest frame cursor to the given point, because a click event
     * was processed at that point.
     */
    @OnThread(Tag.FXPlatform)
    public void clickNearestCursor(double sceneX, double sceneY, boolean shiftDown);
    
    /**
     * Creates a new cursor
     */
    public FrameCursor createCursor(FrameCanvas parent);

    /**
     * Gets an observable value which will change when the window is scrolled
     */
    public Observable getObservableScroll();

    /**
     * Gets an observable double which represents the height of the editor viewport
     * @return
     */
    public DoubleExpression getObservableViewportHeight();
    
    // OverlayPane may return null in some cases, such as while dragging, so check it.
    public WindowOverlayPane getWindowOverlayPane();
    public CodeOverlayPane getCodeOverlayPane();

    /**
     * Register a Frame as modified to trigger/allow some operations needed after edits.
     * @param f the modified frame
     * @param force a boolean flag, which is only true when we need the modification to be registered,
     *              even if the editor tab is loading or with no window (hidden).
     */
    public void modifiedFrame(Frame f, boolean force);

    @OnThread(Tag.FXPlatform)
    public void recordEdits(StrideEditReason reason);
    
    /**
     * Once loading is complete, generates the Java code, parses it, then runs the given action if not-null
     */
    @OnThread(Tag.FXPlatform)
    public void afterRegenerateAndReparse(FXPlatformRunnable action);
    
    /**
     * Starts recording of the Frame state for Undo / Redo operations
     */
    public void beginRecordingState(RecallableFocus f);
    
    /**
     * Ends recording of the Frame state for Undo / Redo operations
     */
    public void endRecordingState(RecallableFocus f);

    /**
     * If the node is not visible, scrolls the view so that it is visible.
     * If it is already visible somewhere, does not scroll (unlike scrollTo,
     * which would still scroll to the given position).
     */
    public void ensureNodeVisible(Node node);
    

    /**
     * Scrolls the view to the top of the given node, plus yOffsetFromTop pixels.
     * Takes duration time to do it, or instant if duration is null.
     */
    public void scrollTo(Node n, double yOffsetFromTop, Duration duration);
    
    default public void scrollTo(Node n, double yOffsetFromTop) { scrollTo(n, yOffsetFromTop, null); }
    
    public FrameSelection getSelection();

    public void registerStackHighlight(Frame frame);

    /**
     * Returns true while frame is initially being loaded, used to mask off
     * unnecessary modification notifications
     * @return
     */
    @OnThread(Tag.FX)
    public boolean isLoading();

    public StringExpression getFontCSS();

    public ReadOnlyObjectProperty<Frame.View> viewProperty();

    public void showUndoDeleteBanner(int totalEffort);

    public static interface FileCompletion
    {
        public File getFile();
        public String getType(); // e.g. "Image" or "Sound"
        public Node getPreview(double maxWidth, double maxHeight);
        Map<KeyCode, Runnable> getShortcuts();
    }
}
