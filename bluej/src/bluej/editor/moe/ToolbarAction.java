package bluej.editor.moe;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * An abstract action which delegates to a sub-action, and which
 * mirrors the "enabled" state of the sub-action. This allows having
 * actions with alternative labels.
 *
 * @author Davin McCall
 */
public class ToolbarAction extends AbstractAction implements PropertyChangeListener
{
    private final Action subAction;

    public ToolbarAction(Action subAction, String label)
    {
        super(label);
        this.subAction = subAction;
        subAction.addPropertyChangeListener(this);
        setEnabled(subAction.isEnabled());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        subAction.actionPerformed(e);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // If the enabled state of the sub-action changed,
        // then we should change our own state.
        if (evt.getPropertyName().equals("enabled")) {
            Object newVal = evt.getNewValue();
            if (newVal instanceof Boolean) {
                boolean state = ((Boolean) newVal);
                setEnabled(state);
            }
        }
    }
}