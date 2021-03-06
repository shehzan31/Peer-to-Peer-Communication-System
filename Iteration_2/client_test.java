/**
 * CPSC 559: Project Iteration 1 solution
 * Client Class which connects with the Registry via TCP Protocol and interacts as described in the rubric
 * @author Shehzan Murad Ali and Humble Chaudhry
 *  */ 


// imports
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.LocalDateTime;




// source class
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

class Peer{
    public String location;
    public LocalDateTime timeStamp; 

    public Peer(String loc, LocalDateTime time){
        this.location = loc;
        this.timeStamp = time;
    }
}

class UDP_Peer_rcd{
    public String location;
    public String source_location;
    public String timeReceived;

    public UDP_Peer_rcd(String location, String source_location, String timeReceived){
        this.location = location;
        this.source_location = source_location;
        this.timeReceived = timeReceived;
    }

}

class UDP_Peer_sent{
    public String location;
    public String destination_location;
    public String timeReceived;

    public UDP_Peer_sent(String location, String destination_location, String timeReceived){
        this.location = location;
        this.destination_location = destination_location;
        this.timeReceived = timeReceived;
    }

}

class Snip{
    
    public int timeStamp;
    public String content;
    public String timeReceived;
    public String source_location;

    public Snip(int timeStamp, String content, String timeReceived, String source_location) {
        this.timeStamp = timeStamp;
        this.content = content;
        this.timeReceived = timeReceived;
        this.source_location = source_location;
    }
}

class VolatileTimeStamp{
    private volatile int timeStamp = 0;
    public int getTimeStamp(){
        return timeStamp;
    }
    public void setTimeStamp(int num){
        timeStamp = num;
    }
    public int incrementTimeStamp(){
        timeStamp++;
        return timeStamp;
    }
}

class SnipSend extends Thread{
    public DatagramSocket peerSock;
    public VolatileTimeStamp timeStamp;
    public ArrayList<Peer> peers;
    public String ourLocation;

    public SnipSend(DatagramSocket sock, VolatileTimeStamp time, ArrayList<Peer> p, String loc){
        this.peerSock = sock;
        this.timeStamp = time;
        this.peers = p;
        this.ourLocation = loc;
    }

