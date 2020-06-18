/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2020 Michael Kölling and John Rosenberg
 
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


import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.SandwichCanvasesElement;
import bluej.stride.framedjava.frames.BlankFrame;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.DebuggableFrame;
import bluej.stride.framedjava.frames.DebuggableParentFrame;
import bluej.stride.framedjava.frames.DebugInfo;
import bluej.stride.framedjava.frames.StrideDictionary;
import bluej.stride.framedjava.frames.GreenfootFrameUtil;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.operations.FrameOperation;
import bluej.stride.operations.PullUpContentsOperation;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Container-block representing a common parent for if/try/switch statements.
 * @author Amjad Altadmri
 */
public abstract class SandwichCanvasesFrame extends MultiCanvasFrame
    implements CodeFrame<SandwichCanvasesElement>, DebuggableFrame
{
    private String stylePrefix;
    private String frameCaption;
    private final JavaCanvas firstCanvas;

    private String intermediateCanvasCaption;
    private final List<JavaCanvas> intermediateCanvases = new ArrayList<>();

    private String tailCanvasCaption;
    private JavaCanvas tailCanvas;
    protected SandwichCanvasesElement element;
    protected final InteractionManager editor; // Saved for creating intermediate/tail canvases.

    private StrideDictionary dictionary = StrideDictionary.getDictionary();

    /**
     * Default constructor.
     *
     * @param frameCaption
     * @param stylePrefix
     */
    protected SandwichCanvasesFrame(final InteractionManager editor, String frameCaption, String intermediateCanvasCaption,
                                    String tailCanvasCaption, String stylePrefix)
    {
        super(editor, frameCaption, stylePrefix);
        this.frameCaption = frameCaption;
        this.editor = editor;
        this.stylePrefix = stylePrefix;
        this.intermediateCanvasCaption = intermediateCanvasCaption;
        this.tailCanvasCaption = tailCanvasCaption;

        firstCanvas = new JavaCanvas(editor, this, stylePrefix, false);
        addCanvas(null, firstCanvas);
    }

    protected void addIntermediateCanvas()
    {
        addIntermediateCanvas(null, null);
    }

    private void addIntermediateCanvas(FrameCanvas canvas, FrameCursor cursor, int at)
    {
        List<Frame> contents = new ArrayList<>();
        if (canvas != null && cursor != null) {
            while( cursor.getFrameAfter() != null ) {
                contents.add(cursor.getFrameAfter());
                cursor = cursor.getDown();
            }
            contents.forEach(c -> canvas.removeBlock(c));
        }
        addIntermediateCanvas(null, contents, at);
    }

    public void addIntermediateCanvas(List<SlotFragment> slots, List contents)
    {
        addIntermediateCanvas(slots, contents, canvases.size());
    }

    private void addIntermediateCanvas(List<SlotFragment> slots, List contents, int at)
    {
        //begin recording state ??
        JavaCanvas intermediateCanvas = new JavaCanvas(editor, this, stylePrefix, false);
        if (contents != null) {
            intermediateCanvas.getFirstCursor().insertFramesAfter(contents);
        }
        // 'at - 1' as we need the index in relation to the intermediate canvases only, not all of them
        final FrameContentRow intermediateHeader = getFrameContentRow(slots, intermediateCanvas, at - 1);
        addCanvas(intermediateHeader, intermediateCanvas, at);
        if (!intermediateHeader.focusLeftEndFromPrev())
            intermediateCanvas.getFirstCursor().requestFocus();
        intermediateCanvases.add(at - 1, intermediateCanvas);
        editor.modifiedFrame(this, false); //notify the editor that a change has been occurred. That will trigger a file save
    }

    protected abstract FrameContentRow getFrameContentRow(List<SlotFragment> slots, JavaCanvas canvas, int at);

    protected void pullUpCanvasContents(FrameCursor cursor, FrameCanvas canvas)
    {
        canvas.getFirstCursor().getUp().requestFocus(); // move cursor to the previous valid position above.
        List<Frame> contents = new ArrayList<>(canvas.getBlockContents());
        contents.forEach(frame -> {
            canvas.removeBlock(frame);
            frame.setParentCanvas(null);
        });
        cursor.insertFramesAfter(contents);
        // Add a BlankFrame in between in case there were any contents
        if (!contents.isEmpty()) {
            cursor.insertBlockAfter(new BlankFrame(editor));
        }
        removeCanvas(canvas);
        editor.modifiedFrame(this, false); //notify the editor that a change has been occurred. That will trigger a file save
    }

    public boolean addTailCanvas()
    {
        if (tailCanvas != null) {
            return false; //Already have one
        }

        tailCanvas = new JavaCanvas(editor, this, stylePrefix, false);
        SlotLabel tailCanvasLabel = new SlotLabel(tailCanvasCaption);
        JavaFXUtil.addStyleClass(tailCanvasLabel, "divider-" + tailCanvasCaption);
        addCanvas(new FrameContentRow(this, tailCanvasLabel), tailCanvas);
        tailCanvas.getFirstCursor().requestFocus();
        editor.modifiedFrame(this, false);
        return true;
    }

    private void addTailCanvas(FrameCanvas canvas, FrameCursor cursor)
    {
        if (addTailCanvas()) {
            List<Frame> contents = new ArrayList<>();
            if (canvas != null && cursor != null) {
                while (cursor.getFrameAfter() != null) {
                    contents.add(cursor.getFrameAfter());
                    cursor = cursor.getDown();
                }
                contents.forEach(c -> canvas.removeBlock(c));
            }
            tailCanvas.getFirstCursor().insertFramesAfter(contents);
        }
        //TODO AA: Are next two lines needed?
//        tailCanvas.getFirstCursor().requestFocus();
//        editor.modifiedFrame(this);
    }

    protected void removeCanvas(FrameCanvas canvas)
    {
        super.removeCanvas(canvas);
        //check if it is an intermediate canvas
        if (intermediateCanvases.contains(canvas)){
            intermediateCanvases.remove(canvas);
        }
        //check if we are removing a tail canvas
        if (tailCanvas == canvas){
            tailCanvas = null;
        }
        editor.modifiedFrame(this, false);
    }

    @Override
    public void regenerateCode()
    {
        List<CodeElement> firstCanvasContents = new ArrayList<>();
        firstCanvas.getBlocksSubtype(CodeFrame.class).forEach(f -> {
            f.regenerateCode();
            firstCanvasContents.add(f.getCode());
        });

        List<List<CodeElement>> intermediateCanvasesContents = Utility.mapList(intermediateCanvases, canvas ->
            Utility.mapList(canvas.getBlocksSubtype(CodeFrame.class), f -> {
                f.regenerateCode();
                return f.getCode();
            })
        );

        List<CodeElement> tailCanvasContents = null;
        if (tailCanvas != null) {
            tailCanvasContents = new ArrayList<>();
            for (CodeFrame<?> f : tailCanvas.getBlocksSubtype(CodeFrame.class)) {
                f.regenerateCode();
                tailCanvasContents.add(f.getCode());
            }
        }
        element = regenerateCodeElement(firstCanvasContents, intermediateCanvasesContents, tailCanvasContents, frameEnabledProperty.get());
    }

    protected abstract SandwichCanvasesElement regenerateCodeElement(List<CodeElement> firstCanvasContents,
                                                                     List<List<CodeElement>> intermediateCanvasesContents,
                                                                     List<CodeElement> tailCanvasContents, boolean enabled);

    @Override
    public SandwichCanvasesElement getCode()
    {
        return element;
    }

    public JavaCanvas getFirstCanvas()
    {
        return firstCanvas;
    }

    public List<JavaCanvas> getIntermediateCanvases() { return intermediateCanvases;}

    public JavaCanvas getTailCanvas()
    {
        return tailCanvas;
    }

    public DebuggableParentFrame getFirstCanvasDebug()
    {
        return new DebuggableParentFrame() {

            @Override
            @OnThread(Tag.FXPlatform)
            public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
            {
                return ((JavaCanvas) getParentCanvas()).showDebugBefore(SandwichCanvasesFrame.this, debug);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public HighlightedBreakpoint showDebugAtEnd(DebugInfo debug)
            {
                return getFirstCanvas().showDebugBefore(null, debug);
            }

            @Override
            public FrameCanvas getParentCanvas()
            {
                return getFirstCanvas();
            }
        };
    }

    public DebuggableParentFrame getIntermediateCanvasDebug(int intermediateCanvasIndex)
    {
        return new DebuggableParentFrame() {

            @Override
            @OnThread(Tag.FXPlatform)
            public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
            {
                return ((JavaCanvas)getParentCanvas()).showDebugBefore(SandwichCanvasesFrame.this, debug);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public HighlightedBreakpoint showDebugAtEnd(DebugInfo debug)
            {
                return intermediateCanvases.get(intermediateCanvasIndex).showDebugBefore(null, debug);
            }

            @Override
            public FrameCanvas getParentCanvas()
            {
                return intermediateCanvases.get(intermediateCanvasIndex);
            }
        };
    }

    public DebuggableParentFrame getTailCanvasDebug()
    {
        return new DebuggableParentFrame() {

            @Override
            @OnThread(Tag.FXPlatform)
            public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
            {
                return ((JavaCanvas)getParentCanvas()).showDebugBefore(SandwichCanvasesFrame.this, debug);
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public HighlightedBreakpoint showDebugAtEnd(DebugInfo debug)
            {
                return getTailCanvas().showDebugBefore(null, debug);
            }

            @Override
            public FrameCanvas getParentCanvas()
            {
                return getTailCanvas();
            }
        };
    }

    @Override
    public boolean focusWhenJustAdded()
    {
        firstCanvas.getFirstCursor().requestFocus();
        return true;
    }

    @Override
    public FrameTypeCheck check(FrameCanvas canvas)
    {
        return StrideDictionary.checkStatement();
    }

    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursor)
    {
        List<ExtensionDescription> inners = new ArrayList<>();
        inners.addAll(super.getAvailableExtensions(canvas, cursor));

        if (canvas == firstCanvas)
        {
            inners.add(new ExtensionDescription('\b', "Remove " + frameCaption + ", keep contents", () ->
                    new PullUpContentsOperation(editor).activate(getFrame()), false, ExtensionSource.INSIDE_FIRST));
        }

        if ( canvas == firstCanvas || intermediateCanvases.contains(canvas) || canvas == null)
        {

            // This extension will be picked if you are in the first (mandatory) canvas, an
            // intermediate canvas, or if you are afterwards and there is no tail canvas.
            List<ExtensionSource> otherSources = new ArrayList<>();
            otherSources.addAll(Arrays.asList(ExtensionSource.INSIDE_FIRST, ExtensionSource.INSIDE_LATER));
            // Only allow AFTER if there's no final canvas:
            if (tailCanvas == null)
                otherSources.add(ExtensionSource.AFTER);
            inners.add(new ExtensionDescription(dictionary.getExtensionChar(intermediateCanvasCaption), "Add " + intermediateCanvasCaption, () -> {
                if (cursor == null)
                    addIntermediateCanvas();
                else
                    addIntermediateCanvas(canvas, cursor, canvases.indexOf(canvas) + 1);
            }, true, ExtensionSource.MODIFIER, otherSources.toArray(new ExtensionSource[0])));
        }

        if (intermediateCanvases.contains(canvas))
        {
            inners.add(new ExtensionDescription('\b', "Remove " + intermediateCanvasCaption + ", keep contents", () ->
                pullUpCanvasContents(getCursorBefore(canvas), canvas), false, ExtensionSource.INSIDE_FIRST));
        }

        if ((canvas == null || Utility.findLast(getCanvases()).orElse(null) == canvas) && tailCanvas == null)
        {
            // This will be picked if you are in the last canvas or outside, and there is no tail canvas:
            inners.add(new ExtensionDescription(dictionary.getExtensionChar(tailCanvasCaption), "Add " + tailCanvasCaption, () -> {
                if (cursor == null)
                    addTailCanvas();
                else
                    addTailCanvas(canvas, cursor);
            }, true, ExtensionSource.INSIDE_FIRST, ExtensionSource.INSIDE_LATER, ExtensionSource.MODIFIER, ExtensionSource.AFTER));
        }

        if (tailCanvas != null && tailCanvas == canvas)
        {
            inners.add(new ExtensionDescription('\b', "Remove " + tailCanvasCaption + ", keep contents", () ->
                    pullUpCanvasContents(getCursorBefore(canvas), canvas), false, ExtensionSource.INSIDE_FIRST));
        }

        return inners;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        super.setView(oldView, newView, animate);
        JavaFXUtil.setPseudoclass("bj-java-preview", newView == View.JAVA_PREVIEW, sidebar.getStyleable());
        getCanvases().forEach(c -> {
            c.getCursors().forEach(cur -> cur.setView(newView, animate));
            if (isFrameEnabled() && (oldView == View.JAVA_PREVIEW || newView == View.JAVA_PREVIEW))
                c.previewCurly(newView == View.JAVA_PREVIEW, header.getLeftFirstItem(), null, animate);
            c.setView(oldView, newView, animate);
        });
    }
}