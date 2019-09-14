import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.*;
import java.security.*;

class Server
{

    public static void main(String[] args) throws Exception{
        Server s = new Server();
        s.runServer(1);
    }
    ConcurrentHashMap<String,Socket> receivingTable;
    ConcurrentHashMap<String,Socket> sendingTable;
    ConcurrentHashMap<String,String> keyTable;
    ArrayList<String> registered;
    int mode;

    public Server(){
        receivingTable = new ConcurrentHashMap<>();
        sendingTable = new ConcurrentHashMap<>();
        keyTable = new ConcurrentHashMap<>();
        registered = new ArrayList<>();

    }


    class ServerSender implements Runnable
    {
        boolean success;
        Socket sender;
        BufferedReader inFromClient ;
        DataOutputStream outToClient;
        DataInputStream inDataStream;
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
            inDataStream = new DataInputStream(sender.getInputStream());
            
            boolean wellFormed = false;
            wellFormed = checkName(recMsg[2]);
            if(wellFormed)
            {
                username = recMsg[2].substring(1,recMsg[2].length()-1);
                if(registered.contains(username))
                {
                    String retMsg = "ERROR 104 Already Registered\n\n";
                    outToClient.writeBytes(retMsg);
                }
                else
                {
                    if(mode == 1 || mode==2)
                    {
                        System.out.println("working");
                        // DataInputStream inFromClientUTF = new DataInputStream(new BufferedInputStream(sender.getInputStream()));
                        // String inpMsg[] = (inFromClientUTF.readUTF()).split("\n",2);
                        String header = inFromClient.readLine();
                        System.out.println(header.length()+" 2nd line: "+header);
                        if(header.startsWith("Content-length: [") && header.endsWith("]"))
                        {
                            try
                            {
                                int mLength  = Integer.parseInt(header.substring(17,header.length()-1));
                                char[] buf = new char[mLength+2];                    
                                inFromClient.read(buf,0,mLength+2);
                                header = new String(buf);
                                // header = inpMsg[1];
                                header = header.substring(1,header.length()-1);
                                System.out.println(this.username+ " key "+header);
                                keyTable.put(this.username,header);
                            }
                            catch(NumberFormatException e)
                            {
                                outToClient.writeBytes("ERROR 102 Header Incomplete\n");
                            }
                            
                        }
                        else
                        {
                            outToClient.writeBytes("ERROR 102 Header Incomplete\n");
                        }
                        
                        //keyTable.put(username, publicKey);
                    }
                    String retMsg = "REGISTERED TOSEND ["+username+"]\n\n";
                    outToClient.writeBytes(retMsg);
                    success=true;
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
                System.out.println(Arrays.toString(e.getStackTrace()));
                registered.remove(this.username);
                      receivingTable.remove(this.username);
                      sendingTable.remove(this.username);
                
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
                  String header = inFromClient.readLine();
                  System.out.println("ServerSender: "+header);
                  int mLength = -1;
                  String destName = "";
                  if(header.startsWith("SEND [") && header.endsWith("]"))
                  {
                      destName = header.substring(6,header.length()-1);
                      
                          
                          header = inFromClient.readLine();
                          if(header.startsWith("Content-length: [") && header.endsWith("]"))
                          {
                           System.out.println("headeryo:"+header);                              
                           try
                           {
                            mLength = Integer. parseInt(header.substring(header.indexOf('[')+1,header.indexOf(']')));
                            System.out.println(mLength);
                           
                           }
                           catch(NumberFormatException e)
                           {
                               System.out.println("Length Field Error \n");
                               System.out.println(e.getMessage());
                           }
                          }
                      
                      if(!registered.contains(destName))
                      {
                        mLength = mLength +4;  
                        char[] buf = new char[mLength];                    
                        inFromClient.read(buf,0,mLength);
                        String Message = new String(buf);
                        System.out.print(Message);
                        outToClient.writeBytes("ERROR 102 Unable to send\n\n");
                        continue;
                      }
                  }
                  else if(header.startsWith("UNREGISTER"))
                  {
                      registered.remove(this.username);
                      receivingTable.remove(this.username);
                      sendingTable.remove(this.username);
                      outToClient.writeBytes("UNREGISTERED SUCCESSFULLY\n");
                      return;
                      
                  }
                  else if(header.startsWith("FETCHKEY") && mode==1 || mode == 2)
                  {
                      try
                      {
                          String keyReq =  header.substring(10, header.lastIndexOf(']'));
                          if(registered.contains(keyReq))
                          {
                              String toSend = "KEY\n";
                              toSend += "Content-length: ["+keyTable.get(keyReq).length()+"]\n";
                              toSend += "["+keyTable.get(keyReq)+"]";
                              outToClient.writeBytes(toSend);
                              System.out.println(toSend);
                              // outToClient.writeUTF("KEY ["+ keyTable.get(keyReq) +"]");
                              // System.out.println("KEY ["+ keyTable.get(keyReq) +"]\n");
                              continue;
                          }
                          else
                          {
                              
                              outToClient.writeBytes("ERROR 105 Requested user not registered\\n\n");
                              continue;
                          }
                      }
                      catch(IOException e)
                      {
                          System.out.println("Error Message Format");
                          outToClient.writeBytes("ERROR 102 Unable to send\\n\n");
                          continue;
                      }
                  }
                  System.out.println("mLength:"+mLength);
                  if(mLength>=0)
                  {
                      /*
                      mLength = mLength + 5;  
                      char[] buf = new char[mLength];
                                         
                      inFromClient.read(buf,0,mLength);
                      for( char l:buf)
                      System.out.println(l);
                      String Message = new String(buf);
                      System.out.print(Message);
                      mLength = mLength-5;*/
                      int c = inFromClient.read();
                      String Message = "";
                      System.out.println(c);
                      for(int j=0;j<(mLength+2);j++)
                        Message += Character.toString((char)inFromClient.read());
                      //String k = inFromClient.readLine();
                      System.out.print(Message + "end");
                              int a = Message.indexOf('[');
                              int b = Message.lastIndexOf(']');
                              System.out.println("a " + a + " b " + b);
                              if(b-a-1 == mLength)
                              {
                                  
                                  String res = "FORWARD ["+ username + "]\nContent-length: ["+mLength+"]\n\n";
                                  res = res + "["  + Message.substring(a+1,b)+"]";
                                  if(registered.contains(destName))
                                  {                                    
                                  Socket s = receivingTable.get(destName);
                                  System.out.println(destName + " " + s.getPort() + " " +res);
                                  
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
                  else
                  {
                      outToClient.writeBytes("ERROR 103 Header incomplete\n\n");
                  }
                  
              
              }
              catch(IOException e)
              {
                  System.out.println("Error receiving message from the port "+ sender.getPort());
                  registered.remove(this.username);
                  receivingTable.remove(this.username);
                  sendingTable.remove(this.username);
                  break;
              }
          }
        }
        
        private int checkHeader(String s)
        {
            String[] splitLines = s.split("\n");
            System.out.println("checkHeader: "+s);
            if (splitLines.length >=1)
            {
                String[] firstline = splitLines[0].split("\\[");
                if(firstline[0].equals("SEND "))
                {
                    String names[] = firstline[1].split("']'");
                    if(!registered.contains((names[0])))
                        return -1;
                    
                }
                    
                if(splitLines[1].startsWith("Content-Length: [") && splitLines[1].endsWith("]"))
                {
                    int a = splitLines[1].indexOf('[');
                    int b = splitLines[1].indexOf(']');
                    if(b==splitLines[1].length()-1)
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
        DataInputStream inFromClient;
        public ServerReceiver(Socket incoming, String recMsg) 
        {
            success = false;
            if(recMsg.charAt(0)=='[' && recMsg.charAt(recMsg.length()-1)==']')
            {
                System.out.println("size: "+registered.size());  
              if(registered.contains(recMsg.substring(1, recMsg.length()-1)))
              {
                  socket = incoming;
                  success = true;
                  username = recMsg.substring(1,recMsg.length()-1);
                  try
                  {
                  inStream = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                  outStream = new DataOutputStream(incoming.getOutputStream());
                  inFromClient = new DataInputStream(incoming.getInputStream());
                  String retMsg = "REGISTERED TORECV ["+username+"]\n\n";
                  outStream.writeBytes(retMsg);
                  }
                  catch(IOException e)
                  {
                      success = false;
                  }
              }
              
            }   
  
        }
        public ServerReceiver(Socket incoming,String thisname, String othername,String sendmessage)
        {
            success = true;
            socket = incoming;
            username = thisname;
            sendname = othername;
            this.message = sendmessage;
            try
            {
                inStream = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                outStream = new DataOutputStream(incoming.getOutputStream());
                inFromClient = new DataInputStream(incoming.getInputStream());
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
           System.out.println("forwarded msg: "+message);
           String msg = inFromClient.readUTF();
           System.out.println(msg);
           if(msg.startsWith("RECEIVED ["+sendname+"]"))
           {
               Socket s = sendingTable.get(sendname);
               DataOutputStream senderOut = new DataOutputStream(s.getOutputStream());
               System.out.println(s.getPort());
               senderOut.writeBytes("SENT ["+username+"]\n");
//               int c = inStream.read();
               System.out.println("Sent");
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
        this.mode = mode;
        ServerSocket welcomeSocket = new ServerSocket(2200);
        while(true)
        {
            Socket incoming = welcomeSocket.accept();
            System.out.println("New Socket Created:" + incoming.getPort());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
            // String recMsg[] = inFromClient.readLine().split(" ");
            String inptemp = inFromClient.readLine();
            System.out.println(inptemp.length()+" 1st line: "+inptemp);
            String recMsg[] = inptemp.split(" ");
            System.out.println(recMsg[1]);
            if(recMsg[1].equals("TOSEND"))
            {
                ServerSender newSender = new ServerSender(incoming,recMsg);
                while(true)
                {
                if(newSender.success)
                {
                    Thread t = new Thread(newSender);
                    t.start();
                    registered.add(newSender.username);
                    sendingTable.put(newSender.username,newSender.sender);
                    System.out.println("Registered: "+newSender.username);
                    break;
                }
                else
                {
                    String s = inFromClient.readLine();
                    System.out.println(s);
                    recMsg = s.split(" ");
                    if(recMsg.length >=3)
                     newSender = new ServerSender(incoming,recMsg);
                }
                }
            }
            else if(recMsg[1].equals("TORECV"))
            {
                ServerReceiver  newReceiver = new ServerReceiver(incoming,recMsg[2]);
                                 
                if(newReceiver.success)
                {
                    System.out.println("success");
                    receivingTable.put(recMsg[2].substring(1,recMsg[2].length()-1),incoming);
                    System.out.println(recMsg[2].substring(1,recMsg[2].length()-1));
                    System.out.println(receivingTable.get(recMsg[2].substring(1,recMsg[2].length()-1)).getPort());
                }                    
                                   
                
            }
        }
        
    }
}   