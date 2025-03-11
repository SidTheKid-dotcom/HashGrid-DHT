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

    public void addToRoutingTable(Integer key, Node neighbor) {
        routingTable.computeIfAbsent(key, k -> new ArrayList<>()).add(neighbor);
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
        int min_xor = Integer.MAX_VALUE;
        Node nearest_node = null;
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
