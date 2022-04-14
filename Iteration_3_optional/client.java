/**
 * CPSC 559: Project Iteration 3 Optional Requirements solution
 * Client Class which connects with the Registry via TCP Protocol and interacts as described in the rubric
 * @author Shehzan Murad Ali and Humble Chaudhry
 **/ 


// imports
import java.io.*;
import java.net.*;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.time.Instant;




/**
 * Source Class
 * This class is an identifier for a source, which contains its own location, the time a peer was received and the peers it contains
 */
class source{
    
    // local variables storing location (ip:port), time stamp, array lst of peers received from this source 
    public String location;
    public String timeReceived;
    public ArrayList<Peer> source_peers;

    // Constructor to store all the values 
    public source(String location, String time, ArrayList<Peer> peers){
        this.location = location;
        this.timeReceived = time;
        this.source_peers = peers;
    }
}

/**
 * Peer class used to define a given peer. A peer is classified by its location and timestamp. 
 */
class Peer{
    //local variables storing location (ip:port) and time stamp for each respective peer
    public String location;
    public LocalDateTime timeStamp; 
    private ConcurrentHashMap<Tuple, String> snipTimeStampLocation = new ConcurrentHashMap<Tuple, String>();
    public String status;
    public Instant startTime;

    //Constructor storing all the values
    public Peer(String loc, LocalDateTime time){
        this.location = loc;
        this.timeStamp = time;
        this.startTime = Instant.now();
        this.status = "active";
    }

    public void set(String key, int timestamp, String value) {
        snipTimeStampLocation.put(new Tuple(key, timestamp), value);
    }

    public String get(String key, int timestamp) {
        
        for (int i = timestamp; i >= 1; i--) {
            String value = snipTimeStampLocation.getOrDefault(new Tuple(key, i), "");
            if (!value.isEmpty())
                return value;
        }
        return null;
    }
    public long checkDuration(Instant endTime){
        return Duration.between(startTime, endTime).getSeconds();
    }
    public void resetStart(Instant newTime){
        this.startTime = newTime;
    }
}

//UDP Peers Received class: Defines a peer which was received via UDP
class UDP_Peer_rcd{
    //local variables storing location (ip:port), the location of the source (ip:port) and the time which the peer was received at
    public String location;
    public String source_location;
    public String timeReceived;

    //Constructor storing all the values
    public UDP_Peer_rcd(String location, String source_location, String timeReceived){
        this.location = location;
        this.source_location = source_location;
        this.timeReceived = timeReceived;
    }

}
//UDP Peers Sent class: Defines a peer which was sent via UDP
class UDP_Peer_sent{
    //local variables storing location (ip:port), the location of the destination (ip:port) and the time which the peer was received at
    public String location;
    public String destination_location;
    public String timeReceived;
    //Constructor storing all the values
    public UDP_Peer_sent(String location, String destination_location, String timeReceived){
        this.location = location;
        this.destination_location = destination_location;
        this.timeReceived = timeReceived;
    }

}

/**
 * Snippet Class
 * This class defines a snippet, using the time it was received, its unique time stamp, the content which it contains
 * and the source which the snip was sent from
 */
class Snip{
   /*
   local variables storing the timestamp of the snippet, the content which the snippet contains
   the location of the source (ip:port) and the time which the peer was received at
   */ 
    public int timeStamp;
    public String content;
    public String timeReceived;
    public String source_location;

    //Constructor to store all values
    public Snip(int timeStamp, String content, String timeReceived, String source_location) {
        this.timeStamp = timeStamp;
        this.content = content;
        this.timeReceived = timeReceived;
        this.source_location = source_location;
    }

    // int getTimeStamp(String source_location){

    // }
}

/**
 * Volatile Time Stamp Class is used to store and change a dynamic volatile time stamp
 */

class VolatileTimeStamp{
    //local variable stored is a volatile timestamp that may change based on functions
    private volatile int timeStamp = 0;

