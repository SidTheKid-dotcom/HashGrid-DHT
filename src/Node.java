import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    private final Map<String, Integer> hashTable;
    private final Map<Integer, List<Triplet>> routingTable;
    private static final int K_BUCKET_SIZE = 3;
    private static final int BUCKET_COUNT = 32;
    private DatagramSocket socket;
    private static final int BUFFER_SIZE = 1024;
    public Triplet node_information;

    public Node()
    {
        node_information = new Triplet();
        hashTable = new ConcurrentHashMap<>();
        routingTable = new ConcurrentHashMap<>();
        startUDPServer();
    }

    public Node(String IP_ADDR, int UDP_PORT, int NODE_ID, boolean startServer) {
        node_information = new Triplet(IP_ADDR, UDP_PORT, NODE_ID);
        hashTable = new ConcurrentHashMap<>();
        routingTable = new ConcurrentHashMap<>();
        if(startServer) startUDPServer();
    }

    private void startUDPServer() {
        new Thread(() -> {
            try {
                socket = new DatagramSocket(node_information.getUDP_PORT());
                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    handleIncomingPacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void handleIncomingPacket(DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength());
        InetAddress senderAddress = packet.getAddress();
        int senderPort = packet.getPort();

        if (message.startsWith("PING")) {
            // Parse the incoming PING message to extract node information
            String[] parts = message.split(" ");
            String incomingIP = parts[1];
            int incomingPort = Integer.parseInt(parts[2]);
            int incomingNodeID = Integer.parseInt(parts[3]);

            // Create a new Node object with the received information
            Triplet incomingNodeInfo = new Triplet(incomingIP, incomingPort, incomingNodeID);

            // Add the node to routing table
            addToRoutingTable(incomingNodeInfo);

            // Send PONG response
            String response = "PONG" + " " + node_information.getIP_ADDR() + " " + node_information.getUDP_PORT() + " " + node_information.getNODE_ID();
            DatagramPacket pongPacket = new DatagramPacket(response.getBytes(), response.length(), senderAddress, senderPort);
            socket.send(pongPacket);
        } else if (message.startsWith("PONG")) {

            String[] parts = message.split(" ");
            String incomingIP = parts[1];
            int incomingPort = Integer.parseInt(parts[2]);
            int incomingNodeID = Integer.parseInt(parts[3]);

            // Create a new Node object with the received information
            Triplet receivedNodeInfo = new Triplet(incomingIP, incomingPort, incomingNodeID);
            addToRoutingTable(receivedNodeInfo);
        }
        else if (message.startsWith("FIND_KEY")) {
            String[] parts = message.split(" ");
            int searchKey = Integer.parseInt(parts[1]);
            String callerIP = parts[2];
            int callerPort = Integer.parseInt(parts[3]);
            int callerNodeID = Integer.parseInt(parts[4]);

            // Add the sender to our routing table
            Triplet callerNodeInfo = new Triplet(callerIP, callerPort, callerNodeID);
            addToRoutingTable(callerNodeInfo);

            // Generate SHA-1 hash of the key
            String hash = generateSHA1(String.valueOf(searchKey));

            // Check if we have the key locally
            if (hashTable.containsKey(hash)) {
                // We found the key, send FOUND_KEY response
                String response = "FOUND_KEY " + node_information.getNODE_ID();
                DatagramPacket responsePacket = new DatagramPacket(
                        response.getBytes(), response.length(),
                        senderAddress, senderPort
                );
                socket.send(responsePacket);
            } else {
                // We don't have the key, send our k-closest nodes
                List<Triplet> closestNodes = findKClosestNodesToNode(K_BUCKET_SIZE);

                StringBuilder responseBuilder = new StringBuilder("CLOSEST_NODES");
                for (Triplet node : closestNodes) {
                    responseBuilder.append(" ").append(node.getIP_ADDR())
                            .append(" ").append(node.getUDP_PORT())
                            .append(" ").append(node.getNODE_ID());
                }

                String response = responseBuilder.toString();
                DatagramPacket responsePacket = new DatagramPacket(
                        response.getBytes(), response.length(),
                        senderAddress, senderPort
                );
                socket.send(responsePacket);
            }
        }
    }

    public void sendPing(Node targetNode) {
        try {
            DatagramSocket pingSocket = new DatagramSocket();
            String message = "PING" + " " + node_information.getIP_ADDR() + " " + node_information.getUDP_PORT() + " " + node_information.getNODE_ID();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(targetNode.getNodeInformation().getIP_ADDR()), targetNode.getNodeInformation().getUDP_PORT());
            pingSocket.send(packet);
            pingSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPingKClosest() {
        try {
            DatagramSocket pingSocket = new DatagramSocket();

            List<Triplet> KClosestNodes = findKClosestNodesToNode(3);

            for(Triplet targetNodeInfo : KClosestNodes)
            {
                String message = "PING" + " " + targetNodeInfo.getIP_ADDR() + " " + targetNodeInfo.getUDP_PORT() + " " + targetNodeInfo.getNODE_ID();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName(targetNodeInfo.getIP_ADDR()), targetNodeInfo.getUDP_PORT());
                pingSocket.send(packet);
            }
            pingSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int sendFindKey(Triplet targetNode, int searchKey) {
        try {
            DatagramSocket findKeySocket = new DatagramSocket();
            String message = "FIND_KEY " + searchKey + " " + node_information.getIP_ADDR() + " " +
                    node_information.getUDP_PORT() + " " + node_information.getNODE_ID();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(targetNode.getIP_ADDR()),
                    targetNode.getUDP_PORT());
            findKeySocket.send(packet);

            // Set timeout for response
            findKeySocket.setSoTimeout(5000); // 5 seconds timeout

            // Prepare to receive response
            byte[] responseBuffer = new byte[BUFFER_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            findKeySocket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            findKeySocket.close();

            // Parse the response
            if (response.startsWith("FOUND_KEY")) {
                String[] parts = response.split(" ");
                int nodeID = Integer.parseInt(parts[1]);
                return nodeID;  // Return the ID of the node that has the key
            } else if (response.startsWith("CLOSEST_NODES")) {
                // This would be processed by the findKey method
                return -2;  // Indicating we received closest nodes
            }

            return -1; // Key not found

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
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

    public void addKey(int value)
    {
        String hash = generateSHA1(String.valueOf(value));
        hashTable.put(hash, value);
    }

    public int startFindKey(int searchKey) {
        // Generate SHA-1 hash of the key
        String hash = generateSHA1(String.valueOf(searchKey));

        // Check if value exists locally
        if (hashTable.containsKey(hash)) {
            return node_information.getNODE_ID();
        }

        // If not found locally, start recursive lookup
        Set<Integer> queried = new HashSet<>();
        List<Triplet> closestNodes = findKClosestNodesToValue(searchKey, K_BUCKET_SIZE);

        // Clone the list to avoid modification issues
        List<Triplet> nodesToQuery = new ArrayList<>(closestNodes);

        // Continue until we've queried all nodes or found the value
        while (!nodesToQuery.isEmpty()) {
            // Sort nodes by XOR distance to the key
            nodesToQuery.sort(Comparator.comparingInt(node -> searchKey ^ node.getNODE_ID()));

            // Get the closest node we haven't queried yet
            Triplet nextNode = null;
            for (Triplet node : nodesToQuery) {
                if (!queried.contains(node.getNODE_ID())) {
                    nextNode = node;
                    break;
                }
            }

            if (nextNode == null) {
                // We've queried all known nodes
                return -1;
            }

            // Mark this node as queried
            queried.add(nextNode.getNODE_ID());
            nodesToQuery.remove(nextNode);

            // Send FIND_KEY request to the closest node
            int result = sendFindKey(nextNode, searchKey);

            if (result >= 0) {
                // Key found at node with ID = result
                return result;
            }

            // If we received k-closest nodes, we'll requery in the next iteration
        }

        // If we've queried all nodes and didn't find the key
        return -1;
    }

    public void addToRoutingTable(Triplet nodeInfo) {
        int bucketIndex = getBucketIndex(nodeInfo.getNODE_ID());

        // Get the bucket
        List<Triplet> bucket = routingTable.computeIfAbsent(bucketIndex, k -> new ArrayList<>());

        // Prevent duplicate entries
        for (Triplet t : bucket) {
            if (t.getNODE_ID() == nodeInfo.getNODE_ID()) {
                return;
            }
        }

        if (bucket.size() < K_BUCKET_SIZE) {
            bucket.add(nodeInfo);
        } else {
            // Replacement strategy: Remove the oldest node (FIFO)
            bucket.remove(0);
            bucket.add(nodeInfo);
        }
    }

    public void removeFromRoutingTable(int NODE_ID)
    {
        int bucketIndex = getBucketIndex(NODE_ID);
        List<Triplet> bucket = routingTable.computeIfAbsent(bucketIndex, k -> new ArrayList<>());
        bucket.removeIf(node -> node.getNODE_ID() == NODE_ID);
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

    public Map<Integer, List<Triplet>> getRoutingTable()
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
        for(Map.Entry<Integer, List<Triplet>> entry : routingTable.entrySet())
        {
            Integer key = entry.getKey();
            List<Triplet> tripletList = entry.getValue();
            System.out.println(key+": ");
            for(Triplet nodeInfo : tripletList)
            {
                nodeInfo.display();
            }
        }
    }

    public Triplet findNearestNode(int value)
    {
        int min_xor = value ^ node_information.getNODE_ID();
        Triplet nearest_node_info = node_information;
        for(Map.Entry<Integer, List<Triplet>> entry : routingTable.entrySet())
        {
            for(Triplet nearby_nodeInfo : entry.getValue())
            {
                int curr_xor = value ^ nearby_nodeInfo.getNODE_ID();
                if(curr_xor <= min_xor)
                {
                    min_xor = curr_xor;
                    nearest_node_info = nearby_nodeInfo;
                }
            }
        }

        return nearest_node_info;
    }

    public List<Triplet> findKClosestNodesToNode(int k) {
        int targetID = node_information.getNODE_ID();
        Triplet targetNodeInfo = node_information;

        PriorityQueue<Triplet> minHeap = new PriorityQueue<>(Comparator.comparingInt(
                node -> targetID ^ node.getNODE_ID()
        ));

        // Add all nodes from the routing table to the priority queue
        for (List<Triplet> bucket : routingTable.values()) {
            for (Triplet nodeInfo : bucket) {
                if (nodeInfo.getNODE_ID() != targetNodeInfo.getNODE_ID()) { // Avoid adding itself
                    minHeap.offer(nodeInfo);
                }
            }
        }

        // Extract the k closest nodes
        List<Triplet> kClosestNodes = new ArrayList<>();
        while (!minHeap.isEmpty() && kClosestNodes.size() < k) {
            kClosestNodes.add(minHeap.poll());
        }

        return kClosestNodes;
    }

    public List<Triplet> findKClosestNodesToValue(int value, int k) {
        PriorityQueue<Triplet> minHeap = new PriorityQueue<>(Comparator.comparingInt(
                node -> value ^ node.getNODE_ID()
        ));

        // Add all nodes from routing table to the priority queue
        for (List<Triplet> bucket : routingTable.values()) {
            for (Triplet nodeInfo : bucket) {
                minHeap.offer(nodeInfo);
            }
        }

        // Extract the k closest nodes
        List<Triplet> kClosestNodes = new ArrayList<>();
        while (!minHeap.isEmpty() && kClosestNodes.size() < k) {
            kClosestNodes.add(minHeap.poll());
        }

        return kClosestNodes;
    }

    public void close()
    {
        if(socket != null && !socket.isClosed())
        {
            socket.close();
        }
    }
}
