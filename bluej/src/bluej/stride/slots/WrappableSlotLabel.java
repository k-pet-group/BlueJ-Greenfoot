package bluej.stride.slots;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Region;

import bluej.stride.generic.Frame;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * Like SlotLabel, but makes each word a separate Label, to support wrapping in the flow pane
 */
public class WrappableSlotLabel implements HeaderItem
{
    private final ObservableList<String> styleClasses = FXCollections.observableArrayList("wrappable-slot-label");
    private final ObservableList<Label> words = FXCollections.observableArrayList();
    private HangingFlowPane.FlowAlignment alignment;

    public WrappableSlotLabel(String fullText)
    {
        setText(fullText);
    }

    public void setText(String fullText)
    {
        words.clear();
        boolean first = true;
        for (String word : fullText.split(" "))
        {
            // If they setText to blank, we will get one blank in result, but ignore it:
            if (!word.equals(""))
            {
                Label l = new Label(word);
                l.setMinWidth(0.0);
                JavaFXUtil.bindList(l.getStyleClass(), styleClasses);
                JavaFXUtil.setPseudoclass("bj-first", first, l);
                first = false;
                words.add(l);
            }
        }

        // Make sure alignment is set right on all words:
        setAlignment(this.alignment);
    }

    public void addStyleClass(String styleClass)
    {
        if (!styleClasses.contains(styleClass))
            styleClasses.add(styleClass);
    }

    @Override
    public EditableSlot asEditable()
    {
        return null;
    }

    @Override
    public ObservableList<? extends Node> getComponents()
    {
        return words;
    }

    @Override
    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animate)
    {
        // TODO do we need to do anything here?
    }

    public void shrinkHorizontally(SharedTransition animate)
    {
        for (Label l : words)
        {
            l.setTextOverrun(OverrunStyle.CLIP);
            final double cur = l.getWidth();
            l.minWidthProperty().set(0.0);
            l.maxWidthProperty().unbind();
            l.maxWidthProperty().bind(animate.getProgress().negate().add(1.0).multiply(cur));
        }
    }

    public void growHorizontally(SharedTransition animate)
    {
        for (Label l : words)
        {
            l.setMinWidth(Region.USE_COMPUTED_SIZE);
            l.maxWidthProperty().unbind();
            l.maxWidthProperty().bind(animate.getProgress().multiply(JavaFXUtil.measureString(l, l.getText())));
            animate.addOnStopped(() -> {
                l.maxWidthProperty().unbind();
                l.maxWidthProperty().set(Region.USE_COMPUTED_SIZE);
            });
        }
    }

    // setInvisible says whether to set the visible property to false afterwards
    public void fadeOut(SharedTransition animate, boolean setInvisible)
    {
        for (Label l : words)
        {
            l.opacityProperty().bind(animate.getOppositeProgress());
        }
        animate.addOnStopped(() -> {
            for (Label l : words)
            {
                l.opacityProperty().unbind();
                if (setInvisible)
                    l.setVisible(false);
            }
        });
    }

    public void fadeIn(SharedTransition animate)
    {
        for (Label l : words)
        {
            l.setVisible(true);
            l.opacityProperty().bind(animate.getProgress());
        }
        animate.addOnStopped(() -> {
            for (Label l : words)
            {
                l.opacityProperty().unbind();
            }
        });
    }


    public void setAlignment(HangingFlowPane.FlowAlignment alignment)
    {
        this.alignment = alignment;
        words.forEach(l -> HangingFlowPane.setAlignment(l, alignment));
    }
}
