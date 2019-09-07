import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;

class Server
{
    ConcurrentHashMap<String,Socket> ReceivingTable;
    ConcurrentHashMap<String,PublicKey> KeyTable;
    ArrayList<String> Registered;
    class ServerSender implements Runnable
    {
        boolean success;
        Socket sender;
        BufferedReader inFromClient ;
        DataOutputStream outToClient;
        
        public ServerSender(Socket incoming, String[] recMsg) {
            success = false;
            sender = incoming;
            try
            {
            inFromClient = new BufferedReader(new InputStreamReader(sender.getInputStream()));
            outToClient = new DataOutputStream(sender.getOutputStream());
            
            boolean wellFormed = true;
	    //if username is well formed
	    if(wellFormed)
            {
	        boolean exists = false;
	        if(Registered.contains(recMsg[1]))
                    exists = false;
                else
                    exists = checkName(recMsg[1]);
                
        	if(exists)
                {
		    String retMsg = "ERROR 104 Already Registered\n\n";
                    outToClient.writeBytes(retMsg);
                }
		else
                {
		    String retMsg = "Registered TOSEND [username]\n\n";
		    outToClient.writeBytes(retMsg);
		}
	    }
	    else
            {
	        String retMsg = "ERROR 100 Malformed username\n\n";
		outToClient.writeBytes(retMsg);
	    }
           
            }catch(IOException e)
            {
                System.out.println("Error communicating with socket "+ incoming.getInetAddress() + " "+incoming.getPort());
            }
        }
        
        boolean checkName(String s)
        {
            return true;
        }
        @Override
        public void run()
        {

        }
        
    }
    
    class ServerReceiver implements Runnable
    {
        boolean success;
        public ServerReceiver(Socket incoming, String[] recMsg) {
            success = false;
        }
        @Override
        public void run()
        {

        }
        
    }
    void runServer(int mode)throws Exception
    {
        
        ServerSocket welcomeSocket = new ServerSocket(2200);
        while(true)
        {
            Socket incoming = welcomeSocket.accept();
            System.out.println("\nNew Scoket Create" + incoming.getPort());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
	    String recMsg[] = inFromClient.readLine().split(" ");
            if(recMsg[1].equals("TOSEND"))
            {
                ServerSender newSender = new ServerSender(incoming,recMsg);
                if(newSender.success)
                {
                    Thread t = new Thread(newSender);
                    t.start();
                    Registered.add(recMsg[1]);
                }
            }
            else if(recMsg[1].equals("TORCV"))
            {
                ServerReceiver  newReceiver = new ServerReceiver(incoming,recMsg);
                if(newReceiver.success)
                {
                    ReceivingTable.put(recMsg[1],incoming);
                }
            }
        }
        
    }
}	