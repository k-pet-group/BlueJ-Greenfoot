/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2017,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

import bluej.prefmgr.PrefMgr.PrintSize;
import javafx.scene.input.MouseButton;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.fxmisc.richtext.Caret.CaretVisibility;
import org.fxmisc.richtext.StyledTextArea;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;

import bluej.Config;
import bluej.editor.moe.BlueJSyntaxView.ScopeInfo;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.ImagePattern;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.text.DefaultEditorKit;

/**
 * MoeJEditorPane - an editor pane implementation based on StyledTextArea from the RichTextFX library.
 *
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class MoeEditorPane extends StyledTextArea<ScopeInfo, ImmutableSet<String>>
{
    public static final String ERROR_CLASS = "moe-code-error";
    private static final Image UNDERLINE_IMAGE = Config.getFixedImageAsFXImage("error-underline.png");
    /**
     * The editor associated with this editor pane.
     *
     * IMPORTANT: the editor can be null, when we are printing.
     * (For printing, we create an off-screen temporary MoeEditorPane with null editor.)
     */
    private final MoeEditor editor;
    // Disabled during printing if we don't want line numbers:
    private final BooleanProperty showLineNumbers = new SimpleBooleanProperty(true);
    private final AtomicBoolean queuedRecalculation = new AtomicBoolean(false);
    private int firstVisible = -1;
    private int lastVisible = -1;

    public boolean isShowLineNumbers()
    {
        return showLineNumbers.get();
    }

    public BooleanProperty showLineNumbersProperty()
    {
        return showLineNumbers;
    }

    /**
     * Create an editor pane specifically for Moe.
     */
    public MoeEditorPane(MoeEditor editor, org.fxmisc.richtext.model.EditableStyledDocument<ScopeInfo, String, ImmutableSet<String>> doc, BlueJSyntaxView syntaxView, BooleanExpression compiledStatus)
    {
        super(null, (p, s) -> {
            if (s == null)
            {
                p.backgroundProperty().unbind();
                p.setBackground(null);
            }
            else
            {
                p.backgroundProperty().bind(Bindings.createObjectBinding(() ->
                        new Background(new BackgroundImage(syntaxView.getImageFor(s, (int) p.getHeight()),
                                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false),
                                BackgroundSize.DEFAULT))
                    , p.heightProperty()));
            }
        }, ImmutableSet.of(), (t, s) -> {
            if (s.contains(ERROR_CLASS))
            {
                // RichTextFX has built-in support for underlines, which is much easier than trying to construct
                // our own overlay.  It turns out we can even draw a squiggly underline rather than straight underline
                // by using an image-pattern for the stroke.

                // In theory, this is settable using JavaFX CSS, but it seems there is a bug
                // which prevents use of relative URLs in image patterns, so we set it from
                // code instead:
                t.setUnderlineColor(new ImagePattern(UNDERLINE_IMAGE, 0, 0, 2, 2, false));
                t.setUnderlineWidth(1.5);
            }
            t.getStyleClass().addAll(s);

        }, doc, false);
        this.editor = editor;
        styleProperty().bind(PrefMgr.getEditorFontCSS(true));
        if (syntaxView != null)
        {
            syntaxView.setEditorPane(this);
            setParagraphGraphicFactory(syntaxView::getParagraphicGraphic);
        }
        JavaFXUtil.addStyleClass(this, "moe-editor-pane");
        showLineNumbers.bind(PrefMgr.flagProperty(PrefMgr.LINENUMBERS));
        JavaFXUtil.bindPseudoclass(this, "bj-line-numbers", showLineNumbers);
        if (compiledStatus != null) { // Can be null when printing
            JavaFXUtil.addChangeListenerPlatform(compiledStatus,
                    compiled -> JavaFXUtil.setPseudoclass("bj-uncompiled", !compiled, this));
        }
        setPrinting(false, null, false);

        JavaFXUtil.addChangeListenerPlatform(PrefMgr.getEditorFontSize(), sz -> {
            JavaFXUtil.runPlatformLater(() -> requestLayout());
        });
        // We must redraw any incomplete backgrounds once
        // they become visible:
        VirtualFlow<?,?> virtualFlow = (VirtualFlow<?,?>) lookup(".virtual-flow");
        setupRedrawListener(virtualFlow);
    }

    /**
     * Check whether the specified line is currently visible on screen. If a line is
     * is visible its character positions can be determined. 
     * 
     * @param line  the line number, starting at 0
     * @return  true if the specified line is visible
     */
    public boolean lineIsVisible(int line)
    {
        return line >= firstVisible && line <= lastVisible;
    }
    
    /**
     * Set up a listener to calculate scope backgrounds for newly visible lines,
     * and schedule them to be painted if necessary.
     * 
     * @param virtualFlow  the virtual flow to listen to.
     */
    private <T,C extends Cell<T,?>> void setupRedrawListener(VirtualFlow<T,C> virtualFlow)
    {
        virtualFlow.visibleCells().addListener((ListChangeListener<? super C>) c -> {
            if (editor == null || editor.getSourceDocument() == null)
                return;

            // Must run later so that we don't affect visible cells now (which may be during the layout pass):
            // Also, this will stop us making too many getCellIfVisible calls if we have multiple consecutive visibleCells changes:
            if (queuedRecalculation.compareAndSet(false, true))
            {
                JavaFXUtil.runPlatformLater(() ->
                {
                    queuedRecalculation.set(false);
                    int earliestIncomplete = -1, latestIncomplete = -1;
                    firstVisible = -1;
                    lastVisible = -1;
                    
                    VirtualFlowHit<C> vfh = virtualFlow.hit(0, 0);
                    if (! vfh.isCellHit())
                    {
                        return;
                    }
                    
                    firstVisible = vfh.getCellIndex();
                    lastVisible = firstVisible + virtualFlow.visibleCells().size() - 1;
                    
                    for (int i = firstVisible; i <= lastVisible; i++)
                    {
                        ScopeInfo paragraphStyle = getDocument().getParagraphStyle(i);
                        boolean lineVisible = virtualFlow.getCellIfVisible(i).isPresent();
                        if (lineVisible)
                        {
                            if (paragraphStyle != null && paragraphStyle.isIncomplete())
                            {
                                if (earliestIncomplete == -1)
                                {
                                    earliestIncomplete = i;
                                }
                                latestIncomplete = i;
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                    
                    if (earliestIncomplete != -1)
                    {
                        editor.getSourceDocument().recalculateScopesForLinesInRange(earliestIncomplete, latestIncomplete);
                        editor.getSourceDocument().applyPendingScopeBackgrounds();
                    }
                });
            }
        });
    }

    /**
     * Sets the printing state on/off for this editor pane.  We adjust various style bits when
     * printing, compared to when we are on-screen.
     *
     * @param printing Are we printing?  If false, the other parameters are ignored.
     * @param printSize The size of font to print
     * @param showLineNumbers Whether to print line numbers
     */
    public void setPrinting(boolean printing, PrintSize printSize, boolean showLineNumbers)
    {
        JavaFXUtil.selectPseudoClass(this, printing ? 1 : 0, "bj-screen", "bj-printing");
        if (printing)
        {
            styleProperty().unbind();
            // These sizes are picked by hand.  They are small because the Roboto Mono font
            // is very large for its point size
            String fontSize = "7pt";
            switch (printSize)
            {
                case SMALL:
                    fontSize = "5pt";
                    break;
                case STANDARD:
                    fontSize = "7pt";
                    break;
                case LARGE:
                    fontSize = "9pt";
                    break;
            }
            setStyle("-fx-font-size: " + fontSize + ";" + PrefMgr.getEditorFontFamilyCSS());

            this.showLineNumbers.unbind();
            this.showLineNumbers.set(showLineNumbers);
        }
    }

    public void setText(String s)
    {
        replaceText(s);
    }

    public void setCaretPosition(int i)
    {
        moveTo(i);
    }

    public int getCaretDot()
    {
        return getCaretPosition();
    }

    public int getCaretMark()
    {
        return getAnchor();
    }

    public void read(Reader reader) throws IOException
    {
        setText(CharStreams.toString(reader));
        // Position caret at start, not the end:
        setCaretPosition(0);

        // editor can be null when we're printing.  This method shouldn't
        // get called then, but no harm in checking:
        if (editor != null)
        {
            editor.undoManager.forgetHistory();
        }
    }

    /**
     * Selects from the existing anchor position to the new position.
     */
    public void moveCaretPosition(int position)
    {
        int prev = getCaretMark();
        selectRange(prev, position);
    }

    public void select(int start, int end)
    {
        selectRange(start, end);
    }

    public void write(Writer writer) throws IOException
    {
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(getText());
        // Must flush or else changes don't get written:
        bufferedWriter.flush();
    }

    /**
     * Gets the editor associated with this component.  IMPORTANT: this
     * can be null if this is an off-screen copy for the purposes of printing.
     */
    public MoeEditor getEditor()
    {
        return editor;
    }

    public void setFakeCaret(boolean on)
    {
        setShowCaret(on ? CaretVisibility.ON : CaretVisibility.AUTO);
    }

    /**
     * Select word using our rules. By default, RichTextFX doesn't count
     * dots as word separator, so double-clicking "foo.bar" selects
     * the whole lot.  We override to only select the part on our
     * side of the dot.
     */
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectWord()
    {
        MoeActions actions = MoeActions.getActions(editor);
        actions.getActionByName(DefaultEditorKit.beginWordAction).actionPerformed();
        actions.getActionByName(DefaultEditorKit.selectionEndWordAction).actionPerformed();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectParagraph() 
    {
        // In RichTextFX, triple-click should select a paragraph, however this does not work 
        // on moe editor and rather it selects all the lines above the current line where the triple 
        // click happens. So we need to override the triple click behaviour in RichTextFX which is 
        // a private method that calls selectParagraph() method that we can override.
        moveCaretPosition(editor.getSelectionEnd().getLine());
        int offset = 0;
        for (int i = 0; i < editor.getSelectionEnd().getLine(); i++) 
        {
            offset = offset + editor.getLineLength(i);
        }
        select(offset - editor.getLineLength(
                editor.getSelectionEnd().getLine() - 1), offset);
    }
}