    /**
    *Returns the current Time Stamp which is stored
    */
    public int getTimeStamp(){
        return timeStamp;
    }
    /**
    *Changes the current Time Stamp value to number given in the argument
    * @param num 
    */
    public void setTimeStamp(int num){
        timeStamp = num;
    }
    /**
     * Increments the timestamp by 1 when run
     */
    public int incrementTimeStamp(){
        timeStamp++;
        return timeStamp;
    }
}

/**
 * Sending a Snippet class sends snippets from the command line using the udp socket, a timestamp,
 * the peer list and the current location
 */
class SnipSend extends Thread{
    //local variables store the Datagram socket to send through, a volatile time stamp, array list of the current peers and the current location
    public DatagramSocket peerSock;
    public VolatileTimeStamp timeStamp;
    public ArrayList<Peer> peers;
    public String ourLocation;

    public  SnipSend(DatagramSocket sock, VolatileTimeStamp time, ArrayList<Peer> p, String loc){
        this.peerSock = sock;
        this.timeStamp = time;
        this.peers = p;
        this.ourLocation = loc;
    }

    /**
     * Run function to execute sending a Snippet. The function sends the current snippet using a datagram packet to all peers. 
     * The snip is taken via keyboard, gets a current timestamp attached to it and is converted to bytes.
     * Loop through each peer which is not the current peer and send the sdnip with the value of the location. 
     */
    public synchronized void run(){
        try{
            Scanner keyboard = new Scanner(System.in);
            while(!Thread.currentThread().isInterrupted()){
                
                String content = keyboard.nextLine();   
                int timeStampSend = timeStamp.incrementTimeStamp();
                
                byte[] toSend = ("snip"+Integer.toString(timeStampSend)+" "+content).getBytes();
                for(Peer p : peers){
                    if(!p.location.equals(ourLocation)){
                        outer: for (int counter = 0; counter < 3; counter++) {
                        LocalDateTime now = LocalDateTime.now();
                        if(Duration.between(p.timeStamp, now).getSeconds() < 10){
                            InetAddress host = InetAddress.getByName(p.location.split(":")[0]);
                            Integer port = Integer.valueOf(p.location.split(":")[1].trim());
                            DatagramPacket packet = new DatagramPacket(toSend, toSend.length, host, port);
                            peerSock.send(packet);
                            System.out.println("Snip sent to " + p.location);   

                            //from https://stackoverflow.com/questions/19727109/how-to-exit-a-while-loop-after-a-certain-time
                            long start_time = System.currentTimeMillis();
                            long wait_time = 10000;
                            long end_time = start_time + wait_time;

                            while (System.currentTimeMillis() < end_time) {
                                if (p.get(ourLocation, timeStampSend) != null){
                                    System.out.println("ack received!");
                                    break outer;
                                }
                            }

                            if(counter == 2){
                                p.status = "missing_ack";
                            }

                        }  
                    }   
                    }
                }


            }
            if(Thread.currentThread().isInterrupted()){
                keyboard.close();
                System.out.println("Keyboard is closed");
            }
        }
        catch(Exception err){

        }
    }
}


//Making Tuple Class: https://leetcode.com/problems/time-based-key-value-store/discuss/466306/Java-solution-using-HashMap-with-composite-key-Beats-89.21-submissions-wrt-time
 class Tuple {
    String key;
    int timestamp;

    public Tuple(String key, int timestamp) {
        this.key = key;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tuple))
            return false;
        Tuple tuple = (Tuple) o;
        return timestamp == tuple.timestamp && key.equals(tuple.key);
    }

    @Override
    public int hashCode() {

        return Objects.hash(key, timestamp);
    }
}



/**
 * Initiating the connection with the registry Class
 * The purpose of this class is to start the connection with the registry and make a thread for this initial connection. 
 */
class initiateRegistryContact extends Thread{

    /**
     * local variables are the host number to send to, port number to send to, the UDP port for the peers and snips,    
     * the list of current peers, the list of peers given from the registry, the list of sources where the peers came from, 
     * the list of snippets and two lists for the peers which were received and sent via UDP. 
    */
    public String host;
    public int port;
    public static int UDP_PORT;
    public static ArrayList<Peer> peers;
    public static ArrayList<Peer> peers_Reg;
    public static ArrayList<source> sources;
    public static ArrayList<Snip> snips;
    public static ArrayList<UDP_Peer_rcd> uPeer_rcds;
    public static ArrayList<UDP_Peer_sent> uPeer_sents;



