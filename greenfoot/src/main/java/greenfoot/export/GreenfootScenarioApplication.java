/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018,2021  Poul Henriksen and Michael Kolling
 
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
package greenfoot.export;

import greenfoot.core.Simulation;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;

public class GreenfootScenarioApplication extends Application
{
    @Override
    @OnThread(Tag.FXPlatform)
    public void start(Stage primaryStage) throws Exception
    {
        Platform.setImplicitExit(true);
        GreenfootScenarioViewer greenfootScenarioViewer = new GreenfootScenarioViewer();
        Scene scene = new Scene(greenfootScenarioViewer);
        scene.getStylesheets().add("greenfoot.css");
        primaryStage.setScene(scene);        
        primaryStage.show();
        primaryStage.setOnHiding(e -> {
            Simulation.getInstance().abort();
            
            // Fail safe: if we haven't exited after a second, force exit:
            Thread exiter = new Thread("Greenfoot exit")
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ex)
                    {
                    }
                    System.exit(1);
                }
            };
            // Don't let the exiter fallback prevent us exiting normally:
            exiter.setDaemon(true);
            exiter.start();
        });
    }
}
