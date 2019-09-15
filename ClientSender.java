/*************************************************************/
/*************************************************************/
/*
	Function are completed. Have to implement about how it will close
	Ctrl+C not handeled
	If [ or ] occur in messafe or username then will result in bad output
	
	Sample Valid Input: @[mayur] [messsageadsfa]
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Scanner;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.MessageDigest;
import java.math.BigInteger;

import javax.crypto.Cipher;




class ClientSender extends Thread{

	private Socket soc;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private boolean isEncrypted;
	private byte[] privateKey;
	private int mode;

	//if b is true then it is receiver;
	//else it is a sender
	public ClientSender(Socket s,int enc,byte[]  privateKey) throws Exception{
		this.soc = s;
		this.outToServer = new DataOutputStream(s.getOutputStream());
		this.inFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
		this.privateKey = privateKey;
		this.mode = enc;
		if(enc==0)
			isEncrypted = false;
		else
			isEncrypted = true;
	}

	public static String encrypt(byte[] publicKey, byte[] inputData) throws Exception {
			System.out.println("x");
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
			System.out.println("y");
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedBytes = cipher.doFinal(inputData);
            String encryptedString =  Base64.getEncoder().encodeToString(encryptedBytes);
            System.out.println("Encoded String: "+encryptedString);
            return encryptedString;
        }

    public static byte[] encryptPvt(byte[] privateKey, byte[] inputData)
            throws Exception {
        PrivateKey key = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKey));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(inputData);

        return encryptedBytes;
    }

	@Override
	public void run(){
		String userStr;

		Scanner sc = new Scanner(System.in);
		while(true){

			try{

				userStr = sc.nextLine();

				if(userStr.equals("Unregister")){
					String toSend = "UNREGISTER\n\n";
					outToServer.writeBytes(toSend);
                    String ack = inFromServer.readLine();
                    System.out.println(ack);
                    if(ack.startsWith("REGISTERED SUCCESSFULLY"))
                        System.out.println("Exit");
                    this.soc.close();
                    return;
				}
				
				//new class will be made which will return username messege and boolean;
				ParserS pr = new ParserS(userStr);


				if(!pr.isValidInput()){
					System.out.println("Type Again...");
					continue;
				}

				String userToSend = pr.getUserName();
				String message = pr.getMessage();
				String senderPublicKey = "";
				if(isEncrypted){
					String send = "FETCHKEY ["+userToSend+"]\n";
					System.out.println("toSendEnc: "+send);
					outToServer.writeBytes(send);

                    // DataInputStream inFromServerUTF = new DataInputStream(new BufferedInputStream(soc.getInputStream()));
					// String ackt = inFromServerUTF.readUTF();
					// String  rectMsg[] = ackt.split(" ",2);
					String inp = inFromServer.readLine();
					if(inp.equals("KEY")){
						System.out.println("reached here");
						inp = inFromServer.readLine();
						System.out.println(inp);
						String rectMsg[] = inp.split(" ",2);
						System.out.println(rectMsg[1]);
		                // int mLength = Integer.parseInt(inp.substring(1,rectMsg[1].length()-1));
		                int mLength = 128;
		                char[] buf = new char[mLength+2];                    
		                inFromServer.read(buf,0,mLength+2);
		                String Message = new String(buf);
		                System.out.println("Message:"+Message);
		                senderPublicKey = Message.substring(1,Message.length()-1);
                        System.out.println("SenderPublicKey:"+senderPublicKey);                
					}

					else if (inp.equals("ERROR")){
						System.out.println("Unable to Send");
                        continue;
                    }
					else if (inp.equals("ERROR")){
						System.out.println("Header Incomplete");
                        continue;
                    }
                	else{
	           			System.out.println("Header Incomplete");
	                    continue;
                    }
				}

				String toSend = "SEND [" + userToSend + "]\n";
				System.out.println("There\n");
				if(isEncrypted){
					// System.out.println(Base64.getDecoder().decode(senderPublicKey));
					System.out.println("MessageIMP"+message);
					byte[] decodedMsg = message.getBytes();
					message = encrypt(Base64.getDecoder().decode(senderPublicKey),decodedMsg);
					toSend += "Content-length: [" + message.length() + "]\n\n";
					toSend += "[" + message + "]";
				}
				if(mode==2){
					//assuming message is already encrypted
					MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] messageDigest = md.digest(Base64.getDecoder().decode(message));
                    BigInteger no = new BigInteger(1,messageDigest);
                    String hashText = no.toString(16);
                    while (hashText.length() < 32) { 
                            hashText = "0" + hashText;
                    }
                    System.out.println("hashText"+hashText);
                    byte[] h = Base64.getDecoder().decode(hashText);
                    byte[] hDash = encryptPvt(this.privateKey,h);
                    System.out.println("hi");
                    String hDashStr = Base64.getEncoder().encodeToString(hDash);
                    toSend += "Content-length: [" + hDashStr.length() + "]\n\n";
                    toSend += "[" + hDashStr + "]";
                    System.out.println("\nmsgEncr:"+message);
                    System.out.println("\ndDashStr:"+hDashStr);
				}
				if(mode==0){
					toSend += "Content-length: [" + message.length() + "]\n\n";
					toSend += "[" + message + "]";
				}
				System.out.println("toSend "+toSend);
				outToServer.writeBytes(toSend);
                String ack = inFromServer.readLine();
                System.out.println(ack);
				String receiveMsg[] = ack.split(" ");
				if(receiveMsg[0].equals("SENT"))
					System.out.println("Message Sent Successfully");
				else if (receiveMsg[1].equals("102"))
					System.out.println("Unable to Send");
				else if (receiveMsg[1].equals("103"))
					System.out.println("Header Incomplete");
				else if(receiveMsg[0].equals("UNREGISTERED") && receiveMsg[1].equals("SUCCESSFULLY")){
					return;
				}
			}
			catch(Exception e){
				System.out.println("Exception");
				System.out.println(e);
				e.printStackTrace();
			}

		}

	}


}

class ParserS{
	private String recipientUserName;
	private String message;
	private boolean valid;

	
	//constructor class
	public ParserS(String inp){
		if(!(inp.charAt(0)=='@' && inp.charAt(1)=='[' && inp.charAt(inp.length()-1)==']')){
			this.recipientUserName = "";
			this.message = "";
			this.valid = false;
			System.out.println("Ill Formed Message");
		}
		else{
			int i=2;
			this.recipientUserName = "";
			for(i=2;i<inp.length();i++){
				if(inp.charAt(i)==']')
					break;
				recipientUserName += inp.charAt(i);
			}

			//no message body
			if(i>=inp.length()){
				this.recipientUserName = "";
				this.message = "";
				this.valid = false;
				System.out.println("No Message");
			}

			//assuming no extra special brakcets ]
			i += 3; //assuming space berween brackets
			this.message = "";
			while(i!=inp.length()){
				if(inp.charAt(i)==']')
					break;
				message += inp.charAt(i);
				i++;
			}

			if(!(inp.charAt(inp.length()-1)==']')){
				this.recipientUserName = "";
				this.message = "";
				this.valid = false;
				System.out.println("Ill Formed Message");
			}

			this.valid = true;

		}
	}

	public boolean isValidInput(){
		return this.valid;
	}

	public String getUserName(){
		return this.recipientUserName;
	}

	public String getMessage(){
		return this.message;
	}

}