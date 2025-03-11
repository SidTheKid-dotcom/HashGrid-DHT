import java.util.*;

public class NetworkSimulator {
    private List<Node> nodes;
    private final int NUM_NODES = 8;
    private final Random random = new Random();

    private static final int K_BUCKET_SIZE = 3;

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
            TreeMap<Integer, Node> potentialNeighbors = new TreeMap<>();

            for (Node neighbor : nodes) {
                if (node != neighbor) {
                    Integer distanceKey = node.getNodeInformation().getNODE_ID() ^ neighbor.getNodeInformation().getNODE_ID();
                    potentialNeighbors.put(distanceKey, neighbor); // TreeMap auto-sorts based on distanceKey
                }
            }

            int count = 0;
            for (Map.Entry<Integer, Node> entry : potentialNeighbors.entrySet()) {
                //if (count >= K_BUCKET_SIZE) break;

                node.addToRoutingTable(entry.getKey(), entry.getValue());
                count++;
            }
        }
    }


    public void addNode(String IP, int UDP_PORT, int NODE_ID) {
        Node newNode = new Node(IP, UDP_PORT, NODE_ID);
        nodes.add(newNode);

        // Step 1: Update existing nodes' routing tables
        for (Node existingNode : nodes) {
            if (existingNode != newNode) {
                Integer distanceKey = existingNode.getNodeInformation().getNODE_ID() ^ newNode.getNodeInformation().getNODE_ID();

                // Add the new node to the routing table if space allows
                List<Node> bucket = existingNode.getRoutingTable().computeIfAbsent(distanceKey, k -> new ArrayList<>());
                /*if (bucket.size() < K_BUCKET_SIZE) {
                    bucket.add(newNode);
                } else {
                    // If bucket is full, check if the new node is closer than the farthest node
                    bucket.sort(Comparator.comparingInt(t -> t.getNodeInformation().getNODE_ID())); // Sort by XOR distance
                    if (distanceKey < bucket.get(bucket.size() - 1).getNodeInformation().getNODE_ID()) {
                        bucket.remove(bucket.size() - 1); // Remove farthest
                        bucket.add(newNode); // Add new node
                    }
                }*/
                bucket.add(newNode);
            }
        }

        // Step 2: Populate the new node’s routing table
        for (Node existingNode : nodes) {
            if (existingNode != newNode) {
                Integer distanceKey = newNode.getNodeInformation().getNODE_ID() ^ existingNode.getNodeInformation().getNODE_ID();

                // Add only if space allows
                List<Node> bucket = newNode.getRoutingTable().computeIfAbsent(distanceKey, k -> new ArrayList<>());
                if (bucket.size() < K_BUCKET_SIZE) {
                    bucket.add(existingNode);
                } else {
                    // Same logic as above, replace farthest node if necessary
                    bucket.sort(Comparator.comparingInt(t -> t.getNodeInformation().getNODE_ID()));
                    if (distanceKey < bucket.get(bucket.size() - 1).getNodeInformation().getNODE_ID()) {
                        bucket.remove(bucket.size() - 1);
                        bucket.add(existingNode);
                    }
                }
            }
        }

        System.out.println("Successfully added node to network node, displaying node ID: "+newNode.getNodeInformation().getNODE_ID());
    }

    public Node findNode(int NODE_ID)
    {
        for(Node node : nodes)
        {
            if(node.getNodeInformation().getNODE_ID() == NODE_ID)
            {
                return node;
            }
        }

        return null;
    }

    public void removeNode(int NODE_ID)
    {
        Node nodeToDelete = findNode(NODE_ID);
        if (nodeToDelete == null) {
            System.out.println("Node ID " + NODE_ID + " not found in network.");
            return;
        }

        // Delete the node
        nodes.remove(nodeToDelete);

        // Update Routing Tables of All Remaining Nodes
        for (Node node : nodes) {
            Map<Integer, List<Node>> routingTable = node.getRoutingTable();

            for (Iterator<Map.Entry<Integer, List<Node>>> iterator = routingTable.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Integer, List<Node>> entry = iterator.next();
                List<Node> nodeList = entry.getValue();

                nodeList.removeIf(n -> n.getNodeInformation().getNODE_ID() == NODE_ID);

                // If the list becomes empty, remove the key to keep routing table clean
                if (nodeList.isEmpty()) {
                    iterator.remove();
                }
            }
        }

        // Redistribute Hash Table
        Map<String, Integer> table = nodeToDelete.getHashTable();
        for(Map.Entry<String, Integer> entry : table.entrySet())
        {
            int key = entry.getValue();
            addKey(key);
        }

        System.out.println("Successfully redistributed hash table");
    }

    public void addKey(int key)
    {
        Node node = nodes.get(0);
        Node nearestNode = node.findNearestNode(key);
        if (nearestNode != null) {
            System.out.println("Nearest node to value " + key + " found by Node ID " + node.getNodeInformation().getNODE_ID() +
                    " is Node ID " + nearestNode.getNodeInformation().getNODE_ID());

            nearestNode.addValue(key);
        } else {
            System.out.println("No nearest node found.");
        }
    }

    public int findKey(int searchKey)
    {
        Node node = nodes.get(0);
        return node.findValue(searchKey, new HashSet<Integer>());
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