    //Constructor for storing variables
    public initiateRegistryContact(String h, int p, int udp, ArrayList<Peer> peers, ArrayList<Peer> peers_Reg, ArrayList<source> sources, ArrayList<Snip> snips, 
                                    ArrayList<UDP_Peer_rcd> uPeer_rcds, ArrayList<UDP_Peer_sent> uPeer_sents){
        this.host = h;
        this.port = p;
        this.UDP_PORT = udp;
        this.peers = peers;
        this.peers_Reg = peers_Reg;
        this.sources = sources;
        this.snips = snips;
        this.uPeer_rcds = uPeer_rcds;
        this.uPeer_sents = uPeer_sents;
    }

    
    /**
     * Sends the type of Language followed by a newline and then whole code base (this file) via reading 
     * one line at a time with a newline and followed by <end_of_code> (...) followed by a new line. Sending
     * it through the Buffered Writter of the OutputStream in the socket connection 
     * @param writer
     */
    public synchronized static void sendCode(BufferedWriter writer){
        try{  
            // Buffered reader to read the document
            BufferedReader in = new BufferedReader(new FileReader("client.java"));
            String line = in.readLine();
            
            //Writes the language
            writer.write("Java\n");

            // While the document isnt complete
            while(line != null)
            {
                // writes a line followed by a newline and flushes
                writer.write(line+"\n");
                writer.flush();
                line = in.readLine();
            }
            // writes end of code and flushes
            writer.write("\n...\n");
            writer.flush();
            // closes the reader
            in.close();

        }
        catch(Exception err) {
            //Exception handling
            System.out.println("Error: " + err.getMessage());
        }
    }

    /**
     * Recieves the number of peers that will be send (on its own line) followed by a 
     * list of peers in the form of IP addresses and port numbers.  The IP address and
     * port number are separated by a colon.  Each peer is on their own line. STores all
     * the information received appropriately in their respective datasets
     * @param reader
     * @param sock
     */
    public synchronized static void receivePeers(BufferedReader reader, Socket sock){
        try{
            // keeps track of all the peers received from this source
            ArrayList<Peer> localPeers = new ArrayList<Peer>();
            // reads num of peers
            int numPeers = Integer.parseInt(reader.readLine());
            // Date stamp recorded
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();
            // create a new source with source ip:port, timestamp, peers received
            // for loop for each peer
            for (int i = 0; i < numPeers; i++) {
                //reads peer per each line
                String loc = reader.readLine().trim();
                // boolean to check if theh peer is duplicate
                boolean received = false;
                //checking if the peer is already in the list
                for (int j = 0; j < peers.size(); j++){
                    // if found change the bool to true
                    if (peers.get(j).location.equals(loc)){
                        received = true;
                    }
                }
                // if no duplicate, then add to the master list of peers
                if (!received){
                    Peer newPeer = new Peer(loc, now);
                    peers.add(newPeer);
                    peers_Reg.add(newPeer);
                }
                // add peerto the local list of peers
                for(Peer find:peers){
                    if(find.location.equals(loc) && find.timeStamp == now){
                        localPeers.add(find);
                        break;
                    }
                }
                
                
            }
            
            source newSource = new source(sock.getInetAddress().getLocalHost().getHostAddress()+":"+sock.getPort(), dtf.format(now), localPeers);  
            // add to the master list of sources
            sources.add(newSource);
        }
        catch(Exception err) {
            //Exception handling
            System.out.println("Error: " + err.getMessage());
        }
        
    }

    /**Used Merge sort from https://www.baeldung.com/java-merge-sort */

