package bluej.debugmgr.texteval;

import java.util.HashMap;

/**
 * This class implements the textual meta commands (such as help,
 * get, inspect, list) that can be entered in the text 
 * evaluation area -- a singleton.
 * 
 * @author Michael Kolling
 */
public class TextCommands {

    // singleton instance
    private static TextCommands instance = new TextCommands();
    
    /**
     * Singleton factory method.
     * 
     * @return The singleton instance.
     */
    public static TextCommands getInstance()
    {
        return instance;
    }
    
    // instance fields:
    
    private HashMap commands;

    /**
     * Initialise the text commands instance.
     */
    private TextCommands()
    {
        commands = new HashMap(4);
        commands.put("help", new HelpCommand());
        commands.put("list", new ListCommand());
        commands.put("get", new GetCommand());
        commands.put("inspect", new InspectCommand());
    }
    
    /**
     * Check whether an input line contains a meta command (help,
     * get, inspect, list). Execute the command, if one is found.
     * 
     * @return  true, is a command was recognised, false if not
     */
    public boolean evalMetaCommand(String input,  TextEvalArea textArea)
    {
        String[] words = input.split("\\s");
        if(words.length == 0)
            return false;
        
        TextCommand command = (TextCommand) commands.get(words[0]);
        
        if(command != null) {
            command.execute(words, textArea);
            return true;
        }
        else
            return false;
    }

    abstract class TextCommand {
        public abstract void execute(String[] words, TextEvalArea textArea);
    }
    
    
    final class HelpCommand extends TextCommand {

        /**
         * Implementation of the 'help' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            textArea.output("Type a Java expression or statement for evaluation.");
            textArea.output("Environment commands: " + commands.keySet());
            textArea.output("Type 'help <command>' for command specific help.");            
        }
    }

    
    final class ListCommand extends TextCommand {

        /**
         * Implementation of the 'list' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            textArea.listObjectBench();
        }
    }

    
    final class GetCommand extends TextCommand {

        /**
         * Implementation of the 'get' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            textArea.getObjectToBench();
        }
    }

    
    final class InspectCommand extends TextCommand {

        /**
         * Implementation of the 'inspect' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
        }
    }
}
