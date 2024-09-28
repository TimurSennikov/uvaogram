package uvaogram;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import static javax.swing.JOptionPane.showMessageDialog;

import java.io.*;
import java.net.*;

public class Dox{
    public static void main(String[] args){
        DoxFrame f = new DoxFrame();
        f.show();
    }
};

class DoxFrame extends JFrame{
	private boolean running;

	private String name;
	private JTextField field;
	private JTextField host;
	private JButton button;
	private JTextArea area;
	private SockThread currentWork;
	private String logName = "log.txt";
	private boolean enable_logging = true;

	private boolean area_editable = false;

	private boolean ciphering = false;
	private int cipher = 0x00;

	private String defHost = "46.98.9.75:8888";
	private String hostname = "";

	public final String programName;

	public DoxFrame(){
	programName = "Юваограм";

	Container contentPane = getContentPane();
	setTitle(programName);
	setSize(750, 600);

	running = false;

	ciphering = true;
	cipher = new Random().nextInt(999999999);
	setTitle(programName + " (" + cipher + ")");

	field = new JTextField();
	field.setSize(350, 50);
	field.setToolTipText("ENTER для отправки в чат.");

	button = new JButton("Отправить");
	button.setSize(300, 50);

	area = new JTextArea(10, 15);
	area.setEditable(area_editable);

	JScrollPane pane = new JScrollPane(area);

	SigmaBar bar = new SigmaBar(this);

	currentWork = new SockThread();
	currentWork.start();

	contentPane.add(bar, BorderLayout.NORTH);
	contentPane.add(pane, BorderLayout.CENTER);
	//contentPane.add(connect, BorderLayout.CENTER);
	contentPane.add(field, BorderLayout.SOUTH);

	pack();
	}

	public void askExit(){
		showMessageDialog(null, "Вы выходите из приложения...");
		System.exit(0);
	}

	private class SockThread extends Thread{
		private Socket connection;
		private PrintWriter fw;
		private BufferedReader in;
		private PrintWriter out;
		private PrintWriter fW;

		public boolean ready = false;

