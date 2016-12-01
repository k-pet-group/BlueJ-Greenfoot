package bluej.utility;

import bluej.utility.javafx.BetterVBox;
import bluej.utility.javafx.HangingFlowPane;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Tests the HangingFlowPane class, specifically its layout algorithm.
 */
public class TestHangingFlowPane
{
    // Need to run tests on FX thread:
    @Rule
    public TestRule runOnFXThreadRule = new TestRule() {
        boolean initialised = false;
        @Override public Statement apply(Statement base, Description d) {
            if (!initialised)
            {
                // Initialise JavaFX:
                new JFXPanel();
                initialised = true;
            }
            return new Statement() {
                @Override public void evaluate() throws Throwable {
                    // Run on FX thread, rethrow any exceptions back on this thread:
                    CompletableFuture<Throwable> thrown = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        try
                        {
                            base.evaluate();
                            thrown.complete(null);
                        } catch (Throwable throwable)
                        {
                            thrown.complete(throwable);
                        }
                    });
                    Throwable t = thrown.get();
                    if (t != null)
                        throw t;
                }
            };
        }

    };

    /**
     * Helper class for testing: a Node with a fixed size.
     */
    private static class FixedSizeNode extends Canvas
    {
        // If baseline is not specified, superclass (bottom) is used instead
        private final Optional<Double> baseline;

        public FixedSizeNode(double width, double height)
        {
            super(width, height);
            baseline = Optional.empty();
        }

        public FixedSizeNode(double width, double height, double baseline)
        {
            super(width, height);
            this.baseline = Optional.of(baseline);
        }

        @Override
        public double getBaselineOffset()
        {
            // If we've set a baseline use it, otherwise delegate:
            if (baseline.isPresent())
                return baseline.get();
            else
                return super.getBaselineOffset();
        }
    }

    /**
     * Helper for constructing a HangingFlowPane with the given nodes.
     * Also performs the layout.
     */
    protected HangingFlowPane make(float width, float height, List<Node> ns)
    {
        return make(width, height, ns.toArray(new Node[0]));
    }

    /**
     * Helper for constructing a HangingFlowPane with the given nodes.
     * Also performs the layout.
     */
    protected HangingFlowPane make(float width, float height, Node... ns)
    {
        HangingFlowPane p = new HangingFlowPane(ns);
        p.setRowValignment(VPos.BASELINE);
        p.resize(width, height);
        p.requestLayout();
        p.layout();
        return p;
    }



    @Test
    public void testSingleFixed()
    {
        // One fixed size node should just go in the top left:
        final FixedSizeNode n = new FixedSizeNode(100.0, 50.0);
        HangingFlowPane p = make(500, 500, n);
        assertLayout(n, 0, 0, 100, 50);
    }

    @Test
    public void testTripleFixed()
    {
        // All three nodes fit on the same line, but should get baselines aligned:
        final FixedSizeNode n0 = new FixedSizeNode(100.0, 40.0);
        final FixedSizeNode n1 = new FixedSizeNode(90.0, 60.0);
        final FixedSizeNode n2 = new FixedSizeNode(120.0, 50.0);
        HangingFlowPane p = make(500, 500, n0, n1, n2);
        // Fixed size nodes have their baseline at the bottom, so they all align to the bottom:
        assertLayout(n0, 0, 20, 100, 40);
        assertLayout(n1, 100, 0, 90, 60);
        assertLayout(n2, 190, 10, 120, 50);
    }

    @Test
    public void testTripleFixedBaseline()
    {
        // Similar to testTripleFixed(), but we specify different baselines:
        final FixedSizeNode n0 = new FixedSizeNode(100.0, 40.0, 35);
        final FixedSizeNode n1 = new FixedSizeNode(90.0, 60.0, 40);
        final FixedSizeNode n2 = new FixedSizeNode(120.0, 50.0, 20);
        HangingFlowPane p = make(500, 500, n0, n1, n2);
        // Baseline will be at 40, the largest baseline:
        assertLayout(n0, 0, 5, 100, 40);
        assertLayout(n1, 100, 0, 90, 60);
        assertLayout(n2, 190, 20, 120, 50);
    }

    /**
     * Helper class wrapping a 50 high FixedSizeNode that remembers its width
     * and expected X position.  Can be right-aligned.
     */
    private static class TestNodeInfo
    {
        private final double width;
        private final double expectedX;
        private final FixedSizeNode node;

        public TestNodeInfo(double width, double expectedX)
        {
            this(width, expectedX, 50.0);
        }

        public TestNodeInfo(double width, double expectedX, double height)
        {
            this.width = width;
            this.expectedX = expectedX;
            this.node = new FixedSizeNode(width, height);
        }

        public TestNodeInfo(double width, double expectedX, double height, double baseline)
        {
            this.width = width;
            this.expectedX = expectedX;
            this.node = new FixedSizeNode(width, height, baseline);
        }
        
        Node getNode()
        {
            return node;
        }
    }


    // Shorthand for ™new TestNodeInfo"
    private TestNodeInfo n(float width, float expectedX)
    {
        return new TestNodeInfo(width, expectedX);
    }

    // Shorthand for ™new TestNodeInfo", that also sets right alignment on the node
    private TestNodeInfo nr(float width, float expectedX)
    {
        TestNodeInfo tni = new TestNodeInfo(width, expectedX);
        HangingFlowPane.setAlignment(tni.getNode(), HangingFlowPane.FlowAlignment.RIGHT);
        return tni;
    }

    // Shorthand for ™new TestNodeInfo", that also prevents breaking before this node
    private TestNodeInfo nb(float width, float expectedX)
    {
        TestNodeInfo tni = n(width, expectedX);
        HangingFlowPane.setBreakBefore(tni.getNode(), false);
        return tni;
    }
    
    // Shorthand for Arrays.asList
    private <T> List<T> l(T... xs)
    {
        return Arrays.asList(xs);
    }


    /**
     * Tests given nodes on a flow pane with width 500
     * @param hang The amount of hanging indent on lines after the first. 
     * @param nodes The list of lists is a list of expected rows.  These are flattened
     *              and passed to the HangingFLowPane, then we check if we get back the rows
     *              we expected.
     */
    private void testRows500(double hang, List<List<TestNodeInfo>> nodes)
    {
        HangingFlowPane p = make(500, 500, nodes.stream().flatMap(ns -> ns.stream().map(TestNodeInfo::getNode)).collect(Collectors.toList()));
        // make does layout, but we must layout again after setting the hanging indent:
        p.setHangingIndent(hang);
        p.requestLayout();
        p.layout();

        double y = 0;
        for (List<TestNodeInfo> row : nodes)
        {
            for (TestNodeInfo n : row)
            {
                assertLayout(n.getNode(), n.expectedX, y, n.width, 50);
            }
            y += 50;
        }
    }

    @Test
    public void testRowsNoHang()
    {
        // Whether first row is 500, or just under, last item ends up on row beneath:
        testRows500(0, l(
            l(n(240, 0), n(260, 240)),
            l(n(40, 0))
            ));
        testRows500(0, l(
            l(n(240, 0), n(259, 240)),
            l(n(40, 0))
        ));

        // Second item makes first row just over the 500, so moves to next row:
        testRows500(0, l(
            l(n(240, 0)),
            l(n(261, 0), n(40, 261))
        ));
    }

    @Test
    public void testRowsHang()
    {
        // Versions of testRowsNoHang, but with hanging indent.
        // First row has no indent, so 500 fits:
        testRows500(20, l(
            l(n(240, 0), n(260, 240)),
            l(n(40, 20))
        ));
        // But 501 doesn't:
        testRows500(20, l(
            l(n(240, 0)),
            l(n(261, 20), n(40, 281))
        ));

        // Over-large item always on row by itself:
        testRows500(20, l(
            l(n(501, 0))
        ));
        testRows500(20, l(
            l(n(1, 0)),
            l(n(501, 20))
        ));
        testRows500(20, l(
            l(n(1, 0)),
            l(n(501, 20)),
            l(n(1, 20))
        ));
    }

    @Test
    public void testRightAlign()
    {
        // Right aligned item ends up... right-aligned!
        testRows500(20.0, l(l(n(100, 0), nr(100, 400))));

        testRows500(20.0, l(l(n(300, 0)), l(nr(300, 200))));
    }

    // We're dealing with simple integers, so only need a small comparison delta:
    private static double e = 0.000001;

    private static void assertLayout(Node n, double x, double y, double width, double height)
    {
        assertEquals("X", x, n.getLayoutX(), e);
        assertEquals("Y", y, n.getLayoutY(), e);
        assertEquals("Width", width, n.getLayoutBounds().getWidth(), e);
        assertEquals("Height", height, n.getLayoutBounds().getHeight(), e);
    }

    @Test
    public void testFixedNoBreak()
    {
        final FixedSizeNode n0 = new FixedSizeNode(100.0, 40.0);
        final FixedSizeNode n1 = new FixedSizeNode(90.0, 60.0);
        final FixedSizeNode n2 = new FixedSizeNode(80.0, 50.0);
        final FixedSizeNode n3 = new FixedSizeNode(70.0, 70.0);
        HangingFlowPane.setBreakBefore(n2, false);
        HangingFlowPane p = make(200, 500, n0, n1, n2, n3);
        // Fixed size nodes have their baseline at the bottom, so they all align to the bottom.
        // Normally, we'd expect {n0, n1}, {n2, n3}.  But due to no-breakconstraint,
        // we should get {n0}, {n1, n2}, {n3}:
        assertLayout(n0, 0, 0, 100, 40);
        assertLayout(n1, 0, 40, 90, 60);
        assertLayout(n2, 90, 40 + 10, 80, 50);
        assertLayout(n3, 0, 40 + 60, 70, 70);
    }
    
    @Test
    public void testNoBreak()
    {
        testRows500(0, l(
            l(n(300, 0)),
            // The 100 should fit above, but the trailing no-break forces on to new row:
            l(n(100, 0), nb(200, 100))
        ));
        
        testRows500(20, l(
            l(n(300, 0)),
            // Would fit above, but no break drags it down:
            l(n(100, 20), nb(200, 120)),
            // Ditto:
            l(n(110, 20), nb(210, 130)),
            l(n(400, 20)),
            // And again:
            l(n(110, 20), nb(210, 130), n(50, 340)),
            l(n(400, 20))
        ));

        testRows500(0, l(
            l(n(300, 0)),
            // The break here should be disallowed, but HFP will have no choice but to break:
            l(n(100, 0), nb(200, 100)),
            l(nb(400, 0))
        ));
    }
    
    @Test
    public void testHeight()
    {
        checkHeight(0);
        checkHeight(0,  new TestNodeInfo(10, 0, 0));
        checkHeight(17,  new TestNodeInfo(10, 0, 0, 17));
        checkHeight(50, n(10, 0), n(20, 10));
        checkHeight(50, n(300, 0));
        checkHeight(50, n(300, 0), n(100, 300));
        checkHeight(50, n(300, 0), n(199, 300));
        checkHeight(50, n(300, 0), n(200, 300));
        checkHeight(100, n(300, 0), n(201, 0));
        checkHeight(100, n(300, 0), n(300, 0));

        checkHeight(50, n(300, 0), n(198, 300), n(1, 498));
        checkHeight(50, n(300, 0), n(199, 300), n(1, 499));
        checkHeight(100, n(300, 0), n(200, 300), n(1, 0));
        
    }
    
    // Makes the computePrefHeight method public
    private static class TestBetterVBox extends BetterVBox
    {
        public TestBetterVBox(double minWidth)
        {
            super(minWidth);
        }

        public double computePrefHeight(double width)
        {
            return super.computePrefHeight(width);
        }
    }

    private void checkHeight(double expectedHeight, TestNodeInfo... content)
    {
        TestBetterVBox surround = new TestBetterVBox(0) {
            @Override
            public double getLeftMarginFor(Node n)
            {
                return 3;
            }

            @Override
            public double getRightMarginFor(Node n)
            {
                return 2;
            }
        };
        HangingFlowPane hfp = new HangingFlowPane();
        surround.getChildren().add(hfp);
        surround.resize(505, 1000);
        hfp.setRowValignment(VPos.BASELINE);
        hfp.getChildren().setAll(Arrays.stream(content).map(TestNodeInfo::getNode).collect(Collectors.toList()));
        surround.requestLayout();
        surround.layout();


        assertEquals("Checking height", expectedHeight, hfp.getHeight(), 0.001);
        assertEquals("Checking height", expectedHeight, surround.computePrefHeight(505), 0.001);
    }
}
