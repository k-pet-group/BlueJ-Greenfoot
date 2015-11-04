package bluej.utility;

import java.util.concurrent.Callable;

/**
 * Wraps a Callable and ensures that it is only called once, with the result cached thereafter.
 */
public class CachedCallable<T> implements Callable<T>
{
    private final Callable<T> wrapped;
    private T result;
    private boolean called = false;
    public CachedCallable(Callable<T> wrapped)
    {
        this.wrapped = wrapped;
    }
    @Override
    public T call() throws Exception
    {
        if (!called)
        {
            result = wrapped.call();
            called = true;
        }
        return result;
    }
    
    
}
