
import bluej.runtime.Shell;
import bluej.runtime.ObjectResultWrapper;
import java.util.Hashtable;

public class __SHELL5 extends Shell
{
    public static Object __bluej_runtime_result;


    public static void main(String[] args)
	throws Exception
    {
	Hashtable __bluej_runtime_scope = getScope("/projects/bluej/examples2/earth");
		Simulator simulator_1 = (Simulator)__bluej_runtime_scope.get("simulator_1");

	simulator_1.createTime();

    }
}

