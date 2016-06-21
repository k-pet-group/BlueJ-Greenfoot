/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2016  Michael Kolling and John Rosenberg
 
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
package bluej.testmgr;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A panel with test run counters.
 *
 * @author  Andrew Patterson (derived from JUnit src)
 */
@OnThread(Tag.FXPlatform)
public class CounterPanel extends HBox
{
    private Label fNumberOfErrors;
    private Label fNumberOfFailures;
    private Label fTotalTime;
    private Label fNumberOfRuns;
    final static Image fFailureIcon = Config.getFixedImageAsFXImage("failure.gif");
    final static Image fErrorIcon = Config.getFixedImageAsFXImage("error.gif");

    private int fTotal;

    public CounterPanel()
    {
        fNumberOfErrors= createOutputField(5);
        fNumberOfFailures= createOutputField(5);
        fNumberOfRuns= createOutputField(9);
        fTotalTime = createOutputField(9);

        getChildren().addAll(
                new Label(Config.getString("testdisplay.counter.runs")),
                fNumberOfRuns,
                new Label(Config.getString("testdisplay.counter.errors")),
                new ImageView(fErrorIcon),
                fNumberOfErrors,
                new Label(Config.getString("testdisplay.counter.failures")),
                new ImageView(fFailureIcon),
                fNumberOfFailures,
                new Label(Config.getString("testdisplay.counter.totalTime")),
                fTotalTime
        );
    }

    private Label createOutputField(int width) {
        //TODO add CSS class as param
        return new Label("0");
    }

    public void reset() {
        setLabelValue(fNumberOfErrors, 0);
        setLabelValue(fNumberOfFailures, 0);
        setLabelValue(fTotalTime, 0);
        setLabelValue(fNumberOfRuns, 0);
        fTotal= 0;
    }

    public void setTotal(int value) {
        fTotal= value;
    }

    public void setRunValue(int value) {
        fNumberOfRuns.setText(Integer.toString(value) + "/" + fTotal);
    }

    public void setErrorValue(int value) {
        setLabelValue(fNumberOfErrors, value);
    }

    public void setFailureValue(int value) {
        setLabelValue(fNumberOfFailures, value);
    }

    public void setTotalTime(int value) {
        fTotalTime.setText(Integer.toString(value)+"ms");
    }

    private void setLabelValue(Label label, int value) {
        label.setText(Integer.toString(value));
    }
}