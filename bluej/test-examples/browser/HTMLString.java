import java.util.*;

/** This is a small class that encapsulated the parsing of a HTML string,
    its methods allow the text and the tags to be extracted.
 */
public class HTMLString
{
  private String html;

  private StringTokenizer st;

  private boolean insideTag;

  private boolean previousInsideTag = false;

  private String URLLocation;
 
/** Creates a HTMLString with the contents specified. */
 public HTMLString( String base, String contents )
 { html = contents;
   this.URLLocation = base;
   insideTag = false;
   st = new StringTokenizer( contents, "<>\n\r \t", true );
 }



 public boolean hasMoreTokens( )
 { return ( st.hasMoreTokens() );
 }



 public boolean insideTag()
 { return ( insideTag );
 }


 public String getURLBase()
 { return ( this.URLLocation );
 }

 public void reset()
 { insideTag = false;
   previousInsideTag = false;
   this.st = new StringTokenizer( html, "<>\n\r", true );
 }


 public String getNext( )
 { if ( st.hasMoreTokens() )
   { String token = st.nextToken();

     insideTag = previousInsideTag;    

     if ( token.equals( "<" ) )
     { previousInsideTag = true;
       insideTag = true;
     }

     else if ( token.equals( ">" ) )
     { previousInsideTag = false;
       insideTag = true;
     }

     else if ( token.equals( "\n" ) )
     { return( "" );
     }

     else if ( token.equals( "\r" ) )
     { return( "" );
     }

     else if ( token.equals( "\t" ) )
     { return( "" );
     }

     return ( token );
   }
   else
     return ( "" );
 }

 }

    



