import java.util.List;
public class Test {
    public static void main(String[] args) {
        System.out.println("=== STARTING COMPREHENSIVE DHT NETWORK TEST ===");

        try {
            // Section 1: Network Initialization
            System.out.println("\n=== 1. NETWORK INITIALIZATION ===");
            NetworkSimulator simulator = new NetworkSimulator();

            // Create an initial network with nodes spread across the ID space
            System.out.println("Creating initial nodes with distributed IDs...");
            simulator.addNode("127.0.0.1", 9001, 20);   // Node ID 20
            simulator.addNode("127.0.0.1", 9002, 60);   // Node ID 60
            simulator.addNode("127.0.0.1", 9003, 100);  // Node ID 100
            simulator.addNode("127.0.0.1", 9004, 140);  // Node ID 140
            simulator.addNode("127.0.0.1", 9005, 180);  // Node ID 180
            simulator.addNode("127.0.0.1", 9006, 220);  // Node ID 220

            // Allow time for node discovery
            System.out.println("Waiting for nodes to discover each other...");
            Thread.sleep(2000);

            // Display network state
            simulator.displayNetworkState();

            // Section 2: Routing Table Verification
            System.out.println("\n=== 2. ROUTING TABLE VERIFICATION ===");
            System.out.println("Displaying routing tables of all nodes:");
            for (Node node : simulator.getNodes()) {
                System.out.println("\nRouting table for Node ID: " + node.getNodeInformation().getNODE_ID());
                node.displayRoutingTable();
            }

            // Section 3: Key Storage and Retrieval
            System.out.println("\n=== 3. KEY STORAGE AND RETRIEVAL ===");
            int[] testKeys = {25, 70, 120, 160, 190, 230};

            // Store keys
            System.out.println("Adding keys to the network...");
            for (int key : testKeys) {
                System.out.println("Adding key: " + key);
                simulator.addKey(key);
            }

            // Allow time for key distribution
            Thread.sleep(1000);

            // Display hash tables to verify key storage
            System.out.println("\nHash tables after adding keys:");
            for (Node node : simulator.getNodes()) {
                System.out.println("\nNode ID: " + node.getNodeInformation().getNODE_ID() + " Hash Table:");
                node.displayHashTable();
            }

            // Test key lookups
            System.out.println("\nLooking up keys:");
            for (int key : testKeys) {
                int foundAt = simulator.findKey(key);
                System.out.println("Key " + key + " lookup result: " +
                        (foundAt != -1 ? "Found at Node " + foundAt : "Not found"));
            }

            // Test lookup for non-existent key
            int nonExistentKey = 999;
            int foundAt = simulator.findKey(nonExistentKey);
            System.out.println("Non-existent key " + nonExistentKey + " lookup result: " +
                    (foundAt != -1 ? "Found at Node " + foundAt : "Not found (expected)"));

            // Section 4: Network Dynamics - Adding Nodes
            System.out.println("\n=== 4. NETWORK DYNAMICS - ADDING NODES ===");

            // Add new nodes
            System.out.println("Adding new nodes to the network...");
            simulator.addNode("127.0.0.1", 9007, 40);   // Node ID 40
            simulator.addNode("127.0.0.1", 9008, 150);  // Node ID 150

            // Allow time for integration
            System.out.println("Waiting for new nodes to integrate...");
            Thread.sleep(2000);

            // Refresh routing tables
            System.out.println("Refreshing routing tables...");
            simulator.refreshRoutingTables();
            Thread.sleep(1000);

            // Display updated network state
            simulator.displayNetworkState();

            // Test key lookups again to ensure they still work after network changes
            System.out.println("\nLooking up keys after adding new nodes:");
            for (int key : testKeys) {
                foundAt = simulator.findKey(key);
                System.out.println("Key " + key + " lookup result: " +
                        (foundAt != -1 ? "Found at Node " + foundAt : "Not found"));
            }

            // Section 5: Network Dynamics - Removing Nodes
            System.out.println("\n=== 5. NETWORK DYNAMICS - REMOVING NODES ===");

            // Remove a node and see if keys are redistributed
            System.out.println("Removing Node with ID 100...");
            simulator.removeNode(100);

            // Allow time for the network to stabilize
            System.out.println("Waiting for network to stabilize...");
            Thread.sleep(2000);

            // Display updated network state
            simulator.displayNetworkState();

            // Test key lookups again to ensure redistribution
            System.out.println("\nLooking up keys after removing a node:");
            for (int key : testKeys) {
                foundAt = simulator.findKey(key);
                System.out.println("Key " + key + " lookup result: " +
                        (foundAt != -1 ? "Found at Node " + foundAt : "Not found"));
            }

            // Section 6: Node Lookup Testing
            System.out.println("\n=== 6. NODE LOOKUP TESTING ===");

            // Test node lookup for existing node
            int nodeIdToLookup = 180;
            System.out.println("Looking up Node ID: " + nodeIdToLookup);
            List<Triplet> closestNodes = simulator.lookupNode(nodeIdToLookup);

            System.out.println("Found " + closestNodes.size() + " closest nodes:");
            for (Triplet nodeInfo : closestNodes) {
                System.out.println("  Node ID: " + nodeInfo.getNODE_ID() +
                        ", IP: " + nodeInfo.getIP_ADDR() +
                        ", Port: " + nodeInfo.getUDP_PORT());
            }

            // Test node lookup for non-existent node
            int nonExistentNodeId = 255;
            System.out.println("\nLooking up non-existent Node ID: " + nonExistentNodeId);
            closestNodes = simulator.lookupNode(nonExistentNodeId);

            System.out.println("Found " + closestNodes.size() + " closest nodes:");
            for (Triplet nodeInfo : closestNodes) {
                System.out.println("  Node ID: " + nodeInfo.getNODE_ID() +
                        ", IP: " + nodeInfo.getIP_ADDR() +
                        ", Port: " + nodeInfo.getUDP_PORT());
            }

            // Section 7: Network Resilience Test
            System.out.println("\n=== 7. NETWORK RESILIENCE TEST ===");
            System.out.println("Testing network resilience...");
            simulator.testNetworkResilience();

            // Section 8: Edge Cases
            System.out.println("\n=== 8. EDGE CASES ===");

            // Test adding a node with existing ID
            System.out.println("Testing adding a node with existing ID (60)...");
            if (simulator.nodeExists(60)) {
                System.out.println("Node with ID 60 already exists.");
                simulator.addNode("127.0.0.1", 9009, 60);
                System.out.println("Attempt to add duplicate node completed.");
            }

            // Test adding a key to an empty network (should be handled gracefully)
            System.out.println("\nCreating a separate empty simulator for edge case testing...");
            NetworkSimulator emptySimulator = new NetworkSimulator();
            System.out.println("Testing adding a key to empty network...");
            emptySimulator.addKey(42);

            // Test finding a key in an empty network
            System.out.println("Testing finding a key in empty network...");
            foundAt = emptySimulator.findKey(42);
            System.out.println("Key 42 lookup result in empty network: " +
                    (foundAt != -1 ? "Found at Node " + foundAt : "Not found (expected)"));

            // Section 9: Final Network State
            System.out.println("\n=== 9. FINAL NETWORK STATE ===");
            System.out.println("Displaying final state of the network:");
            simulator.displayNetworkState();

            // Display routing tables of remaining nodes
            System.out.println("\nFinal routing tables:");
            for (Node node : simulator.getNodes()) {
                System.out.println("\nRouting table for Node ID: " + node.getNodeInformation().getNODE_ID());
                node.displayRoutingTable();
            }

            // Display hash tables of remaining nodes
            System.out.println("\nFinal hash tables:");
            for (Node node : simulator.getNodes()) {
                System.out.println("\nNode ID: " + node.getNodeInformation().getNODE_ID() + " Hash Table:");
                node.displayHashTable();
            }

            // Section 10: Clean Shutdown
            System.out.println("\n=== 10. CLEAN SHUTDOWN ===");
            System.out.println("Shutting down network simulators...");
            simulator.shutdown();
            emptySimulator.shutdown();

            System.out.println("\n=== COMPREHENSIVE DHT NETWORK TEST COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.err.println("\nTest failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}