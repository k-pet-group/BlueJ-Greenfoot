package bluej.editor.moe.autocomplete;

/**
 * This class provides some simple debug output that
 * can be customised depending upon the debug information
 * that the developer needs to see.  It is possible to
 * turn on just the messages that apply to the bug
 * that the developer needs to fix.
 *
 * @version 1.2 (10/03/2003)
 *
 * @author Chris Daly (<A href="mailto:cdd1@ukc.ac.uk">cdd1@ukc.ac.uk</A>),
 *         Darren Link (<A href="mailto:drl1@ukc.ac.uk">drl1@ukc.ac.uk</A>),
 *         Mike Stewart (<A href="mailto:mjs7@ukc.ac.uk">mjs7@ukc.ac.uk</A>)
 */
public class Debug{

    private static boolean printStandardMessages = false;
    private static boolean printConfigMessages = false;
    private static boolean printOwnedWindowsMessages = false;
    private static boolean printAutoCompleteInfoMessages = false;
    private static boolean printParserResultsMessages = false;
    private static boolean printParserMessages = false;
    private static boolean printScopeMessages = false;
    private static boolean printWindowEventMessages = false;
    private static boolean printAvailablePackagesMessages = false;

    public static void printStandardMsg(String msg){
        if(!printStandardMessages) return;
        System.out.println("Std: " + msg);
    }

    public static void printConfigMsg(String msg){
        if(!printConfigMessages) return;
        System.out.println("Config: " + msg);
    }

    public static void printOwnedWindowsMsg(String msg){
        if(!printOwnedWindowsMessages) return;
        System.out.println("Owned Wins: " + msg);
    }

    public static void printAutoCompleteInfo(String msg){
        if(!printAutoCompleteInfoMessages) return;
        System.out.println("Auto Comp Info: " + msg);
    }

    public static void printParserResultsMessage(String msg){
        if(!printParserResultsMessages) return;
        System.out.println("Parser Res: " + msg);
    }

    public static void printParserMessage(String msg){
        if(!printParserMessages) return;
        System.out.println("Parser: " + msg);
    }

    public static void printScopeMessage(String msg){
        if(!printScopeMessages) return;
        System.out.println("Scope: " + msg);
    }

    public static void printWindowEventMessage(String msg){
        if(!printWindowEventMessages) return;
        System.out.println("Win Event: " + msg);
    }

    public static void printAvailablePackagesMessage(String msg){
        if(!printAvailablePackagesMessages) return;
        System.out.println("Pkg Info: " + msg);
    }

}
