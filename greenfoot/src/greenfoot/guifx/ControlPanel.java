/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018,2019  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of f
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.guifx;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.core.Simulation;
import greenfoot.guifx.GreenfootStage.State;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.List;

/**
 * The control panel in GreenfootStage: the act/run/reset buttons,
 * speed slider and execution twirler that appear at the bottom
 * left of the window.
 */
@OnThread(Tag.FXPlatform)
public class ControlPanel extends GridPane
{
    private static final String PAUSE_BUTTON_TEXT = Config.getString("controls.pause.button");
    private static final String RUN_BUTTON_TEXT = Config.getString("controls.run.button");
    private static final String RUN_BUTTON_TOOLTIP_TEXT = Config.getString("controls.run.shortDescription");
    private static final String PAUSE_BUTTON_TOOLTIP_TEXT = Config.getString("controls.pause.shortDescription");
    
    private final Node run_icon = makeRunIcon();
    private final Node pause_icon = makePauseIcon();
    private final Node act_icon = makeActIcon();
    private final Node reset_icon = makeResetIcon();
    
    private final Button actButton;
    private final Button runPauseButton;
    private final Label speedLabel;
    private final Slider speedSlider;

    private final ControlPanelListener listener;

    private final BooleanProperty actDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty resetDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty runDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty pauseDisabled = new SimpleBooleanProperty(true);
    private final BooleanBinding runPauseDisabled = runDisabled.and(pauseDisabled);

