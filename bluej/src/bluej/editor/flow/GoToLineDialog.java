/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2016,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.flow;

import javafx.stage.Window;

import bluej.Config;
import bluej.utility.javafx.dialog.InputDialog;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Dialog for user to input a line number to traverse source file in editor
 * 
 * @author Bruce Quig
 */
@OnThread(Tag.FXPlatform)
public class GoToLineDialog extends InputDialog<Integer>
{
    private static final String goToLineTitle = Config.getString("editor.gotoline.title");
    private static final String goToLineLabel = Config.getString("editor.gotoline.label");
    private static final String notNumericMessage = Config.getString("editor.gotoline.notNumericMessage");
    private int rangeMax;

    public GoToLineDialog(Window parent)
    {
        super(goToLineTitle, goToLineLabel, Config.getString("editor.gotoline.prompt"), "goto-line-dialog");
        initOwner(parent);
        setOKEnabled(false);
    }

    public void setRangeMax(int max)
    {
        this.rangeMax = max;
        setPrompt(goToLineLabel +  " ( 1 - " + max + " )");
    }

    @Override
    protected Integer convert(String number)
    {
        try
        {
            return Integer.parseInt(number);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    @Override
    protected boolean validate(String oldInput, String newInput)
    {
        Integer n = convert(newInput);
        if (n == null)
        {
            setErrorText(notNumericMessage);
            setOKEnabled(!oldInput.equals(""));
            return newInput.equals("");
        }
        else if (n >= 1 && n <= rangeMax)
        {
            setErrorText("");
            setOKEnabled(true);
            return true;
        }
        else
        {
            setErrorText("");
            setOKEnabled(!oldInput.equals(""));
            return false;
        }
    }
} // end class GoToLineDialog
