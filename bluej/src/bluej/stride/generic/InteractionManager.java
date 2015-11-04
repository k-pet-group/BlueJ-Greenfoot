/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import bluej.editor.stride.FrameCatalogue;
import bluej.stride.slots.LinkedIdentifier;
import bluej.stride.framedjava.ast.links.PossibleLink;
import javafx.beans.Observable;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import bluej.editor.stride.CodeOverlayPane;
import bluej.editor.stride.FrameSelection;
import bluej.editor.stride.WindowOverlayPane;
import bluej.parser.AssistContent;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.GreenfootFrameCategory;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.slots.EditableSlot;
import bluej.utility.javafx.FXConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public interface InteractionManager
{
    /**
     * Gets completions at that point in the file
     */
    void withCompletions(JavaFragment.PosInSourceDoc pos, ExpressionSlot<?> completing, CodeElement codeEl, FXConsumer<List<AssistContentThreadSafe>> handler);

    /**
     * Gets fields for the class.  posInfile is a bit of a workaround to make sure
     * we are in a method in the class.
     */
    void withAccessibleMembers(JavaFragment.PosInSourceDoc pos, Set<AssistContent.CompletionKind> kinds, boolean includeOverridden, FXConsumer<List<AssistContentThreadSafe>> handler);

    void withSuperConstructors(FXConsumer<List<AssistContentThreadSafe>> handler);

    /**
     * Gets a list of available types
     */
    @OnThread(Tag.Any) void withTypes(FXConsumer<List<AssistContentThreadSafe>> handler);

    /**
     * Gets a list of available types that have the given type as a super type (direct or indirect)
     */
    @OnThread(Tag.Any) void withTypes(Class<?> superType, boolean includeSelf, Set<Kind> kinds, FXConsumer<List<AssistContentThreadSafe>> handler);

    /**
     * Gets a list of classes that are commonly imported in Java programs,
     * e.g. classes from java.util, java.io, and so on.
     *
     * This list will not feature any class that is already imported in the program.
     */
    Collection<AssistContentThreadSafe> getOtherPopularImports();

    /**
     * Adds the given import to the import list (if not already present).  Should either be fully qualified
     * class name or package.name.*
     */
    void addImport(String importSrc);

    FrameCursor getFocusedCursor();

    /**
     * A list of available file names (from images or sounds directory in Greenfoot)
     */
    List<FileCompletion> getAvailableFilenames();

    // Used by constructors to set their name to the class name
    ObservableStringValue nameProperty();

    FrameDictionary<GreenfootFrameCategory> getDictionary();
/*
    // The openAction will always be called, even if it's with an empty list
    void checkVar(String name, CodeElement el, int startPosition, int endPosition, UnderlineContainer slot,
                  FXConsumer<List<StringSlotFragment.LinkedIdentifier>> openAction);

    // The openAction will always be called, even if it's with an empty list
    void checkType(String typeName, int startPosition, int endPosition, UnderlineContainer slot, FXConsumer<List<StringSlotFragment.LinkedIdentifier>> openAction);

    // The openAction will always be called, even if it's with an empty list
    void checkMethod(String qualClassName, String methodName,
                     List<String> paramTypes, int startPosition, int endPosition,
                     UnderlineContainer slot, FXConsumer<List<StringSlotFragment.LinkedIdentifier>> openAction);
*/
    public void searchLink(PossibleLink link, FXConsumer<Optional<LinkedIdentifier>> callback);

    Pane getDragTargetCursorPane();

    void ensureImportsVisible();

    void updateCatalog(FrameCursor f);

    void updateErrorOverviewBar();

    Paint getHighlightColor();

    List<AssistContentThreadSafe> getThisConstructors();

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
    public void setupFocusableSlotComponent(EditableSlot parent, Node focusableComponent, boolean canCodeComplete, List<FrameCatalogue.Hint> hints);

    /**
     * Add any necessary listeners to a code completion window
     */
    public void setupSuggestionWindow(Stage window);
    
    /**
     * Focuses the nearest frame cursor to the given point, because a click event
     * was processed at that point.
     */
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

    public void modifiedFrame(Frame f);
    
    /**
     * Generates the Java code, parses it 
     */
    public void regenerateAndReparse(ExpressionSlot<?> completing);
    
    /**
     * Starts recording of the Frame state for Undo / Redo operations
     */
    public void beginRecordingState(RecallableFocus f);
    
    /**
     * Ends recording of the Frame state for Undo / Redo operations
     */
    public void endRecordingState(RecallableFocus f);
    

    /**
     * Scrolls the view to the top of the given node, plus yOffsetFromTop pixels.
     * Takes duration time to do it, or instant if duration is null.
     */
    public void scrollTo(Node n, double yOffsetFromTop, Duration duration);
    
    default public void scrollTo(Node n, double yOffsetFromTop) { scrollTo(n, yOffsetFromTop, null); }
    
    public FrameSelection getSelection();
    
    public Point2D sceneToScreen(Point2D scenePoint);

    public void registerStackHighlight(Frame frame);

    public KeyCode getKey(ShortcutKey keyPurpose);
    
    /**
     * Returns true while frame is initially being loaded, used to mask off
     * unnecessary modification notifications
     * @return
     */
    public boolean isLoading();

    public StringExpression getFontSizeCSS();

    public ReadOnlyObjectProperty<Frame.View> viewProperty();

    public static enum Kind
    {
        CLASS_NON_FINAL, CLASS_FINAL, INTERFACE, ENUM, PRIMITIVE;
        private final static Set<InteractionManager.Kind> all = new HashSet<>(Arrays.asList(values()));
        @OnThread(Tag.Any)
        public static Set<InteractionManager.Kind> all() { return all; }
    }

    public static interface FileCompletion
    {
        public File getFile();
        public String getType(); // "Image" or "Sound"
        public Node getPreview(double maxWidth, double maxHeight);
        Map<KeyCode, Runnable> getShortcuts();
    }
}
