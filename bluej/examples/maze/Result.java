public class Result
{
    private boolean two_words;
    private Command cmd = new Command();
    private Item item = new Item("","");

    /**
     * Constructor for Result class
     */
    public Result(Command cmd,Item item,boolean two_words)
    {
        this.cmd = cmd;
        this.item = item;
        this.two_words = two_words;
    }

    /**
     * return value of this Result's command instance variable
     */
    public Command getCommand()
    {
        return cmd;
    }

    /**
     * return value of this Result's item instance variable
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * returns boolean value representing two_words value
     */
    public boolean getTwo_Words()
    {
        return two_words;
    }
    
    /**
     * returns true if this object has an item associated with it that is not   
     * the default intialised value. 
     */
    public boolean hasItem()
    {
        return !item.equals("");
    }
}

