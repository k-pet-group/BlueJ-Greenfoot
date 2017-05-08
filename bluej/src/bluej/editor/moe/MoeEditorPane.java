/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.editor.moe.BlueJSyntaxView.ScopeInfo;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.StyledText;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

/**
 * MoeJEditorPane - a variation of JEditorPane for Moe. The preferred size
 * is adjusted to allow for the tag line.
 *
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class MoeEditorPane extends StyledTextArea<ScopeInfo, ImmutableSet<String>>
{
    public static final String ERROR_CLASS = "moe-code-error";
    private static final Image UNDERLINE_IMAGE = Config.getFixedImageAsFXImage("error-underline.png");
    private final MoeEditor editor;

    /**
     * Create an editor pane specifically for Moe.
     */
    public MoeEditorPane(MoeEditor editor, org.fxmisc.richtext.model.EditableStyledDocument<ScopeInfo, StyledText<ImmutableSet<String>>, ImmutableSet<String>> doc, BlueJSyntaxView syntaxView, BooleanExpression compiledStatus)
    {
        super(null, (p, s) -> {
            //Debug.message("Setting background for " + p.getChildren().stream().map(c -> c instanceof Text ? ((Text)c).getText() : "").collect(Collectors.joining()) + " to " + s);
            if (s == null)
            {
                p.backgroundProperty().unbind();
                p.setBackground(null);
            }
            else
            {
                p.backgroundProperty().bind(Bindings.createObjectBinding(() ->
                        new Background(new BackgroundImage(syntaxView.getImageFor(s, (int) p.getHeight()), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, new BackgroundPosition(Side.LEFT, 0, false, Side.TOP, 0, false), BackgroundSize.DEFAULT))
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
                t.setUnderlineColor(new ImagePattern(UNDERLINE_IMAGE, 0, 0, 4, 5, false));
            }
            t.getStyleClass().addAll(s);

        }, doc, false);
        this.editor = editor;
        styleProperty().bind(PrefMgr.getEditorFontCSS(true));
        setParagraphGraphicFactory(syntaxView::getParagraphicGraphic);
        JavaFXUtil.addStyleClass(this, "moe-editor-pane");
        JavaFXUtil.bindPseudoclass(this, "bj-line-numbers", PrefMgr.flagProperty(PrefMgr.LINENUMBERS));
        JavaFXUtil.addChangeListenerPlatform(compiledStatus, compiled -> JavaFXUtil.setPseudoclass("bj-uncompiled", !compiled, this));
        syntaxView.setEditorPane(this);
    }

    /*
     * Make sure, when we are scrolling to follow the caret,
     * that we can see the tag area as well.
     */
    /*
    public void scrollRectToVisible(Rectangle rect)
    {
        super.scrollRectToVisible(new Rectangle(rect.x - (MoeSyntaxView.TAG_WIDTH + 4), rect.y,
                                                rect.width + MoeSyntaxView.TAG_WIDTH + 4, rect.height));
    }
    */

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
        editor.undoManager.forgetHistory();
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

    public MoeEditor getEditor()
    {
        return editor;
    }

    public void setFakeCaret(boolean on)
    {
        setShowCaret(on ? CaretVisibility.ON : CaretVisibility.AUTO);
    }
}
