/**
  * Author:  Morten Knudsen & Kent Hansen
  *          Ported from the Blue program "Maze" by Michael Kolling.
  * Version: 1.0
  * Date:    July 1998
  * Short:   Main class of "Maze", a simple adventure gam
  * 
  *  This class is the main class of the "Maze" application.
  *  Maze is a very simple, text based adventure game.  Users can walk around
  *  some scenery and take items which they find on the way with them.
  * 
  *  To play this game, create an instance of this class and call the "play"
  *  routine.
  * 
  *  This main class creates and initialises all the others: it creates all
  *  rooms, creates all item, places the items into the rooms, creates and
  *  calls the parser and starts the game.  It also evaluates the commands
  *  that the parser returns.
  * 
  */

public class Game {
    private Parser parser;
    private Room curr_Room;
    private Room last_Room;
    private Item carrying = new Item("","");
    private Room solve_Room;
    private Item brain, problem, muffin;
    Item item = new Item("","");
    Command cmd = new Command();
    
    /** Try to go to one direction. If there is an exit, enter the new
     * room, otherwise print an error message.
     */
    private void goRoom(Command dir) {
        // Try to leave current room.
        Room new_Room = curr_Room.nextRoom(dir);
        if (new_Room == null)
            System.out.println("There is no door!\n");
        else {
            last_Room = curr_Room;
            curr_Room = new_Room;
            System.out.println(curr_Room);
        }
    }


    /** Go back to the last room we've been to. If we've been to no
     * other room before, print an error message.
     */
    private void goBack() {
        if (last_Room == null)
            System.out.println
                ("You haven't been anywhere before.\n");
        else {
            Room tmp;
            tmp = last_Room;
            last_Room = curr_Room;
            curr_Room = tmp;
            System.out.println(curr_Room);
        }
    }


    /** Try to take an object. If the object is really there and we are
     * not carrying anything, take it. Otherwise print an error message.
     */
    private void takeObject(Item anObject) {
        if (carrying.equals(""))
            if(curr_Room.takeObject(anObject)) {
                carrying = anObject;
                System.out.println("You have got the "
                    +anObject.getName()+" now.\n");
            }
            else {
                System.out.println("The "+anObject.getName()
                    +" is not here.\n");
            }
        else {
            System.out.println("You are already carrying the "
                +carrying.getName()+"\n");
            System.out.print ("Drop it if you want to take ");
            System.out.println ("something else.\n");
        }
    }


    /**
     * Try to drop an object. If we do not have that object or there
     * is already an object in the room, print an error message.
     * Otherwise drop it.
     */
    private void dropObject(Item anObject) {
        if (!(carrying.equals(anObject)))
            System.out.println("You don't have a "
                +anObject.getName()+".\n");
        else {
            if (curr_Room.putObject(anObject)) {
                carrying = new Item("","");
                System.out.println("You dropped the "
                    +anObject.getName()+".\n");
            }
            else
            {
                System.out.print ("You cannot drop it, the ");
                System.out.println ("room is too full.\n");
            }
        }
    }

    /**
     * Try to solve your problem. If the problem and the brain are 
     * here and the current room is the "solve room" then the game is
     * finished. Otherwise the solve attempt was unsuccessful. In any  
     * case an appropriate message is printed.
     * 'finished' is true if the game was successfully completed.
     */
    private boolean trySolve() {
        boolean bFinished = true;

        if (!curr_Room.equals(solve_Room)) {
            System.out.println("You cannot solve problems here");
            bFinished = false;
        }
        
        if (!(carrying.getName()).equals("brain")) {
            System.out.println("You haven't got a brain!");
            bFinished = false;
        }

        if (!(curr_Room.getObject()==null))
            if (!(((curr_Room.getObject()).getName()).equals("problem"))) {
                System.out.println("You don't know what your problem is!\n");
                bFinished = false;
            }

        if(bFinished)
            System.out.println("You solved the problem and win.\n");
        else
            System.out.println("Think again.\n");

        return bFinished;
    }

