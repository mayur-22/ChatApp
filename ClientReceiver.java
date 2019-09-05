class ClientReceiver extends Thread throws Exception{

	private Socket soc;
	private DataOutputStream outToServer;
	private DataInputStream inFromServer;


	//if b is true then it is receiver;
	//else it is a sender
	public ClientSender(Socket s){
		this.soc = s;
		this.outToServer = new DataOutputStream(s.getOutputStream());
		this.inFromServer = new DataInputStream(s.getInputStream());
	}

	@Override
	public void run(){
		while(true){
			int length = inFromServer.available()
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
				System.out.println("New Message Received.\n Sender:"+this.getSenderName()
					+"\nMessage: "+this.getMessage());
			}

		}

		//dont know about this
		soc.close();
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
		String temp[] = msgs[0].split(" [",2);
		if(temp[0].equals("FORWARD")){
			this.senderUserName = temp[1].substring(0,temp[1].length()-1);
		}

		int msgLen = 0;
		String tmp[] = msgs[1].split(" [",2)
		if(tmp[0].equals("Content-length:")){
			msgLen = Integer.parseInt(tmp[1].substring(0,tmp[1].length()-1));
		}

		msgs[2] = msgs[2].substring(1,msgs[2].length()-1)
		if(msgs[2].length==msgLen){
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