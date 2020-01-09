/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2018,2020 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.pkgmgr.target.role.Kind;
import javafx.beans.binding.StringExpression;
import javafx.scene.Node;

import bluej.Config;
import bluej.editor.stride.FrameCatalogue;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.frames.CodeFrame;
import bluej.stride.framedjava.frames.ReturnFrame;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SuggestedFollowUpDisplay;
import bluej.stride.slots.CopyableHeaderItem;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.utility.Utility;
import bluej.utility.javafx.FXBiConsumer;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 22/05/2016.
 */
public class TypeSlot extends StructuredSlot<TypeSlotFragment, InfixType, TypeCompletionCalculator> implements CopyableHeaderItem
{
    private final InteractionManager editor;
    private boolean isReturnType = false;
    private final List<FXSupplier<Boolean>> backspaceListeners = new ArrayList<>();
    private final List<FXSupplier<Boolean>> deleteListeners = new ArrayList<>();
    
    public static enum Role
    {
        /** Declaring arbitrary variable; can be any type */
        DECLARATION,
        /** Return type; like DECLARATION but could also be void */
        RETURN,
        /** Extends; must be a non-final class (not interface or primitive) */
        EXTENDS,
        /** Must be an interface */
        INTERFACE,
        /** Throws or Catch; must be a throwable */
        THROWS_CATCH;
    }
    
    public TypeSlot(InteractionManager editor, Frame parentFrame, CodeFrame<?> parentCodeFrame, FrameContentRow row, Role role, String stylePrefix)
    {
        super(editor, parentFrame, parentCodeFrame, row, stylePrefix, calculatorForRole(editor, role), hintsForRole(role));
        this.editor = editor;
        onTextPropertyChangeOld(this::adjustReturnFrames);
    }

    private static TypeCompletionCalculator calculatorForRole(InteractionManager editor, Role role)
    {
        switch (role)
        {
            case THROWS_CATCH:
                return new TypeCompletionCalculator(editor, Throwable.class);
            case INTERFACE:
                return new TypeCompletionCalculator(editor, Kind.INTERFACE);
            case EXTENDS:
                return new TypeCompletionCalculator(editor, Kind.CLASS_NON_FINAL);
        }
        return new TypeCompletionCalculator(editor);
    }

    private static List<FrameCatalogue.Hint> hintsForRole(Role role)
    {
        FrameCatalogue.Hint hintInt = new FrameCatalogue.Hint("int", "An integer (whole number)");
        FrameCatalogue.Hint hintDouble = new FrameCatalogue.Hint("double", "A number value");
        FrameCatalogue.Hint hintVoid = new FrameCatalogue.Hint("void", "No return");
        FrameCatalogue.Hint hintString = new FrameCatalogue.Hint("String", "Some text");
        FrameCatalogue.Hint hintActor = new FrameCatalogue.Hint("Actor", "A Greenfoot actor");
        FrameCatalogue.Hint hintList = new FrameCatalogue.Hint("List<String>", "A list of String");
        FrameCatalogue.Hint hintObj = Config.isGreenfoot() ? hintActor : hintList;
        FrameCatalogue.Hint hintIO = new FrameCatalogue.Hint("IOException", "An IO exception");
        switch (role)
        {
            case DECLARATION:
                return Arrays.asList(hintInt, hintDouble, hintString, hintObj);
            case RETURN:
                return Arrays.asList(hintInt, hintDouble, hintString, hintVoid);
            case EXTENDS:
                if (Config.isGreenfoot())
                    return Arrays.asList(hintActor);
                else
                    return Collections.emptyList();
            case INTERFACE:
                return Collections.emptyList();
            case THROWS_CATCH:
                return Arrays.asList(hintIO);
        }
        return Collections.emptyList();
    }

    @Override
    protected TypeSlotFragment makeSlotFragment(String content, String javaCode)
    {
        return new TypeSlotFragment(content, javaCode, this);
    }

