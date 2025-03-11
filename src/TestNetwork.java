public class TestNetwork {

    public static void displayDHT(NetworkSimulator simulator)
    {
        System.out.println("***** Displaying Entire DHT *****");
        for(Node node : simulator.getNodes())
        {
            System.out.println(node.getNodeInformation().getNODE_ID());
            node.displayHashTable();
        }
    }
    public static void main(String[] args) {
        NetworkSimulator simulator = new NetworkSimulator();
        Node node1 = simulator.getNodes().get(0);

        // Simulate adding a value to a random node
        int valueToStore = 42;

        // Simulate searching for the value from another node
        Node nearestNode = node1.findNearestNode(valueToStore);
        if (nearestNode != null) {
            System.out.println("Nearest node to value " + valueToStore + " found by Node ID " + node1.getNodeInformation().getNODE_ID() +
                    " is Node ID " + nearestNode.getNodeInformation().getNODE_ID());

            nearestNode.addValue(valueToStore);
        } else {
            System.out.println("No nearest node found.");
        }

        // Simulate adding a value to a random node
        valueToStore = 75;

        // Simulate searching for the value from another node
        nearestNode = node1.findNearestNode(valueToStore);
        if (nearestNode != null) {
            System.out.println("Nearest node to value " + valueToStore + " found by Node ID " + node1.getNodeInformation().getNODE_ID() +
                    " is Node ID " + nearestNode.getNodeInformation().getNODE_ID());

            nearestNode.addValue(valueToStore);
        } else {
            System.out.println("No nearest node found.");
        }

        displayDHT(simulator);
    }
}
