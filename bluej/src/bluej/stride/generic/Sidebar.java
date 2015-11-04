package bluej.stride.generic;

import javafx.beans.property.StringProperty;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Created by neil on 30/04/15.
 */
public class Sidebar
{
    private final Node node;
    private final Label label;

    public Sidebar(Label label, Node node)
    {
        this.label = label;
        this.node = node;
    }

    public Node getNode()
    {
        return node;
    }

    public StringProperty textProperty()
    {
        return label.textProperty();
    }

    public void setText(String value)
    {
        label.setText(value);
    }

    public Node getStyleable()
    {
        return label;
    }
}
