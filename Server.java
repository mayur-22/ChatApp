import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;

class Server
{
    ConcurrentHashMap<String,Socket> ReceivingTable;
    ConcurrentHashMap<String,Socket> SendingTable;
    ConcurrentHashMap<String,PublicKey> KeyTable;
    ArrayList<String> Registered;
    class ServerSender implements Runnable
    {
        boolean success;
        Socket sender;
        BufferedReader inFromClient ;
        DataOutputStream outToClient;
        String username;
        
        public String getName()
        {
         return username;   
        }                
        public ServerSender(Socket incoming, String[] recMsg) 
        {
            success = false;
            sender = incoming;
            try
            {
            inFromClient = new BufferedReader(new InputStreamReader(sender.getInputStream()));
            outToClient = new DataOutputStream(sender.getOutputStream());
            
            boolean wellFormed = false;
	    wellFormed = checkName(recMsg[1]);
	    if(wellFormed)
            {
	        if(Registered.contains(username))
                {
                    String retMsg = "ERROR 104 Already Registered\n\n";
                    outToClient.writeBytes(retMsg);
                }
        	
	        String retMsg = "REGISTERED TOSEND ["+username+"]\n\n";
	        outToClient.writeBytes(retMsg);
	
	    }
	    else
            {
	        String retMsg = "ERROR 100 Malformed username\n\n";
		outToClient.writeBytes(retMsg);
	    }
           
            }catch(IOException e)
            {
                System.out.println("Error communicating with socket "+ incoming.getInetAddress() + " "+incoming.getPort());
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }
        
        private boolean checkName(String s)
        {
            if(!s.endsWith("]") || !s.startsWith("["))
                return false;
            s = s.substring(1,s.length()-1);
            char[] array = s.toCharArray();
            for(char c:array)
            {
                if((c>='a' && c<='z')||(c>='A' && c<='Z') || (c>='0' && c<='9')) 
                    continue;
                return false;
            }
            this.username = s;
            return true;
        }
        @Override
        public void run()
        {
          while(true)
          {
              try
              {
                  String Message = inFromClient.readLine();
                  int mLength = checkHeader(Message);
                  if(mLength>=0)
                  {
                      String[] splitted = Message.split("\n");
                      if(splitted.length>=4)
                      {
                          if((splitted[3].startsWith("[")) && splitted[3].endsWith("]"))
                          {
                              int a = splitted[3].indexOf('[');
                              int b = splitted[3].lastIndexOf(']');
                              if(b-a-1 == mLength)
                              {
                                  String destName = splitted[1].substring(7,splitted[1].length()-1);
                                  String res = "FORWARD ["+ username + "]\n" + splitted[2] + "\n\n";
                                  res = res + "["  + splitted[3].substring(a+1,b)+"]";
                                  
                                  if(Registered.contains(destName))
                                  {                                      
                                  Socket s = ReceivingTable.get(destName);
                                  ServerReceiver recv = new ServerReceiver(s,destName,username,res);
                                  Thread t = new Thread(recv);
                                  t.start();
                                  }
                              }
                              else
                              {
                                  outToClient.writeBytes("ERROR 102 Unable to send\\n\n");
                              }
                          }
                      }
                      else
                          outToClient.writeBytes("ERROR 102 Unable to send\\n\n");
                  }
                  else
                  {
                      outToClient.writeBytes("ERROR 103 Header incomplete\\n\n");
                  }
                  
              
              }
              catch(IOException e)
              {
                  System.out.println("Error receiving message from the port "+ sender.getPort());
              }
          }
        }
        
        private int checkHeader(String s)
        {
            String[] splitLines = s.split("\n",2);
            if (splitLines.length >=1)
            {
                String[] firstline = splitLines[0].split("\\[");
                if(firstline[0].equals("SEND "))
                {
                    String names[] = firstline[1].split("']'");
                    if(!Registered.contains((names[0])))
                        return -1;
                    
                }
                    
                if(splitLines[1].startsWith("Content-Length: [") && splitLines[1].endsWith("]"))
                {
                    int a = splitLines[1].indexOf('[');
                    int b = splitLines[1].indexOf(']');
                    if(b==splitLines[1].length())
                    {
                        String lng = splitLines[1].substring(a+1,b);
                        int length = Integer.parseInt(lng);
                        return length;
                    }
                    else
                        return -1;
                }
                    
            }
            else
             return -1;
            return -1;
        }
        
    }
    
    class ServerReceiver implements Runnable
    {
        boolean success;
        Socket socket;
        String message;
        String username;
        String sendname;
        BufferedReader inStream;
        DataOutputStream outStream; 
        public ServerReceiver(Socket incoming, String recMsg) 
        {
            success = false;
            if(recMsg.charAt(0)=='[' && recMsg.charAt(recMsg.length()-1)==']')
            {
              if(Registered.contains(recMsg.substring(1, recMsg.length()-1)))
              {
                  socket = incoming;
                  success = true;
                  username = recMsg.substring(1,recMsg.length()-1);
                  try
                  {
                  inStream = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                  outStream = new DataOutputStream(incoming.getOutputStream());
                  }
                  catch(IOException e)
                  {
                      success = false;
                  }
              }
              
            }   
  
        }
        public ServerReceiver(Socket incoming,String thisname, String othername,String messgae)
        {
            success = true;
            socket = incoming;
            username = thisname;
            sendname = othername;
            try
            {
                inStream = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                outStream = new DataOutputStream(incoming.getOutputStream());
            }
            catch(IOException e)
            {
                success = false;
            }
        }
        @Override
        public void run()
        {
          try
          {
           outStream.writeBytes(message);
           String msg = inStream.readLine();
           if(msg.equals("RECEIVED ["+username+"]\n"))
           {
               Socket s = SendingTable.get(sendname);
               DataOutputStream senderOut = new DataOutputStream(s.getOutputStream());
               senderOut.writeBytes("SENT ["+sendname+"]\n");
           }
          }
          catch(IOException e)
          {
              throw new RuntimeException(e);
          }
          
        }
        
        
    }
    void runServer(int mode)throws Exception
    {
        
        ServerSocket welcomeSocket = new ServerSocket(2200);
        while(true)
        {
            Socket incoming = welcomeSocket.accept();
            System.out.println("\nNew Scoket Created" + incoming.getPort());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
	    String recMsg[] = inFromClient.readLine().split(" ");
            if(recMsg[1].equals("TOSEND"))
            {
                ServerSender newSender = new ServerSender(incoming,recMsg);
                if(newSender.success)
                {
                    
                    Thread t = new Thread(newSender);
                    t.start();
                    Registered.add(newSender.username);
                    SendingTable.put(newSender.username,newSender.sender);
                }
            }
            else if(recMsg[1].equals("TORECV"))
            {
                ServerReceiver  newReceiver = new ServerReceiver(incoming,recMsg[1]);
                if(newReceiver.success)
                {
                    ReceivingTable.put(recMsg[1].substring(1,recMsg[1].length()-1),incoming);
                }
            }
        }
        
    }
}	