    public synchronized static void merge_snips(ArrayList<Snip> original, ArrayList<Snip> left_snips, ArrayList<Snip> right_snips, int left_size, int right_size){

        // pointers in each lists
        int left_pointer = 0;
        int right_pointer = 0;

        original.clear();
       

        // Look up each element and merge
        while(left_pointer < left_size && right_pointer < right_size){
            if(left_snips.get(left_pointer).timeStamp <= right_snips.get(right_pointer).timeStamp){
                original.add(left_snips.get(left_pointer));
                left_pointer++;
            }
            else{
                original.add(right_snips.get(right_pointer));
                right_pointer++;
            }
        }

        // add leftover points
        while(left_pointer<left_size){
            original.add(left_snips.get(left_pointer));
            left_pointer++;
        }
        while(right_pointer<right_size){
            original.add(right_snips.get(right_pointer));
            right_pointer++;
        }
    }

    public synchronized static void sort_snips(ArrayList<Snip> original, int size){
        if(size >= 2){
            int middle = size/2;

            // Creating two temp sub snips
            ArrayList<Snip> left_snips = new ArrayList<Snip>();
            ArrayList<Snip> right_snips = new ArrayList<Snip>();

            // filling temp sub snips with data from original
            for(int i = 0; i < middle; i++){
                left_snips.add(original.get(i));
            }
            for(int i = middle; i < size; ++i){
                right_snips.add(original.get(i));
            }

            // recursive calls for each respective halfs
            sort_snips(left_snips, middle);
            sort_snips(right_snips, size-middle);

            // merge these halfs
            merge_snips(original, left_snips, right_snips, middle, size-middle);
        }
    }

    /**
     * Sends current list of peers followed by a report that indicates all sources of this 
     * peer list
     * @param writer
     */
    public synchronized static void sendReport(BufferedWriter writer){
        try{
            // Writes the number of peers followed by a newline character
            writer.write(Integer.toString(peers_Reg.size())+"\n");
            for(Peer p : peers_Reg){
                // for each peer it reads, it send the peer followed by a new line character
                writer.write(p.location+"\n");
            }
            // Writes the number of sources followed by a new line character
            writer.write(Integer.toString(sources.size())+"\n");
            for(source s : sources){
                // for each source it writes its ip:port followed by a new line followed by time
                // it received followed by a new line followed by number of peers in each source
                // followed by a newline character
                writer.write(s.location + "\n" + s.timeReceived + "\n" + Integer.toString(s.source_peers.size()) + "\n");
                for(Peer p : s.source_peers) {
                    // for each peer it writes a peer (ip:port) followed by a newline character
                    writer.write(p.location+"\n");
                }
            }
            writer.write(Integer.toString(uPeer_rcds.size())+"\n");
            for(UDP_Peer_rcd uPeer : uPeer_rcds){
                writer.write(uPeer.source_location + " " + uPeer.location + " " + uPeer.timeReceived + "\n");
            }
            writer.write(Integer.toString(uPeer_sents.size())+"\n");
            for(UDP_Peer_sent uPeer : uPeer_sents){
                writer.write(uPeer.destination_location + " " + uPeer.location + " " + uPeer.timeReceived + "\n");
            }
            sort_snips(snips, snips.size());
            writer.write(Integer.toString(snips.size())+"\n");
            for(Snip s : snips){
                writer.write(Integer.toString(s.timeStamp) + " " + s.content.trim() + " " + s.source_location.trim() + "\n");
            }
            // flushes everything in the writer
            writer.flush();
        }
        catch(Exception err){
            // Exception handling
            System.out.println("Error: " + err.getMessage());
        } 
    }

    /**
     * Send the location address of UDP server connection
     * @param writer
     */
    public synchronized static void sendLocation(BufferedWriter writer){
        try{
            writer.write(InetAddress.getLocalHost().getHostAddress()+":"+Integer.toString(UDP_PORT)+"\n");
            writer.flush();
        }
        catch(Exception err){
            System.out.println("Error: " + err.getMessage());
        }
    }

    /**
     * Sends the team name through the BufferedWritter of the OutputStream in the socket connection.
     * @param writer
     */
    public synchronized static void sendTeamName(BufferedWriter writer){
        String teamName = "The Social Network";

        try{
            //writes then flushes
            writer.write(teamName+"\n");
            writer.flush();
        }
        catch(Exception err) {
            //exception handling
            System.out.println("Error: " + err.getMessage());
            }
    }

