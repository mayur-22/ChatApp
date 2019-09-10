import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;
import java.security.*;

class Client{

	public static void main(String[] args) throws Exception{
		String username;
	 	String serverIPAddress;

	 	Scanner sc = new Scanner(System.in);
	 	System.out.print("Enter IPAddress: ");
	 	serverIPAddress = "localhost";

	 	//port number = 2200
	 	Socket sendSocket = new Socket(serverIPAddress,2200);
	 	System.out.println("Connected...");
	 	String privateKey = "";

	 	int enc=0;

	 	while(true){
	 		System.out.print("Please Enter New UserName: ");
	 		// username = "aca";
	 		username = sc.nextLine();
	 		// if(!username.equals("asa")){
	 		// 	System.out.println("xx");
	 		// 	username = "asa";}

	 		System.out.println("Do u want encryption?");
	 		enc = sc.nextInt();

	 		if(enc==1){
	 			KeyPairGenerator kgp = KeyPairGenerator.getInstance("RSA");
	 			KeyPair kp = kgp.generateKeyPair();
	 			System.out.println(kp.getPublic() + " " + kp.getPrivate());

	 			String publicKey = new String(kp.getPublic().getEncoded());
	 			privateKey = new String(kp.getPrivate().getEncoded());
	 			String sendMsg = "REGISTER TOSEND ["+username+"] ["+publicKey+"]\n\n";
		 		DataOutputStream outToServer = new DataOutputStream(sendSocket.getOutputStream());
		 		outToServer.writeBytes(sendMsg);
	 		}

	 		//output to server
	 		else{
		 		String sendMsg = "REGISTER TOSEND ["+username+"]\n\n";
		 		DataOutputStream outToServer = new DataOutputStream(sendSocket.getOutputStream());
		 		outToServer.writeBytes(sendMsg);
		 	}

	 		//input from server
	 		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
	 		String receiveMsg[] = (inFromServer.readLine()).split(" ");
            String r = inFromServer.readLine();
	 		if(receiveMsg[0].equals("ERROR")){
	 			if(receiveMsg[1].equals("100"))
	 				System.out.println("Ill Formed UserName");
	 			else if(receiveMsg[1].equals("104"))
	 				System.out.println("UserName Already Registered");
	 		}
	 		else
	 			break;
	 	}

		//registering for receiving
		Socket receiveSocket = new Socket(serverIPAddress,2200);
		String sendMsg = "REGISTER TORECV ["+username+"]\n\n";
	 	DataOutputStream outToServer = new DataOutputStream(receiveSocket.getOutputStream());
	 	outToServer.writeBytes(sendMsg);

	 	
	 	//now we have successfully registered

	 	
	 	
 		Thread sen = new ClientSender(sendSocket,enc);
 		Thread rec = new ClientReceiver(receiveSocket,enc,privateKey);
 		sen.start();
 		rec.start();

 		sen.join();
 		
	 	sendSocket.close();
	 	receiveSocket.close();

	}

}


