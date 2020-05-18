package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.EditableTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.Utility;
import bluej.utility.javafx.AbstractOperation;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCombination;

import java.util.Arrays;
import java.util.List;

public abstract class EditableTargetOperation extends AbstractOperation<Target>
{
    private final MenuItemOrder menuItemOrder;
    private final List<String> styleClasses;
    private final String label;

    public EditableTargetOperation(String identifier, Combine combine, KeyCombination shortcut, String label, MenuItemOrder menuItemOrder, String... styleClasses)
    {
        super(identifier, combine, shortcut);
        this.label = label;
        this.menuItemOrder = menuItemOrder;
        this.styleClasses = Arrays.asList(styleClasses);
    }

    @Override
    protected final void activate(List<Target> targets)
    {
        for (Target target : targets)
        {
            if (target instanceof EditableTarget)
            {
                executeEditable((EditableTarget) target);
            }
        }
    }

    protected abstract void executeEditable(EditableTarget target);

    @Override
    public final List<ItemLabel> getLabels()
    {
        return List.of(new ItemLabel(new ReadOnlyStringWrapper(label), menuItemOrder));
    }

    @Override
    protected List<String> getStyleClasses()
    {
        return styleClasses;
    }
}