    public synchronized void run(){
        try (
                // Socket connection via host and port 
                Socket clientSocket = new Socket(host, port);
                // Buffered Reader and Buffered Writer for input and output streams on socket (to read and write to the socket)
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            )
        {
            System.out.println("Connecting to the Registry via TCP");
            // while socket is open
            Boolean open = true;
            while(open){
                // read response from the server
                String response;
                response = reader.readLine();
                // upon the request switch
                switch(response){
                    // send team name
                    case "get team name":
                        System.out.println("Requesting team name");
                        sendTeamName(writer);
                        System.out.println("Sent team name");
                        break;
                    // send code base
                    case "get code":
                        System.out.println("Requesting code base");
                        sendCode(writer);
                        System.out.println("Sent code base");
                        break;
                    // receive request 
                    case "receive peers":
                        System.out.println("Receiving peers");
                        receivePeers(reader, clientSocket);
                        System.out.println("Received");
                        break;
                    // send report
                    case "get report":
                        System.out.println("Requesting report");
                        sendReport(writer);
                        System.out.println("Sent report");
                        break;
                    // get location
                    case "get location":
                        System.out.println("Requesting location");
                        sendLocation(writer);
                        System.out.println("Sent Location");
                        break;
                    // close request
                    case "close":
                        System.out.println("Requesting to close the socket");
                        // change boolean value so connection gets closed
                        open = false;
                        break;
                }
            }
            // close the socket
            Thread.sleep(100);
            clientSocket.close();
            System.out.println("Disconnected from the Registry via TCP");
            
        }
        catch(Exception err){
            System.out.println("Error: "+err);
        }
    }
}

// Client class - main class
public class client {

    // master arraylist to store peers (no duplicates) and sources (class provided above)
    public static ArrayList<Peer> peers = new ArrayList<Peer>();
    public static ArrayList<Peer> peers_Reg = new ArrayList<Peer>();
    public static ArrayList<source> sources = new ArrayList<source>();
    public static ArrayList<Snip> snips = new ArrayList<Snip>();
    public static ArrayList<UDP_Peer_rcd> udpPeersReceived = new ArrayList<UDP_Peer_rcd>();
    public static ArrayList<UDP_Peer_sent> udpPeersSent = new ArrayList<UDP_Peer_sent>();
    // host address and port number of Registry
    public static String registryHost = "136.159.5.22"; // 136.159.5.22:55921change it to localhost if running on your pc
    // TCP PORT
    public static int registryPort = 55921;
    // stop UDP
    public static volatile boolean recieveStop = false;
    public static volatile boolean recieveStop2 = false;
    public static int counter = 0; 
    //Timestamp to organize the snippets and peers
    public static VolatileTimeStamp timeStamp = new VolatileTimeStamp();
    //the current location 
    public static String ourLocation;
    
    /**
     * The shut down procedure is a function which closes the datagram socket to send the peers. 
     * @param peerSock
     */
    public static void shutDownProcedure(DatagramSocket peerSock, InetAddress udpHost,int source_port){

        String teamName = "The Social Network";

        byte[] toSend = ("ack" + teamName).getBytes();
        DatagramPacket packet = new DatagramPacket(toSend, toSend.length, udpHost, source_port);

        try{
            peerSock.send(packet);
        }

        catch(Exception err){

        }
        
        try{
            peerSock.close();
            recieveStop = true;
        }
        catch(Exception err){
            System.out.println("Error: " + err);
        }
    }

    /**
     * Snip Received function changes the global variable received to the new string and location that were passed
     * in the arguments. 
     * It then takes the current snippet and adds it to the list of snippets. 
     * @param received
     * @param source_location
     */
    public static void snipReceived(String received, String source_location,DatagramSocket peerSock){
        received = received.substring(4, received.length()).trim();
        int timeStampReceived = Integer.parseInt(received.split(" ", 2)[0]);
        String content = received.split(" ", 2)[1].trim();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();
        String timeReceived = dtf.format(now);
        timeStamp.setTimeStamp(Integer.max(timeStamp.getTimeStamp(), timeStampReceived)+1);
        Snip snip = new Snip(timeStampReceived, content, timeReceived, source_location);
        snips.add(snip);


        try{
            byte[] toSend = ("ack " + timeStampReceived).getBytes();
            InetAddress udpHost = InetAddress.getByName(source_location.split(":")[0]);
            int source_port = Integer.valueOf(source_location.split(":")[1].trim());
            DatagramPacket packet = new DatagramPacket(toSend, toSend.length, udpHost, source_port);
            peerSock.send(packet);
        }
        catch(Exception err) {
            //Exception handling
            System.out.println("Error: " + err.getMessage());
        }





        System.out.println(Integer.toString(timeStampReceived) + " " + content + " " + timeReceived + " " + source_location);
    }

