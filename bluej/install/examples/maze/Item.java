/*
Author:  Morten Knudsen & Kent Hansen
         Ported from the Blue program "Maze" by Michael Kolling.
Version: 1.0
Date:    July 1998
Short:   An item (a thing that is found) in an adventure game.

 This class is part of Maze. Maze is a simple, text based adventure game.

 "Item" represents a thing that i slying somewhere in a room in this
 game. Items can be picked up and carried around.

*/

class Item {
	private String name;
	private String description;

/*	Create an item with == Create name 'name'.  'name' is used to refer
	to the item in commands.  It should be a single word. 'description'
	describes what you see when looking at the item. It should be
	written in a way that, when prefixed with the words  "You see", a
	correct sentence is formed (i.e. "You see <description>", with
	description being something like "an ugly blob of slime").
*/
	public Item (String name, String description) {
		this.name = name;
		this.description = description;
	}

//	Copy another item into this item.
	public void copy (Item item) {
		name = item.name;
		description = item.description;
	}

//	Return the name of this item.
	public String getName () {
		return name;
	}

//	Return a string with a description of this item.
	public String toString () {
		return description;
	}

//	Compare this item with a string or another item.
	public boolean equals (Object anObject) {
		// Is it a string?
		if (anObject instanceof String)
			return (name.equals (anObject));
		// Or another Item?
		else if (anObject instanceof Item)
			return (((Item)anObject).name.equals (name));
		// No, it's something else. Use lowest common denominator.
		else
			return (anObject.equals (this));
	}
}
