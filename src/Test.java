public class Test {
    public static void main(String[] args) {
        System.out.println("Starting DHT Network Test");

        try {
            // Initialize network simulator
            NetworkSimulator simulator = new NetworkSimulator();

            // Create a small network of nodes with different IDs
            System.out.println("\n=== Creating Initial Nodes ===");
            simulator.addNode("127.0.0.1", 9100, 10);  // Node ID 10
            simulator.addNode("127.0.0.1", 9200, 50);  // Node ID 50
            simulator.addNode("127.0.0.1", 9300, 90);  // Node ID 90
            simulator.addNode("127.0.0.1", 9400, 130); // Node ID 130
            simulator.addNode("127.0.0.1", 9500, 170); // Node ID 170

            // Give time for UDP servers to initialize and exchange ping messages
            System.out.println("\nWaiting for nodes to discover each other...");
            Thread.sleep(2000);

            // Display routing tables to confirm node discovery
            System.out.println("\n=== Routing Tables After Initial Discovery ===");
            for (Node node : simulator.getNodes()) {
                node.displayRoutingTable();
                System.out.println();
            }

            // Add some keys to the network
            System.out.println("\n=== Adding Keys to the Network ===");
            int[] keysToAdd = {15, 55, 90, 135, 175, 210};
            for (int key : keysToAdd) {
                System.out.println("Adding key: " + key);
                simulator.addKey(key);
            }

            Thread.sleep(5000);

            // Display hash tables to confirm key storage
            System.out.println("\n=== Hash Tables After Adding Keys ===");
            for (Node node : simulator.getNodes()) {
                System.out.println("Node ID: " + node.getNodeInformation().getNODE_ID());
                node.displayHashTable();
                System.out.println();
            }

            // Test key lookups
            System.out.println("\n=== Testing Key Lookups ===");
            for (int key : keysToAdd) {
                int nodeId = simulator.findKey(key);
                System.out.println("Key " + key + " found at node ID: " +
                        (nodeId != -1 ? nodeId : "Not found"));
            }

            // Test adding a new node to the network
            System.out.println("\n=== Adding a New Node ===");
            simulator.addNode("127.0.0.1", 9600, 200);  // Node ID 200

            // Give time for the new node to be integrated
            System.out.println("Waiting for the new node to integrate...");
            Thread.sleep(2000);

            // Refresh routing tables to ensure the new node is discovered
            System.out.println("Refreshing routing tables...");
            simulator.refreshRoutingTables();
            Thread.sleep(1000);

            // Display routing tables after adding the new node
            System.out.println("\n=== Routing Tables After Adding New Node ===");
            for (Node node : simulator.getNodes()) {
                node.displayRoutingTable();
                System.out.println();
            }

            // Test removing a node
            System.out.println("\n=== Removing a Node ===");
            simulator.removeNode(50);  // Remove node with ID 50

            // Give time for the network to stabilize
            System.out.println("Waiting for the network to stabilize...");
            Thread.sleep(1000);

            // Display routing tables after removing a node
            System.out.println("\n=== Routing Tables After Removing Node ===");
            for (Node node : simulator.getNodes()) {
                node.displayRoutingTable();
                System.out.println();
            }

            // Test lookups after node removal to ensure keys were redistributed
            System.out.println("\n=== Testing Key Lookups After Node Removal ===");
            for (int key : keysToAdd) {
                int nodeId = simulator.findKey(key);
                System.out.println("Key " + key + " found at node ID: " +
                        (nodeId != -1 ? nodeId : "Not found"));
            }

            // Shutdown the network
            System.out.println("\n=== Shutting Down Network ===");
            simulator.shutdown();

            System.out.println("\nDHT Network Test Completed Successfully");

        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}