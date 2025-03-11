import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
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

    public int findValue(int searchKey) {
        String hash = generateSHA1(String.valueOf(searchKey));
        if (hashTable.containsKey(hash)) {
            System.out.println("Value found locally: " + hashTable.get(hash));
            return hashTable.get(hash);
        } else {
            Node nearestNode = findNearestNode(searchKey);
            System.out.println("Forwarding request to node ID: " + nearestNode.getNodeInformation().getNODE_ID());
            // Simulate calling the method on the nearest node
            // For a real system, this would be a network call
            return -1;
        }
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

    private void displayRoutingTable()
    {
        if(routingTable.size() == 0)
        {
            return;
        }

        System.out.println("Displaying Routing Table");
        for(Map.Entry<Integer, List<Node>> entry : routingTable.entrySet())
        {
            Integer key = entry.getKey();
            List<Node> tripletList = entry.getValue();
            System.out.println(key+": ");
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
