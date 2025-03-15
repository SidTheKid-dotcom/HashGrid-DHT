import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Set;
public class Node {
    private Map<String, Integer> hashTable;
    private Map<Integer, List<Node>> routingTable;
    private static final int K_BUCKET_SIZE = 3;
    private static final int BUCKET_COUNT = 32;
    public Triplet node_information;

    public Node()
    {
        node_information = new Triplet();
        hashTable = new HashMap<>();
        routingTable = new HashMap<>();
    }

    public Node(String IP_ADDR, int UDP_PORT, int NODE_ID) {
        node_information = new Triplet(IP_ADDR, UDP_PORT, NODE_ID);
        hashTable = new HashMap<>();
        routingTable = new HashMap<>();
    }

    public Node getSelfNode()
    {
        return this;
    }

    public Triplet getNodeInformation()
    {
        return node_information;
    }

    private String generateSHA1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(input.getBytes());

            // Convert byte array to hex string
            Formatter formatter = new Formatter();
            for (byte b : hashBytes) {
                formatter.format("%02x", b);
            }
            String sha1 = formatter.toString();
            formatter.close();
            return sha1;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addValue(int value)
    {
        String hash = generateSHA1(String.valueOf(value));
        hashTable.put(hash, value);
    }


    public int findValue(int searchKey, Set<Integer> visited) {
        // Generate SHA-1 hash of the key
        String hash = generateSHA1(String.valueOf(searchKey));

        // Step 1: Check if value exists locally
        if (hashTable.containsKey(hash)) {
            return node_information.getNODE_ID();
        }

        // Step 2: Add this node to visited set
        visited.add(node_information.getNODE_ID());

        // Step 3: Search in the routing table
        for (Map.Entry<Integer, List<Node>> entry : routingTable.entrySet()) {
            for (Node node : entry.getValue()) {
                if (!visited.contains(node.getNodeInformation().getNODE_ID())) {  // Avoid revisiting nodes
                    int result = node.findValue(searchKey, visited);
                    if (result != -1) {
                        return result;  // Stop as soon as we find the value
                    }
                }
            }
        }

        // search key not found
        return -1;
    }

    public void addToRoutingTable(Node neighbor) {
        int bucketIndex = getBucketIndex(neighbor.getNodeInformation().getNODE_ID());

        // Get the bucket
        List<Node> bucket = routingTable.computeIfAbsent(bucketIndex, k -> new ArrayList<>());

        // Prevent duplicate entries
        for (Node n : bucket) {
            if (n.getNodeInformation().getNODE_ID() == neighbor.getNodeInformation().getNODE_ID()) {
                return;
            }
        }

        if (bucket.size() < K_BUCKET_SIZE) {
            bucket.add(neighbor);
        } else {
            // Replacement strategy: Remove the oldest node (FIFO)
            bucket.remove(0);
            bucket.add(neighbor);
        }
    }

    public void removeFromRoutingTable(int NODE_ID)
    {
        int bucketIndex = getBucketIndex(NODE_ID);
        List<Node> bucket = routingTable.computeIfAbsent(bucketIndex, k -> new ArrayList<>());
        bucket.removeIf(node -> node.getNodeInformation().getNODE_ID() == NODE_ID);
    }


    public void displayHashTable()
    {
        for(Map.Entry<String, Integer> entry : hashTable.entrySet())
        {
            System.out.println("\t"+entry.getKey()+" "+entry.getValue());
        }
    }

    public Map<String, Integer> getHashTable()
    {
        return hashTable;
    }

    public Map<Integer, List<Node>> getRoutingTable()
    {
        return routingTable;
    }

    public int getBucketIndex(int nodeID) {
        int xorDistance = node_information.getNODE_ID() ^ nodeID;

        // Avoid log(0) issue: If the XOR distance is 0, return the highest bucket
        if (xorDistance == 0) return BUCKET_COUNT - 1;

        // Compute bucket index as log2(xorDistance)
        return BUCKET_COUNT - 1 - Integer.numberOfLeadingZeros(xorDistance);
    }
    public void displayRoutingTable()
    {
        if(routingTable.size() == 0)
        {
            return;
        }

        System.out.println("Displaying Routing Table for node ID: "+node_information.getNODE_ID());
        for(Map.Entry<Integer, List<Node>> entry : routingTable.entrySet())
        {
            Integer key = entry.getKey();
            List<Node> tripletList = entry.getValue();
            //System.out.println(key+": ");
            for(Node node : tripletList)
            {
                node.getNodeInformation().display();
            }
        }
    }

    public Node findNearestNode(int value)
    {
        int min_xor = value ^ node_information.getNODE_ID();
        Node nearest_node = getSelfNode();
        for(Map.Entry<Integer, List<Node>> entry : routingTable.entrySet())
        {
            for(Node nearby_node : entry.getValue())
            {
                int curr_xor = value ^ nearby_node.getNodeInformation().getNODE_ID();
                if(curr_xor < min_xor)
                {
                    min_xor = curr_xor;
                    nearest_node = nearby_node;
                }
            }
        }

        return nearest_node;
    }
}
