import java.util.*;
import java.io.*;
import java.net.*;

class Main{
	public static String lastMessage;

	public static void main(String[] args){
		try{
			ServerSocket s = new ServerSocket(8888);

			while(true){
				Socket incoming = s.accept();
				SockThread t = new SockThread(incoming);
				t.start();
			}
		}
		catch(Exception e){
			System.err.println(e);
			System.exit(1);
		}
	}
};

public class SockThread extends Thread{
	private BufferedReader in;
	private PrintWriter out;
	private PrintWriter oF = new PrintWriter(new FileWriter("chat.log"), true);

	private BufferedReader iF = new BufferedReader(new FileReader("chat.log"));

	public SockThread(Socket sock) throws Exception{
		in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		out = new PrintWriter(sock.getOutputStream(), true);
	}

	@Override
	public void run(){
		try{
			UpdateThread t = new UpdateThread(out, 100);
			t.start();

			String line;

			out.println("ДОБРО ПОЖАЛОВАТЬ В ЧАТ!");

			while((line = in.readLine()) != null){
				Main.lastMessage = line;
				sleep(100);
			}
		}
		catch(Exception e){
			System.err.println(e);
		}
	}
}

public class UpdateThread extends Thread{ // доделать уже конструктор
	private PrintWriter out;
	private int d;
	private String lastMsg;

	public UpdateThread(PrintWriter o, int delay){
		out = o;
		d = delay;
	}

	@Override
	public void run(){
		try{
			String line;
			while(!interrupted()){
				sleep(d / 2);
				if(Main.lastMessage != lastMsg){out.println(Main.lastMessage); lastMsg = Main.lastMessage;}
				sleep(d / 2);
			}
		}
		catch(Exception e){
			System.err.println(e);
			interrupt();
		}
	}
};
