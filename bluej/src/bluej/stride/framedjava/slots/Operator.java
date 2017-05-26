/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 
 
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

import java.util.stream.Stream;

import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * An operator is a read-only operator label (e.g. +, *, >>, etc) in an
 * expression slot.
 */
// Package visible
class Operator
{
    /** The label showing the operator */
    private final Label l;
    /**
     * The operator in the Stride source.
     * The Label can show different to sourceProperty, when we are doing Java preview.
     */
    private final StringProperty sourceProperty = new SimpleStringProperty();
    /** Keeps track of whether we are in Java preview */
    private final BooleanProperty showingJava = new SimpleBooleanProperty();
    /**
     * The text to show for range operator when we are in Java preview mode.
     * For most ranges, this is a comma (since the range becomes a method call)
     * to separate the beginning and end.  But in a for-each loop condition,
     * this can be more complex as it gets turned into part of a Java classic for loop.
     */
    private final StringProperty rangeJavaPreview = new SimpleStringProperty(", ");
    /** The precedence category of the operator.  This is determined relative
     * to other operators around it in the same expression level, not solely
     * by the operator itself. */
    private Precedence precedence;

    public Operator(String op, InfixStructured parent)
    {
        sourceProperty.set(op);
        l = new Label();
        JavaFXUtil.addStyleClass(l, "expression-operator");
        l.setOnMousePressed(e -> {
            parent.moveTo(e.getSceneX(), e.getSceneY(), true);
            e.consume();
        });
        l.setOnMouseDragged(e -> {
            parent.selectTo(e.getSceneX(), e.getSceneY());
            e.consume();
        });
        l.setOnMouseReleased(e -> {
            parent.selected();
            e.consume();
        });
        l.setOnMouseMoved(e -> {
            if (e.isShortcutDown()) parent.getSlot().getOverlay().hoverAtPos(-1);
        });
        l.setOnMouseClicked(MouseEvent::consume);
        l.setOnDragDetected(MouseEvent::consume);
        
        l.textProperty().bind(
            new When(showingJava)
                .then(new When(sourceProperty.isEqualTo("<:")).then(" instanceof ")
                    .otherwise(new When(sourceProperty.isEqualTo("..")).then(rangeJavaPreview)
                        .otherwise(sourceProperty)))
                .otherwise(sourceProperty));
        
        JavaFXUtil.addChangeListener(sourceProperty, s -> updateBreaks());
        updateBreaks();
    }

    private void updateBreaks()
    {
        // Can break before every operator except comma:
        HangingFlowPane.setBreakBefore(l, !sourceProperty.get().equals(","));
    }

    /**
     * Gets the precedence for the given operator.
     * @param op Operator to determine precedence for
     * @param unary Whether this operator is acting as unary (true) or binary (false)
     * @return An integer precedence, higher value binds tighter.
     */
    static int getOperatorPrecedence(String op, boolean unary)
    {
        int prec;
        switch (op)
        {
        case ",": prec = 0; break; // Not an operator as such, but appears in method calls.  Lowest priority.
        case "..": prec = 1; break;
        case "||": prec = 1; break;
        case "&&": prec = 2; break;
        case "|": prec = 3; break;
        case "^": prec = 4; break;
        case "&": prec = 5; break;
        case "=": // Not a valid operator, but can appear before user has completed ==, so give it same precedence as that.
        case "==": case "!=": prec = 6; break;
        case "<": case ">": case ">=": case "<=": case "<:" /*instanceof */: prec = 7; break;
        case "<<": case ">>": case ">>>": prec = 8; break;
        case "+": case "-": prec = unary ? 11 : 9; break;
        case "*": case "/": case "%": prec = 10; break;
        case "~": case "!": prec = 12; break;
        // 13 is cast, TODO
        case "new ": prec = 14; break;
        case ":": // Not a valid operator, but can appear before user has completed ::, so give it same precedence as that.
        case "::": prec = 15; break;
        case ".": prec = 16; break;
        case "->": prec = 17; break;
        default: throw new IllegalStateException("Unknown operator: " + op);
        }
        return prec;
    }

    /**
     * Given a numeric index of precedence group, returns a corresponding Precedence
     * category.
     * @param ourLevel Numeric index of precedence group.  Highest precedence
     *                 operator in the expression gets 0 (all get 0 if joint between
     *                 several operators), next highest group gets 1, then 2, etc.
     * @return
     */
    static Precedence getPrecForLevel(int ourLevel)
    {
        if (ourLevel == 0)
        {
            return Precedence.HIGH;
        }
        else if (ourLevel == 1)
        {
            return Precedence.MEDIUM;
        }
        else
        {
            return Precedence.LOW;
        }
    }
    
    public String getCopyText()
    {
        return sourceProperty.get();
    }
    
    public Node getNode()
    {
        return l;
    }

    /** Gets the source text of the operator */
    public String get()
    {
        return sourceProperty.get();
    }

    /** Sets the source text of the operator */
    public void set(String s)
    {
        sourceProperty.set(s);
    }

    // Needed for testing
    public Precedence getPrecedence()
    {
        return precedence;
    }


    /** Sets the operator precedence and updates visual state accordingly */
    public void setPrecedence(Precedence chosen)
    {
        for (Precedence p : Precedence.values())
            JavaFXUtil.setPseudoclass(p.getPseudoClass(), false, l);
        JavaFXUtil.setPseudoclass(chosen.getPseudoClass(), true, l);
        this.precedence = chosen;
    }

    public Stream<TextOverlayPosition> getStartEndPositions(InfixStructured parent)
    {
        return Stream.of(TextOverlayPosition.nodeToOverlay(l, 0.0, 0.0, l.getBaselineOffset(), l.getHeight()),
                         TextOverlayPosition.nodeToOverlay(l, l.getWidth(), 0.0, l.getBaselineOffset(), l.getHeight()));
    }

    public String getJavaCode()
    {
        if ("<:".equals(get()))
            return " instanceof ";
        else
            // Add spaces around operators (except before comma and dot, to prevent code like {1}+{+2} generating the ++
            // operator by accident.
            if (get().equals(","))
                return ", ";
            else if (get().equals("."))
                return ".";
            else
                return " " + get() + " ";
    }
    
    public void setView(View view, SharedTransition animate)
    {
        // When Java turned off, reset override:
        if (view != View.JAVA_PREVIEW)
            rangeJavaPreview.set(", ");
        showingJava.set(view == View.JAVA_PREVIEW);
        // TODO animate size?
        JavaFXUtil.setPseudoclass("bj-java-preview", view == View.JAVA_PREVIEW, l);
    }

    public void setJavaPreviewRangeOverride(String s)
    {
        rangeJavaPreview.set(s);
    }

    public Node makeDisplayClone(InteractionManager editor)
    {
        return JavaFXUtil.cloneLabel(l, editor.getFontCSS());
    }

    public static enum Precedence
    {
        // DOT is like ULTRA; highest priority, but only used for dots
        // Similarly, COMMA and NEW are like ZERO; lowest priority, but only used for new/commas
        DOT, HIGH, MEDIUM, LOW, NEW, COMMA;
        
        public String getPseudoClass()
        {
            switch (this)
            {
            case DOT: return "bj-op-dot";
            case HIGH: return "bj-op-high";
            case MEDIUM: return "bj-op-medium";
            case LOW: return "bj-op-low";
            case NEW: return "bj-op-new";
            case COMMA: return "bj-op-comma";
            default: return null; // Impossible case
            }
        }
    }

}
