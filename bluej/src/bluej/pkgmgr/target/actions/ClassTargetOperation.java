package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import javafx.scene.input.KeyCombination;

import java.util.List;

public abstract class ClassTargetOperation extends EditableTargetOperation
{
    public ClassTargetOperation(String identifier, Combine combine, KeyCombination shortcut, String label, MenuItemOrder menuItemOrder, String... styleClasses)
    {
        super(identifier, combine, shortcut, label, menuItemOrder, styleClasses);
    }

    @Override
    protected final void executeEditable(EditableTarget target)
    {
        if (target instanceof ClassTarget)
            execute((ClassTarget)target);
    }
    
    protected abstract void execute(ClassTarget target);
}
