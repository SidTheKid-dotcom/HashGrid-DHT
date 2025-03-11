import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Test {
    public static void main(String[] args) {
        System.out.println("\n******** Initializing Kademlia Network ********");
        NetworkSimulator simulator = new NetworkSimulator();

        System.out.println("\n******** Displaying Initial Routing Tables ********");
        displayRoutingTables(simulator);

        System.out.println("\n******** Testing Value Storage and Lookup ********");
        testValueStorage(simulator);

        System.out.println("\n******** Testing Node Addition ********");
        testNodeAddition(simulator);

        System.out.println("\n******** Testing Key Lookup After Node Addition ********");
        testKeyLookupAfterAddition(simulator);

        System.out.println("\n******** Testing Node Removal ********");
        testNodeRemoval(simulator);
    }

    // Display routing tables for debugging
    private static void displayRoutingTables(NetworkSimulator simulator) {
        for (Node node : simulator.getNodes()) {
            node.displayRoutingTable();
        }
    }

    // Test storing and retrieving values in the network
    private static void testValueStorage(NetworkSimulator simulator) {
        Random random = new Random();
        List<Node> nodes = simulator.getNodes();

        // Select a random node to store values
        Node storageNode = nodes.get(random.nextInt(nodes.size()));
        int[] testValues = {42, 99, 150, 255};

        for (int value : testValues) {
            System.out.println("\nStoring value: " + value + " at Node ID: " + storageNode.getNodeInformation().getNODE_ID());
            storageNode.addValue(value);

            // Try finding the value
            int nodeID = simulator.findKey(value);
            if (nodeID != -1) {
                System.out.println("✅ Value " + value + " found in Node ID: " + nodeID);
            } else {
                System.out.println("❌ Key " + value + " not found in the network!");
            }
        }
    }

    // Test adding a new node and verifying if routing tables update correctly
    private static void testNodeAddition(NetworkSimulator simulator) {
        int newNodeID = 200;
        System.out.println("\nAdding new node with ID: " + newNodeID);
        simulator.addNode("127.0.0.1", 9000, newNodeID);

        System.out.println("\nVerifying if new node is part of the routing tables...");
        displayRoutingTables(simulator);
    }

    // Test key lookup after adding a node
    private static void testKeyLookupAfterAddition(NetworkSimulator simulator) {
        int newTestValue = 202;
        List<Node> nodes = simulator.getNodes();
        Node randomNode = nodes.get(new Random().nextInt(nodes.size()));

        System.out.println("\nStoring new value " + newTestValue + " in Node ID: " + randomNode.getNodeInformation().getNODE_ID());
        randomNode.addValue(newTestValue);

        int foundNodeID = simulator.findKey(newTestValue);
        if (foundNodeID != -1) {
            System.out.println("✅ Value " + newTestValue + " found in Node ID: " + foundNodeID);
        } else {
            System.out.println("❌ Key " + newTestValue + " not found!");
        }
    }

    // Simulate removing a node and verifying consistency
    private static void testNodeRemoval(NetworkSimulator simulator) {
        List<Node> nodes = simulator.getNodes();
        if (nodes.size() < 2) {
            System.out.println("Not enough nodes to test removal.");
            return;
        }

        Node nodeToRemove = nodes.get(0);
        System.out.println("\nRemoving Node ID: " + nodeToRemove.getNodeInformation().getNODE_ID());
        simulator.getNodes().remove(nodeToRemove);

        System.out.println("\nVerifying routing tables after removal...");
        displayRoutingTables(simulator);
    }
}
