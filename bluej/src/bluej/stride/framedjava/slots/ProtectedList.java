package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A clone/wrapper of the List interface which adds an extra ModificationToken
 * parameter to any methods which modify the list.  This helps to ensure
 * that modifications only take place within a modification block in StructuredSlot.
 */
public class ProtectedList<T>
{
    private ObservableList<T> content = FXCollections.observableArrayList();
    
    public T get(int index)
    {
        return content.get(index);
    }

    public boolean contains(T o)
    {
        return content.contains(o);
    }

    public int indexOf(T o)
    {
        return content.indexOf(o);
    }

    public boolean isEmpty()
    {
        return content.isEmpty();
    }

    public int size()
    {
        return content.size();
    }

    public Stream<T> stream()
    {
        return content.stream();
    }

    public void forEach(Consumer<? super T> action)
    {
        content.forEach(action);
    }

    public boolean add(T t, StructuredSlot.ModificationToken token)
    {
        token.check();
        return content.add(t);
    }

    public void add(int index, T element, StructuredSlot.ModificationToken token)
    {
        token.check();
        content.add(index, element);
    }

    public T remove(int index, StructuredSlot.ModificationToken token)
    {
        token.check();
        return content.remove(index);
    }

    public void clear(StructuredSlot.ModificationToken token)
    {
        token.check();
        content.clear();
    }

    // Should only be used for observation, not for modification
    // (Hence returning <?>, not <T>).
    public ObservableList<?> observable()
    {
        return content;
    }
    
    public Optional<Integer> findFirst(Predicate<T> function)
    {
        for (int i = 0; i < content.size(); i++)
        {
            if (function.test(content.get(i)))
                return Optional.of(i);
        }
        return Optional.empty();
    }
}