    @Override
    public ExpressionSlot asExpressionSlot() { return null; }

    public void setText(TypeSlotFragment rhs)
    {
        rhs.registerSlot(this);
        setText(rhs.getContent());        
    }

    @Override
    protected InfixType newInfix(InteractionManager editor, ModificationToken token)
    {
        return new InfixType(editor, this, token);
    }

    public void markReturnType()
    {
        isReturnType = true;
    }
    
    private void adjustReturnFrames(String oldValue, String newValue)
    {
        if (!isReturnType)
            return;
        
        if ((oldValue.equals("void") || oldValue.equals("")) && !(newValue.equals("void") || newValue.equals("")))
        {
            // Added a return type; need to go through and add empty slots for all returns that don't have them:
            for (Frame f : Utility.iterableStream(getParentFrame().getAllFrames()))
            {
                if (f instanceof ReturnFrame)
                {
                    ReturnFrame rf = (ReturnFrame) f;
                    rf.showValue();
                }
            }
        }
        else if (!oldValue.equals("void") && newValue.equals("void"))
        {
            // Removed a return type; prompt about removing return values from all returns
            List<FXRunnable> removeActions = getParentFrame().getAllFrames()
                .filter(f -> f instanceof ReturnFrame)
                .map(f -> (ReturnFrame)f)
                .map(rf -> rf.getRemoveFilledValueAction())
                .filter(a -> a != null)
                .collect(Collectors.toList());

            if (!removeActions.isEmpty())
            {
                JavaFXUtil.runNowOrLater(() -> {
                    SuggestedFollowUpDisplay disp = new SuggestedFollowUpDisplay(editor, "Return type changed to void.  Would you like to remove return values from all return frames in this method?", () -> removeActions.forEach(FXRunnable::run));
                    disp.showBefore(getComponents().get(0));
                });
            }
        }
    }

    @Override
    public void saved()
    {
        
    }

    @Override
    public boolean canCollapse()
    {
        // Type slots can never be collapsed:
        return false;
    }

    @Override
    public List<PossibleTypeLink> findLinks()
    {
        return topLevel.findTypeLinks();
    }
    
    public StringExpression javaProperty()
    {
        // Java is the text until we sort out wildcards:
        return textMirror;
    }

    /**
     * An action to call when a comma is inserted at the top-level of
     * the type slot (i.e. "HashMap<Int,>" would not trigger this because
     * it's inside a generic sub-structured item).  Passes the text
     * before the comma and after the comma.
     */
    public void onTopLevelComma(FXBiConsumer<String, String> listener)
    {
        afterModify.add(() -> {
            topLevel.runIfCommaDirect(listener);
        });
    }

    @Override
    public @OnThread(Tag.FXPlatform) boolean backspaceAtStart()
    {
        // Must make sure that we run all listeners, and not short-circuit because one returned true:
        boolean transferredFocus = Utility.mapList(backspaceListeners, FXSupplier::get).stream().reduce(false, (a, b) -> a || b); 
        if (transferredFocus)
            return true;
        else
            return super.backspaceAtStart();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean deleteAtEnd()
    {
        // Must make sure that we run all listeners, and not short-circuit because one returned true:
        boolean transferredFocus = Utility.mapList(deleteListeners, FXSupplier::get).stream().reduce(false, (a, b) -> a || b);
        if (transferredFocus)
            return true;
        else
            return super.deleteAtEnd();
    }
    
    // Should return true if focus has been transferred out of the slot
    public void addBackspaceAtStartListener(FXSupplier<Boolean> listener)
    {
        backspaceListeners.add(listener);
    }

    // Should return true if focus has been transferred out of the slot
    public void addDeleteAtEndListener(FXSupplier<Boolean> listener)
    {
        deleteListeners.add(listener);
    }

    @Override
    public Stream<? extends Node> makeDisplayClone(InteractionManager editor)
    {
        return topLevel.makeDisplayClone(editor);
    }
}
