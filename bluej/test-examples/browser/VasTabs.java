/*
******************************************************************
*   Copyright (C) Vasile CALMATUI (vasile@chez.com) 07/1997      *
*                                                                *
* Permission is granted to use, copy, modify and distriubte this *
* software for NON-COMMERCIAL use. For commercial use, please    *
* contact the author. I'm not liable for damages resulting from  *
* the use of this software or its derivates.                     *
******************************************************************
* Feel free to contact me for help, comments, bugs,contributions,*
* job offers. Now I'm student in France, and if you have to offer*
* a job or a training anywhere in the world,  please contact me. *
******************************************************************
* e-mail: vasile@chez.com or vasile@club-internet.fr             *
* http://www.chez.com/vasile/                                    *
******************************************************************

VasTabs version 1.0, 07/1997
Makes in Java Windows-like tabbed panels
*/

import java.awt.*;

public class VasTabs extends Panel {
  //police used and his bold cousine :-)
  Font normalFont=new Font("Helvetica",Font.PLAIN,12);  
  Font boldFont=new Font("Helvetica",Font.BOLD,12);    
  //metrics of the bold fond
  FontMetrics fmBoldFont=getFontMetrics(boldFont); 
  
  //colours used (light and shadow)
  Color light=new Color(223,223,223);
  Color shadow=new Color(127,127,127);
    
  //layout used to manage the tabs
  CardLayout tabLayout=new CardLayout();
  //panel where the tabs are located
  Panel tabPanel=new Panel();  
  
  //how much pixels upper is the tab is selected
  static final int selUpper=2;

  //the width of the line to draw the tabs (1,2 or 3)
  static final int lineWidth=2;

  //Horizontal and Vertical space to make "round corners"
  static final int horRound=2;  
  static final int verRound=2;

  //where begin to draw the string
  static final int XTitle=4;
  static final int YTitle=2;
  
  //the total height of image of the tabs
  final int Z=fmBoldFont.getHeight()+2*lineWidth+2*YTitle+1;
  //total length and width of the tabs 
  int X=400;
  int Y=400;
    
  //information about each tab : name, beginning, end (in pixels)
  //so, 25 tabs at most=enough
  String arrName[]=new String[25];
  int arrEnd[]=new int[25];
  int arrBeg[]=new int[25];
  
  //current number of tabs
  int nbTab=0;
  //the selected tab
  int selected=0;

  //X,Y= total length and width of tabs (with the panels)
  //posX,posY : the left upper corner of the Tabs inside of the applet
  public VasTabs(int posX,int posY,int X,int Y) {
    this.X=X;
    this.Y=Y;
    this.reshape(posX,posY,X,Y);
    this.setBackground(Color.lightGray);
    this.setLayout(null);    
    tabPanel.reshape(lineWidth,Z+1,X-2*lineWidth,Y-Z-2*lineWidth);
    tabPanel.setLayout(tabLayout);
    this.add(tabPanel);
  }//end constructor

  //add a tab with the given name
  public void addTab(String name, Panel c) {
    if(nbTab>=arrBeg.length || c==this) return;
    arrName[nbTab]=name;
    arrBeg[nbTab]=(nbTab==0) ? 0 : ((nbTab==1) ? arrEnd[nbTab-1]-lineWidth : arrEnd[nbTab-1]);
    arrEnd[nbTab]=(nbTab==0) ? 2*XTitle+fmBoldFont.stringWidth(name)+3*lineWidth : arrBeg[nbTab]+2*XTitle+fmBoldFont.stringWidth(name)+2*lineWidth;
    //you cannot add twice the same Panel
    for(int i=0;i<nbTab;i++) if(tabPanel.getComponent(i)==c) return;
    //you cannot add tabs with the total width larger then the panel
    if(arrEnd[nbTab]>X) return;
    tabPanel.add(Integer.toString(nbTab), c);
    nbTab++;
  }//end addTab

  //remove a tab with this panel inside
  public void removeTab(Panel c) {
    Component[] allc=tabPanel.getComponents();
    //finding the tab
    for(int i=0;i<allc.length;i++) {
      if(allc[i]==c) {
        tabPanel.remove(c);
        select(0);
        int tabLength=arrEnd[i]-arrBeg[i];
        //recompute the dimension of tabs
        for(int j=i;j<nbTab-1;j++) {
          arrEnd[j]=arrEnd[j+1]-tabLength;
          arrBeg[j]=arrBeg[j+1]-tabLength;
          if(j==0) {arrEnd[j]+=lineWidth;arrBeg[j]+=lineWidth;}
          arrName[j]=arrName[j+1];          
        }
        nbTab--;
        paint(getGraphics());
        break;
      }
    }
  }//end removeTab

