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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;



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

    public void start(){
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
                            for(Peer peer_info : peers){
                                if(Duration.between(peer_info.timeStamp, now).getSeconds() < 10){
                                    DatagramPacket packet = new DatagramPacket(toSend, 256, host, port);
                                    peerSock.send(packet);
                                }
                            }   
                        } 
                    }
                }
            }
        }
        catch(InterruptedException err){

        }
        catch(Exception err){

        }
    }
}

// Client class - main class
public class client {

    // master arraylist to store peers (no duplicates) and sources (class provided above)
    public static ArrayList<Peer> peers = new ArrayList<Peer>();
    public static ArrayList<source> sources = new ArrayList<source>();
    // host address and port number of Registry
    public static String registryHost = "localhost"; //"136.159.5.22"; // change it to localhost if running on your pc
    // TCP PORT
    public static int registryPort = 55921;
    // UDP port
    public static int UDP_PORT = 33333;
    // stop UDP
    public static boolean recieveStop = false;
    

    public static String ourLocation;

    /**
     * Sends the team name through the BufferedWritter of the OutputStream in the socket connection.
     * @param writer
     */
    public static void sendTeamName(BufferedWriter writer){
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

    /**
     * Sends current list of peers followed by a report that indicates all sources of this 
     * peer list
     * @param writer
     */
    public static void sendReport(BufferedWriter writer){
        try{
            // Writes the number of peers followed by a newline character
            writer.write(Integer.toString(peers.size())+"\n");
            for(Peer p : peers){
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

    public static void shutDownProcedure(DatagramSocket peerSock){
        
        try{
            peerSock.close();
        }
        catch(Exception err){
            System.out.println("Error: " + err);
        }
        
        initiateRegistryContact(registryHost, registryPort);
    }

    public static void snipReceived(String received){

    }

    public static void peerReceived(String received, String source_location){
        received = received.substring(4, received.length()).trim();
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
        System.out.println("Received peer "+received+" from "+source_location);
    }

    public static void createUDPReceiveThread(DatagramSocket peerSock){
        Thread t = new Thread() {
            public void run(){
                byte[] buf = new byte[256];
                DatagramPacket pack = new DatagramPacket(buf, buf.length);
                while(!recieveStop){
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
                                snipReceived(received);
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
                                            System.out.println("Sent peer "+ peer_info.location + " to " + p.location);
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


    public static void initiateRegistryContact(String host, int port){
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
            ourLocation = InetAddress.getLocalHost().getHostAddress()+":"+UDP_PORT;
            initiateRegistryContact(registryHost, registryPort);
            VolatileTimeStamp timeStamp = new VolatileTimeStamp();
            // Starting a datagram socket
            DatagramSocket peerSock = new DatagramSocket(UDP_PORT);
            createUDPReceiveThread(peerSock);
            sendPeerPackets(peerSock);
            SnipSend snipSend = new SnipSend(peerSock, timeStamp, peers, ourLocation);   
		}
		catch(Exception err) {
            // Exception handling
			System.out.println("Error: " + err.getMessage());
		}
	}
}
