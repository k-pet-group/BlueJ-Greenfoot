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

import bluej.utility.Debug;
import com.google.common.io.CharStreams;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.text.TextFlow;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyledText;

import javax.swing.text.Caret;
import javax.swing.text.StyledDocument;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * MoeJEditorPane - a variation of JEditorPane for Moe. The preferred size
 * is adjusted to allow for the tag line.
 *
 * @author Michael Kolling
 */
public final class MoeEditorPane extends StyledTextArea<Integer, String>
{
    private static PaintObjectBinding latestBinding;
    private static MoeEditorPane latestEditor; // MOEFX: TODO this is a total hack

    /**
     * Create an editor pane specifically for Moe.
     */
    public MoeEditorPane(org.fxmisc.richtext.model.EditableStyledDocument<Integer, StyledText<String>, String> doc)
    {
        super(0, (p, s) -> {
            p.backgroundProperty().unbind();
            p.backgroundProperty().bind(new PaintObjectBinding(p, s, latestEditor));
        }, "", (t, s) -> {}, doc, true);
        latestEditor = this;
        /*MOEFX Maybe stop using style for this?
        getParagraphs().addListener((ListChangeListener<? super Paragraph<Integer, StyledText<String>, String>>) c -> {
            for (int n = 0; n < getParagraphs().size(); n++)
            {
                if (getParagraph(n).getParagraphStyle() != n)
                    setParagraphStyle(n, n);
            }
        });
        */
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
    }

    public void addMouseListener(MoeEditor moeEditor)
    {

    }

    public void addMouseMotionListener(MoeEditor moeEditor)
    {

    }

    public void setFont(java.awt.Font standardEditorFont)
    {

    }

    public void moveCaretPosition(int position)
    {
        int prev = getCaretPosition();
        selectRange(prev, position);
    }

    public void select(int start, int end)
    {
        selectRange(start, end);
    }

    public void write(Writer writer) throws IOException
    {
        new BufferedWriter(writer).write(getText());
    }

    private static class PaintObjectBinding extends ObjectBinding<Background>
    {
        private final TextFlow t;
        private final int lineNo;
        private MoeEditorPane editor;

        public PaintObjectBinding(TextFlow t, int line, MoeEditorPane e)
        {
            this.t = t;
            this.lineNo = line;
            super.bind(t.getChildren());
            setEditor(e);
        }

        @Override
        protected Background computeValue()
        {
            return null;
            /*
            String line = ""; //editor.getParagraph(lineNo).getText();
            int startingSpaces = line.indexOf(line.trim());
            int pos = editor.getAbsolutePosition(lineNo, startingSpaces);
            double spaceStartX = editor.getCharacterBoundsOnScreen(pos, pos + 1).map(editor::screenToLocal).map(Bounds::getMinX).orElse(0.0);
            Debug.message("Line " + lineNo + ": " + spaceStartX);
            return new Background(new BackgroundFill(new LinearGradient(0, 0, spaceStartX + 1, 0, false, CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(0.99, Color.RED), new Stop(1.0, Color.GREEN)), null, null));
            */
        }

        public void setEditor(MoeEditorPane moeEditorPane)
        {
            this.editor = moeEditorPane;
            super.bind(moeEditorPane.widthProperty());
        }
    }
}
