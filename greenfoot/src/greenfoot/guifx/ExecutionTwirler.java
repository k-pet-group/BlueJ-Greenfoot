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

public class ExecutionTwirler extends MenuButton
{
    private final RotateTransition rotateTransition;
    private Project project;
    private GreenfootDebugHandler greenfootDebugHandler;

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
        // Important to use fields here, not constructor parameters, as fields may change later:
        getItems().setAll(
            JavaFXUtil.makeMenuItem(Config.getString("executionDisplay.restart"), () -> this.project.restartVM(), null),
            JavaFXUtil.makeMenuItem(Config.getString("executionDisplay.openDebugger"), () -> {
                this.project.getExecControls().show();
                this.greenfootDebugHandler.haltSimulationThread();
            }, null)
        );
    }

    public void setProject(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        this.project = project;
        this.greenfootDebugHandler = greenfootDebugHandler;
    }
    
    public void show()
    {
        setVisible(true);
        rotateTransition.playFromStart();
    }
    
    public void hide()
    {
        setVisible(false);
        rotateTransition.stop();
    }
}
