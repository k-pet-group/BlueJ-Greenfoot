import bluej.runtime.Shell;

/**
 * Test involving breakpoints.
 * 
 * CAUTION: Be careful editing. The magic line numbers must be correct.
 * 
 * @author Davin McCall
 * @version $Id: breakpointTester.java 3077 2004-11-09 04:33:53Z davmac $
 */
public class breakpointTester extends Shell
{
    public static final int breakpointLine1 = 21;
    public static final int breakpointLine2 = 22;
    
    public static int i;
    public static int j;
    
    public static void run()
    {
        i = 3; // System.err.println("i = 3"); // BREAKPOINT
        j = 4; // System.err.println("j = 4"); // BREAKPOINT
    }
}
