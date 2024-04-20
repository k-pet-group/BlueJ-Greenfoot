/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018,2024 Michael Kölling and John Rosenberg
 
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
package bluej.utility.javafx;

import java.util.List;

import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

public final class DelegableScalableTextField<DELEGATE_IDENT> extends ScalableHeightTextField
{
    private final SimpleStyleableDoubleProperty bjMinWidthProperty = new SimpleStyleableDoubleProperty(BJ_MIN_WIDTH_META_DATA);
    /**
     * When we let super class handle nextWord, it may call end, but this can produce the wrong result
     * when we've also overridden end.  This flag indicates whether we are currently in the middle of a next-word call:
     */
    private boolean inNextWord = false;

    private final SimpleStyleableDoubleProperty bjMinWidthProperty() { return bjMinWidthProperty; }

    private static final CssMetaData<DelegableScalableTextField<?>, Number> BJ_MIN_WIDTH_META_DATA =
            JavaFXUtil.cssSize("-bj-min-width", DelegableScalableTextField::bjMinWidthProperty);

    private static final List<CssMetaData <? extends Styleable, ? > > cssMetaDataList =
            JavaFXUtil.extendCss(TextField.getClassCssMetaData())
                    .add(BJ_MIN_WIDTH_META_DATA)
                    .build();

    public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() { return cssMetaDataList; }
    @Override public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() { return cssMetaDataList; }

    private final TextFieldDelegate<DELEGATE_IDENT> delegate;
    private final DELEGATE_IDENT delegateId;

    // Keep track of InputMethod (IM) positions, e.g. for entering Chinese with
    // the on-screen IME keyboard.  imStart is the start of the current sequence of
    // "composing" characters that could end up staying as English or transformed into
    // a related Chinese (or other language) string.  imLength is the length (starting at
    // imStart) of this portion.  When there is no current IM entry, imStart is set to -1
    // and imLength is set to 0.
    private int imStart = -1;
    private int imLength;
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void insertText(int index, String text)
    {
        nonIMEdit();
        delegate.insert(delegateId, index, index, text);
    }

