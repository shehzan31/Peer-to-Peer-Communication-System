import java.io.*;
import java.net.*;

public class client {

    public static void sendTeamName(BufferedWriter writer){
        String teamName = "The Social Network";
        writer.write(teamName+"/n");
        writer.flush();
    }

    public static void sendCode(BufferedWriter writer){
    
    }

    public static void receiveInfo(String info){
    
    }

    public static void sendReport(BufferedWriter writer){
    
    }
    
    
    public static void main(String[] args) {
        String host = "localhost";
        String portNum = 55921;
        
        try{
            Socket clientSocket = new clientSocket(host, portNum);
            BufferedReader reader = new BufferedReader(clientSocket.getInputStream());
            BufferedWriter writer = new BufferedWriter(clientSocket.getOutputStream());
            Boolean open = true;
            while(open){
                String request;
                request = reader.readLine().trim();
                switch(request){
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
                    case "receive request":
                        System.out.println("Receiving request");
                        receiveInfo(request);
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
            System.out.println("Closed the socket");
        }
        catch(SocketException e){
            System.out.println("Ran into some error");
        }
    }
}
