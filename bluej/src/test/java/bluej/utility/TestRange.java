package bluej.utility;

import lang.stride.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by neil on 13/07/2016.
 */
public class TestRange
{
    @Test
    public void testRange()
    {
        for (int attempt = 0; attempt < 1000; attempt++)
        {
            int start = ThreadLocalRandom.current().nextInt(100000) - 50000;
            // Allows for the possibility of negative ranges:
            int end = start - 2 + ThreadLocalRandom.current().nextInt(100);

            List<Integer> range = lang.stride.Utility.makeRange(start, end);
            List<Integer> full = new ArrayList<>();
            for (int i = 0; i < end - start + 1; i++)
                full.add(start + i);

            Assert.assertEquals("Size", full.size(), range.size());
            Assert.assertEquals("Empty", full.isEmpty(), range.isEmpty());
            Assert.assertEquals("Equals A", full, range);
            Assert.assertEquals("Equals B", range, full);
            // Test get, contains, indexOf:
            withRandoms(-2, end - start + 5, i -> assertSame(() -> range.get(i), () -> full.get(i)));
            withRandoms(-2, end - start + 5, i -> assertSame(() -> range.contains(i), () -> full.contains(i)));
            withRandoms(-2, end - start + 5, i -> assertSame(() -> range.indexOf(i), () -> full.indexOf(i)));
        }
    }

    private void withRandoms(int low, int high, Consumer<Integer> action)
    {
        for (int i = 0; i < 1000; i++)
        {
            action.accept(ThreadLocalRandom.current().nextInt(low, high));
        }
    }

    private <T> void assertSame(Supplier<T> a, Supplier<T> b)
    {
        T ra;
        try
        {
            ra = a.get();
        }
        catch (Exception e)
        {
            try
            {
                b.get();
                // If no exception, failure:
                Assert.fail("Exception in A but not in B");
            }
            catch (Exception e2)
            {
                // Check exceptions are same type:
                Assert.assertTrue("Different exception types", e.getClass().isAssignableFrom(e2.getClass()) || e2.getClass().isAssignableFrom(e.getClass()));
            }
            return;
        }

        try
        {
            T rb = b.get();
            Assert.assertEquals("Results differ", ra, rb);
        }
        catch (Exception e)
        {
            // Exception here but not in A; problem
            Assert.fail("Exception in B but not in A");
        }
        // Otherwise all is fine.
    }
}
