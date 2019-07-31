package se306.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se306.scheduler.graph.Node;
import se306.scheduler.logic.Scheduler;

/**
 * This class tests various methods within the se306.scheduler.DotFile class
 */
public class DotFileTest {

    private Scheduler scheduler = Scheduler.getScheduler();

    private DotFile dot;
    
    @BeforeEach
    void graphSetup() {
        
    }

    @AfterEach
    void graphReset(){
        scheduler.clearGraph();
    }
    
    /**
     * Test that a graph can be successfully loaded from a file
     */
    @Test
    void testLoadGraph() {
        try {
            dot = new DotFile("test_data/test1.dot");
            dot.read();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found");
        }
        
        List<Node> nodes = scheduler.getNodes();
        Node nodeA = nodes.get(0);
        Node nodeB = nodes.get(1);
        Node nodeC = nodes.get(2);
        Node nodeD = nodes.get(3);
        
        assertEquals(2, nodeA.getChildren().size(), "Node A should have 2 children");
        assertTrue(nodeA.getChildren().containsKey(nodeB) && nodeA.getChildren().containsKey(nodeC),
                "Node A should have Nodes B & C as its children");
        assertEquals(1, nodeB.getChildren().size(), "Node B should have 1 child");
        assertTrue(nodeB.getChildren().containsKey(nodeD), "Node B should have Node D as its child");
        assertEquals(1, nodeC.getChildren().size(), "Node C should have 1 child");
        assertTrue(nodeC.getChildren().containsKey(nodeD), "Node C should have Node D as its child");
        assertEquals(0, nodeD.getChildren().size(), "Node D should have 0 children");
    }
    
    /**
     * Test that a graph can be successfully saved to a file
     * 
     * TODO: don't use hashmap because we don't know the key ordering
     */
    @Test
    void testWriteGraph() {
        Node nodeA = new Node("a", 2);
        Node nodeB = new Node("b", 3);
        Node nodeC = new Node("c", 1);
        Node nodeD = new Node("d", 2);
        Node nodeE = new Node("e", 1);
        
        scheduler.addNode(nodeA);
        scheduler.addNode(nodeB);
        scheduler.addNode(nodeC);
        scheduler.addNode(nodeD);
        scheduler.addNode(nodeE);

        scheduler.addChild(nodeA.getName(), nodeC.getName(), 1);
        scheduler.addChild(nodeA.getName(), nodeD.getName(), 1);
        scheduler.addChild(nodeB.getName(), nodeD.getName(), 1);
        scheduler.addChild(nodeC.getName(), nodeE.getName(), 1);
        scheduler.addChild(nodeD.getName(), nodeE.getName(), 1);

        scheduler.schedule();
        
        dot = new DotFile("");
        String outFile = "test_data/test1_out.dot";
        String outFileValid = "test_data/test1_out_valid.dot";
        try {
            dot.write(outFile, scheduler.getNodes());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not save to test_data/test1_out.dot");
        }

        // https://stackoverflow.com/a/3403112
        try {
            Scanner outScanner = new Scanner(new File(outFile));
            Scanner outScannerValid = new Scanner(new File(outFileValid));
            
            String outContent = outScanner.useDelimiter("\\Z").next();
            String outContentValid = outScannerValid.useDelimiter("\\Z").next();
            assertEquals(outContentValid, outContent);

            outScanner.close();
            outScannerValid.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("Shouldn't happen");
        }
    }
}