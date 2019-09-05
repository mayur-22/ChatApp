import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

class Server throws Exception{

	private ArrayList<Thread> table;

	public static void main(String[] args) {
		ServerSocket welcomeSocket = new ServerSocket(2200);
		table = new ArrayList<Thread>();

		while(true){
			Socket connSocket = welcomeSocket.accept();

			//new client is connected
			System.out.println("New Client: "+connSocket);

			
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
			String recMsg[] = inFromClient.readLine().split(" ");

			if(recMsg[1].equals("TOSEND")){
				Thread sen = new ServerSender(connSocket);
				
				/***check by parsing***/
				while(true){
					boolean wellFormed = true;
					//if username is well formed
					if(wellFormed){
						boolean exists = false;
						//check if name already exists

						if(exists){
							String retMsg = "ERROR 104 Already Registered\n\n";
							DataOutputStream outToClient = new DataOutputStream(connSocket.getOutputStream());
							outToClient.writeBytes(retMsg);
						}
						else{
							String retMsg = "Registered TOSEND [username]\n\n";
							DataOutputStream outToClient = new DataOutputStream(connSocket.getOutputStream());
							outToClient.writeBytes(retMsg);
							break;
						}
					}
					else{
						String retMsg = "ERROR 100 Malformed username\n\n";
						DataOutputStream outToClient = new DataOutputStream(connSocket.getOutputStream());
						outToClient.writeBytes(retMsg);
					}

					recMsg[] = inFromClient.readLine().split(" ");
				}
				sen.start();
			}

			else if(recMsg[1].equals("TOREC")){
				Thread rec = new ServerReceiver(connSocket);
				//it will be received correctly
				rec.start();
				table.add(rec);
				rec.close();
			}

		}
	}
}