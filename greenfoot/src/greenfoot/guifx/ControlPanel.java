package greenfoot.guifx;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.core.Simulation;
import greenfoot.guifx.GreenfootStage.State;
import greenfoot.util.GreenfootUtil;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;

import java.util.Arrays;
import java.util.List;

/**
 * The control panel in GreenfootStage: the act/run/reset buttons,
 * speed slider and execution twirler that appear at the bottom
 * left of the window.
 */
class ControlPanel extends GridPane
{
    private static final String PAUSE_BUTTON_TEXT = Config.getString("controls.pause.button");
    private static final String RUN_BUTTON_TEXT = Config.getString("controls.run.button");
    private static final String RUN_BUTTON_TOOLTIP_TEXT = Config.getString("controls.run.shortDescription");
    private static final String PAUSE_BUTTON_TOOLTIP_TEXT = Config.getString("controls.pause.shortDescription");
    private static final Node RUN_ICON = GreenfootUtil.makeRunIcon();
    private static final Node PAUSE_ICON = GreenfootUtil.makePauseIcon();
    private static final Node ACT_ICON = GreenfootUtil.makeActIcon();
    private static final Node RESET_ICON = GreenfootUtil.makeResetIcon();
    
    private final Button runPauseButton;
    private final Slider speedSlider;
    private final GreenfootStage greenfootStage;

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
    public ControlPanel(GreenfootStage greenfootStage, Node executionTwirler)
    {
        this.greenfootStage = greenfootStage;
        Button actButton = new Button(Config.getString("run.once"));
        actButton.setTooltip(new Tooltip(Config.getString("controls.runonce.shortDescription")));
        actButton.setGraphic(ACT_ICON);
        runPauseButton = new Button(RUN_BUTTON_TEXT);
        runPauseButton.setGraphic(RUN_ICON);
        runPauseButton.setTooltip(new Tooltip(RUN_BUTTON_TOOLTIP_TEXT));
        Button resetButton = new Button(Config.getString("reset.world"));
        resetButton.setTooltip(new Tooltip(Config.getString("controls.reset.shortDescription")));
        resetButton.setGraphic(RESET_ICON);
        actButton.disableProperty().bind(actDisabled);
        runPauseButton.disableProperty().bind(runPauseDisabled);
        resetButton.disableProperty().bind(resetDisabled);
        for (Button button : Arrays.asList(actButton, runPauseButton, resetButton))
        {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        int min = 0;
        int max = Simulation.MAX_SIMULATION_SPEED;
        speedSlider = new Slider();
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

        actButton.setOnAction(e -> greenfootStage.act());
        runPauseButton.setOnAction(e -> greenfootStage.doRunPause());
        resetButton.setOnAction(e -> greenfootStage.doReset());
        // Note - if you alter this listener code, make sure to check notifySimulationSpeed() as well:
        JavaFXUtil.addChangeListener(speedSlider.valueProperty(), newSpeed -> greenfootStage.setSpeedFromSlider(newSpeed.intValue()));
        
        TilePane controlPanel = new TilePane(actButton, runPauseButton, resetButton);
        controlPanel.setPrefColumns(3);
        controlPanel.getStyleClass().add("control-panel");
        controlPanel.setAlignment(Pos.CENTER);
        Label speedLabel = new Label(Config.getString("controls.speed.label"));
        add(controlPanel, 0, 0);
        Pane speedAndTwirler = new BorderPane(speedSlider, null, executionTwirler, null, speedLabel);
        BorderPane.setAlignment(speedLabel, Pos.CENTER_RIGHT);
        add(speedAndTwirler, 1, 0);
        GridPane.setHalignment(speedAndTwirler, HPos.CENTER);
        ColumnConstraints leftHalf = new ColumnConstraints();
        ColumnConstraints rightHalf = new ColumnConstraints();
        leftHalf.setPercentWidth(50);
        rightHalf.setPercentWidth(50);
        getColumnConstraints().setAll(leftHalf, rightHalf);
    }

    /**
     * Called by GreenfootStage to update the state of our buttons.
     */
    public void updateState(State newState, boolean atBreakpoint)
    {
        actDisabled.setValue(newState != State.PAUSED || atBreakpoint);
        runDisabled.setValue(newState != State.PAUSED || atBreakpoint);
        pauseDisabled.setValue(newState != State.RUNNING || atBreakpoint);
        resetDisabled.setValue(newState == State.NO_PROJECT || newState == State.UNCOMPILED);

        boolean showingPause = newState == State.RUNNING || newState == State.RUNNING_REQUESTED_PAUSE;
        if (showingPause)
        {
            // Only change button text and tooltip if needed; changing the tooltip to another
            // tooltip with the same text causes it to disappear needlessly if the user is currently viewing it:
            if (!runPauseButton.getText().equals(PAUSE_BUTTON_TEXT))
            {
                runPauseButton.setGraphic(PAUSE_ICON);
                runPauseButton.setText(PAUSE_BUTTON_TEXT);
                runPauseButton.setTooltip(new Tooltip(PAUSE_BUTTON_TOOLTIP_TEXT));
            }
        }
        else
        {
            // Ditto: only change text and tooltip if needed
            if (!runPauseButton.getText().equals(RUN_BUTTON_TEXT))
            {
                runPauseButton.setGraphic(RUN_ICON);
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
     * Make the act/run/pause/reset menu items for GreenfootStage.
     * They are made here because their disabled state is managed by ControlPanel.
     */
    public List<MenuItem> makeMenuItems()
    {
        return Arrays.asList(
        JavaFXUtil.makeMenuItem("run.once",
                new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN),
                greenfootStage::act, actDisabled),
                JavaFXUtil.makeMenuItem("controls.run.button",
                        new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN),
                        greenfootStage::doRunPause, runDisabled),
                JavaFXUtil.makeMenuItem("controls.pause.button",
                        new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN,
                                KeyCombination.SHIFT_DOWN),
                        greenfootStage::doRunPause, pauseDisabled),
                JavaFXUtil.makeMenuItem("reset.world",
                        new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN),
                        greenfootStage::doReset, resetDisabled)
        );
    }
}
