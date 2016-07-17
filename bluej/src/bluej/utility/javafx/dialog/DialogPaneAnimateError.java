package bluej.utility.javafx.dialog;

import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A custom DialogPane that runs a "jiggle" animation on a given
 * error label whenever the user mouses over the disabled OK button,
 * or clicks an enabled OK button with a non-empty error label.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class DialogPaneAnimateError extends DialogPane
{
    /** An extra action to run when the OK button is mouse overed */
    private final FXPlatformRunnable extraMouseEnter;
    /** The jiggle animation.  Null when not running */
    private RotateTransition animation = null;
    /** The error label to animate */
    private Node errorLabel;
    /** Whether the error label is empty */
    private SimpleBooleanProperty errorLabelEmpty = new SimpleBooleanProperty(true);
    /** The actual OK button */
    private Button okButton;

    /**
     * Constructs a new Dialog Pane.  Note that the constructor does not actually
     * set the buttons in the dialog (you should do this yourself, meaning you can
     * choose OK-Cancel or OK by itself) or add stylesheets or anything else.
     * 
     * @param errorLabel The error label to animate
     * @param extraMouseEnter An extra action to run when the OK button is mouse overed.
     *                        This may well affect the disabled state of the OK button;
     *                        we only decide whether to run the animation after this action has run.
     */
    public DialogPaneAnimateError(Label errorLabel, FXPlatformRunnable extraMouseEnter)
    {
        this.errorLabel = errorLabel;
        this.errorLabelEmpty.bind(errorLabel.textProperty().isEmpty());
        this.extraMouseEnter = extraMouseEnter;
    }

    // We must override createButton to wrap the OK button in a pane.  The problem is that
    // when the OK button is disabled, it receives no mouse events, so we can't listen
    // for a mouse-entered directly on the OK button.  Instead we wrap the OK button
    // in a pane on which we can listen for mouse events, even when the button inside is disabled.
    @Override
    protected Node createButton(ButtonType buttonType)
    {
        // Take the actual buttons from the superclass:
        Node normal = super.createButton(buttonType);
        // We only bother to wrap the OK button:
        if (buttonType == ButtonType.OK)
        {
            okButton = (Button)normal;
            // AnchorPane makes the button resize when the pane gets resized (to match other button widths)
            AnchorPane okWrapper = new AnchorPane(normal);
            AnchorPane.setTopAnchor(okButton, 0.0);
            AnchorPane.setBottomAnchor(okButton, 0.0);
            AnchorPane.setLeftAnchor(okButton, 0.0);
            AnchorPane.setRightAnchor(okButton, 0.0);
            // Copy across information from the button so that ButtonBar behaves right:
            ButtonBar.setButtonData(okWrapper, buttonType.getButtonData());
            ButtonBar.setButtonUniformSize(okWrapper, true);
            okWrapper.setOnMouseEntered(e -> {
                extraMouseEnter.run();
                if (okButton.isDisable())
                {
                    animate();
                }
            });
            okButton.addEventFilter(ActionEvent.ACTION, event -> {
                // runLater as a hack to run after other event filters:
                JavaFXUtil.runAfterCurrent(() -> {
                    if (!errorLabelEmpty.get())
                        animate();
                });
            });
            return okWrapper;
        }
        return normal;
    }

    private void animate()
    {
        if (animation == null)
        {
            animation = new RotateTransition(Duration.millis(70), errorLabel);
            animation.setByAngle(5);
            // Rotate, rotate back, rotate, rotate back:
            animation.setAutoReverse(true);
            animation.setCycleCount(4);
            // When the animation finishes, set reference back to null:
            animation.setOnFinished(ev -> {
                animation = null;
            });
            animation.play();
        }
    }

    /**
     * Returns the reference to the actual OK button (e.g. for disabling it).
     * This is different than lookupButton(ButtonType.OK) which will only
     * return the wrapper for the button, not the button itself.
     */
    public Button getOKButton()
    {
        return okButton;
    }
}
