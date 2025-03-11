import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NetworkSimulator {
    private List<Node> nodes;
    private final int NUM_NODES = 8;
    private final Random random = new Random();

    public NetworkSimulator()
    {
        nodes = new ArrayList<>();
        initializeNodes();
        connectNodes();
    }

    private void initializeNodes() {
        for (int i = 0; i < NUM_NODES; i++) {
            String ip = "127.0.0.1"; // Localhost for simplicity
            int port = 8100 + i; // Assigning unique ports
            int nodeId = random.nextInt(256); // For 8-bit ID space (0-255)
            Node node = new Node(ip, port, nodeId);
            nodes.add(node);
            System.out.println("Created node: " + ip + ":" + port + " with ID " + nodeId);
        }
    }

    private void connectNodes() {
        for (Node node : nodes) {
            for (Node neighbor : nodes) {
                if (node != neighbor) {
                    Integer distanceKey = node.getNodeInformation().getNODE_ID() ^ neighbor.getNodeInformation().getNODE_ID();
                    node.addToRoutingTable(distanceKey, neighbor);
                }
            }
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
