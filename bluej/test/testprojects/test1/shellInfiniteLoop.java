import bluej.runtime.Shell;

/**
 * go into an infinite loop
 * 
 * @author Davin McCall
 * @version $Id: shellInfiniteLoop.java 3077 2004-11-09 04:33:53Z davmac $
 */
public class shellInfiniteLoop extends Shell
{
    public static void run()
    {
        int i = 0;
        while(i < 100) {
            i++;
            i = i - 1;
        }
    }
}
