public class Result
{
    private boolean two_words;
    private Command cmd = new Command();
    private Item item = new Item("","");

    public Result(Command cmd,Item item,boolean two_words)
    {
        this.cmd = cmd;
        this.item = item;
        this.two_words = two_words;
    }

    public Command getCommand()
    {
        return cmd;
    }

    public Item getItem()
    {
        return item;
    }

    public boolean getTwo_Words()
    {
        return two_words;
    }
    
    public boolean hasItem()
    {
        return !item.equals("");
    }
}

