public class initException
{
    static private int x = exceptionMethod();
    
    static private int exceptionMethod()
    {
        throw new NullPointerException();
    }
}
