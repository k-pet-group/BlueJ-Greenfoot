/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017 Michael Kölling and John Rosenberg
 
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.generic;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.frames.TopLevelFrame;
import bluej.utility.javafx.ScalableHeightLabel;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.slots.TextOverlayPosition;
import bluej.stride.generic.Frame.View;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ScrollFreeTextArea;
import bluej.utility.javafx.SharedTransition;
import javafx.scene.layout.BorderPane;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A custom text area for documentation comment of a class, method, etc.
 * @author Fraser McKay
 */
public class DocumentationTextArea extends ScrollFreeTextArea implements EditableSlot, FrameContentItem
{
    private Frame frameParent;
    private View curView = View.NORMAL;
    // Assume Javadoc comment:
    private final ScalableHeightLabel previewCommentStart = new ScalableHeightLabel("/**", true);
    private final ScalableHeightLabel previewCommentEnd = new ScalableHeightLabel("*/", true);
    private final BorderPane wrapper;

    private boolean hacking;

    public DocumentationTextArea(InteractionManager editor, Frame frameParent, FocusParent<? super DocumentationTextArea> focusParent, final String stylePrefix)
    {
        this(editor, frameParent, focusParent, stylePrefix, null);
    }
    /**
     * If enterAction is not null, pressing Enter will trigger it, and shift-Enter will add newline instead.
     */
    public DocumentationTextArea(InteractionManager editor, Frame frameParent, FocusParent<? super DocumentationTextArea> focusParent, final String stylePrefix, FXRunnable enterAction)
    {
        super(editor);
        wrapper = new BorderPane(super.getNode());
        JavaFXUtil.addStyleClass(wrapper, "documentation-text-wrapper", stylePrefix + "documentation-text-wrapper");
        this.frameParent = frameParent;
        addTextStyleClasses("documentation-text", stylePrefix + "documentation-text");
        // If we are the top-level documentation frame, add an image preview on the right:
        if (frameParent instanceof TopLevelFrame)
        {
            ImageView classImage = editor.makeClassImageView();
            if (classImage != null)
            {
                BorderPane.setMargin(classImage, new Insets(8));
                classImage.setOpacity(0.6);
                wrapper.setRight(classImage);
            }
        }
        
        //maxHeightProperty().bind(cssMaxHeightProperty);
        setFocusTraversable(true);
        editor.setupFocusableSlotComponent(this, super.getNode(), false, Collections::emptyList, Collections.emptyList());
        textProperty().addListener((e, oldValue, newValue) -> {
            if (!hacking) editor.modifiedFrame(frameParent, false);
        });

        // TOSO set insets to new Insets(0, 6, 4, 6)

        JavaFXUtil.addStyleClass(previewCommentStart, "preview-slashstar");
        JavaFXUtil.addStyleClass(previewCommentEnd, "preview-slashstar");
        wrapper.setTop(previewCommentStart);
        wrapper.setBottom(previewCommentEnd);
        
        // Originally I tried binding to textProperty().isEqualTo("") but that seemed weirdly flakey
        // (on Mac OS X, 8u20).  So we add a listener direct to the text property:
        setPseudoclass("bj-blank", true); // We are blank to begin with
        textProperty().addListener((a, b, c) -> {
            setPseudoclass("bj-blank", getText().equals(""));
        });
        
        // Make tab move focus, not insert a tab:
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                switch(event.getCode()) {
                    case TAB:
                        if (event.isShiftDown()) {
                            focusParent.focusLeft(this);
                        }
                        else {
                            focusParent.focusRight(this);
                        }
                        event.consume();
                        break;
                
                    case UP:
                        if (getCaretPosition() != 0)
                        {
                            final int oldPos = getCaretPosition();
                            JavaFXUtil.runAfterCurrent(() -> {
                                if (getCaretPosition() == oldPos)
                                {
                                    // So if user pressed up, but caret hasn't moved,
                                    // they were on the top line, and we want to focus up:
                                    focusParent.focusUp(this, true);
                                }
                            });
                            // Do not consume event; want it to still be processed
                        }
                        // Fall-through:
                    case LEFT:
                        if (caretPositionProperty().get() == 0) {
                            focusParent.focusUp(this, true);
                            event.consume();
                        }
                        break;

                    case DOWN:
                        if (getCaretPosition() != getLength())
                        {
                            final int oldPos = getCaretPosition();
                            JavaFXUtil.runAfterCurrent(() -> {
                                if (getCaretPosition() == oldPos)
                                {
                                    // So if user pressed down, but caret hasn't moved,
                                    // they were on the last line, and we want to focus down:
                                    focusParent.focusDown(this);
                                }
                            });
                            // Do not consume event; want it to still be processed
                        }
                        // Fall-through:
                    case RIGHT:    
                        if (caretPositionProperty().get() == getLength()) {
                            focusParent.focusDown(this);
                            event.consume();
                        }
                        break;
                    case ENTER:
                        // Any Ctrl/Cmd/Shift modifier inserts newline anyway:
                        if (event.isShiftDown() || event.isControlDown() || event.isShortcutDown())
                        {
                            insertAtCaret("\n");
                            event.consume();
                        }
                        else
                        {
                            if (enterAction != null) {
                                enterAction.run();
                                event.consume();
                            }
                        }
                        break;
        }});
    }


    @Override
    public void requestFocus(Focus on)
    {
        requestFocus();
        if (on == Focus.LEFT) {
            positionCaret(0);
        }
        else if (on == Focus.RIGHT) {
            positionCaret(getLength());
        }
        else if (on == Focus.SELECT_ALL) {
            selectAll();
        }
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return FXCollections.observableArrayList(getNode());
    }

    @Override
    public TextOverlayPosition getOverlayLocation(int caretPos, boolean javaPos)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeOldErrors()
    {
    }

    @Override
    public void flagErrorsAsOld()
    {       
    }

    @Override
    public void cleanup()
    {        
    }

    @Override
    public void addError(CodeError err)
    {
    }
    
    @Override
    public void focusAndPositionAtError(CodeError err)
    {
    }
    
    @Override
    public Stream<CodeError> getCurrentErrors()
    {
        return Stream.empty();
    }

    @Override
    public void addUnderline(Underline u)
    {        
    }

    @Override
    public void removeAllUnderlines()
    {
    }

    @Override
    public void saved()
    {   
    }

    @Override
    public int getFocusInfo()
    {
        return getCaretPosition();
    }

    @Override
    public Node recallFocus(int info)
    {
        positionCaret(info);
        return getNode();
    }

    @Override
    public List<? extends PossibleLink> findLinks()
    {
        // No links in documentation (at the moment; could add in future)
        return Collections.emptyList();
    }
    
    @Override
    public Frame getParentFrame()
    {
        return frameParent;
    }
    
    @Override
    public void lostFocus()
    {
        
    }
    
    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        setDisable(newView != View.NORMAL);

        // Do we need to collapse documentation?
        if (newView == View.BIRDSEYE_NODOC)
        {
            // The only view in which we show no documentation, and thus we must
            // have been showing documentation before:
            shrinkToNothingUsing(animate);
        }
        // Were we previously collapsed?
        else if (oldView == View.BIRDSEYE_NODOC)
        {
            // vice versa:
            growFromNothingUsing(animate);
        }
        
        if (newView == View.JAVA_PREVIEW && (getParentFrame() == null || getParentFrame().isFrameEnabled()))
        {
            previewCommentStart.growToFullHeightWith(animate, true);
            previewCommentEnd.growToFullHeightWith(animate, true);
            wrapper.setSnapToPixel(false);
        }
        else if (curView == View.JAVA_PREVIEW)
        {
            previewCommentStart.shrinkToNothingWith(animate, true);
            previewCommentEnd.shrinkToNothingWith(animate, true);
            animate.addOnStopped(() -> wrapper.setSnapToPixel(true));
        }
        
        curView = newView;
    }

    public String getJavadocs(String prefix)
    {
        String out = "";
        if (! getText().isEmpty()) {
            String[] lines = getText().split("\n");
            for (int i = 0; i < lines.length; i++) {
                out += prefix + " * " + lines[i] + "\n";
            }
        }
        return out;
    }

    @OnThread(Tag.FXPlatform)
    public void hackFixSizing()
    {
        // It seems that with a parent FlowPane, ScrollFreeTextArea doesn't set its height correctly on first load.
        // The following is a dirty hack to fix the sizing in this case:
        hacking = true;
        String s = getText();
        setText("");
        setText(s);
        hacking = false;
    }

    @Override
    public JavaFragment getSlotElement()
    {
        return null;
    }

    @Override
    public boolean isAlmostBlank()
    {
        return getText().trim().isEmpty();
    }

    @Override
    public Optional<FrameCanvas> getCanvas()
    {
        return Optional.empty();
    }

    @Override
    public Stream<HeaderItem> getHeaderItemsDeep()
    {
        return Stream.of(this);
    }

    @Override
    public Stream<HeaderItem> getHeaderItemsDirect()
    {
        return Stream.of(this);
    }

    @Override
    public boolean focusTopEndFromPrev()
    {
        requestFocus(Focus.LEFT);
        return true;
    }

    @Override
    public boolean focusBottomEndFromNext()
    {
        requestFocus(Focus.LEFT); // Focus start anyway
        return true;
    }

    @Override
    public boolean focusLeftEndFromPrev()
    {
        requestFocus(Focus.LEFT);
        return true;
    }

    @Override
    public boolean focusRightEndFromNext()
    {
        requestFocus(Focus.RIGHT);
        return true;
    }

    @Override
    public boolean isEditable()
    {
        return !isDisable();
    }

    @Override
    public void setEditable(boolean editable)
    {
        setDisable(!editable);
    }

    @Override
    public Node getNode()
    {
        return wrapper;
    }

    public void setDocComment(boolean docComment)
    {
        previewCommentStart.setText(docComment ? "/**" : "/*");
    }

    @Override
    public ObservableBooleanValue effectivelyFocusedProperty()
    {
        // No dropdowns etc, so effectively-focused just means focused:
        return focusedProperty();
    }

    @Override
    public int calculateEffort()
    {
        return getText().length();
    }
}
