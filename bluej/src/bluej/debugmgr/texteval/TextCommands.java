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
        public abstract String[] getHelp();
    }
    
    
    final class HelpCommand extends TextCommand {

        /**
         * Implementation of the 'help' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            if(words.length > 1) {
                TextCommand command = (TextCommand) commands.get(words[1]);
                if(command == null) {
                    textArea.error("'" + words[1] + "' is not a known command.");
                }
                else {
                    String[] s = command.getHelp();
                    for(int i=0; i < s.length; i++) {
                        textArea.output(s[i]);
                    }
                }
            }
            else {
                textArea.output("Type a Java expression or statement for evaluation.");
                textArea.output("Environment commands: " + commands.keySet());
                textArea.output("Type 'help <command>' for command specific help.");
            }
        }
        
        public String[] getHelp()
        {
            return new String[] { "help:           general help", 
                                  "help <command>: command specific help" }; 
        }
    }

    
    final class ListCommand extends TextCommand {

        /**
         * Implementation of the 'list' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            if(words.length > 1)
                textArea.error("This command does not expect parameters");
            else
                textArea.listObjectBench();
        }
        
        public String[] getHelp()
        {
            return new String[] { "list:  list the objects currently on the object bench" }; 
        }
    }

    
    final class GetCommand extends TextCommand {

        /**
         * Implementation of the 'get' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            if(words.length > 1)
                textArea.error("This command does not expect parameters");
            else
                textArea.getObjectToBench();
        }
        
        public String[] getHelp()
        {
            return new String[] { "get:  get the result object of the last expression onto the object bench" }; 
        }
    }

    
    final class InspectCommand extends TextCommand {

        /**
         * Implementation of the 'inspect' command.
         * @param words  The words from the imput line
         */
        public void execute(String[] words, TextEvalArea textArea) {
            if(words.length > 2)
                textArea.error("This command does not expect parameters");
            else if(words.length == 2)
                textArea.inspectObject(words[1]);
            else
                textArea.inspectObject(null);
        }
        
        public String[] getHelp()
        {
            return new String[] { "inspect:          inspect the result of the last expression", 
                                  "inspect <object>: inspect a named object from the object bench" }; 
        }
    }
}