  //to rename a tab with this name
  public void renameTab(String oldName, String newName) {
    //finding the tab
    for(int i=0;i<nbTab;i++) {
      if(arrName[i].equals(oldName)) {
        int diff=fmBoldFont.stringWidth(newName)-fmBoldFont.stringWidth(oldName);
        //if the last is not larger then the panel
        if((arrEnd[nbTab-1])+diff>X) return;
        arrName[i]=new String(newName);          
        arrEnd[i]+=diff;
        //recompute the dimension of the tabs
        for(int j=i+1;j<nbTab;j++) {
          arrEnd[j]+=diff;
          arrBeg[j]+=diff;
        }
        paint(getGraphics());
        break;
      }
    }
  }//end renameTab

  //to select and show a tab
  public void select (int num) {
    if(num<0||num>nbTab||num==selected) return;
    //restore the size of the previously selected tab
    arrBeg[selected]+=lineWidth;
    arrEnd[selected]-=lineWidth;
    //selects and show the tab
    selected=num;
    tabLayout.show(tabPanel,Integer.toString(selected));
    //make "larger" the selected tab
    arrBeg[num]-=lineWidth;
    arrEnd[num]+=lineWidth;
    //redraws everything
    paint(getGraphics());    
  }//end select

  //draws the tabs and the outline
  public void paint(Graphics g){    
    //clear everything
    g.setColor(getBackground());
    g.fillRect(0,0,X,Z+1);
         
    //draws each tab one after one
    for (int curr=0;curr<nbTab;curr++) {
      //for the selected one, it's special
      int upper=(curr==selected) ? 0 : selUpper;
      g.setFont((curr==selected) ? boldFont : normalFont);
      
      g.setColor(shadow);
      for (int i=lineWidth;curr!=(selected-1) && i>0;i--) {
        if(i==1) g.setColor(Color.black);
        //draw the right line of the tab
        g.drawLine(arrEnd[curr]-i,upper+verRound,arrEnd[curr]-i,Z-lineWidth);
        //draw the right round corner=point on the diagonal line
        if(i==1) g.drawLine(arrEnd[curr]-lineWidth,upper+1,arrEnd[curr]-lineWidth,upper+1);
      }//fin for

      //draws the string
      g.setColor(Color.black);
      g.drawString(arrName[curr],arrBeg[curr]+lineWidth+XTitle,upper+YTitle+lineWidth+fmBoldFont.getAscent());
      
      g.setColor(Color.white);
      for (int i=0;i<lineWidth;i++) {
        if(i==1) g.setColor(light);
        //draws the upper line of the tab
        g.drawLine(arrBeg[curr]+horRound,upper+i,arrEnd[curr]-lineWidth-horRound+1,upper+i);      
        if(curr!=(selected+1)) {
          //draws the left line of the tab
          g.drawLine(arrBeg[curr]+i,upper+verRound,arrBeg[curr]+i,Z);
          //left round corner=black point on the diagonal line
          if(i==0)g.drawLine(arrBeg[curr]+1,upper+1,arrBeg[curr]+1,upper+1);
        }//end if
      }//end for
    }//end large for
    
    //makes the large outline
    g.setColor(Color.white);
    for (int i=0;i<lineWidth;i++) {
      if(i==1) g.setColor(light);
      //left line of the outline
      g.drawLine(i,Z,i,Y);
      //draws bottom line of tabs==upper of the outline
      //1:before the selected tab
      if(selected!=0) g.drawLine(0,Z-lineWidth+i+1,arrBeg[selected],Z-lineWidth+i+1);
      //2:behind the selected tab
      g.drawLine(arrEnd[selected],Z-lineWidth+i+1,X,Z-lineWidth+i+1);
    }//end for
    g.setColor(shadow);
    for (int i=lineWidth;i>0;i--) {
      if(i==1) g.setColor(Color.black);
      //draws the bottom line of the outline
      g.drawLine(0,Y-i,X,Y-i);
      //draws the right line of the outline
      g.drawLine(X-i,Z,X-i,Y);
    }//end for
    
  }//end paint

  public boolean mouseDown(Event event,int x,int y) {
    //compute what different tab was clicked
    if(y<Z) {
      for (int i=0;i<nbTab;i++) {
        if (x>arrBeg[i] && x<arrEnd[i] && i!=selected) {
          //restore the size of the previously selected tab
          arrBeg[selected]+=lineWidth;
          arrEnd[selected]-=lineWidth;
          //selects and show the tab
          selected=i;
          tabLayout.show(tabPanel,Integer.toString(selected));
          //make "larger" the selected tab
          arrBeg[i]-=lineWidth;
          arrEnd[i]+=lineWidth;
          //redraws everything
          paint(getGraphics());
          return true;
        }//end if
      }//end for
    }//end if
    return false;
  }//end mouseDown
}//end class
