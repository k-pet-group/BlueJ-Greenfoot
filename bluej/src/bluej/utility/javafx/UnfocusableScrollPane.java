package bluej.utility.javafx;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * A version of ScrollPane which overrides the default behaviour of capturing
 * focus when clicked.  This scroll pane cannot be focused.
 */
public class UnfocusableScrollPane extends ScrollPane
{
    public UnfocusableScrollPane(Node content)
    {
        super(content);
    }

    @Override
    public void requestFocus()
    {
        // Override behaviour to do nothing: this pane is not focusable
    }
}
