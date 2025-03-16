/*
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestNetwork {

    public static void displayDHT(NetworkSimulator simulator) {
        System.out.println("\n***** Displaying Entire DHT *****");
        for (Node node : simulator.getNodes()) {
            System.out.println("Node ID " + node.getNodeInformation().getNODE_ID() + " (" +
                    node.getNodeInformation().getIP_ADDR() + ":" +
                    node.getNodeInformation().getUDP_PORT() + ")");
            System.out.println("Hash Table Contents:");
            node.displayHashTable();
            System.out.println("Routing Table Contents:");
            node.displayRoutingTable();
            System.out.println("----------------------------");
        }
    }

    public static void testBasicOperations() {
        System.out.println("\n===== TESTING BASIC OPERATIONS =====");
        NetworkSimulator simulator = new NetworkSimulator();

        // Display initial network state
        System.out.println("Initial network state:");
        displayDHT(simulator);

        // Test adding values to the network
        System.out.println("\nAdding values to the network:");
        int[] valuesToAdd = {42, 100, 255, 1024, 7777};
        for (int value : valuesToAdd) {
            System.out.println("\nAdding value: " + value);
            simulator.addKey(value);
        }

        // Display network after adding values
        System.out.println("\nNetwork state after adding values:");
        displayDHT(simulator);

        // Test finding values
        System.out.println("\nFinding values in the network:");
        for (int value : valuesToAdd) {
            int nodeID = simulator.findKey(value);
            if (nodeID != -1) {
                System.out.println("Value " + value + " found in Node ID " + nodeID);
            } else {
                System.out.println("Value " + value + " not found in the network");
            }
        }

        // Test finding a non-existent value
        int nonExistentValue = 9999;
        System.out.println("\nAttempting to find non-existent value: " + nonExistentValue);
        int nodeID = simulator.findKey(nonExistentValue);
        if (nodeID != -1) {
            System.out.println("Value " + nonExistentValue + " found in Node ID " + nodeID);
        } else {
            System.out.println("Value " + nonExistentValue + " not found in the network");
        }
    }

    public static void testNodeAdditionAndRemoval() {
        System.out.println("\n===== TESTING NODE ADDITION AND REMOVAL =====");
        NetworkSimulator simulator = new NetworkSimulator();

        // Add some initial values
        int[] initialValues = {123, 456, 789};
        for (int value : initialValues) {
            simulator.addKey(value);
        }

        // Display initial state
        System.out.println("Initial network state:");
        displayDHT(simulator);

        // Add a new node
        System.out.println("\nAdding a new node with ID 200:");
        simulator.addNode("192.168.1.100", 9000, 200);

        // Display network after adding node
        System.out.println("Network state after adding node:");
        displayDHT(simulator);

        // Add a new value that should be stored on the new node
        System.out.println("\nAdding value 200 to the network:");
        simulator.addKey(200);

        // Verify the value is stored
        int nodeID = simulator.findKey(200);
        if (nodeID != -1) {
            System.out.println("Value 200 found in Node ID " + nodeID);
        } else {
            System.out.println("Value 200 not found in the network");
        }

        // Remove the node
        System.out.println("\nRemoving node with ID 200:");
        simulator.removeNode(200);

        // Display network after removing node
        System.out.println("Network state after removing node:");
        displayDHT(simulator);

        // Verify the values are still accessible
        System.out.println("\nVerifying values are still accessible after node removal:");
        for (int value : initialValues) {
            nodeID = simulator.findKey(value);
            if (nodeID != -1) {
                System.out.println("Value " + value + " found in Node ID " + nodeID);
            } else {
                System.out.println("Value " + value + " not found in the network");
            }
        }

        // Try to remove a non-existent node
        System.out.println("\nAttempting to remove non-existent node with ID 999:");
        simulator.removeNode(999);
    }

    public static void testRoutingTable() {
        System.out.println("\n===== TESTING ROUTING TABLE FUNCTIONALITY =====");

        // Create a network with a specific node ID to test routing table
        Node testNode = new Node("127.0.0.1", 8000, 128, true); // ID 128 (binary 10000000)

        // Create nodes with varying XOR distances
        Node node1 = new Node("127.0.0.1", 8001, 129, true);  // ID 129 (binary 10000001) - XOR distance 1
        Node node2 = new Node("127.0.0.1", 8002, 160, true);  // ID 160 (binary 10100000) - XOR distance 32
        Node node3 = new Node("127.0.0.1", 8003, 192, true);  // ID 192 (binary 11000000) - XOR distance 64
        Node node4 = new Node("127.0.0.1", 8004, 0, true);    // ID 0 (binary 00000000) - XOR distance 128

        // Add nodes to routing table
        System.out.println("Adding nodes to test routing table:");
        testNode.addToRoutingTable(node1);
        testNode.addToRoutingTable(node2);
        testNode.addToRoutingTable(node3);
        testNode.addToRoutingTable(node4);

        // Display routing table
        System.out.println("\nRouting table for node with ID 128:");
        testNode.displayRoutingTable();

        // Test bucket index calculation
        System.out.println("\nTesting bucket index calculation:");
        System.out.println("Bucket index for node 129: " + testNode.getBucketIndex(129));
        System.out.println("Bucket index for node 160: " + testNode.getBucketIndex(160));
        System.out.println("Bucket index for node 192: " + testNode.getBucketIndex(192));
        System.out.println("Bucket index for node 0: " + testNode.getBucketIndex(0));

        // Test finding nearest node
        System.out.println("\nTesting nearest node search:");
        int testValue = 130;
        Node nearest = testNode.findNearestNode(testValue);
        System.out.println("Nearest node to value " + testValue + " is Node ID " +
                nearest.getNodeInformation().getNODE_ID());

        // Test removing a node from routing table
        System.out.println("\nRemoving node 129 from routing table:");
        testNode.removeFromRoutingTable(129);
        System.out.println("Routing table after removal:");
        testNode.displayRoutingTable();
    }

    public static void testHashTable() {
        System.out.println("\n===== TESTING HASH TABLE FUNCTIONALITY =====");
        NetworkSimulator simulator = new NetworkSimulator();

        Node testNode = simulator.getNodes().get(0);

        // Add values to hash table
        System.out.println("Adding values to hash table:");
        int[] valuesToAdd = {42, 73, 151, 256};
        for (int value : valuesToAdd) {
            Node node = testNode.findNearestNode(value);
            node.addValue(value);
            System.out.println("Added value: " + value);
        }

        // Display hash table
        for (Node node : simulator.getNodes()) {
            System.out.println("Displaying hash table for: "+node.getNodeInformation().getNODE_ID());
            node.displayHashTable();
        }

        // Test finding values
        System.out.println("\nTesting value lookup:");
        Set<Integer> visited = new HashSet<>();
        for (int value : valuesToAdd) {
            int result = testNode.findValue(value, visited);
            System.out.println("Searching for value " + value + ": " +
                    (result != -1 ? "Found in node ID " + result : "Not found"));
            visited.clear();
        }

        // Test finding a non-existent value
        int nonExistentValue = 9999;
        int result = testNode.findValue(nonExistentValue, visited);
        System.out.println("Searching for non-existent value " + nonExistentValue + ": " +
                (result != -1 ? "Found in node ID " + result : "Not found"));
    }

    public static void main(String[] args) {
        System.out.println("=== KADEMLIA DHT IMPLEMENTATION TEST ===\n");

        // Test all functionality
        testBasicOperations();
        testNodeAdditionAndRemoval();
        testRoutingTable();
        testHashTable();

        System.out.println("\n=== COMPREHENSIVE TEST COMPLETE ===");
    }
}*/