    /**
     * Print out some help information.
     * Here we print some stupid, cryptic message and a list of the 
     * command words.
     */
    private void printHelp() {
        System.out.println
        ("\nYou are lost. You are alone. You have problem. But");
        System.out.println
        ("what was it? You cannot remember. You need to find out");
        System.out.println
        ("what it was and find a place where you can solve your");
        System.out.println
        ("problem - but it is not easy...\n");
        System.out.println
        ("Your command words are:\n");
        cmd.showAll();
        System.out.println("\n");
    }

    /**
     * Given a command and an item, process the command.  
     * 'cmd' might be nil if the word was not recognised. The item
     * might be nil in case no second word was entered.
     * If this command finishes the game 'done' returns true, otherwise
     * 'done' is false.
     */
    private boolean processCommand(Result result) {
        boolean bDone = false;
        cmd = result.getCommand();
        item = result.getItem();
        if (cmd.equals("north") || cmd.equals("south") ||
                cmd.equals("east") || cmd.equals("west")) {
            if(!result.hasItem())
                goRoom(cmd);
            else
                System.out.println("Huh\n");
        }
        else if (cmd.equals("back")) {
            if(!result.hasItem())
                goBack();
            else
                System.out.println("back what?\n");
        }
        else if (cmd.equals("quit")) {
            if(!result.hasItem())
                bDone = true;
            else 
                System.out.println("Quit what?\n");
        }
        else if (cmd.equals("take")) {
            if(!result.hasItem())
                System.out.println("Take what?\n");
            else
                takeObject(item);
        }
        else if (cmd.equals("drop")) {
            if(!result.hasItem())
                System.out.println("Drop what?\n");
            else
                dropObject(item);
            
        }
        else if (cmd.equals("solve")) {
            if(!result.hasItem())
                bDone = trySolve();
            else
                System.out.println
                ("The command 'solve' has no second word.\n");
        }
        else if (cmd.equals("help"))
            printHelp();
        
        return bDone;
    }

    /**
     * Create the game object and initialise internal map and items.
     */
    public Game() {
        Room out, wentworth, corr1, corr2, tute, lab;
      
        // create the rooms
        out = new Room ("an outside area on campus");
        wentworth = new Room ("the Wentworth Building");
        corr1 = new Room ("a corridor");
        corr2 = new Room ("a corridor");
        tute = new Room ("a Comp101 tutorial room");
        lab = new Room ("a computer lab");
    
        // initialise room exits
        out.setExits (corr1, null, null, wentworth);
        wentworth.setExits (null, out, null, null);
        corr1.setExits (tute, corr2, out, null);
        corr2.setExits (lab, null, null, corr1);
        tute.setExits (null, lab, corr1, null);
        lab.setExits (null, null, corr2, tute);
    
        // create the items and place them in their rooms
        muffin = new Item ("muffin", "a chocolate chip muffin");
        wentworth.putObject (muffin);

        brain = new Item ("brain", "a nice looking brain");
        corr2.putObject (brain);

        problem = new Item ("problem", 
                "a PBL problem (not too hard, not too easy)");
        tute.putObject (problem);

        // now put all the items in an array to pass them to the parser
        Item items[] = {muffin, brain, problem};

        // create the parser (and inform it of existing items)
        parser = new Parser (items);

        // now initialise the current state
        curr_Room = out;
        last_Room = null;
        
        solve_Room = lab;
    }

    /**
     *  Main play routine.  Loops until end of play.
     */
    public void play() {
        
        boolean bFinish = false;
        
        Result result;
    
        System.out.println("\nWelcome to Maze!\n\n");
        System.out.println("Try to solve all your problems - good luck!\n");
        System.out.println("Type 'help' if you need help.\n\n");
        System.out.println(curr_Room+"\n");

        // enter the main command loop.  here we read a command and
        // then execute it until the game is over.
        
        while (!bFinish) {
                
            result = parser.getCommand ();

                if(result.getTwo_Words() && !result.hasItem())
                // if has_second_word is true and the item is
                // nil, then a word was entered that was not
                // known.
                System.out.println
                ("I don't know what you are talking about...\n");
            else
                bFinish = processCommand (result);
        }    
        System.out.println("\nThank you for playing.  Good bye.\n");
      }


//    Main routine for starting game.
//
//    public static void main(String[] argv) {
//        Game aGame = new Game();
//        aGame.play();
//    }
}
