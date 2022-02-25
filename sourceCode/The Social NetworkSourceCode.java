Fri Feb 25 14:12:35 MST 2022
java
/**
 * CPSC 559: Project Iteration 1 solution
 * Client Class which connects with the Registry via TCP Protocol and interacts as described in the rubric
 * @author Shehzan Murad Ali and Humble Chaudhry
 *  */ 


// imports
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;



// source class
class source{
    
    // local variables storing location (ip:port), time stamp, array lst of peers received from this source 
    public String location;
    public String timeReceived;
    public ArrayList<String> source_peers;

    // Constructor to store all the values 
    public source(String location, String time, ArrayList<String> peers){
        this.location = location;
        this.timeReceived = time;
        this.source_peers = peers;
    }
}

// Client class - main class
public class client {

    // master arraylist to store peers (no duplicates) and sources (class provided above)
    public static ArrayList<String> peers = new ArrayList<String>();
    public static ArrayList<source> sources = new ArrayList<source>();
    // host address and port number of Registry
    public static String registryHost = "localhost"; //"136.159.5.22"; // change it to localhost if running on your pc
    // TCP PORT
    public static int registryPort = 55921;
    // UDP port
    public static int UDP_PORT = 33333;
    // stop UDP
    public static boolean recieveStop = false;

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
            ArrayList<String> localPeers = new ArrayList<String>();
            // reads num of peers
            int numPeers = Integer.parseInt(reader.readLine());
            // for loop for each peer
            for (int i = 0; i < numPeers; i++) {
                //reads peer per each line
                String peer = reader.readLine().trim();
                // boolean to check if theh peer is duplicate
                boolean received = false;
                //checking if the peer is already in the list
                for (int j = 0; j < peers.size(); j++){
                    // if found change the bool to true
                    if (peers.get(j) == peer){
                        received = true;
                    }
                }
                // if no duplicate, then add to the master list of peers
                if (!received) peers.add(peer);
                // add peerto the local list of peers
                localPeers.add(peer);
            }
            // Date stamp recorded
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();
            // create a new source with source ip:port, timestamp, peers received
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
            for(String p : peers){
                // for each peer it reads, it send the peer followed by a new line character
                writer.write(p+"\n");
            }
            // Writes the number of sources followed by a new line character
            writer.write(Integer.toString(sources.size())+"\n");
            for(source s : sources){
                // for each source it writes its ip:port followed by a new line followed by time
                // it received followed by a new line followed by number of peers in each source
                // followed by a newline character
                writer.write(s.location + "\n" + s.timeReceived + "\n" + Integer.toString(s.source_peers.size()) + "\n");
                for(String p : s.source_peers) {
                    // for each peer it writes a peer (ip:port) followed by a newline character
                    writer.write(p+"\n");
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

    public static void shutDownProcedure(){
        initiateRegistryContact(registryHost, registryPort);
        recieveStop = true;
    }

    public static void snipReceived(String received){

    }

    public static void peerReceived(String received){

    }

    public static void createUDPReceiveThread(DatagramSocket peerSock){
        Thread t = new Thread() {
            public void run(){
                System.out.println("Received from UDP");
                byte[] buf = new byte[256];
                DatagramPacket pack = new DatagramPacket(buf, buf.length);
                while(true){
                    try{
                        peerSock.receive(pack);
                        System.out.println("Received from UDP");
                        String received = new String(buf);
                        String first4char = null;
                        if(received.length() > 4){
                            first4char = received.substring(0, 4);
                        }
                        switch(first4char){
                            case "stop":
                                System.out.println("Stop received from UDP");
                                shutDownProcedure();
                                break;
                            case "snip":
                                snipReceived(received);
                                break;
                            case "peer":
                                peerReceived(received);
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

    public static void collabPeers(DatagramSocket peerSock){

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
        
		try (
                // Starting a datagram socket
                DatagramSocket peerSock = new DatagramSocket(UDP_PORT);
			)
		{
            createUDPReceiveThread(peerSock);
            initiateRegistryContact(registryHost, registryPort);
            while(!recieveStop) collabPeers(peerSock);
		}
		catch(Exception err) {
            // Exception handling
			System.out.println("Error: " + err.getMessage());
		}
	}
}