    /**
     * Peers Received function overwrites the received global variable to the string that was passed down in the argument
     * Looping through every peer, we check whether or no th the peer is part of the same source and if it is, we 
     * set the timestamp to now and the boolean sourceAvail to true
     * The Source Avail boolean is used to check if a peer location matches the source location
     * If it does not, then we add the source as a peer
     * Peer avail checks if the peer is already in the list, if not then we add the peer to the list of peers
     * We also add the peer to the list of udp peers which were received. 
     * @param received
     * @param source_location
     */
    public static void peerReceived(String received, String source_location){
        received = received.substring(4, received.length()).trim();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();
        Boolean sourceAvail = false;
        for(Peer p : peers){
            if(p.location.equals(source_location)){
                p.timeStamp = now;
                sourceAvail = true;
            }
        }
        if(!sourceAvail){
            Peer source = new Peer(source_location, now);
            peers.add(source);
        }
        Boolean peerAvail = false;
        for(Peer p : peers){
            if(p.location.equals(received)){
                peerAvail = true;
            }
        }
        if(!peerAvail){
            Peer peerAdd = new Peer(received, now);
            peers.add(peerAdd);
        }
        UDP_Peer_rcd udp_peer = new UDP_Peer_rcd(received, source_location, dtf.format(now));
        udpPeersReceived.add(udp_peer);
    }

    private static void receiveAcks(String source_location, DatagramSocket peerSock) {
		boolean socketOpen = true;
		while (socketOpen) {
			byte[] message = new byte[1024];
			DatagramPacket packet = new DatagramPacket(message,1024);
			try {
				peerSock.receive(packet);
				String ackMessage = new String(message);
				if (ackMessage.substring(0,3).equalsIgnoreCase("ack")) {
					int timeStamp = Integer.valueOf(ackMessage.substring(3).trim());
                    
                    for(Peer peer: peers){
                        if(source_location == peer.location){
                            peer.set(ourLocation, timeStamp, "ack");
                        }

                    }
				}
			} catch (IOException e) {
				// do nothing.  When socket closes we can end this method.
				socketOpen = false;
			}
		}
	}

    /**
     * The UDP receive thread makes a new thread to check the packet which was received via UDP. 
     * in this packet we receive the port, source and received string. 
     * The first 4 characters will determine what the received packet was a stop case, snippet or peer. 
     * @param peerSock
     */
    public static void createUDPReceiveThread(DatagramSocket peerSock){
        Thread t = new Thread() {
            public synchronized void run(){

                while(!recieveStop){
                    for(Peer peer : peers){
                        if (peer.checkDuration(Instant.now()) == 181 ){
                            peer.status = "silent";
                        }
                    }
                try{
                    byte[] buf = new byte[256];
                    if(counter>0){
                        
                        peerSock.setSoTimeout(11000);
                    }
 
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    try{
                        peerSock.receive(pack);
                        int source_port = pack.getPort();
                        String source_location = ((InetSocketAddress) pack.getSocketAddress()).getHostString() + ":" + Integer.toString(source_port);
                        String received = new String(buf);
                        String first4char = null;
                        
                        InetAddress udpHost = InetAddress.getByName(source_location.split(":")[0]);

                        for(Peer peer : peers){
                            if(peer.location == source_location){
                                peer.resetStart(Instant.now());
                            }
                        }

                        if(received.length() > 4){
                            first4char = received.substring(0, 4);
                        }
                        switch(first4char){
                            case "stop":
                                System.out.println("Received 'stop' from the registry");
                                counter = counter + 1;
                                shutDownProcedure(peerSock, udpHost, source_port); 
                                recieveStop2 = true;
                                initiateRegistryContact initContact2 = new initiateRegistryContact(registryHost, registryPort, peerSock.getLocalPort(), peers, peers_Reg, sources, snips, udpPeersReceived, udpPeersSent);
                                initContact2.start();
                                break;
                            case "snip":
                                snipReceived(received, source_location, peerSock);
                                break;
                            case "peer":
                                peerReceived(received, source_location);
                                break;
                            case "ack":
                                receiveAcks(source_location,peerSock);
                        }
                    }
                    catch (SocketTimeoutException e) {
                        recieveStop = true;
                        peerSock.close();
                        System.out.println("timeout and socket closed");
                    }
                }   catch(Exception err){
                        System.out.println("Error: "+ err);
                    }
                
                    
                }
            }
        };
        t.start();
    }



