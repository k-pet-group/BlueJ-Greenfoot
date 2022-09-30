/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018,2022  Michael Kolling and John Rosenberg 
 
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
package bluej;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * A JUnit {@link Rule} for running tests on the JavaFX thread and performing
 * JavaFX initialisation.  To include in your test case, add the following code:
 *
 * <pre>
 * {@literal @}Rule
 * public JavaFXThreadingRule jfxRule = new JavaFXThreadingRule();
 * </pre>
 *
 * @author Andy Till
 *
 * From: http://andrewtill.blogspot.co.uk/2012/10/junit-rule-for-javafx-controller-testing.html
 *
 */
public class JavaFXThreadingRule implements TestRule
{

    /**
     * Flag for setting up the JavaFX, we only need to do this once for all tests.
     */
    private static boolean jfxIsSetup;

    @Override
    public Statement apply(Statement statement, Description description) {

        return new OnJFXThreadStatement(statement);
    }

    private static class OnJFXThreadStatement extends Statement
    {

        private final Statement statement;

        public OnJFXThreadStatement(Statement aStatement) {
            statement = aStatement;
        }

        private Throwable rethrownException = null;

        @Override
        public void evaluate() throws Throwable {

            if(!jfxIsSetup) {
                setupJavaFX();

                jfxIsSetup = true;
            }

            final CountDownLatch countDownLatch = new CountDownLatch(1);

            rethrownException = null;
            
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        statement.evaluate();
                    } catch (Throwable e) {
                        rethrownException = e;
                    }
                    countDownLatch.countDown();
                }});

            if (!countDownLatch.await(60, TimeUnit.SECONDS))
            {
                fail("Timed out waiting for test completion during " + statement.toString());
            }

            // if an exception was thrown by the statement during evaluation,
            // then re-throw it to fail the test
            if(rethrownException != null) {
                throw rethrownException;
            }
        }
        protected void setupJavaFX() throws Throwable
        {
            System.out.println("javafx initialising...");
            System.out.flush();
            long timeMillis = System.currentTimeMillis();

            final CountDownLatch latch = new CountDownLatch(1);
            
            rethrownException = null;

            SwingUtilities.invokeLater(() -> {
                // initializes JavaFX environment
                try
                {
                    new JFXPanel();
                }
                catch (Throwable e)
                {
                    rethrownException = e;
                }

                latch.countDown();
            });

            
            if (!latch.await(10, TimeUnit.SECONDS))
            {
                if (rethrownException != null)
                {
                    throw rethrownException;
                }
                else
                {
                    throw new RuntimeException("Timed out awaiting JavaFX initialisation");
                }
            }
            System.out.println("javafx is initialised in " + (System.currentTimeMillis() - timeMillis) + "ms");
        }

    }
}
