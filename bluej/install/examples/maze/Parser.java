/*
Author:  Morten Knudsen & Kent Hansen
         Ported from the Blue program "Maze" by Michael Kolling.
Version: 1.0
Date:    July 1998
Short:   Command line parser for Maze, a simple text based game.

 This class is part of Maze. Maze is a simple, text based adventure game.

 This parser reads user input and tries to interpret it as a "Maze" 
 command. Every time it is called it reads a line from the terminal and
 tries to interpret the line as a two word command. The first word is
 returned as a command the second  word in a command line always refers
 to an item.  The parser returns the item object itself. For instance,
 for the input line

 	"take brain"

 the parser returns the enumeration value 'take' and a reference to the
 item with name "brain".

 The valid command words are defined in the enumeration class Command.

 The second word is optional.
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Parser {
	Item[] items = new Item[3]; // all the items used in this game

//	Create the parser. 'items' contains all item objects used in this
//	game.  (Needed to recognise item names in commands.)

	public Parser(Item[] items) {
		this.items = items;
	}


//	Read a command from the terminal.
//	First, a prompt is printed, and then one line of input is read.
//	'cmd' returns the command used. If the command word is an unknown
//	word, 'cmd' returns nil.
//	If the command line has only one word, 'two_word' is false and
//	'item' is nil.  If a second word was typed, 'two_word' is
//	true.  If it was the name of a known item, 'item' returns a
//	reference to that item, otherwise 'item' is nil. */

	public Result getCommand() {
		String word1= new String("");
		String word2= new String("");
		String word3= new String("");
		int idx;

		boolean two_words=true;
		Command cmd = new Command();
		Item item = new Item("","");

		System.out.print("> "); // print prompt

		BufferedReader buf = new BufferedReader
			(new InputStreamReader(System.in));

		try {
			word3=buf.readLine();
		}
		catch(java.io.IOException e) {
			System.out.println ("An exception has been caught:"
				+e.getMessage());
		}

		StringTokenizer aTokenizer = new StringTokenizer(word3);

		if( aTokenizer.hasMoreTokens())
			word1 = aTokenizer.nextToken(); // get first word
		if( aTokenizer.hasMoreTokens())
			word2 = aTokenizer.nextToken(); // get second word
		cmd.gotoFirstElement(); // match word1 against commands
		
		while(cmd.hasMoreElements()) {
			if((cmd.nextElement()).equals(word1))
				break;
	    	}

		 		// initially no item
		if (word2.equals(""))  // no second word...
			two_words=false;
			
		

		for (idx = 0; idx<items.length; idx++)
			// match word2 agains item names
			if (items[idx].equals(word2))
				item=items[idx]; 
		return new Result(cmd,item,two_words);
    }
}