    /**
     * Sending the peer packets function creates a new thread, which checks for all peers that is not a current peer, gives it a timestamp, 
     * and sends the peer through the udp socket. It also adds the peer to the list of udp peers with the date. 
     * @param peerSock
     */
    public static void sendPeerPackets(DatagramSocket peerSock){
        Thread t = new Thread(){
            public synchronized void run(){
                while(!recieveStop){
                    try{
                        for(Peer p : peers){
                            if(!p.location.equals(ourLocation)){
                                LocalDateTime now = LocalDateTime.now();
                                if(Duration.between(p.timeStamp, now).getSeconds() < 10){
                                    InetAddress host = InetAddress.getByName(p.location.split(":")[0]);
                                    Integer port = Integer.valueOf(p.location.split(":")[1].trim());
                                    Boolean sent = false;
                                    while(!sent){
                                        Random rand = new Random();
                                        Peer peer = peers.get(rand.nextInt(peers.size()));
                                        if(Duration.between(peer.timeStamp, now).getSeconds() < 10 || peer.location.equals(ourLocation)){
                                            byte[] toSend = ("peer"+peer.location).getBytes();
                                            DatagramPacket packet = new DatagramPacket(toSend, toSend.length, host, port);
                                            peerSock.send(packet);
                                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
                                            UDP_Peer_sent udp_peer = new UDP_Peer_sent(peer.location, p.location, dtf.format(now));
                                            udpPeersSent.add(udp_peer);
                                            sent = true;
                                            System.out.println("Sent peer " + peer.location + " to " + p.location);
                                        }
                                    }  
                                } 
                            }
                        }
                        Thread.sleep(6000);

                    }
                    catch (Exception err){
                        System.out.println("Error: "+ err);
                    }
                }
            }
        };
        t.start();
    }


    
    
    /**
     * Main method to drive the whole client program, connects to the Registry
     * via ip address and port number thru a TCP connection. Reads the requests 
     * and handles each of them respectively. After a close request is read, it 
     * closes the connection and ends the program.
     * @param args
     */
    public static void main(String[] args)
	{              
		try{
            
            // Starting a datagram socket
            DatagramSocket peerSock = new DatagramSocket();
            int UDP_PORT = peerSock.getLocalPort();
            ourLocation = InetAddress.getLocalHost().getHostAddress()+":"+UDP_PORT;
            initiateRegistryContact initContact = new initiateRegistryContact(registryHost, registryPort, UDP_PORT, peers, peers_Reg, sources, snips, udpPeersReceived, udpPeersSent);
            initContact.start();

            createUDPReceiveThread(peerSock);
            sendPeerPackets(peerSock);
            SnipSend snipSend = new SnipSend(peerSock, timeStamp, peers, ourLocation);
            snipSend.start();
            while(!recieveStop2){
                
            }
            snipSend.interrupt();
            // initiateRegistryContact initContact2 = new initiateRegistryContact(registryHost, registryPort, UDP_PORT, peers, peers_Reg, sources, snips, udpPeersReceived, udpPeersSent);
            // initContact2.start();
            
		}
		catch(Exception err) {
            // Exception handling
			System.out.println("Error: " + err.getMessage());
		}
	}
}
