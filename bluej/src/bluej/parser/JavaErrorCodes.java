package bluej.parser;

public class JavaErrorCodes
{
    /** Method declaration not followed by ';' or method body or "throws" clause */
    public static final String BJ00 = "BJ00";
    
    public static final String BJ01 = "BJ01"; // Bracket expected (after "if" or "while" etc)
    public static final String BJ02 = "BJ02"; // Condition expected (after "if" or "while" etc) - occurs
        // when a brace is found but a bracket was expected
}