    public void run(){
        try{
            Scanner keyboard = new Scanner(System.in);
            while(!Thread.currentThread().isInterrupted()){
                
                String content = keyboard.nextLine();   
                int timeStampSend = timeStamp.incrementTimeStamp();
                
                byte[] toSend = ("snip"+Integer.toString(timeStampSend)+" "+content).getBytes();
                for(Peer p : peers){
                    if(!p.location.equals(ourLocation)){
                        LocalDateTime now = LocalDateTime.now();
                        if(Duration.between(p.timeStamp, now).getSeconds() < 10){
                            InetAddress host = InetAddress.getByName(p.location.split(":")[0]);
                            Integer port = Integer.valueOf(p.location.split(":")[1].trim());
                            DatagramPacket packet = new DatagramPacket(toSend, toSend.length, host, port);
                            peerSock.send(packet);
                            System.out.println("Snip sent to " + p.location);   
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

class initiateRegistryContact extends Thread{

    public String host;
    public int port;
    public static int UDP_PORT;
    public static ArrayList<Peer> peers;
    public static ArrayList<Peer> peers_Reg;
    public static ArrayList<source> sources;
    public static ArrayList<Snip> snips;
    public static ArrayList<UDP_Peer_rcd> uPeer_rcds;
    public static ArrayList<UDP_Peer_sent> uPeer_sents;

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
    public static void sendCode(BufferedWriter writer){
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
    public static void receivePeers(BufferedReader reader, Socket sock){
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

    /**Used Merge sort from https://www.geeksforgeeks.org/merge-sort/ */

    public static void merge_snips(ArrayList<Snip> original, ArrayList<Snip> left_snips, ArrayList<Snip> right_snips, int left_size, int right_size){

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

    public static void sort_snips(ArrayList<Snip> original, int size){
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
    public static void sendReport(BufferedWriter writer){
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
    public static void sendLocation(BufferedWriter writer){
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
    public static void sendTeamName(BufferedWriter writer){
        String teamName = "test";

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

    public void run(){
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
            clientSocket.close();
            System.out.println("Disconnected from the Registry via TCP");
            
        }
        catch(Exception err){
            System.out.println("Error: "+err);
        }
    }
}

// Client class - main class
public class client_test {

    // master arraylist to store peers (no duplicates) and sources (class provided above)
    public static ArrayList<Peer> peers = new ArrayList<Peer>();
    public static ArrayList<Peer> peers_Reg = new ArrayList<Peer>();
    public static ArrayList<source> sources = new ArrayList<source>();
    public static ArrayList<Snip> snips = new ArrayList<Snip>();
    public static ArrayList<UDP_Peer_rcd> udpPeersReceived = new ArrayList<UDP_Peer_rcd>();
    public static ArrayList<UDP_Peer_sent> udpPeersSent = new ArrayList<UDP_Peer_sent>();
    // host address and port number of Registry
    public static String registryHost = "localhost"; //"136.159.5.22"; // change it to localhost if running on your pc
    // TCP PORT
    public static int registryPort = 55921;
    // stop UDP
    public static volatile boolean recieveStop = false;

    public static VolatileTimeStamp timeStamp = new VolatileTimeStamp();
    
    public static String ourLocation;
    
    public static void shutDownProcedure(DatagramSocket peerSock){
        
        try{
            peerSock.close();
            recieveStop = true;
        }
        catch(Exception err){
            System.out.println("Error: " + err);
        }
    }

    public static void snipReceived(String received, String source_location){
        received = received.substring(4, received.length()).trim();
        int timeStampReceived = Integer.parseInt(received.split(" ", 2)[0]);
        String content = received.split(" ", 2)[1].trim();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();
        String timeReceived = dtf.format(now);
        timeStamp.setTimeStamp(Integer.max(timeStamp.getTimeStamp(), timeStampReceived)+1);
        Snip snip = new Snip(timeStampReceived, content, timeReceived, source_location);
        snips.add(snip);
        System.out.println(Integer.toString(timeStampReceived) + " " + content + " " + timeReceived + " " + source_location);
    }

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

    public static void createUDPReceiveThread(DatagramSocket peerSock){
        Thread t = new Thread() {
            public void run(){
                while(!recieveStop){
                    byte[] buf = new byte[256];
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    try{
                        peerSock.receive(pack);
                        int source_port = pack.getPort();
                        String source_location = ((InetSocketAddress) pack.getSocketAddress()).getHostString() + ":" + Integer.toString(source_port);
                        String received = new String(buf);
                        String first4char = null;
                        if(received.length() > 4){
                            first4char = received.substring(0, 4);
                        }
                        switch(first4char){
                            case "stop":
                                System.out.println("Received 'stop' from the registry");
                                recieveStop = true;
                                shutDownProcedure(peerSock); 
                                break;
                            case "snip":
                                snipReceived(received, source_location);
                                break;
                            case "peer":
                                peerReceived(received, source_location);
                                break;
                        }
                    }
                    catch(Exception err){
                        System.out.println("Error: "+ err);
                    } 
                }
            }
        };
        t.start();
    }

    public static void sendPeerPackets(DatagramSocket peerSock){
        Thread t = new Thread(){
            public void run(){
                while(!recieveStop){
                    try{
                        for(Peer p : peers){
                            if(!p.location.equals(ourLocation)){
                                LocalDateTime now = LocalDateTime.now();
                                if(Duration.between(p.timeStamp, now).getSeconds() < 10){
                                    InetAddress host = InetAddress.getByName(p.location.split(":")[0]);
                                    Integer port = Integer.valueOf(p.location.split(":")[1].trim());
                                    for(Peer peer_info : peers){
                                        if(Duration.between(peer_info.timeStamp, now).getSeconds() < 10){
                                            byte[] toSend = ("peer"+peer_info.location).getBytes();
                                            DatagramPacket packet = new DatagramPacket(toSend, toSend.length, host, port);
                                            peerSock.send(packet);
                                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
                                            UDP_Peer_sent udp_peer = new UDP_Peer_sent(peer_info.location, p.location, dtf.format(now));
                                            udpPeersSent.add(udp_peer);
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
            while(!recieveStop){
                
            }
            snipSend.interrupt();
            initiateRegistryContact initContact2 = new initiateRegistryContact(registryHost, registryPort, UDP_PORT, peers, peers_Reg, sources, snips, udpPeersReceived, udpPeersSent);
            initContact2.start();
            
		}
		catch(Exception err) {
            // Exception handling
			System.out.println("Error: " + err.getMessage());
		}
	}
}
