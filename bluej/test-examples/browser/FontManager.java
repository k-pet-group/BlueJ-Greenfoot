 import java.awt.*;
import java.util.*;

/** This is a stack class that allows the web browser to keep checking
   the current font, as well as pushing new ones, and popping old ones.
   */
public class FontManager
{
  private Stack fontStack;
  private Font defaultFont;

  public FontManager( Font normal_font )
  { this.fontStack = new Stack();
    this.defaultFont = normal_font;
  }

  public void push( Font fnt )
  { fontStack.push( fnt );
  }

  public Font pop()
  { if ( !fontStack.empty() )
    { return ( (Font)fontStack.pop() );
    }
    else
      return( defaultFont );
  }

  public Font getFont()
  { if ( !fontStack.empty() )
    { return ( (Font)fontStack.peek() );
    }
    else
      return( defaultFont );
  }
}
