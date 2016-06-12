/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.utility.javafx;

import java.io.OutputStream;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import bluej.views.FormattedPrintWriter;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 11/06/2016.
 */
@OnThread(value = Tag.FX, ignoreParent = true)
public class FXFormattedPrintWriter extends FormattedPrintWriter
{
    private final TextFlow flow = new TextFlow();
    private boolean bold = false;
    private boolean italic = false;

    public FXFormattedPrintWriter()
    {
        // PrintWriter needs to be passed a valid outputstream
        // even if we are going to not actually print to it.
        // We pass it the standard System output stream
        super(System.out);
    }

    @Override
    public void setBold(boolean bold)
    {
        this.bold = bold;
    }

    @Override
    public void setItalic(boolean italic)
    {
        this.italic = italic;
    }

    @Override
    protected void indentLine()
    {
        flow.getChildren().add(new Text("    "));
    }

    @Override
    public void println(String line)
    {
        Text t = new Text(line + "\n");
        // It seems we can't change bold and italic via CSS in TextFlow so we must do it in code:
        // We use Source Sans Pro so that we get italic, which System font on Mac doesn't support:
        t.setFont(Font.font("Source Sans Pro", bold ? FontWeight.BOLD : FontWeight.NORMAL, italic ? FontPosture.ITALIC : FontPosture.REGULAR, 16));
        flow.getChildren().add(t);
    }

    public Node getNode()
    {
        return flow;
    }
}
