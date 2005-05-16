package bluej.utility;

import junit.framework.TestCase;

public class JavaUtilTests extends TestCase
{
    protected void setUp()
    {
        // nothing to do
    }
    
    protected void tearDown()
    {
        // nothing to do
    }

    /**
     * Test that types with infinite recursion don't cause us to bomb out.
     * In this case we use Enum, as Enum&lt;E&gt; has E extend Enum&lt;E&gt;.
     */
    public void test1()
    {
        JavaUtils ju = JavaUtils.getJavaUtils();
        ju.getTypeParams(Enum.class);
        // ok test passed
    }
    
}
