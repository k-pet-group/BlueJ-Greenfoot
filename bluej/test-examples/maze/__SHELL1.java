
import javablue.runtime.Shell;
import javablue.runtime.ObjectResultWrapper;
import java.util.Hashtable;

public class __SHELL1 extends Shell
{
    public static Object __bluej_runtime_result;


    public static void main(String[] args)
	throws Exception
    {
	Hashtable __bluej_runtime_scope = getScope("examples2/maze");
		Game game_1 = (Game)__bluej_runtime_scope.get("game_1");

	game_1.play();

    }
}