    /**
     * Called whenever the content of the field, or the position of the caret, has been changed
     * by an action *other* than an InputMethodEvent.  So we need to call this method at the start of
     * just about every other method in this class.
     */
    private void nonIMEdit()
    {
        imStart = -1;
        imLength = 0;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public boolean deletePreviousChar()
    {
        nonIMEdit();
        if (delegate.deleteSelection() || delegate.deletePrevious(delegateId, getCaretPosition(), getCaretPosition() == 0))
        {
            return true;
        }
        else
        {
            return super.deletePreviousChar();
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public boolean deleteNextChar()
    {
        nonIMEdit();
        if (delegate.deleteSelection() ||
                delegate.deleteNext(delegateId, getCaretPosition(), getCaretPosition() == getLength()))
        {
            return true;
        }
        else
        {
            return super.deleteNextChar();
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void previousWord()
    {
        nonIMEdit();
        if (!delegate.previousWord(delegateId, getCaretPosition() == 0))
            super.previousWord();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void nextWord()
    {
        nonIMEdit();
        if (!delegate.nextWord(delegateId, getCaretPosition() == getLength()))
        {
            inNextWord = true;
            super.nextWord();
            inNextWord = false;
        }
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void endOfNextWord()
    {
        nonIMEdit();
        if (!delegate.endOfNextWord(delegateId, getCaretPosition() == getLength()))
        {
            super.endOfNextWord();
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void backward()
    {
        nonIMEdit();
        delegate.deselect();
        if (getCaretPosition() == 0)
        {
            delegate.backwardAtStart(delegateId);
        }
        else
        {
            super.backward();
        }
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void cut()
    {
        nonIMEdit();
        if (!delegate.cut())
            super.cut();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void copy()
    {
        if (!delegate.copy())
            super.copy();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void forward()
    {
        nonIMEdit();
        delegate.deselect();
        if (getCaretPosition() == getText().length())
            delegate.forwardAtEnd(delegateId);
        else
            super.forward();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void appendText(String text)
    {
        nonIMEdit();
        insertText(getText().length(), text);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void replaceText(IndexRange range, String text)
    {
        nonIMEdit();
        replaceText(range.getStart(), range.getEnd(), text);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void replaceText(int start, int end, String text)
    {
        // Do not delete, we'll handle case there is a selection:
        //deleteText(start, end);
        // We need to pass the range though, because if user is entering
        // foreign characters (e.g. Chinese) we may need to replace the
        // earlier QWERTY characters with the target character:
        nonIMEdit();
        delegate.insert(delegateId, start, end, text);

    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void deleteText(IndexRange range)
    {
        nonIMEdit();
        super.deleteText(range.getStart(), range.getEnd());
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void deleteText(int start, int end)
    {
        nonIMEdit();
        delegate.delete(delegateId, start, end);
        positionCaret(start);
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectBackward()
    {
        nonIMEdit();
        if (!delegate.selectBackward(delegateId, getCaretPosition())) {
            super.selectBackward();
        }
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void deselect() {
        nonIMEdit();
        delegate.deselect();
        super.deselect();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectForward()
    {
        nonIMEdit();
        if (!delegate.selectForward(delegateId, getCaretPosition(), getCaretPosition() == getLength()))
            super.selectForward();
    }

    public DelegableScalableTextField(TextFieldDelegate<DELEGATE_IDENT> delegate, DELEGATE_IDENT ident, String content)
    {
        super(content);
        this.delegate = delegate;
        this.delegateId = ident;
        // Important to set this before the skin gets set, as this prevents the skin setting the default handler:
        setOnInputMethodTextChanged(this::handleInputMethodEvent);
        // This implementation has been copied from TextInputControlSkin but it's important
        // to copy it because it uses our imStart/imLength fields.
        setInputMethodRequests(new ExtendedInputMethodRequests() {
            @Override public Point2D getTextLocation(int offset) {
                Scene scene = getScene();
                Window window = scene != null ? scene.getWindow() : null;
                if (window == null) {
                    return new Point2D(0, 0);
                }
                // Don't use imstart here because it isn't initialized yet.
                Rectangle2D characterBounds = ((TextFieldSkin)getSkin()).getCharacterBounds(getSelection().getStart() + offset);
                Point2D p = localToScene(characterBounds.getMinX(), characterBounds.getMaxY());
                Point2D location = new Point2D(window.getX() + scene.getX() + p.getX(),
                        window.getY() + scene.getY() + p.getY());
                return location;
            }

            @Override public int getLocationOffset(int x, int y) {
                return 0;
            }

            @Override public void cancelLatestCommittedText() {
                nonIMEdit();
            }

            @Override public String getSelectedText() {
                IndexRange selection = getSelection();
                return getText(selection.getStart(), selection.getEnd());
            }

            @Override public int getInsertPositionOffset() {
                int caretPosition = getCaretPosition();
                if (caretPosition < imStart) {
                    return caretPosition;
                } else if (caretPosition < imStart + imLength) {
                    return imStart;
                } else {
                    return caretPosition - imLength;
                }
            }

            @Override public String getCommittedText(int begin, int end) {
                if (begin < imStart) {
                    if (end <= imStart) {
                        return getText(begin, end);
                    } else {
                        return getText(begin, imStart) + getText(imStart + imLength, end + imLength);
                    }
                } else {
                    return getText(begin + imLength, end + imLength);
                }
            }

            @Override public int getCommittedTextLength() {
                return getText().length() - imLength;
            }
        });



        JavaFXUtil.addStyleClass(this, "delegable-scalable-text-field");
        setMinWidth(Region.USE_PREF_SIZE);
        prefWidthProperty().bind(new DoubleBinding() {
            { super.bind(textProperty());
              super.bind(promptTextProperty());
              super.bind(fontProperty());
              super.bind(focusedProperty());
              super.bind(paddingProperty());
              super.bind(bjMinWidthProperty()); }

            @Override
            protected double computeValue()
            {
                double minWidth;
                if (getPromptText().isEmpty() && getText().isEmpty())
                {
                    // Totally empty:
                    return bjMinWidthProperty.get();
                }
                else if (getPromptText().isEmpty() || !getText().isEmpty())
                {
                    minWidth = bjMinWidthProperty.get(); // If we are not showing prompt text, width is min
                }
                else
                {
                    // If we are showing prompt text, measure it:
                    minWidth = JavaFXUtil.measureString(DelegableScalableTextField.this, getPromptText());
                }
                // We need to add a couple of pixels when text is showing; if we are too close
                // to the wire on sizing the field, then JavaFX can start showing the field as totally blank,
                // with all the text scrolled off to the left (presumably because it sees the text field as
                // having overflowed):
                return Math.max(minWidth, 2 + JavaFXUtil.measureString(DelegableScalableTextField.this, getText()));
            }
            
        });
        setOnMousePressed(e -> { 
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) // Double and triple clicks will be handled by the field.
            {
                nonIMEdit();
                delegate.clicked();
                delegate.moveTo(e.getSceneX(), e.getSceneY(), true);
                e.consume();
            }
        });
        setOnDragDetected(e -> { e.consume(); });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
            {
                nonIMEdit();
                delegate.selectTo(e.getSceneX(), e.getSceneY());
                e.consume();
            }
        });
        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
            {
                nonIMEdit();
                delegate.selected();
                e.consume();
            }
        });
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY)
                e.consume();
        });
        
        caretPositionProperty().addListener(new ChangeListener<Number>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                delegate.caretMoved();
            }
        });
        
        addEventHandler(KeyEvent.KEY_PRESSED, e -> { if (e.getCode() == KeyCode.ESCAPE) delegate.escape(); });
        
    }

    @OnThread(Tag.FXPlatform)
    private void handleInputMethodEvent(InputMethodEvent event) {
        // This is adapted from the default handler in TextInputControlSkin.  That handler
        // uses selection to keep track of some things.  We keep track manually here but
        // there is not currently a visual representation of the highlight.

        // Check we're in an editable state:
        if (this.isEditable() && !this.textProperty().isBound() && !this.isDisabled()) {

            // Committed text is the final bit that the user might select from the on-screen IME keyboard,
            // or the plain English bit by pressing a key.
            // Insert committed text
            if (event.getCommitted().length() != 0) {
                final int start = imStart;
                final int end = imStart + imLength;
                // Must call super so we don't do any extra Stride processing:
                super.replaceText(start, end, event.getCommitted());
                // Set imstart and imlength back to having no selection:
                imStart = -1;
                imLength = 0;
                // I don't think committed and composed can both be there in one event, so return:
                return;
            }

            // If this is the first part of a composed text, set imstart,
            // and delete any existing selection:
            if (imStart == -1)
            {
                delegate.deleteSelection();
                imStart = this.getCaretPosition();
            }
            // Work out the full composed text:
            StringBuilder composed = new StringBuilder();
            for (InputMethodTextRun run : event.getComposed()) {
                composed.append(run.getText());
            }
            // Replace the IM section with the latest text:
            // Must call super so we don't do any extra Stride processing:
            super.replaceText(imStart, imStart + imLength, composed.toString());
            // Update imlength to the new composed length:
            imLength = composed.length();
        }
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectNextWord()
    {
        nonIMEdit();
        if (!delegate.selectNextWord(delegateId))
            super.selectNextWord();
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectEndOfNextWord()
    {
        nonIMEdit();
        if (!delegate.selectNextWord(delegateId))
            super.selectEndOfNextWord();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectPreviousWord()
    {
        nonIMEdit();
        if (!delegate.selectPreviousWord(delegateId))
            super.selectPreviousWord();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectAll()
    {
        nonIMEdit();
        if (!delegate.selectAll(delegateId))
            super.selectAll();
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void home()
    {
        nonIMEdit();
        delegate.deselect();
        if (!delegate.home(delegateId))
            super.home();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void end()
    {
        nonIMEdit();
        delegate.deselect();
        if (!delegate.end(delegateId, inNextWord))
            super.end();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectHome()
    {
        nonIMEdit();
        if (!delegate.selectHome(delegateId, getCaretPosition()))
            super.selectHome();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void selectEnd()
    {
        nonIMEdit();
        if (!delegate.selectEnd(delegateId, getCaretPosition()))
            super.selectEnd();
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void paste()
    {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString())
        {
            nonIMEdit();
            delegate.deleteSelection();
            insertText(getCaretPosition(), clipboard.getString());
        }
    }

    public double calculateSceneX(int caretPos)
    {
        double borderLeft = 0;
        if (getBorder() != null && getBorder().getInsets() != null)
            borderLeft = getBorder().getInsets().getLeft();
        double localX = getPadding().getLeft() + borderLeft +
                JavaFXUtil.measureString(this, getText().substring(0, Math.min(caretPos, getText().length())), true, false);
        // Alternatively above, rather than true, false, we could pass:
        // caretPos != 0, caretPos == getLength()
        // to make the positions extend to the edge of the text field when looking up the edge.
        return localToScene(localX, 0).getX();
    }
}
