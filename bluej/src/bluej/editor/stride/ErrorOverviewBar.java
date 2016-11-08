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
package bluej.editor.stride;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import bluej.Config;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

/**
 * The bar down the right-hand side of the code that shows small red rectangles where the errors are.
 */
public class ErrorOverviewBar extends VBox
{
    /**
     * The overall state of the code.  EDITING means there are fresh frames and thus there
     * could be errors, but we're not showing them yet if there are.
     * NO_ERRORS and ERRORS are the other two self-explanatory states when there are no fresh frames.
     */
    public static enum ErrorState { NO_ERRORS, ERRORS, EDITING }

    /**
     * The small icon at the top of the error bad, indicating the ErrorState
     */
    private final Label status;
    /**
     * The rest of the bar, that houses the red rectangles
     */
    private final ErrorsBar bar;
    /**
     * Link to the editor
     */
    private final InteractionManager editor;
    /**
     * The code pane, used to map heights of errors into heights within the scroll pane
     */
    private final Pane codeContainer;

    private final IntegerProperty showingCount = new SimpleIntegerProperty(0);

    /**
     *
     * @param editor
     * @param codeContainer
     * @param nextError The code to execute in order to move focus to the next error
     */
    public ErrorOverviewBar(InteractionManager editor, Pane codeContainer, FXRunnable nextError)
    {
        this.editor = editor;
        this.codeContainer = codeContainer;
        bar = new ErrorsBar();
        status = new Label();
        JavaFXUtil.addStyleClass(status, "error-overview-bar-status");
        status.setOnMouseClicked(e -> {nextError.run(); e.consume();});
        JavaFXUtil.addStyleClass(this, "error-overview-bar-pane");
        getChildren().addAll(status, bar);
        VBox.setVgrow(bar, Priority.ALWAYS);
    }

    /**
     * Information about an error in the code
     */
    public static class ErrorInfo
    {
        private final String message;
        /**
         * The Node within the code that corresponds to this error.  Used for working out the vertical
         * position of the error rectangle on the error bar.
         */
        private final Node node;
        /**
         * Callback to give focus to this error (e.g. when the error rectangle is clicked)
         */
        private final FXPlatformRunnable giveFocus;
        private final ObservableBooleanValue visible;
        private final ObservableBooleanValue focused;
        ErrorInfo(String message, Node node, ObservableBooleanValue visible, ObservableBooleanValue focused, FXPlatformRunnable giveFocus)
        {
            this.message = message;
            this.node = node;
            this.visible = visible;
            this.focused = focused;
            this.giveFocus = giveFocus;
        }

        public boolean isVisible()
        {
            return visible.get();
        }
    }
    
    public void update(List<ErrorInfo> errors, ErrorState state)
    {
        bar.clear();
        errors.forEach(bar::add);
        updateShowingCount();
        setState(state);
    }

    private void setState(ErrorState state)
    {
        switch (state)
        {
            case ERRORS:
                status.setText("\u2717");
                status.setTooltip(new Tooltip("" + showingCount.get() + " " + Config.getString(showingCount.get() == 1 ? "frame.error.overview.bar.error.single" : "frame.error.overview.bar.error.plural")));
                break;
            case NO_ERRORS:
                status.setText("\u2713");
                status.setTooltip(new Tooltip(Config.getString("frame.error.overview.bar.error.none")));
                break;
            case EDITING:
                status.setText("\u270e");
                status.setTooltip(new Tooltip(Config.getString("frame.error.overview.bar.error.editing")));
                break;
        }
        JavaFXUtil.selectPseudoClass(status, state.ordinal(), "bj-success", "bj-failure", "bj-editing");
    }

    private void updateShowingCount()
    {
        showingCount.set(bar.calculateShowing());
        // Errors only ever switch from not-visible to visible:
        if (showingCount.get() > 0)
        {
            setState(ErrorState.ERRORS);
        }
    }

    public ReadOnlyIntegerProperty showingCount()
    {
        return showingCount;
    }


    private class ErrorsBar extends Pane
    {
        public void clear()
        {
            getChildren().forEach(x -> ((Error)x).cleanup());
            getChildren().clear();
        }
        
        public void add(ErrorInfo info)
        {
            // Need to calculate position of error (don't need to bind as will be re-added
            // each time code is changed)
            
            double posTop = codeContainer.sceneToLocal(info.node.localToScene(0, 0)).getY() / codeContainer.getHeight();
            //double posBottom = codeContainer.sceneToLocal(ref.localToScene(0, ref.getHeight())).getY() / codeContainer.getHeight();
            
            Error e = new Error(info.message, info.focused, info.visible, info.giveFocus);
            e.visibleProperty().bind(info.visible);
            e.setManaged(false);
            e.setX(1.0);
            e.setHeight(8.0);
            e.setY(posTop * getHeight());
            e.setWidth(getWidth() - 2.0);
            getChildren().add(e);
        }

        public int calculateShowing()
        {
            return (int)getChildren().stream().filter(x -> x.visibleProperty().get()).count();
        }
    }
    
    private class Error extends Rectangle implements ChangeListener<Boolean>
    {
        private final ObservableBooleanValue focused;
        private final ObservableBooleanValue visible;

        public Error(String message, ObservableBooleanValue focused, ObservableBooleanValue visible, FXPlatformRunnable onClick)
        {
            JavaFXUtil.addStyleClass(this, "error-overview-error");
            setOnMouseClicked(e -> {onClick.run(); e.consume();});
            this.focused = focused;
            this.visible = visible;
            this.focused.addListener(this);
            this.visible.addListener(this);
            Tooltip.install(this, new Tooltip(message));
        }
        
        public void cleanup()
        {
            focused.removeListener(this);
            visible.removeListener(this);
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable,
                Boolean oldValue, Boolean newValue)
        {
            if (observable == focused)
                JavaFXUtil.setPseudoclass("bj-showing", newValue, this);
            else if (observable == visible)
                updateShowingCount();
        }
    }
}
