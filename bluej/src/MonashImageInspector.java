   import java.awt.*;
   import javax.swing.*;
   import java.awt.image.*;
   import com.sun.jdi.*;

    public class MonashImageInspector extends bluej.debugger.Inspector {
   
      private BufferedImage bufferedImage;
      private JPanel panel;
   
       public  String getInspectedClassname() {
         return "MonashImage";
      }
   
       public  String getInspectorTitle() {
         return "MonashImage";
      }
   
       public boolean initialize(bluej.debugger.DebuggerObject obj) {
         boolean initOK=super.initialize(obj);
         if(!initOK || obj.getObjectReference()==null) 
            return false;
         panel=
                new JPanel(null){
                   public void paint(Graphics g){
                     Graphics2D g2=(Graphics2D)g;
                     boolean result = g2.drawImage(bufferedImage, 0, 0, null);
                  }
               };
         add(new JScrollPane(panel),BorderLayout.CENTER);
         refresh();
         return true;
      }
   
       public void refresh() {
         com.sun.jdi.Field dataField = obj.getObjectReference().referenceType().fieldByName("data");
         ArrayReference img = (ArrayReference)obj.getObjectReference().getValue(dataField);
         int height = img.length();
         ArrayReference scanLineReference=((ArrayReference)img.getValue(0));
         int width = scanLineReference.length();
         bufferedImage = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_RGB);
         WritableRaster raster = (WritableRaster)bufferedImage.getData();
         panel.setPreferredSize(new Dimension(width,height));
         panel.setMinimumSize(new Dimension(width,height));
         panel.setMaximumSize(new Dimension(width,height));
         int[] rgb = new int[3];
         for(int y = 0; y < height; y++) {
            scanLineReference=((ArrayReference)img.getValue(y));
            ShortValue [] scanLine=(ShortValue [])scanLineReference.getValues().toArray(new ShortValue [0]);
            for(int x = 0; x < width; x++) {
               short pixel=((ShortValue)scanLine[x]).value();
               rgb[0] = pixel;
               rgb[1] = pixel;
               rgb[2] = pixel;
               raster.setPixel(x, y, rgb);
            }
         }
         bufferedImage.setData(raster);
      }
   }
