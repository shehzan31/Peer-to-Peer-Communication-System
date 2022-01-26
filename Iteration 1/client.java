import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    


public class client {

    public static ArrayList<String> peers = new ArrayList<String>();

    public static void sendTeamName(BufferedWriter writer){
        String teamName = "The Social Network";

        try{
            writer.write(teamName+"\n");
            writer.flush();
        }
        catch(Exception err) {
            System.out.println("Error: " + err.getMessage());
            }
    }

    public static void sendCode(BufferedWriter writer){
        try{  
            BufferedReader in = new BufferedReader(new FileReader("client.java"));
            String line = in.readLine();
    
            writer.write("Java\n");

            while(line != null)
            {
              writer.write(line+"\n");
              writer.flush();
              line = in.readLine();
            }
            writer.write("\n...\n");
            writer.flush();
            in.close();

        }
        catch(Exception err) {
            System.out.println("Error: " + err.getMessage());
        }
    }

    public static void receivePeers(BufferedReader reader){
        try{
            int numPeers = Integer.parseInt(reader.readLine());
            for (int i = 0; i < numPeers; i++) {
                String peer = reader.readLine().trim();
                boolean received = false;
                for (int j = 0; j < peers.size(); j++){
                    if (peers.get(j) == peer){
                        received = true;
                    }
                }
                if (!received) peers.add(peer);
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();  
            System.out.println(dtf.format(now));  
            System.out.println(peers);
        }
        catch(Exception err) {
            System.out.println("Error: " + err.getMessage());
        }
        
    }

    public static void sendReport(BufferedWriter writer){
    
    }
    
    
    public static void main(String[] args)
	{

		try (
				Socket clientSocket = new Socket("localhost", 55921);
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				
			)
		{
            Boolean open = true;
            while(open){
                String response;
                response = reader.readLine();
                //String type = response.split("/n", 2)[0];
                switch(response){
                    case "get team name":
                        System.out.println("Requesting team name");
                        sendTeamName(writer);
                        System.out.println("Sent team name");
                        break;
                    case "get code":
                        System.out.println("Requesting code base");
                        sendCode(writer);
                        System.out.println("Sent code base");
                        break;
                    case "receive peers":
                        System.out.println("Receiving peers");
                        receivePeers(reader);
                        System.out.println("Received");
                        break;
                    case "get report":
                        System.out.println("Requesting report");
                        sendReport(writer);
                        System.out.println("Sent report");
                        break;
                    case "close":
                        System.out.println("Requesting to close the socket");
                        open = false;
                        break;
                }
            }
            clientSocket.close();
		}
		catch(Exception err) {
			System.out.println("Error: " + err.getMessage());
		}
	}
}
