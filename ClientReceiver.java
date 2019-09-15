import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.MessageDigest;
import java.math.BigInteger;


import javax.crypto.Cipher;

class ClientReceiver extends Thread{

	private Socket soc;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private boolean isEncrypted;
	private byte[] privateKey;
	private byte[] publicKey;
	private int mode;



	//if b is true then it is receiver;
	//else it is a sender
	public ClientReceiver(Socket s,int enc,byte[] privateKey,byte[] publicKey) throws Exception{
		this.soc = s;
		this.outToServer = new DataOutputStream(s.getOutputStream());
		this.inFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
		this.privateKey = privateKey;
		this.mode=enc;
		this.publicKey = publicKey;
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

    public static byte[] decryptPub(byte[] publicKey, byte[] inputData)
            throws Exception {

        PublicKey key = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(publicKey));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return decryptedBytes;
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
                    	String encrypredMsg = new String(message);
                    	if(isEncrypted)
                    		message = decrypt(privateKey,Base64.getDecoder().decode(message));

                    	if(mode==2){
                    		int hashLength = -1;
                    		header = inFromServer.readLine();
		                    System.out.println(header);
		                    if(header.startsWith("Content-length: [") && header.endsWith("]"))
		                   	{
		                   		System.out.println(header);                              
		                           try{
		                           	hashLength = Integer. parseInt(header.substring(header.indexOf('[')+1,header.indexOf(']')));
		                           
		                           }
		                           catch(NumberFormatException e)
		                           {
		                               System.out.println("Length Field Error \n");
		                               System.out.println(e.getMessage());
		                           }

		                   	}

		                   	if(hashLength>=0){


		                   		  int cH = inFromServer.read();
			                      String messagehash = "";
			                      System.out.println(cH);
			                      for(int k=0;k<(hashLength+2);k++)
			                        messagehash += Character.toString((char)inFromServer.read());
								  System.out.println(messagehash);
					              int aH = messagehash.indexOf('[');
			                      int bH = messagehash.lastIndexOf(']');
			                      if( bH-aH-1 == hashLength){

			                      	String hashStr = messagehash.substring(1,messagehash.length()-1);


			                      	//public key of sender
                    				int senderPubKeyLength = -1;
			                      	header = inFromServer.readLine();
				                    System.out.println(header);
				                    if(header.startsWith("Content-length: [") && header.endsWith("]"))
				                   	{
				                   		System.out.println(header);                              
				                           try{
				                           	senderPubKeyLength = Integer. parseInt(header.substring(header.indexOf('[')+1,header.indexOf(']')));
				                           
				                           }
				                           catch(NumberFormatException e)
				                           {
				                               System.out.println("Length Field Error \n");
				                               System.out.println(e.getMessage());
				                           }

				                   	}

				                   	int cK = inFromServer.read();
			                      String messageKey = "";
			                      System.out.println(cK);
			                      for(int l=0;l<(senderPubKeyLength+2);l++)
			                        messageKey += Character.toString((char)inFromServer.read());
								  System.out.println(messageKey);
					              int aK = messageKey.indexOf('[');
			                      int bK = messageKey.lastIndexOf(']');

			                      if( bK-aK-1 == senderPubKeyLength){

			                      		String senderPubKey = messageKey.substring(1,messageKey.length()-1);
			                      		System.out.println("Message:"+message);
			                      		System.out.println("encMs:"+encrypredMsg);
			                      		System.out.println("senderPubKey:"+senderPubKey);
			                      		System.out.println("RecHashStr:"+hashStr);
				                      	byte[] kPubHdash = decryptPub(Base64.getDecoder().decode(senderPubKey),Base64.getDecoder().decode(hashStr));

				                      	MessageDigest md = MessageDigest.getInstance("SHA-256");
								        byte[] messageDigest = md.digest(Base64.getDecoder().decode(encrypredMsg));
								        BigInteger no = new BigInteger(1,messageDigest);

								        String hashText = no.toString(16);
								        while (hashText.length() < 32) { 
								                hashText = "0" + hashText;
								        }
								        System.out.println("HashText:"+hashText);
	        							byte[] h = Base64.getDecoder().decode(hashText);
	        							System.out.println("New");
	        							System.out.println(h.length+" "+kPubHdash.length);
	        							boolean bol=  true;
								        for(int i=0;i<h.length;i++){
								            // System.out.print(h[i]);
								            if(h[i]!=kPubHdash[i]){
								                System.out.print(i);
								                bol=false;
								                break;
								            }
								        }
								        System.out.println(bol);
								    }
			                      }

		                   	}

                    	}


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