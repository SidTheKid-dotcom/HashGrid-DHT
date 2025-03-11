import java.util.ArrayList;

public class TestNetwork {

    public static void displayDHT(NetworkSimulator simulator)
    {
        System.out.println("***** Displaying Entire DHT *****");
        for(Node node : simulator.getNodes())
        {
            System.out.println(node.getNodeInformation().getNODE_ID()+": ");
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

        simulator.addNode("127.0.0.1", 9000, 200);

        int newValue = 202;

        // Simulate searching for the value from another node
        nearestNode = node1.findNearestNode(newValue);
        if (nearestNode != null) {
            System.out.println("Nearest node to value " + newValue + " found by Node ID " + node1.getNodeInformation().getNODE_ID() +
                    " is Node ID " + nearestNode.getNodeInformation().getNODE_ID());

            nearestNode.addValue(newValue);
        } else {
            System.out.println("No nearest node found.");
        }

        int nodeID = simulator.findKey(202);
        if(nodeID != -1)
        {
            System.out.println("Value found locally in Node ID " + nodeID);
        }
        else
        {
            System.out.println("Key not found in the network");
        }

        displayDHT(simulator);
    }
}
