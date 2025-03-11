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

        // Simulate adding a value to a random node
        int valueToStore = 42;
        simulator.addKey(valueToStore);

        simulator.addNode("127.0.0.1", 9000, 200);

        int newValue = 200;
        simulator.addKey(newValue);

        int nodeID = simulator.findKey(200);
        if(nodeID != -1)
        {
            System.out.println("Value found locally in Node ID " + nodeID);
        }
        else
        {
            System.out.println("Key not found in the network");
        }

        displayDHT(simulator);
        System.out.println("*****");
        simulator.removeNode(200);
        System.out.println("*****");
        displayDHT(simulator);

    }
}
