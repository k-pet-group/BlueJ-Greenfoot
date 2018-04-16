package greenfoot.guifx;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.utility.javafx.JavaFXUtil;
import greenfoot.vmcomm.GreenfootDebugHandler;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Side;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The execution twirler component.  This shows up if the user code has been running for too
 * long in any given segment (single act cycle, single user invocation), and allows the user
 * to easily open the debugger or restart the VM.
 */
@OnThread(Tag.FXPlatform)
public class ExecutionTwirler extends MenuButton
{
    // The animation to spin the twirl icon:
    private final RotateTransition rotateTransition;
    private Project project;
    private GreenfootDebugHandler greenfootDebugHandler;

    /**
     * Create the component
     * @param project The associated project (needed to access the debugger)
     * @param greenfootDebugHandler The debug handler for the project
     */
    public ExecutionTwirler(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        this.project = project;
        this.greenfootDebugHandler = greenfootDebugHandler;
        
        ImageView imageView = new ImageView(new Image(getClass().getClassLoader().getResourceAsStream("swirl.png")));
        imageView.setFitHeight(15.0);
        imageView.setPreserveRatio(true);
        setGraphic(imageView);
        
        rotateTransition = new RotateTransition(Duration.millis(3000), imageView);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
        
        // Start invisible:
        setVisible(false);
        
        setPopupSide(Side.BOTTOM);
        // Important to use fields here, not constructor parameters, as the fields may change later
        // if the project shown in this window changes:
        getItems().setAll(
            JavaFXUtil.makeMenuItem(Config.getString("executionDisplay.restart"), () -> this.project.restartVM(), null),
            JavaFXUtil.makeMenuItem(Config.getString("executionDisplay.openDebugger"), () -> {
                this.project.getExecControls().show();
                this.greenfootDebugHandler.haltSimulationThread();
            }, null)
        );
    }

    /**
     * Sets a new project (and project debug handler) for this component
     */
    public void setProject(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        this.project = project;
        this.greenfootDebugHandler = greenfootDebugHandler;
    }

    /**
     * Make the twirler visible and start spinning the icon.  Does nothing if already started.
     */
    public void startTwirling()
    {
        setVisible(true);
        rotateTransition.playFromStart();
    }

    /**
     * Make the twirler invisible and stop spinning the icon.  Does nothing if already stopped.
     */
    public void stopTwirling()
    {
        setVisible(false);
        rotateTransition.stop();
    }
}