    /**
     * Make a new control panel.
     * 
     * @param greenfootStage The GreenfootStage holding this control panel
     * @param executionTwirler The execution twirler.  This class does nothing
     *                         with it apart from add it to the panel; its state
     *                         is managed by GreenfootStage.
     */
    public ControlPanel(ControlPanelListener listener, Node executionTwirler)
    {
        this.listener = listener;
        actButton = unfocusableButton(Config.getString("run.once"));
        actButton.setTooltip(new Tooltip(Config.getString("controls.runonce.shortDescription")));
        actButton.setGraphic(act_icon);
        runPauseButton = unfocusableButton(RUN_BUTTON_TEXT);
        runPauseButton.setGraphic(run_icon);
        runPauseButton.setTooltip(new Tooltip(RUN_BUTTON_TOOLTIP_TEXT));
        Button resetButton = unfocusableButton(Config.getString("reset.world"));
        resetButton.setTooltip(new Tooltip(Config.getString("controls.reset.shortDescription")));
        resetButton.setGraphic(reset_icon);
        actButton.disableProperty().bind(actDisabled);
        runPauseButton.disableProperty().bind(runPauseDisabled);
        resetButton.disableProperty().bind(resetDisabled);
        for (Button button : Arrays.asList(actButton, runPauseButton, resetButton))
        {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        int min = 0;
        int max = Simulation.MAX_SIMULATION_SPEED;
        speedSlider = new Slider() {
            @Override
            @OnThread(Tag.FX)
            public void requestFocus()
            {
                // Not focusable
            }
        };
        speedSlider.setValue(min + (max - min) / 2);
        speedSlider.setShowTickLabels(false);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMin(min);
        speedSlider.setMax(max);
        speedSlider.setMajorTickUnit( max / 2 );
        speedSlider.setMinorTickCount(1);
        speedSlider.setBlockIncrement(20);
        speedSlider.setTooltip(new Tooltip(Config.getString("controls.speedSlider.tooltip")));
        speedSlider.setFocusTraversable(false);
        speedSlider.setMaxWidth(150.0);
        speedSlider.setDisable(true);

        actButton.setOnAction(e -> this.listener.act());
        runPauseButton.setOnAction(e -> this.listener.doRunPause());
        resetButton.setOnAction(e -> this.listener.userReset());
        // Note - if you alter this listener code, make sure to check notifySimulationSpeed() as well:
        JavaFXUtil.addChangeListenerPlatform(speedSlider.valueProperty(),
            newSpeed -> this.listener.setSpeedFromSlider(newSpeed.intValue()));
        
        TilePane controlPanel = new TilePane(actButton, runPauseButton, resetButton);
        controlPanel.setPrefColumns(3);
        controlPanel.getStyleClass().add("buttons-panel");
        controlPanel.setAlignment(Pos.CENTER);
        speedLabel = new Label(Config.getString("controls.speed.label"));
        // Let speed label shrink to nothing if not enough space:
        speedLabel.setMinWidth(0.0);
        speedLabel.setDisable(true);
        add(controlPanel, 0, 0);
        GridPane speedAndTwirler = new GridPane();
        speedAndTwirler.add(speedLabel, 0, 0);
        speedAndTwirler.add(speedSlider, 1, 0, 2, 1);
        if (executionTwirler != null)
        {
            speedAndTwirler.add(executionTwirler, 3, 0);
        }
        speedAndTwirler.getStyleClass().add("speed-panel");
        GridPane.setHalignment(speedLabel, HPos.RIGHT);
        GridPane.setValignment(speedSlider, VPos.BOTTOM);
        // Need some space above speed slider to stop it looking out of alignment:
        GridPane.setMargin(speedSlider, new Insets(8, 0, 0, 0));
        speedLabel.setAlignment(Pos.BASELINE_RIGHT);
        GridPane.setHgrow(speedLabel, Priority.ALWAYS);
        GridPane.setMargin(speedAndTwirler, new Insets(8, 8, 0, 0));
        add(speedAndTwirler, 1, 0);
        GridPane.setHalignment(speedAndTwirler, HPos.CENTER);
        ColumnConstraints leftHalf = new ColumnConstraints();
        ColumnConstraints rightHalf = new ColumnConstraints();
        leftHalf.setPercentWidth(60);
        rightHalf.setPercentWidth(40);
        getColumnConstraints().setAll(leftHalf, rightHalf);
        getStyleClass().add("control-panel");
    }

    /**
     * Makes a button with the given text, which cannot be focused.
     */
    private static Button unfocusableButton(String text)
    {
        return new Button(text) {
            @Override
            @OnThread(Tag.FX)
            public void requestFocus()
            {
                // Not focusable
            }
        };
    }

    /**
     * Called by GreenfootStage to update the state of our buttons.
     */
    public void updateState(State newState, boolean atBreakpoint)
    {
        actDisabled.setValue(newState != State.PAUSED || atBreakpoint);
        runDisabled.setValue(newState != State.PAUSED || atBreakpoint);
        pauseDisabled.setValue(newState != State.RUNNING || atBreakpoint);
        resetDisabled.setValue(newState == State.NO_PROJECT);
        speedSlider.setDisable(newState == State.NO_PROJECT);
        speedLabel.setDisable(newState == State.NO_PROJECT);

        boolean showingPause = newState == State.RUNNING || newState == State.RUNNING_REQUESTED_PAUSE;
        if (showingPause)
        {
            // Only change button text and tooltip if needed; changing the tooltip to another
            // tooltip with the same text causes it to disappear needlessly if the user is currently viewing it:
            if (!runPauseButton.getText().equals(PAUSE_BUTTON_TEXT))
            {
                runPauseButton.setGraphic(pause_icon);
                runPauseButton.setText(PAUSE_BUTTON_TEXT);
                runPauseButton.setTooltip(new Tooltip(PAUSE_BUTTON_TOOLTIP_TEXT));
            }
        }
        else
        {
            // Ditto: only change text and tooltip if needed
            if (!runPauseButton.getText().equals(RUN_BUTTON_TEXT))
            {
                runPauseButton.setGraphic(run_icon);
                runPauseButton.setText(RUN_BUTTON_TEXT);
                runPauseButton.setTooltip(new Tooltip(RUN_BUTTON_TOOLTIP_TEXT));
            }
        }
    }

    /**
     * Change the speed slider's value
     */
    public void setSpeed(int simSpeed)
    {
        speedSlider.setValue(simSpeed);
    }

    /**
     * Locks the controls: hides the act button and the speed slider.
     */
    public void lockControls()
    {
        actButton.setVisible(false);
        speedSlider.setVisible(false);
        speedLabel.setVisible(false);
    }
    
    /**
     * Make the act/run/pause/reset menu items for GreenfootStage.
     * They are made here because their disabled state is managed by ControlPanel.
     */
    public List<MenuItem> makeMenuItems()
    {
        return Arrays.asList(
        JavaFXUtil.makeMenuItem("run.once", getIcon("step.png"),
                new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN),
                listener::act, actDisabled),
                JavaFXUtil.makeMenuItem("controls.run.button", getIcon("run.png"),
                        new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN),
                        listener::doRunPause, runDisabled),
                JavaFXUtil.makeMenuItem("controls.pause.button", getIcon("pause.png"),
                        new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN,
                                KeyCombination.SHIFT_DOWN),
                        listener::doRunPause, pauseDisabled),
                JavaFXUtil.makeMenuItem("reset.world", getIcon("reset.png"),
                        new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN),
                        listener::userReset, resetDisabled)
        );
    }

    /**
     * Loads an image from resources and put it in an imageView to be used as an icon.
     *
     * @param fileName The file name of the image
     * @return An image view containing the image.
     */
    private ImageView getIcon(String fileName)
    {
        return new ImageView(new Image(getClass().getClassLoader().getResourceAsStream(fileName)));
    }

    /**
     * A listener for when the user uses the controls
     */
    public static interface ControlPanelListener
    {
        /**
         * Act button pressed: run one frame
         */
        void act();

        /**
         * Run/pause pressed: flip the run/paused state
         */
        void doRunPause();

        /**
         * Reset pressed: reset the simulation
         */
        void userReset();

        /**
         * The user has moved the speed slider.
         * @param speed The new speed.
         */
        void setSpeedFromSlider(int speed);
    }
    
    /**
     * The green &gt; symbol for act.
     * Currently, can't be used in a menuItem as JavaFX doesn't deal
     * with this type of nodes properly on menuItem, at least on Mac.
     */
    @OnThread(Tag.FXPlatform)
    private static Node makeActIcon()
    {
        return JavaFXUtil.withStyleClass(
                new Polyline(
                    0, 0,
                    12, 5,
                    0, 10
                ),
                "act-icon");
    }

    /**
     * The green triangle symbol for run.
     * Currently, can't be used in a menuItem as JavaFX doesn't deal
     * with this type of nodes properly on menuItem, at least on Mac.
     */
    @OnThread(Tag.FXPlatform)
    private static Node makeRunIcon()
    {
        return JavaFXUtil.withStyleClass(
                new Polygon(
                    0, 0,
                    12, 5,
                    0, 10
                ), 
                "run-icon");
    }

    /**
     * The red pause icon.
     * Currently, can't be used in a menuItem as JavaFX doesn't deal
     * with this type of nodes properly on menuItem, at least on Mac.
     */
    @OnThread(Tag.FXPlatform)
    private static Node makePauseIcon()
    {
        return JavaFXUtil.withStyleClass(
                new Path(
                    new MoveTo(2, 0),
                    new LineTo(2, 10),
                    new MoveTo(8, 0),
                    new LineTo(8, 10)
                ),
                "pause-icon");
    }

    /**
     * The brown reset icon.
     * Currently, can't be used in a menuItem as JavaFX doesn't deal
     * with this type of nodes properly on menuItem, at least on Mac.
     */
    @OnThread(Tag.FXPlatform)
    private static Node makeResetIcon()
    {
        Canvas canvas = new Canvas(15, 15);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setStroke(javafx.scene.paint.Color.SADDLEBROWN);
        g.setLineWidth(2);
        g.setFill(null);
        int centreX = 7;
        int centreY = 7;
        // The loop part, with a 100 degree slightly asymmetric gap:
        g.arc(centreX, centreY, 5, 5, 135, 260);
        g.stroke();
        // sin(45) = cos(45) = 1/sqrt(2):
        double arcEndX = centreX - 5 / Math.sqrt(2);
        double arcEndY = centreY - 5 / Math.sqrt(2);
        // Tweak the positioning of the arrow head to make it look right at small scale:
        arcEndX += 1.0;
        // Triangle should point at 45 degrees, but looks better if curving down, so 25 is about right
        int triangleHeading = 25;
        // The size of the arrow head, measured by distance from the centre:
        int arrowRadius = 4;
        g.setFill(g.getStroke());
        g.setStroke(null);
        // Arrow head is a rotated equilateral triangle around arcEndX, arcEndY:
        g.fillPolygon(
                new double[] {
                    arcEndX + arrowRadius * Math.cos(Math.toRadians(triangleHeading)),
                    arcEndX + arrowRadius * Math.cos(Math.toRadians(triangleHeading + 120)),
                    arcEndX + arrowRadius * Math.cos(Math.toRadians(triangleHeading + 240))
                },
                new double[] {
                    arcEndY - arrowRadius * Math.sin(Math.toRadians(triangleHeading)),
                    arcEndY - arrowRadius * Math.sin(Math.toRadians(triangleHeading + 120)),
                    arcEndY - arrowRadius * Math.sin(Math.toRadians(triangleHeading + 240))
                },
                3
        );
        return canvas;
    }
    
}
