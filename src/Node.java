import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Node {
    private final Map<String, Integer> hashTable;
    private final Map<Integer, List<Triplet>> routingTable;
    public Triplet node_information;
    private DatagramSocket socket;

    private boolean isLocked = false;
    private static final int K_BUCKET_SIZE = 3;
    private static final int BUCKET_COUNT = 32;
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_TABLE_SIZE = 50;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Node(String IP_ADDR, int UDP_PORT, int NODE_ID) {
        node_information = new Triplet(IP_ADDR, UDP_PORT, NODE_ID);
        hashTable = new ConcurrentHashMap<>();
        routingTable = new ConcurrentHashMap<>();
        startUDPServer();
        startPingScheduler();
    }

    private void startUDPServer() {
        new Thread(() -> {
            try {
                socket = new DatagramSocket(node_information.getUDP_PORT());
                byte[] buffer = new byte[BUFFER_SIZE];
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        handleIncomingPacket(packet);
                    } catch (SocketException e) {
                        // Socket closed, break the loop
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void startPingScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendPingKClosest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS); // Runs every 10 seconds
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
        }
        // Add this to your handleIncomingPacket method
        else if (message.startsWith("FIND_NODE")) {
            String[] parts = message.split(" ");
            int targetNodeId = Integer.parseInt(parts[1]);
            String callerIP = parts[2];
            int callerPort = Integer.parseInt(parts[3]);
            int callerNodeID = Integer.parseInt(parts[4]);

            // Add the sender to our routing table
            Triplet callerNodeInfo = new Triplet(callerIP, callerPort, callerNodeID);
            addToRoutingTable(callerNodeInfo);

            // Find k closest nodes to the target node ID
            List<Triplet> closestNodes = findKClosestNodesForKeyFromSelf(targetNodeId, K_BUCKET_SIZE);

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
        else if (message.startsWith("FIND_KEY")) {
            String[] parts = message.split(" ");
            int searchKey = Integer.parseInt(parts[1]);
            String callerIP = parts[2];
            int callerPort = Integer.parseInt(parts[3]);
            int callerNodeID = Integer.parseInt(parts[4]);

            Triplet callerNodeInfo = new Triplet(callerIP, callerPort, callerNodeID);
            addToRoutingTable(callerNodeInfo);

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
                List<Triplet> closestNodes = findKClosestNodesToSelf(K_BUCKET_SIZE);

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
        else if (message.startsWith("STORE_KEY")) {
            String[] parts = message.split(" ");
            int key = Integer.parseInt(parts[1]);
            String callerIP = parts[2];
            int callerPort = Integer.parseInt(parts[3]);
            int callerNodeID = Integer.parseInt(parts[4]);

            Triplet callerNodeInfo = new Triplet(callerIP, callerPort, callerNodeID);
            addToRoutingTable(callerNodeInfo);

            boolean stored = storeKey(key);

            if(stored) {
                String response = "STORED_KEY " + key + " NODE_ID " + node_information.getNODE_ID();
                DatagramPacket responsePacket = new DatagramPacket(
                        response.getBytes(), response.length(),
                        senderAddress, senderPort
                );
                socket.send(responsePacket);
            }
            else {
                // If we can't store the key, send our k-closest nodes
                List<Triplet> closestNodes = findKClosestNodesToSelf(K_BUCKET_SIZE);

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
        else if (message.startsWith("GOODBYE")) {
            String[] parts = message.split(" ");
            String callerIP = parts[1];
            int callerPort = Integer.parseInt(parts[2]);
            int callerNodeID = Integer.parseInt(parts[3]);

            // Add the sender to our routing table
            removeFromRoutingTable(callerNodeID);
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

            pingSocket.setSoTimeout(2000); // 2-second timeout
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

            pingSocket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();

            // Check if response is a valid PONG
            if (response.equals("PONG " + targetNode.getNodeInformation().getIP_ADDR() + " " + targetNode.getNodeInformation().getUDP_PORT() + " " + targetNode.getNodeInformation().getNODE_ID())) {
                addToRoutingTable(targetNode.getNodeInformation());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPingKClosest() {
        try (DatagramSocket pingSocket = new DatagramSocket()) {
            List<Triplet> KClosestNodes = findKClosestNodesToSelf(3);
            Set<Integer> unresponsiveNodes = new HashSet<>();
            int maxRetries = 1;

            for (Triplet targetNodeInfo : KClosestNodes) {
                boolean isResponsive = false;

                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        // Send PING message
                        String message = "PING " + node_information.getIP_ADDR() + " " + node_information.getUDP_PORT() + " " + node_information.getNODE_ID();
                        byte[] buffer = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                InetAddress.getByName(targetNodeInfo.getIP_ADDR()), targetNodeInfo.getUDP_PORT());

                        pingSocket.send(packet);
                        //System.out.println("Attempt " + attempt + ": Sent PING to Node ID " + targetNodeInfo.getNODE_ID());

                        // Wait for response
                        pingSocket.setSoTimeout(2000); // 2-second timeout
                        byte[] responseBuffer = new byte[1024];
                        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

                        pingSocket.receive(responsePacket);
                        String response = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();

                        // Check if response is a valid PONG
                        if (response.equals("PONG " + targetNodeInfo.getNODE_ID())) {
                            isResponsive = true;
                            //System.out.println("Node ID " + targetNodeInfo.getNODE_ID() + " responded with PONG.");
                            break;
                        } else {
                            //System.out.println("Invalid response from Node ID: " + targetNodeInfo.getNODE_ID() + " - Retrying...");
                        }

                    } catch (SocketTimeoutException e) {
                        //System.out.println("Attempt " + attempt + ": No response from Node ID " + targetNodeInfo.getNODE_ID());
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                // If the node never responded, mark it as unresponsive
                if (!isResponsive) {
                    //System.out.println("Node ID " + targetNodeInfo.getNODE_ID() + " is unresponsive after " + maxRetries + " attempts. Removing from routing table.");
                    unresponsiveNodes.add(targetNodeInfo.getNODE_ID());
                }
            }

            // Remove unresponsive nodes from the routing table
            for (Integer nodeId : unresponsiveNodes) {
                removeFromRoutingTable(nodeId);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Triplet> sendFindNodeRequest(Triplet targetNode, int targetNodeId) {
        List<Triplet> returnedNodes = new ArrayList<>();
        DatagramSocket findNodeSocket = null;

        try {
            findNodeSocket = new DatagramSocket();
            findNodeSocket.setSoTimeout(5000); // Set 5-second timeout

            String message = "FIND_NODE " + targetNodeId + " " +
                    node_information.getIP_ADDR() + " " +
                    node_information.getUDP_PORT() + " " +
                    node_information.getNODE_ID();

            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(targetNode.getIP_ADDR()),
                    targetNode.getUDP_PORT());

            try {
                findNodeSocket.send(packet);
                //System.out.println("Sent FIND_NODE request to " + targetNode.getIP_ADDR() + " " + targetNodeId);

                // Prepare to receive response
                byte[] responseBuffer = new byte[BUFFER_SIZE];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                findNodeSocket.receive(responsePacket); // Blocking call

                // If response is received, parse and return nodes
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                if (response.startsWith("CLOSEST_NODES")) {
                    String[] parts = response.split(" ");
                    for (int i = 1; i < parts.length; i += 3) {
                        if (i + 2 < parts.length) {
                            String ip = parts[i];
                            int port = Integer.parseInt(parts[i + 1]);
                            int nodeId = Integer.parseInt(parts[i + 2]);
                            Triplet nodeInfo = new Triplet(ip, port, nodeId);
                            returnedNodes.add(nodeInfo);
                            addToRoutingTable(nodeInfo);
                        }
                    }
                    return returnedNodes; // Successful response, return nodes
                }
            }
            catch (SocketTimeoutException e) {
                //System.err.println("Timeout: No response received from " + targetNode.getIP_ADDR());
            }

            // If all retries fail, fallback mechanism
            //System.err.println("All attempts failed. Node " + targetNode.getIP_ADDR() + " is unresponsive.");
            removeFromRoutingTable(targetNodeId); // Fallback method (implement as needed)

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (findNodeSocket != null && !findNodeSocket.isClosed()) {
                findNodeSocket.close();
            }
        }

        return returnedNodes;
    }

    private int sendFindKey(Triplet targetNode, int searchKey) {
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

    private boolean sendSplitHashTableRequest() {
        try {
            Set<Integer> visitedNodes = new HashSet<>();
            visitedNodes.add(node_information.getNODE_ID());

            List<Triplet> closestNodes = findKClosestNodesToSelf(K_BUCKET_SIZE);

            // Keep track of available nodes for round-robin distribution
            List<Triplet> availableNodes = new ArrayList<>(closestNodes);
            if (availableNodes.isEmpty()) {
                System.err.println("No nodes available for distribution");
                return false;
            }

            // Create a list of keys to distribute
            List<String> keysToDistribute = new ArrayList<>();
            int keysToSplit = hashTable.size() / 2;
            List<String> allKeys = new ArrayList<>(hashTable.keySet());
            Collections.shuffle(allKeys);
            for (int i = 0; i < keysToSplit && i < allKeys.size(); i++) {
                keysToDistribute.add(allKeys.get(i));
            }

            System.out.println("Keys to Distribute: " + keysToDistribute.size() + " keys");

            DatagramSocket splitHashTableSocket = new DatagramSocket();
            splitHashTableSocket.setSoTimeout(1000);

            isLocked = true;

            // Track successfully stored keys
            List<String> successfullyStoredKeys = new ArrayList<>();

            // Track nodes that have been found to be unreachable
            Set<Integer> unreachableNodes = new HashSet<>();

            // Current node index for round-robin
            int currentNodeIndex = 0;

            // Continue until we've distributed all keys or exhausted all nodes
            while (!keysToDistribute.isEmpty() && !availableNodes.isEmpty()) {
                // Remove unreachable nodes from available nodes
                availableNodes.removeIf(node -> unreachableNodes.contains(node.getNODE_ID()));

                if (availableNodes.isEmpty()) {
                    System.err.println("Node: " + node_information.getNODE_ID() + " has run out of available nodes to distribute keys");
                    break;
                }

                // Get the next available node in round-robin fashion
                currentNodeIndex = currentNodeIndex % availableNodes.size();
                Triplet targetNodeInfo = availableNodes.get(currentNodeIndex);

                System.out.println("Node: "+ node_information.getNODE_ID() + " Send split to node (ID: " +
                        targetNodeInfo.getNODE_ID() + ")");

                // Get the next key to distribute
                String hash = keysToDistribute.get(0);
                Integer key = hashTable.get(hash);

                if (key == null) {
                    // Key no longer exists in hash table, remove from distribution list
                    keysToDistribute.remove(0);
                    continue;
                }

                // Send STORE_KEY request
                String message = "STORE_KEY " + key + " " + node_information.getIP_ADDR() + " " +
                        node_information.getUDP_PORT() + " " + node_information.getNODE_ID();
                byte[] buffer = message.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName(targetNodeInfo.getIP_ADDR()),
                        targetNodeInfo.getUDP_PORT());

                try {
                    splitHashTableSocket.send(packet);

                    byte[] responseBuffer = new byte[BUFFER_SIZE];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    splitHashTableSocket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());

                    if (response.startsWith("STORED_KEY")) {
                        // Key stored successfully
                        successfullyStoredKeys.add(hash);
                        keysToDistribute.remove(0);
                        // Move to next node for next key
                        currentNodeIndex++;
                    }
                    else if (response.startsWith("CLOSEST_NODES")) {
                        // Process closest nodes response to find more potential nodes
                        String[] parts = response.split(" ");
                        System.out.print("CLOSEST NODES SPLIT: ");

                        for (int i = 1; i < parts.length; i += 3) {
                            if (i + 2 < parts.length) {
                                String ip = parts[i];
                                int port = Integer.parseInt(parts[i + 1]);
                                int nodeId = Integer.parseInt(parts[i + 2]);
                                System.out.println(port + " " + nodeId);

                                if (!visitedNodes.contains(nodeId) && !unreachableNodes.contains(nodeId)) {
                                    Triplet nodeInfo = new Triplet(ip, port, nodeId);
                                    availableNodes.add(nodeInfo);
                                    visitedNodes.add(nodeId);
                                    addToRoutingTable(nodeInfo);
                                }
                            }
                        }
                        // Try the next node with the same key
                        currentNodeIndex++;
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout: No response from " + targetNodeInfo.getIP_ADDR() + " " + targetNodeInfo.getNODE_ID());
                    // Mark this node as unreachable
                    unreachableNodes.add(targetNodeInfo.getNODE_ID());
                    // Try the next node with the same key
                    currentNodeIndex++;
                }
            }

            // Remove successfully stored keys from our hash table
            for (String hash : successfullyStoredKeys) {
                hashTable.remove(hash);
            }

            splitHashTableSocket.close();
            isLocked = false;

            if(keysToDistribute.size() == keysToSplit) {
                System.err.println("Cannot split keys from node: "+node_information.getNODE_ID());
            }
            // Atleast a few keys were distributed
            else if (!keysToDistribute.isEmpty()) {
                return true;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            isLocked = false;
            return false;
        }
    }

    private void sendGoodbyeMessage(Triplet targetNode) {
        try {
            DatagramSocket goodbyeSocket = new DatagramSocket();
            String message = "GOODBYE " + node_information.getIP_ADDR() + " " +
                    node_information.getUDP_PORT() + " " + node_information.getNODE_ID();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName(targetNode.getIP_ADDR()),
                    targetNode.getUDP_PORT());
            goodbyeSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
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

    public List<Triplet> findNode(int targetNodeId) {
        // Initialize seen nodes and closest nodes
        Set<Integer> queriedNodes = new HashSet<>();
        Set<Integer> seenNodes = new HashSet<>();

        // Add self to queried set
        queriedNodes.add(node_information.getNODE_ID());

        // Start with k closest nodes from our own routing table
        List<Triplet> closestNodes = findKClosestNodesForKeyFromSelf(targetNodeId, K_BUCKET_SIZE);
        for (Triplet node : closestNodes) {
            seenNodes.add(node.getNODE_ID());
        }

        // Continue until we've queried all nodes or found the target
        boolean improved = true;
        while (improved) {
            improved = false;

            // Sort nodes by XOR distance to targetNodeId
            closestNodes.sort(Comparator.comparingInt(node -> targetNodeId ^ node.getNODE_ID()));

            // Create a list of nodes to query in this round
            List<Triplet> nodesToQuery = new ArrayList<>();
            for (Triplet node : closestNodes) {
                if (!queriedNodes.contains(node.getNODE_ID())) {
                    nodesToQuery.add(node);
                    if (nodesToQuery.size() >= K_BUCKET_SIZE) break;
                }
            }

            if (nodesToQuery.isEmpty()) {
                break; // No more nodes to query
            }

            // Query each node
            for (Triplet nodeToQuery : nodesToQuery) {
                queriedNodes.add(nodeToQuery.getNODE_ID());

                // Send FIND_NODE request
                List<Triplet> returnedNodes = sendFindNodeRequest(nodeToQuery, targetNodeId);

                // Process returned nodes
                for (Triplet returnedNode : returnedNodes) {
                    if (!seenNodes.contains(returnedNode.getNODE_ID())) {
                        seenNodes.add(returnedNode.getNODE_ID());
                        closestNodes.add(returnedNode);
                        improved = true;
                    }
                }
            }

            // Keep only the k closest nodes
            if (closestNodes.size() > K_BUCKET_SIZE) {
                closestNodes.sort(Comparator.comparingInt(node -> targetNodeId ^ node.getNODE_ID()));
                //closestNodes = closestNodes.subList(0, K_BUCKET_SIZE);
            }
        }

        return closestNodes;
    }

    public int findKey(int searchKey) {
        // Generate SHA-1 hash of the key
        String hash = generateSHA1(String.valueOf(searchKey));

        // Check if value exists locally
        if (hashTable.containsKey(hash)) {
            return node_information.getNODE_ID();
        }

        // If not found locally, find the closest nodes to the key
        List<Triplet> closestNodes = findNode(searchKey);

        // Query each of the closest nodes directly
        for (Triplet node : closestNodes) {
            int result = sendFindKey(node, searchKey);
            if (result >= 0) {
                return result; // Key found at node with ID = result
            }
        }

        // If we've queried all nodes and didn't find the key
        return -1;
    }
    public boolean storeKey(int key)
    {
        if(hashTable.containsKey(key)) {
            System.out.println("Duplicate key detected, key already stored in table");
            return false;
        }

        if(isFull())
        {
            System.out.println("Node ID " + node_information.getNODE_ID() + " is full");
            boolean possible = sendSplitHashTableRequest();
            if(!possible) {
                return false;
            }
        }

        if(isLocked) {
            return storeKeyInClosestNode(key);
        }

        String hash = generateSHA1(String.valueOf(key));
        hashTable.put(hash, key);

        return true;
    }

    private boolean storeKeyInClosestNode(int key) {
        // Find the closest nodes to the key
        List<Triplet> closestNodes = findKClosestNodesForKeyFromSelf(key, K_BUCKET_SIZE);

        if (closestNodes.isEmpty()) {
            System.out.println("No available nodes to store key " + key);
            return false;
        }

        // Set to track nodes we've already tried
        Set<Integer> triedNodes = new HashSet<>();
        triedNodes.add(node_information.getNODE_ID()); // Add self to tried nodes

        // Queue of nodes to try
        Queue<Triplet> nodesToTry = new LinkedList<>(closestNodes);

        while (!nodesToTry.isEmpty()) {
            Triplet targetNode = nodesToTry.poll();

            // Skip if we've already tried this node
            if (triedNodes.contains(targetNode.getNODE_ID())) {
                continue;
            }

            triedNodes.add(targetNode.getNODE_ID());

            try {
                DatagramSocket storeSocket = new DatagramSocket();
                storeSocket.setSoTimeout(1000); // 5 second timeout

                // Prepare STORE_KEY message
                String message = "STORE_KEY " + key + " " +
                        node_information.getIP_ADDR() + " " +
                        node_information.getUDP_PORT() + " " +
                        node_information.getNODE_ID();

                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length,
                        InetAddress.getByName(targetNode.getIP_ADDR()),
                        targetNode.getUDP_PORT()
                );

                // Send the request
                storeSocket.send(packet);

                // Wait for response
                byte[] responseBuffer = new byte[BUFFER_SIZE];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

                try {
                    storeSocket.receive(responsePacket);
                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());

                    // Process the response
                    if (response.startsWith("STORED_KEY")) {
                        storeSocket.close();
                        return true; // Key stored successfully
                    }
                    else if (response.startsWith("CLOSEST_NODES")) {
                        // Add returned nodes to our queue
                        String[] parts = response.split(" ");
                        for (int i = 1; i < parts.length; i += 3) {
                            if (i + 2 < parts.length) {
                                String ip = parts[i];
                                int port = Integer.parseInt(parts[i + 1]);
                                int nodeId = Integer.parseInt(parts[i + 2]);

                                if (!triedNodes.contains(nodeId)) {
                                    Triplet nodeInfo = new Triplet(ip, port, nodeId);
                                    nodesToTry.add(nodeInfo);
                                    addToRoutingTable(nodeInfo);
                                }
                            }
                        }
                    }
                }
                catch (SocketTimeoutException e) {
                    System.out.println("No response from node ID " + targetNode.getNODE_ID());
                    removeFromRoutingTable(targetNode.getNODE_ID()); // Remove unresponsive node
                }

                storeSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // If we've tried all nodes and couldn't store the key
        System.out.println("Cannot store key " + key + " anywhere in the network");
        return false;
    }

    public boolean isFull()
    {
        return hashTable.size() >= MAX_TABLE_SIZE;
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

    public List<Triplet> findKClosestNodesToSelf(int k) {
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

    public List<Triplet> findKClosestNodesForKeyFromSelf(int key, int k) {
        PriorityQueue<Triplet> minHeap = new PriorityQueue<>(Comparator.comparingInt(
                node -> key ^ node.getNODE_ID()
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

    public void close() {

        List<Triplet> closestNodes = findKClosestNodesToSelf(K_BUCKET_SIZE);
        for (Triplet node : closestNodes) {
            sendGoodbyeMessage(node);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        scheduler.shutdown();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
