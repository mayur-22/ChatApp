import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import javax.crypto.Cipher;

class ClientReceiver extends Thread{

	private Socket soc;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private boolean isEncrypted;
	private byte[] privateKey;


	//if b is true then it is receiver;
	//else it is a sender
	public ClientReceiver(Socket s,int enc,byte[] privateKey) throws Exception{
		this.soc = s;
		this.outToServer = new DataOutputStream(s.getOutputStream());
		this.inFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
		this.privateKey = privateKey;
		if(enc==0)
			isEncrypted = false;
		else
			isEncrypted = true;
        String p = inFromServer.readLine();
		p = inFromServer.readLine();
	}

	public static String decrypt(byte[] privateKey, byte[] inputData) throws Exception {

        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);
        String decryptedString = new String(decryptedBytes);

        return decryptedString;
    }

	@Override
	public void run(){
		while(true){
			try{
               	
               	String header = inFromServer.readLine();
               	int mLength = -1;
               	String senderUserName = "";
               	if(header.startsWith("FORWARD [") && header.endsWith("]"))
               	{
               		senderUserName = header.substring(9,header.length()-1);
               		System.out.println("senderUserName: "+senderUserName);

               		header = inFromServer.readLine();
                    System.out.println(header);
                    if(header.startsWith("Content-length: [") && header.endsWith("]"))
                   	{
                   		System.out.println(header);                              
                           try{
                           	mLength = Integer. parseInt(header.substring(header.indexOf('[')+1,header.indexOf(']')));
                           
                           }
                           catch(NumberFormatException e)
                           {
                               System.out.println("Length Field Error \n");
                               System.out.println(e.getMessage());
                           }

                   	}	
               	}
               	else{
					String ret = "ERROR 103 Header Incomplete\n\n";
					outToServer.writeBytes(ret);
               	}

               	if(mLength>=0)
                {
					/*
		            mLength = mLength + 4;  
		            char[] buf = new char[mLength];                    
		            inFromServer.read(buf,0,mLength);
		            String message = new String(buf);
		            System.out.println("Message received: "+message);
		            mLength = mLength-4;
					*/
					int c = inFromServer.read();
                      String message = "";
                      System.out.println(c);
                      for(int j=0;j<(mLength+2);j++)
                        message += Character.toString((char)inFromServer.read());
						System.out.println(message);
		            int a = message.indexOf('[');
                    int b = message.lastIndexOf(']');
                    if( b-a-1 == mLength)
                    {
                    	//message = message.substring(1,message.length()-1);
                    	if(isEncrypted)
                    		message = message.substring(1,message.length()-1);

                    	System.out.println("encrsmg" +message+":");
                    	System.out.println("privatekey: "+ privateKey);
                    	System.out.println("\n\n\nYYYYYYYOOOOOOO\n\n");
                    	if(isEncrypted)
                    		message = decrypt(privateKey,Base64.getDecoder().decode(message));
                    	String ret = "RECEIVED ["+senderUserName+"]\n\n";
						outToServer.writeUTF(ret);
						System.out.println("New Message Received.\n Sender:"+senderUserName +"\nMessage: "+message);

                    }
					else
					{
						String ret = "ERROR 103 Header Incomplete\n\n";
					   outToServer.writeBytes(ret);
					}
                }
                else{
                	String ret = "ERROR 103 Header Incomplete\n\n";
					outToServer.writeBytes(ret);
                }

				/*
				int length = inFromServer.available();
				byte[] buf = new byte[length];
				inFromServer.readFully(buf);
				String inp = new String(buf);
				ParserR pr = new ParserR(inp);
				if(!pr.isValid()){
					String ret = "ERROR 103 Header Incomplete\n\n";
					outToServer.writeBytes(ret);
				}
				else{
					String ret = "RECEIVED ["+pr.getSenderName()+"]\n\n";
					outToServer.writeBytes(ret);
					System.out.println("New Message Received.\n Sender:"+pr.getSenderName()
						+"\nMessage: "+pr.getMessage());
				}*/


			}
			catch(Exception e){e.printStackTrace();
                        return;}

		}
	}
}

class ParserR{
	private String senderUserName;
	private String message;
	private boolean valid;


	public ParserR(String inp){
		this.senderUserName = "";
		this.message = "";
		this.valid = false;

		String msgs[] = inp.split("\n");
		String temp[] = msgs[0].split("\\[",2);
		if(temp[0].equals("FORWARD")){
			this.senderUserName = temp[1].substring(0,temp[1].length()-1);
		}

		int msgLen = 0;
		String tmp[] = msgs[1].split("' '\\[",2);
		if(tmp[0].equals("Content-length:")){
			msgLen = Integer.parseInt(tmp[1].substring(0,tmp[1].length()-1));
		}

		msgs[2] = msgs[2].substring(1,msgs[2].length()-1);
		if(msgs[2].length()==msgLen){
			this.message = msgs[2];
			this.valid = true;
		}
		else{
			this.senderUserName = "";
			this.message = "";
			this.valid = false;
		}
	}


	public boolean isValid(){
		return this.valid;
	}

	public String getSenderName(){
		return this.senderUserName;
	}

	public String getMessage(){
		return this.message;
	}


}