package bluej.utility.javafx;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Duration;

/**
 * A transition that is shared between multiple entities.  For example, you
 * may want to animate collapsing all the frames for bird's eye view.  You create
 * an instance of this class, pass it to all frames to hook into the progress
 * (so that all frames collapse at the same rate), then call animateOver to 
 * run all the animations together.
 * 
 * This class is used rather than passing a Timeline around, because you can register
 * multiple stop listeners to this class more easily.
 */
public class SharedTransition
{
    private final List<FXRunnable> onStopped = new ArrayList<>();
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final DoubleExpression opposite = progress.negate().add(1.0);
    private Timeline timeline;
    private boolean runStopped;

    /**
     * An expression which will animate from 0.0 to 1.0 during the transition
     */
    public DoubleExpression getProgress()
    {
        return progress;
    }

    /**
     * An expression which will animate from 1.0 to 0.0 during the transition
     */
    public DoubleExpression getOppositeProgress()
    {
        return opposite;
    }

    /**
     * Adds a listener to be execute if the transition finishes naturally,
     * or is stopped via the stop() method.
     */
    public void addOnStopped(FXRunnable stopped)
    {
        onStopped.add(stopped);
    }

    /**
     * Animates the transition over the given duration.  Should only be called once.
     */
    public void animateOver(Duration duration)
    {
        if (timeline != null)
        {
            throw new IllegalStateException("Cannot animate SharedTransition more than one time");
        }
        
        timeline = new Timeline(new KeyFrame(duration, e -> { 
            if (!runStopped)
            {
                runStopped = true;
                onStopped.forEach(FXRunnable::run);
            }
        }, new KeyValue(progress, 1.0, Interpolator.EASE_BOTH)));
        timeline.play();
    }
    
    public void stop()
    {
        if (timeline == null)
        {
            throw new IllegalStateException("Cannot stop SharedTransition before it is started");
        }
        timeline.stop();
        progress.set(1.0);
        if (!runStopped)
        {
            runStopped = true;
            onStopped.forEach(FXRunnable::run);
        }
    }
}
