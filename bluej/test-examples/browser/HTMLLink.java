 

public class HTMLLink
{

   private int x;
   private int y;
   private String target;
   private int width;
   private int height;

   public HTMLLink( int x, int y, String url, int fontw, int fonth )
   { this.x = x;
     this.y = y;
     this.target = url;
     this.width = fontw;
     this.height = fonth;
   }

   public String getTarget()
   { return this.target;
   }

   public boolean isInside( int X, int Y )
   { if ( ! ( ( X > this.x ) && ( X < ( this.x + this.width ) ) ) )
       return false;
     if ( ! ( ( Y > ( this.y - this.height ) ) && ( Y < this.y ) ) )
       return false;

     return true;
   }

}