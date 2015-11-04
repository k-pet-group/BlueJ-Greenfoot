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
package bluej.stride.slots;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ScalableHeightLabel;

class Suggestion
{
    public static final Duration FADE_IN_SPEED = Duration.millis(150);
    public static final Duration FADE_OUT_SPEED = Duration.millis(150);
    private final String text;
    private boolean showing = true; // Are we currently visible?
    private final ScalableHeightLabel type;
    private final ScalableHeightLabel prefix;
    private final ScalableHeightLabel matching;
    private final ScalableHeightLabel next; // next char to input
    private final ScalableHeightLabel suffix;
    private final ScalableHeightLabel fixedPostSuffix;
    private final ScalableHeightLabel buttonHint;
    private boolean showingHint;
    private Animation animation;
    private final BorderPane pane;
    
    private List<ScalableHeightLabel> allLabels()
    {
        return Arrays.asList(type, prefix, matching, next, suffix, fixedPostSuffix, buttonHint);
    }
    
    public Suggestion(String text, String unmatchableSuffix, String type, boolean typeMatch, DoubleExpression typeWidth, boolean direct)
    {
        this.text = text;
        this.type = new ScalableHeightLabel(type, false);
        this.type.minWidthProperty().bind(typeWidth);
        this.type.maxWidthProperty().bind(typeWidth);
        JavaFXUtil.selectStyleClass(typeMatch ? 1 : 0, this.type, "suggestion-type-normal", "suggestion-type-match");
        this.prefix = new ScalableHeightLabel("", false);
        this.matching = new ScalableHeightLabel("", false);
        this.next = new ScalableHeightLabel(text.substring(0, 1), false);
        this.suffix = new ScalableHeightLabel(text.substring(1), false);
        this.fixedPostSuffix = new ScalableHeightLabel(unmatchableSuffix, false);
        
        JavaFXUtil.addStyleClass(this.type, "suggestion-type");
        JavaFXUtil.addStyleClass(prefix, "suggestion-prefix");
        JavaFXUtil.addStyleClass(matching, "suggestion-matching");
        JavaFXUtil.addStyleClass(next, "suggestion-next");
        JavaFXUtil.addStyleClass(suffix, "suggestion-suffix");
        prefix.setMinWidth(Region.USE_PREF_SIZE);
        matching.setMinWidth(Region.USE_PREF_SIZE);
        next.setMinWidth(Region.USE_PREF_SIZE);
        // Suffix will be abbreviated if we run out of space:
        suffix.setMinWidth(0.0);
        fixedPostSuffix.setMinWidth(0.0);
        
        this.buttonHint = new ScalableHeightLabel("\u21B5", false);
        JavaFXUtil.addStyleClass(buttonHint, "suggestion-button-hint");
        buttonHint.setMinWidth(Region.USE_PREF_SIZE);
        buttonHint.setOpacity(0);
        showingHint = false;
        
        HBox hbox = new HBox();
        hbox.getChildren().addAll(this.type, prefix, matching, next, suffix, fixedPostSuffix);
        hbox.setSpacing(0);
        
        // By using a BorderPane, buttonHint will always appear,
        // and hbox will shrink if needed (shrinking suffix)
        pane = new BorderPane();
        pane.setCenter(hbox);
        pane.setRight(buttonHint);
        
        JavaFXUtil.addStyleClass(pane, "suggestion", "suggestion-nohighlight", direct ? "suggestion-direct" : "suggestion-similar");
    }
    
    public void setHighlight(boolean on)
    {
        JavaFXUtil.selectStyleClass(on ? 1 : 0, pane, "suggestion-nohighlight", "suggestion-highlight");
        setHintShowing(on, true);
    }

    /**
     * Animates out a suggestion, if it is currently showing
     * @param immediate true to do it straight away, false to animare
     */
    public void animateOut(boolean immediate)
    {
        if (showing)
        {
            if (animation != null)
            {
                animation.stop();
            }
            showing = false;
            if (immediate)
            {
                allLabels().forEach(l -> l.setToNothing());
                pane.setManaged(false);
                pane.setVisible(false);
            }
            else
            {
                animation = new ParallelTransition(
                        allLabels().stream()
                            .map(l -> l.getShrinkToNothingTimeline(FADE_OUT_SPEED))
                            .collect(Collectors.toList()).toArray(new Timeline[0])
                );
                animation.setOnFinished(e -> {
                    pane.setManaged(false);
                    pane.setVisible(false);
                }); 
                animation.play();
            }
        }
    }
    
    /**
     * Animates in a suggestion, if it is not already showing
     * @param immediate true to do it straight away, false to animare
     */
    public void animateIn(boolean immediate)
    {
        if (!showing)
        {
            if (animation != null)
            {
                animation.stop();
            }
            showing = true;
            // If you want to remove it from width calculation of list:
            pane.setManaged(true);
            pane.setVisible(true);
            if (immediate)
            {
                allLabels().forEach(l -> l.setToFullHeight());
            }
            else
            {
                animation = new ParallelTransition(
                        allLabels().stream()
                            .map(l -> l.getGrowToFullHeightTimeline(FADE_IN_SPEED))
                            .collect(Collectors.toList()).toArray(new Timeline[0])
                );
                animation.play();
            }
        }
    }

    public void notifyEligible(int at, int len, boolean canTab, boolean immediate)
    {
        if (canTab)
        {
            setHintShowing(true, immediate);
        }
        else
        {
            setHintShowing(false, immediate);
        }

        prefix.setText(text.substring(0, at));
        int end = Math.min(at + len, text.length());
        matching.setText(text.substring(at, end));
        String rest = text.substring(end);
        if (rest.length() >= 1)
        {
            next.setText(rest.substring(0, 1));
            suffix.setText(rest.substring(1));
        }
        else
        {
            next.setText("");
            suffix.setText("");
        }
    }

    private void setHintShowing(boolean shouldShow, boolean immediate)
    {
        if (showingHint != shouldShow)
        {
            showingHint = shouldShow;
            double targetOpacity = shouldShow ? 1.0 : 0.0;
            if (immediate)
            {
                buttonHint.setOpacity(targetOpacity);
            }
            else
            {
                FadeTransition ft = new FadeTransition(Duration.millis(200), buttonHint);
                ft.setToValue(targetOpacity);
                ft.play();
            }
        }
    }

    public String getText()
    {
        return text;
    }
    
    public Region getNode()
    {
        return pane;
    }
}