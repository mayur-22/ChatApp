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
import java.util.Scanner;


class ClientSender extends Thread{

	private Socket soc;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;


	//if b is true then it is receiver;
	//else it is a sender
	public ClientSender(Socket s) throws Exception{
		this.soc = s;
		this.outToServer = new DataOutputStream(s.getOutputStream());
		this.inFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
	}

	@Override
	public void run(){
		String userStr;

		Scanner sc = new Scanner(System.in);
		while(true){

			try{

				userStr = sc.nextLine();

				if(userStr.equals("UNREGISTER")){

				}
				
				//new class will be made which will return username messege and boolean;
				ParserS pr = new ParserS(userStr);


				if(!pr.isValidInput()){
					System.out.println("Type Again...");
					continue;
				}

				String userToSend = pr.getUserName();
				String message = pr.getMessage();
				System.out.println(message);

				String toSend = "";
				toSend += "SEND [" + userToSend + "]\n";
				toSend += "Content-length: [" + message.length() + "]\n\n";
				toSend += "[" + message + "]";
				System.out.println("toSend "+toSend);
				outToServer.writeBytes(toSend);
                                String ack = inFromServer.readLine();
                                System.out.println("1" + ack);
				String receiveMsg[] = ack.split(" ");
				if(receiveMsg[0].equals("SENT"))
					System.out.println("Message Sent Successfully");
				else if (receiveMsg[1].equals("102"))
					System.out.println("Unable to Send");
				else if (receiveMsg[1].equals("103"))
					System.out.println("Header Incomplete");
			}
			catch(Exception e){e.printStackTrace();
                        return;}

		}

	}


}

class ParserS{
	private String recipientUserName;
	private String message;
	private boolean valid;

	
	//constructor class
	public ParserS(String inp){
		if(!(inp.charAt(0)=='@' && inp.charAt(1)=='[')){
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
