/*************************************************************/
/*************************************************************/
/*
	Function are completed. Have to implement about how it will close
	Ctrl+C not handeled
	If [ or ] occur in messafe or username then will result in bad output
	
	Sample Valid Input: @[mayur] [messsageadsfa]

*/


class ClientSender extends Thread throws Exception{

	private Socket soc;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;


	//if b is true then it is receiver;
	//else it is a sender
	public ClientSender(Socket s){
		this.soc = s;
		this.outToServer = new DataOutputStream(s.getOutputStream());
		this.inFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
	}

	@Override
	public void run(){
		String userStr;

		Scanner sc = new Scanner(System.in);
		while(true){

			userStr = sc.nextLine();
			
			//new class will be made which will return username messege and boolean;
			ParserS pr = new ParserS(userStr);


			if(!pr.isValidInput()){
				System.out.println("Type Again...");
				continue;
			}

			String userToSend = pr.getUserName();
			String message = pr.getMessage();

			String toSend = "";
			toSend += "SEND [" + userToSend + "]\n";
			toSend += "Content-length: [" + message.length() + "]\n\n";
			toSend += "[" + message + "]";

			outToServer.writeBytes(toSend);
			String receiveMsg[] = (inFromServer.readLine()).split(" ");
			if(receiveMsg[0].equals("SENT"))
				System.out.println("Message Sent Successfully");
			else if (receiveMsg[1].equals("102"))
				System.out.println("Unable to Send");
			else if (receiveMsg[1].equals("103"))
				System.out.println("Header Incomplete");
		}
		//closing 
		soc.close();

	}


}

class ParserS{
	private String recipientUserName;
	private String message;
	private boolean valid;

	
	//constructor class
	public ParserS(String inp){
		if(inp[0].equals('@') && inp[1].equals('[')){
			this.recipientUserName = "";
			this.message = "";
			this.valid = false;
			System.out.println("Ill Formed Message");
		}
		else{
			int i=2;
			this.recipientUserName = "";
			for(i=2;i<inp.length();i++){
				if(inp[i].equals(']'))
					break;
				recipientUserName += inp[i];
			}

			//no message body
			if(i>=inp.length()){
				this.recipientUserName = "";
				this.message = "";
				this.valid = false;
				System.out.println("No Message");
			}

			//assuming no extra special brakcets ]
			i += 2; //assuming space berween brackets
			this.message = "";
			while(i!=inp.length()){
				if(inp[i].equals(']'))
					break;
				message += inp[i];
				i++;
			}

			if(!inp[inp.length()-1].equals(']')){
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