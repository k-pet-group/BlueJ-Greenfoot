 public interface ShowMessage
{  /** Show a message in the browser status bar */
   public abstract void showStatus( String s );

   /** Get a URL, and display it in the main browser window */
   public abstract void getURL( String url, boolean addToHistory );

   /** Save any settings in a file */
   public abstract void saveSettings( );

   /** Return a handle to the browser */
   public abstract WebBrowser getBrowser();
}
