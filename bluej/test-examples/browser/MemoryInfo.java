import java.awt.*;

public class MemoryInfo extends Panel
{

   public static final boolean DEBUG = false;
   public MemoryInfo()
   {
   }

   public void paint( Graphics g )
   { 
     Dimension thisSize = this.getSize();


     FontMetrics fm = g.getFontMetrics( g.getFont() );    
     

     int centeredX = ( thisSize.width - 100 - fm.stringWidth( "00% free" ) ) / 2;
     int centeredY = ( thisSize.height - 20 - fm.getHeight() ) / 2;

     centeredX -= ( fm.stringWidth( "00% free" ) / 2 );

     g.setColor( Color.black );
     g.drawRect( centeredX, centeredY, 100, 20 );

 
     int free = getPercentage();

     g.setColor( Color.red );
     g.fillRect( centeredX + 1, centeredY + 1, free, 19 );

     

     g.setColor( Color.black );
     for ( int i = 0; i < 100; i += 10 )
       g.drawLine( centeredX + i, centeredY, centeredX + i, centeredY + 20 );


     g.drawString( free + "% free", centeredX + 100 + fm.stringWidth( " " ), centeredY + fm.getHeight() ); 

   }

   public int getPercentage()
   { Runtime sys = Runtime.getRuntime();

     long free = sys.freeMemory();
     long total = sys.totalMemory();

     int percentageFree = (int)( ( ( free * 100 ) / total ) );

     if ( DEBUG )
     { System.out.println( "Free " + free );
       System.out.println( "Total " + total );
       System.out.println( "Percentage Free " + percentageFree );
     }

     return( percentageFree );
   }

   public static void main( String args[] )
   { Frame f = new Frame();
     f.add( new MemoryInfo() );
     f.setSize( 400, 400 );
     f.setVisible( true );
   }

}