		public SockThread(){
			field.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
				String text = field.getText();

				if(out != null){
					if(!ciphering){
						out.println(text);
					}
					else{
						String outStr = "";

						for(int i = 0; i < text.length(); i++){
							outStr += String.valueOf((char)(text.charAt(i) ^ cipher));
						}
						outStr += String.valueOf((char)('%' ^ cipher));

						out.println(outStr);
					}
				}

				field.setText(null);
				}
			});
		}

		public void run(){
			String line;
			String lastLogName = logName;
			try{
				String addr = hostname == defHost ? defHost : hostname.length() <= 1 ? defHost : hostname;
				String[] splittedAddr = addr.split(":");
				String ip = splittedAddr[0];
				int port = Integer.parseInt(splittedAddr[1]);

				connection = new Socket(ip, port);
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				out = new PrintWriter(connection.getOutputStream(), true);
				fW = new PrintWriter(new FileWriter(logName), true);

				ready = true;

				sleep(500);

				while(!interrupted()){
					if(lastLogName != logName){fW = new PrintWriter(new FileWriter(logName), true); lastLogName = logName;}

					line = in.readLine();

					if(line == null){interrupt();}

					if(!ciphering){
						if(line.startsWith("?FILE?")){area.append("-> ФАЙЛ <-");}
						else{area.append(line + '\n'); if(enable_logging){fW.println(line);}}

					}
					else{
						String ostr = "";
						String[] linespl = line.split(": ");

						line = linespl.length <= 1 ? line : linespl[1];

						for(int i = 0; i < line.length(); i++){
							ostr += String.valueOf((char)(line.charAt(i) ^ cipher));
						}
						boolean isOurBusiness = ostr.charAt(ostr.length() - 1 <= 0 ? 0 : ostr.length() - 1) == '%';

						ostr = ostr.substring(0, ostr.length() - 1);

						if(isOurBusiness){area.append(ostr + " // ключ " + cipher + "\n"); if(enable_logging){fW.println(linespl[0] + ": " + ostr + " // ключ " + cipher + "\n");}}
					}
					sleep(100);
				}
			}
			catch(InterruptedException eint){
				stopChat();
			}
			catch(UnknownHostException ehost){
				showMessageDialog(null, "Не удалось подключиться к " + hostname + ", проверьте написание адреса и интернет-подключение.");
				stop();
			}
			catch(IOException eio){
				showMessageDialog(null, "Не удалось настроить сокет, проверьте написание адреса и подключение к интернету.");
				stop();
			}
		}

	public void stopChat(){
		try{
			if(connection != null){connection.close();}
			if(in != null){in.close();}
			if(out != null){out.close();}

			stop();
		}
		catch(Exception e){System.err.println(e);}
	}
	};

	public class SigmaBar extends JMenuBar{
		public SigmaBar(DoxFrame frame){
			super();

			JMenu chat = new JMenu("Чат");
			JMenuItem close = new JMenuItem("Отключиться");
			close.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					askExit();
				}
			});
			close.setToolTipText("Закрывает приложение.");

			JMenuItem reconnect = new JMenuItem("Переподключиться");
			reconnect.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					currentWork.stopChat();
					try{currentWork.join();}catch(Exception e){System.exit(1);}
					currentWork = new SockThread();
					currentWork.start();
				}
			});
			reconnect.setToolTipText("Переподключает клиент (вас) к удаленному серверу по выбранному или стандартному IP.");

			JMenuItem setIP = new JMenuItem("Установить адрес");
			setIP.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					String addr = JOptionPane.showInputDialog("Введите адрес в формате ip:port");
					String[] splittedAddr = (addr == null) ? defHost.split(":") : addr.split(":");

					if(splittedAddr.length <= 1){showMessageDialog(null, "Неверный формат ввода.");}
					else{
						hostname = addr;
						currentWork.stopChat();
						try{currentWork.join();}catch(Exception e){System.err.println(e); System.exit(1);}
						currentWork = new SockThread();
						currentWork.start();
					}
				}
			});
			setIP.setToolTipText("Позволяет выбрать IP сервера.");

			JMenuItem reset =  new JMenuItem("Очистить");
			reset.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					area.setText(null);
				}
			});
			reset.setToolTipText("Очищает поле чата.");

			JCheckBoxMenuItem editable = new JCheckBoxMenuItem("Константность");
			editable.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					area_editable = !area_editable;
					area.setEditable(area_editable);
					editable.setSelected(!area_editable);
				}
			});
			editable.setToolTipText("Включает/выключает возможность редактировать поле чата.");
			editable.setSelected(!area_editable);

			JCheckBoxMenuItem weCipher = new JCheckBoxMenuItem("Шифрование");
			weCipher.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					ciphering = !ciphering;

					if(ciphering){
						String key = JOptionPane.showInputDialog("Введите ключ шифрования.");
						cipher = Integer.parseInt(key);
						weCipher.setText("Шифрование (" + key + ")");
						frame.setTitle(frame.programName + "(" + key + ")");
					}
					else{
						weCipher.setText("Шифрование");
						frame.setTitle(frame.programName);
					}
				}
			});
			weCipher.setSelected(ciphering);
			weCipher.setToolTipText("Устанавливает шифрование для чата по принципу char ^ key.");

			JMenu log = new JMenu("Логирование");
			JCheckBoxMenuItem weLog = new JCheckBoxMenuItem("Лог сообщений");
			weLog.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					enable_logging = !enable_logging;
					weLog.setSelected(enable_logging);
				}
			});
			weLog.setSelected(enable_logging);
			weLog.setToolTipText("Определяет, будут ли сообщения записаны в файл (по умолчанию ./log.txt).");

			JMenuItem selectLog = new JMenuItem("Логировать в...");
			selectLog.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					JFileChooser chooser = new JFileChooser();
					int val = chooser.showOpenDialog(null);
					if(val == JFileChooser.APPROVE_OPTION){
						String name = chooser.getSelectedFile().getPath();
						System.out.println(name);
						logName = name;
					}
				}
			});

			JMenu credits = new JMenu("Пейджер Ромы");
			JMenuItem coderInfo = new JMenuItem("Кодер");
			coderInfo.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					showMessageDialog(null, "Автор в телеграмме: https://t.me/Govnovzo \n при обнаружении багов пишите сюда.");
				}
			});
			JMenuItem version = new JMenuItem("О приложении");
			version.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					showMessageDialog(null, "Клиент чата 'Пейджер Ромы' версии 0.1, написано by Timur_Sennikov (by это типа англицизм ведь я крутой)");
				}
			});
			JMenu outlook = new JMenu("Внешний вид");
			JMenuItem bgcolorpicker = new JMenuItem("Цвет фона");
			bgcolorpicker.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					Color c = JColorChooser.showDialog(null, "Выбор цвета фона", Color.WHITE);
					if(c != null){
						area.setBackground(c);
					}
				}
			});

			outlook.add(bgcolorpicker);

			log.add(weLog);
			log.add(selectLog);

			chat.add(close);
			chat.add(reset);
			chat.add(editable);
			chat.add(weCipher);
			chat.add(reconnect);
			chat.add(setIP);

			credits.add(version);
			credits.add(coderInfo);

			add(chat);
			add(log);
			add(credits);
			add(outlook);
		}
	};
};
