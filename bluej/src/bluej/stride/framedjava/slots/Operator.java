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
package bluej.stride.framedjava.slots;

import java.util.stream.Stream;

import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import bluej.stride.generic.Frame.View;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;


// Package visible
class Operator
{
    private final Label l;
    // The Label can show different to sourceProperty, when we are doing Java preview:
    private final StringProperty sourceProperty = new SimpleStringProperty();
    private final BooleanProperty showingJava = new SimpleBooleanProperty();
    private final StringProperty rangeJavaPreview = new SimpleStringProperty(", ");
    private Precedence precedence;

    public Operator(String op, InfixExpression parent)
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

    static boolean canBeUnary(String s)
    {
        if (s == null)
            return false;
        
        switch (s)
        {
            case "+": case "-": case "~": case "!": case "new ":
                return true;
            default:
                return false;
        }
    }

    //package visible for testing
    static boolean isOperator(String s)
    {
        switch (s)
        {
            case "+": case "-": case "*": case "/":
            case "==": case "!=": case ">": case ">=":
            case "<=": case "<": case "%": case "&":
            case "&&": case "|": case "||": case "^":
            case "~": case "!": case ".": case "..": case "<:": case ",":
            case "<<": case ">>": case ">>>":
            case "->": case "::":
                return true;
            default:
                return false;
        }
    }
    
    static boolean beginsOperator(char c)
    {
        switch (c)
        {
            case '+': case '-': case '*': case '/':
            case '=': case '!': case '>': case '<':
            case '%': case '&': case '|': case '^':
            case '~': case '.': case ',': case ':':
                return true;
            default:
                return false;
        }
    }
    
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
        case ".": prec = 15; break;
        default: throw new IllegalStateException("Unknown operator: " + op);
        }
        return prec;
    }

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

    public String get()
    {
        return sourceProperty.get();
    }

    public void set(String s)
    {
        sourceProperty.set(s);
    }

    public Object getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Precedence chosen)
    {
        for (Precedence p : Precedence.values())
            JavaFXUtil.setPseudoclass(p.getPseudoClass(), false, l);
        JavaFXUtil.setPseudoclass(chosen.getPseudoClass(), true, l);
        this.precedence = chosen;
    }

    public Stream<TextOverlayPosition> getStartEndPositions(InfixExpression parent)
    {
        return Stream.of(TextOverlayPosition.nodeToOverlay(l, 0.0, 0.0, l.getBaselineOffset(), l.getHeight()),
                         TextOverlayPosition.nodeToOverlay(l, l.getWidth(), 0.0, l.getBaselineOffset(), l.getHeight()));
    }

    public ObservableStringValue textProperty()
    {
        return sourceProperty;
    }

    public String getJavaCode()
    {
        if ("<:".equals(get()))
            return " instanceof ";
        else
            // Add spaces around operators (except before comma and dor, to prevent code like {1}+{+2} generating the ++
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

    static class OpPrec
    {
        int prec;
        int levels;
        OpPrec(int prec, int levels)
        {
            this.prec = prec;
            this.levels = levels;
        }
    }
}
