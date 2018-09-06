package server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import service.MedysFTPOperations;

/**
 * Die Klasse MedysFTPConnectionClient ist ein FTP-Client für den MEDYS
 * FTP-Server &quot;ftp.medys.de&quot; </br> </br> Sie nutzt die
 * Funktionalit&auml;t der Klasse FTPClient des Apache Commons-Net 3.4
 * Frameworks. </br></br> Aus diesem Grund ist ein gewisses Ma&amp; an Vorwissen
 * über das FTP-Protokol </br> erforderlich </br></br>
 * 
 * <a href="https://tools.ietf.org/html/rfc959">FTP-Protokol RFC
 * 959</a></br></br>
 * 
 * @author Hayri Emrah Kayaman, MEDYS GmbH Wülfrath 2016
 */
public class MedysFTPConnectionClient 
{
	private boolean dateiAufServerVerfuegbar;
	private boolean downloadDauerErrechnet;
	private boolean downloadDateiHeruntergeladen;
	
	private boolean noop_command_gesetzt;
	
	// Variablen zur voraussichtliche Downloadzeitberechnung
	// während des Downloads
	//
	private long anfaenglichTransferierteAnzahlBytes = 0L;
	private int downloadProzentZaehler = 0;
	
	// Statusmeldungen, die bei Operationen auf dem Server
	// erfolgen können
	//
	private String serverStatus;

	// der Dateiname einer heruntergeladenen Datei
	//
	private String downloadDateiName;

	private String voraussichtlicheDownloadDauerAnzeige;
	
	private final String UPDATE_FEHLT = "udpate_fehlt";
	
	private final String LOGGED_IN = "logged_in";
	private final String NOT_LOGGED_IN = "not_logged_in";
	
	private final String VERBUNDEN = "verbunden";
	private final String NICHT_VERBUNDEN = "nicht_verbunden";
	
	private FTPClient client;

	private MedysFTPOperations medysFtpOps;
	
	/**
	 * Erzeugt eine neue Instanz von MedysFTPConnectionClient und verbindet diese
	 * mit einer {@link service.MedysFTPOperations}-Instanz
	 * 
	 * @param medysFtpOps eine Instanz der Klasse MedysFTPOperations
	 */
	public MedysFTPConnectionClient(MedysFTPOperations medysFtpOps)
	{	
		this.medysFtpOps = medysFtpOps;
		
		dateiAufServerVerfuegbar = false;
		
		setServerStatus(NICHT_VERBUNDEN);

		noop_command_gesetzt = false;
		
		downloadDauerErrechnet = false;
		
		client = new FTPClient();

		// default Timeout
		//
		client.setConnectTimeout(15000);
	}
	
	/**
	 * stellt mit den spezifischen Verbindungsparamtern eine Verbindung zum MEDYS
	 * FTP-SERVER her
	 * 
	 * </br></br>
	 * 
	 * @param hostname
	 *            FTP-server Host-URL-Name, wie &quot;ftp.servername.de&quot;
	 * @param port
	 *            FTP-Server Port
	 * @param username
	 *            authentifizierter Benutzername auf dem FTP-Server
	 * @param password
	 *            Password des authentifizierten Benutzers
	 */
	public void connect(
			String hostname, 
			String port, 
			String username,
			String password) 
	{
		try 
		{
			String serverStatus = gibServerStatus().toLowerCase();
			
			if ((serverStatus.compareTo(NICHT_VERBUNDEN) == 0)
				|| (serverStatus.compareTo(NOT_LOGGED_IN) == 0))
			{
				client.connect(hostname, Integer.parseInt(port));

				if (getLastServerStatus().startsWith("220")) 
				{

					// for DEBUG ONLY
					showServerStatus(VERBUNDEN + " mit dem Medys FTP-Server\n"
							+ "warte auf Benutzerauthentifizierung...");
					
					setServerStatus(VERBUNDEN);
				}
			}
		}
		catch (IOException connectionErr) 
		{
			// DEBUG ONLY
			// connectionErr.printStackTrace();
			showServerStatus("Verbindungsfehler zum FTP-Server " + hostname
					+ "\n\nFTP-Server ist nicht erreichbar !");
			
			setServerStatus(NICHT_VERBUNDEN);
		}
	}

	//TODO den Disconnect am Ende der Donwload-Zeit Berechnung verhindern und umgestalten
	
	/*
	 * Hilfsmetode für die errechneVoraussichtlicheDownloadDauer-Methode
	 * 
	 * Diese Methode (hier) berechnet die voraussichtlich Downloadzeit 
	 * 
	 * dazu müssen wir einen kleinen Anteil "chunk" (preDownloadAnzahlBytes)
	 * von der Donwload-Datei erhalten und berechnen mit dessen Hilfe und der gesamten Dateigröße
	 * die voraussichtliche Donwload-Zeit
	 * 
	 * Wichtig
	 * -------
	 * 
	 * Diese Methode bricht die Verbindung zum Server ab, damit ein neuer Donwload-Task
	 * möglich ist (dazu benötigt es einen Re-Connect zum FTP-Server)
	 * 
	 * 
	 * @param updateVerzeichnisse 
	 * 			die Update-Versionsverzeichnisse, aus denen die Update-Verion erhalten wird
	 * 
	 * @param updateVersion
	 * 			die Update-Version, die vom Server heruntergeladen werden soll
	 * 
	 * @param preDownloadAnzahlBytes
	 * 			diesen Parameter benutzen, um vorab eine voraussichtliche
	 * 			Download-Zeit der Update-Version zu erhalten
	 */
	private boolean errechneDownloadZeit(final String quellverzeichnis, final String datei, final byte[] preDownloadAnzahlBytes)
	{	
		// Megabyte Downloadangaben
		//
		int bisMB = 0;

		// textueller Downloadzeit
		//
		String transferDauer = "";

		String qv = "";

		//
		// falls quellverzeichnis abschliessenden Verzeichnistrenner hat
		//
		if (quellverzeichnis.endsWith(File.separator)) 
		{
			// entfernen, sonst kann der FTPClient nicht in das
			// Quellverzeichnis wechseln: changeWorkingDirectory() schlägt fehl
			// -> Fehler 501 !!
			//
			qv = quellverzeichnis.substring(0,
					quellverzeichnis.lastIndexOf(File.separator));
		}
		else
		{
			qv = quellverzeichnis;
		}

		setEnterLocalPassiveMode(true);
		
		if (noop_command_gesetzt) 
		{
			try 
			{	
				// falls wir im ROOT-Directory sind müssen wir in das jeweilige
				// Update-Verzeichnis wechseln
				// -> sonst ( else-teil entfällt ), sind wir bereits im notwendigen Update-Verzeichnis
				//
				if(client.printWorkingDirectory().toLowerCase().compareTo("/") == 0)
				{
					client.changeWorkingDirectory(qv);
				}

				FTPFile[] directoryInhalt = client.listFiles();
				
				FTPFile downloadDatei = null;
				
				for(FTPFile file : directoryInhalt)
				{
					if(file.getName().toLowerCase().compareTo(datei) == 0)
					{
						downloadDatei = file;
						
						break;
					}
				}
				
				if(downloadDatei != null)
				{
					long downloadDateiGroesse = downloadDatei.getSize();

					bisMB = Math.round((downloadDateiGroesse / 1024) / 1000);

					BufferedInputStream fileStream = 
							new BufferedInputStream(client.retrieveFileStream(datei));

					byte[] bytesTransfer = new byte[4096];

					int bisherigeTransferGroesse = 0;

					// miss die Zeit die verbraucht wird um "bytesTransfer"-Bytes zu
					// erhalten
					//
					bisherigeTransferGroesse = fileStream.read(bytesTransfer);

					setzeAnfaenglicheTransferChunkSize(bisherigeTransferGroesse);

					transferDauer = 
							gibVoraussichtlicheDateiDownloadzeit(
									downloadDateiGroesse,
									bisherigeTransferGroesse);

					System.out.println(getLastServerStatus());
					
					if (transferDauer.length() > 0) 
					{
						voraussichtlicheDownloadDauerAnzeige = 
								"Voraussichtliche Dauer " + transferDauer
									+ "\nDateigröße " + bisMB + " MB";

						downloadDauerErrechnet = true;
					}
					
					fileStream.close();
				}
				else
				{
					setServerStatus(UPDATE_FEHLT);
				}
			}
			catch (Exception e) 
			{
				// for DEBGU ONLY
				e.printStackTrace();

				setServerStatus(NICHT_VERBUNDEN);
			}

		}
		return downloadDauerErrechnet;
	}
	
	/*
	 * Download-Task für eine Medys-Version von den befindlichen Update-Verzeichnissen
	 * auf dem FTP-Server
	 * 
	 * <br />
	 * 
	 * @param updateVerzeichnisse die Update-Versionsverzeichnisse, aus denen die Update-Verion erhalten wird
	 * @param updateVersion die Update-Version, die vom Server heruntergeladen werden soll
	 */
	private final Task<Double> runDownloadMedysUpdate(final String quellverzeichnis, final String datei, final String zielverzeichnis)
	{	
		final Task<Double> task = new Task<Double>() 
		{
			@Override
			protected Double call() throws Exception 
			{	
				// von-bis Megabyte Downloadangaben
				//
				int vonMB = 0;
				int bisMB = 0;
				
				// prozentualle Anzeige der Anzahl heruntergeladener Dateisegmente
				//
				double downloadProzent = 0.0;
				
				String qv = "";
				String zv = zielverzeichnis;

				//
				// falls quellverzeichnis abschliessenden Verzeichnistrenner hat
				//
				if (quellverzeichnis.endsWith(File.separator)) 
				{
					// entfernen, sonst kann der FTPClient nicht in das
					// Quellverzeichnis wechseln: changeWorkingDirectory() schlägt fehl -> Fehler 501 !!
					//
					qv = quellverzeichnis.substring(0, quellverzeichnis.lastIndexOf(File.separator));
				}
				else
				{
					qv = quellverzeichnis;
				}

				if (!zielverzeichnis.endsWith(File.separator)) 
				{
					zv = zielverzeichnis + File.separator;
				}

				// DEBUG ONLY
				// System.out.println(MedysVersionPruefer.zeigMedysUpdates());

				// setzte Timeout-Anfrage für Dateitransfer-Latenzzeiten auf
				// FTP-Server
				// auf 15 sekunden für NOOP-FTP-Command
				//
				setzeDateiTransferZeitTimeout(15000);

				// modus von "server-to-client" auf "client-to-server"
				// umstellen
				// um Datentransfer richtig zu initialisieren und um die
				// ggfs. Firewall nicht zu belästigen
				//
				setEnterLocalPassiveMode(true);

				try 
				{
					setFileTransferMode(FTP.BINARY_FILE_TYPE);
				}
				catch (IOException e) 
				{
					// for DEBUG ONLY
					e.printStackTrace();
					
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() 
						{
							medysFtpOps.sendeFtpServerVerbindungsAbbruchNachricht();
						}
					});
				}
				
				// download-prozedur mit datentransferermittelung
				// und zeitmessung
				//
				if (noop_command_gesetzt) 
				{
					try 
					{
						client.changeWorkingDirectory(qv);

						FTPFile[] directoryInhalt = client.listFiles();
						
						FTPFile downloadDatei = null;
						
						for(FTPFile file : directoryInhalt)
						{
							if(file.getName().toLowerCase().compareTo(datei) == 0)
							{
								downloadDatei = file;
								
								break;
							}
						}

						long downloadDateiGroesse = downloadDatei.getSize();
						
						bisMB = Math.round((downloadDateiGroesse / 1024) / 1000);
						
						BufferedInputStream fileStream = new BufferedInputStream(client.retrieveFileStream(datei));

						FileOutputStream fileOutputStream = new FileOutputStream(file(zv + datei));

						byte[] bytesTransfer = new byte[4096];
						
						int bisherigeTransferGroesse = 0;

						long totalBytesTransferred = 0L;

						bisherigeTransferGroesse = fileStream.read(bytesTransfer);
								
						String resultat = 
								vonMB + " MB "
								+ " von " + bisMB + " MB "
								+ ", Fertig gestellt " + downloadProzentZaehler
								+ "%"
								+ "\n\nverbleibende Zeit "
								+ gibVoraussichtlicheDateiDownloadzeit(downloadDateiGroesse, bisherigeTransferGroesse);
						
						while (bisherigeTransferGroesse != -1) 
						{		
							fileOutputStream.write(bytesTransfer, 0, bisherigeTransferGroesse);

							totalBytesTransferred += bisherigeTransferGroesse;

							vonMB = Math.round((totalBytesTransferred / 1024) / 1000);
							
							downloadProzent = totalBytesTransferred * 100 / downloadDateiGroesse;

							downloadProzent = downloadProzent / 1.0;
							
							long downloadShrinkSize = downloadDateiGroesse;
							
							
							if(bisherigeTransferGroesse >= gibAnfaenglicheTransferChunkSize())
							{
								downloadShrinkSize = downloadDateiGroesse - totalBytesTransferred;
								
								resultat = 
										vonMB + " MB "
										+ " von " + bisMB + " MB "
										+ ", Fertig gestellt " + downloadProzent
										+ "%"
										+ "\n\nverbleibende Zeit "
										+ gibVoraussichtlicheDateiDownloadzeit(downloadShrinkSize, bisherigeTransferGroesse);
							}
							
							// "resultat" in der entsprechenden Text-Komponente der MedysFTPClientGUI
							// aktualisieren
							//
							updateMessage(resultat);
							
							// den verknüpften ProgressBar-Prozessstatus aktualisieren
							// bis 100% erreicht
							//
							updateProgress(downloadProzent, 100);
							
							bisherigeTransferGroesse = fileStream.read(bytesTransfer);
						}
						
						// hier sind wir außerhalb des JavaFX-Application Threads..daher
						//
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() 
							{
								// da wir nicht immer garantieren können wie die Public-methoden aufgerufen werden
								//
								
//								medysFtpOps.gibClientUI().gibProgressComponentsBox().getChildren().remove(medysFtpOps.gibClientUI().gibProgressBar());
								
								medysFtpOps.leiteUnzipRoutineEin();
								
							}
						});
						
						fileOutputStream.close();
						fileStream.close();
						
						logout();
					}
					catch (Exception e) 
					{
						// for DEBGU ONLY
						e.printStackTrace();
						
						setServerStatus(UPDATE_FEHLT);
						
						Platform.runLater(new Runnable() {
							
							@Override
							public void run() 
							{
								medysFtpOps.sendeFtpServerVerbindungsAbbruchNachricht();
							}
						});
					}
					
					
				}
				return downloadProzent;
			}
		};
		
		return task;
	}
	
	/**
	 * Bricht die Verbidnung zum FTP-Server ab
	 * 
	 * @throws IOException - falls es zu einem Verbindungsfehler kommt
	 */
	public void disconnect() throws IOException
	{
		client.disconnect();
		setServerStatus(NICHT_VERBUNDEN);
	}
	
	/**
	 * Methode zum Herunterladen einer Datei aus einem Server-Verzeichnis
	 * mit der Angabe in welches Zielverzeichnis gespeichert werden soll
	 * 
	 * <br /><br />
	 * Diese Methode ist an eine javafx.concurrency.
	 * @param quellverzeichnis Server-Verzeichnis, welches die Datei zum herunterladen enth&auml;lt
	 * @param datei die Datei (Dateiname), die heruntergeladen werden soll
	 * @param zielVerzeichnis Ablageort der Datei
	 */
	public void downloadFile(
			String quellverzeichnis, 
			String datei,
			String zielverzeichnis) 
	{	
		long downloadDateiGroesse = 0L;
		
		try 
		{
			String qv = "";
			String zv = zielverzeichnis;

			//
			// falls quellverzeichnis abschliessenden Verzeichnistrenner hat
			//
			if (quellverzeichnis.endsWith(File.separator)) 
			{
				// entfernen, sonst kann der FTPClient nicht in das
				// Quellverzeichnis wechseln: changeWorkingDirectory() schlägt
				// fehl -> Fehler 501 !!
				//
				qv = quellverzeichnis
						.substring(0,quellverzeichnis.length() - 1);
			}
			else
			{
				qv = quellverzeichnis;
			}

			if (!zielverzeichnis.endsWith(File.separator))
			{
				zv = zielverzeichnis + File.separator;
			}

			// DEBUG ONLY
			// System.out.println(MedysVersionPruefer.zeigMedysUpdates());

			// setzte Timeout-Anfrage für Dateitransfer-Latenzzeiten im
			// FTP-Server auf 15 sekunden für NOOP-FTP-Command
			//
			setzeDateiTransferZeitTimeout(15000);

			// modus von "server-to-client" auf "client-to-server"
			// umstellen
			// um Datentransfer richtig zu initialisieren und um die
			// ggfs. Firewall nicht zu belästigen
			//
			setEnterLocalPassiveMode(true);

			// Datentransfer-Modus auf FTP-Server festlegen (was wird heruntergeladen ?)
			//
			client.type(FTP.BINARY_FILE_TYPE);

			if (noop_command_gesetzt)
			{
				
				FTPFile downloadDatei = null;
				
				String pwd = client.printWorkingDirectory();
				
				pwd = pwd.startsWith("/") ? pwd.substring(1, pwd.length()) : pwd;
				
				if(!pwd.equals(qv))
				{
					client.changeWorkingDirectory(qv);
				}

				FTPFile[] dirContent = client.listFiles();
				
				for(FTPFile dirFile : dirContent)
				{
					if(dirFile.isFile())
					{
						if(dirFile.getName().toLowerCase().equals(datei.toLowerCase()))
						{
							dateiAufServerVerfuegbar = true;
							
							// speichere gefundene Datei, temporär und hole Dateigröße
							//
							downloadDatei = dirFile;
							
							downloadDateiGroesse = downloadDatei.getSize();
							
							break;
						}
					}
				}
				
				if(isDateiAufServerVerfuegbar() && (downloadDateiGroesse > 0))
				{
					BufferedInputStream fileStream = 
							new BufferedInputStream(client.retrieveFileStream(datei));

					FileOutputStream fileOutputStream = 
							new FileOutputStream(file(zv + datei));

					byte[] bytesTransfer = new byte[4096];

					int bisherigeTransferGroesse = 0;

					long totalBytesTransferred = 0L;

					String resultat = "";

					// leite Download der gesamten Daeti ein

					long zeit = -System.currentTimeMillis();

					while ((bisherigeTransferGroesse = 
							fileStream.read(bytesTransfer)) != -1)
					{
						fileOutputStream
						.write(bytesTransfer, 0, bisherigeTransferGroesse);

						totalBytesTransferred += bisherigeTransferGroesse;
					}

					// ende : Zeitmessung für den restlichen Download

					zeit += System.currentTimeMillis();

					// für die aktuelle Anwendung wird die Ausgabe nur zum DEBUGGEN
					// genutzt, kann aber in einer anderen Anwendung natürlich zur Anzeige
					// in der entsprechenden GUI angezeigt werden
					//
					// siehe hierzu: runDownloadMedysUpdate(final String, final String, final String)
					//
					resultat = 
							"Datei " + datei + "heruntergeladen in: "
							+ (zeit / 1000) + " sec., Größe "
							+ totalBytesTransferred;

					System.out.println(resultat);

					File downloadedFile = new File(zielverzeichnis, datei);
					
					// kontrolle, ob die Datei auch vollständig heruntergeladen wurde 
					//
					if(downloadedFile.exists())
					{
						if(downloadDateiGroesse == downloadedFile.length())
						{
							downloadDateiHeruntergeladen = true;
						}
						else
						{
							downloadDateiHeruntergeladen = false;
							
							System.out.println(datei + " konnte in MedysFTOConnectionClient.downloadFile(String,String,String)"
									+ " nicht vollständig heruntergeladen werden !!");
						}
						
						System.out.println(datei + " HERUNTERGELADEN in MedysFTOConnectionClient.downloadFile(String,String,String) !!");
					}
					else
					{
						// for DEBUG ONLY
						System.out.println(datei + " NICHT HERUNTERGELADEN in MedysFTOConnectionClient.downloadFile(String,String,String) !!");
					}
					
					fileOutputStream.close();
					fileStream.close();
					
					client.completePendingCommand();
					setEnterLocalPassiveMode(false); // wechsele vom server zum client-mode
					
				}
				else
				{
					downloadDateiHeruntergeladen = false;
					
					// for DEBUG ONLY
					System.out.println(datei + " NICHT HERUNTERGELADEN in MedysFTOConnectionClient.downloadFile(String,String,String) !!");
				}
			}
			else
			{
				downloadDateiHeruntergeladen = false;
				
				// for DEBUG ONLY
				System.out.println(datei + " NICHT HERUNTERGELADEN in MedysFTOConnectionClient.downloadFile(String,String,String) !!");
			}
		}
		catch (Exception e)
		{
			downloadDateiHeruntergeladen = false;
			
			// for DEBUG ONLY
			e.printStackTrace();
			
			// für die MedysUpdate-Anwendung wird dies nicht benötigt, für eine
			// generelle FTP-Nutzung könnte dies von Nutzen sein
			//
			// daher: stehen lassen !
			//
			
//			Platform.runLater(new Runnable()
//			{
//				@Override
//				public void run()
//				{
//					medysFtpOps.sendeFtpServerVerbindungsAbbruchNachricht();
//				}
//			});
		}
	}
	
	/**
	 * Methode zum Herunterladen der Update-Datei aus einem Server-Verzeichnis
	 * mit der Angabe in welches Zielverzeichnis gespeichert werden soll
	 * 
	 * <br /><br />
	 * Diese Methode ist an eine javafx.concurrency.
	 * @param quellverzeichnis
	 * 			Server-Verzeichnis, welches die Datei zum herunterladen enth&auml;lt
	 * @param datei
	 * 			die Update-Datei (Dateiname), die heruntergeladen werden soll
	 * @param zielVerzeichnis
	 * 			Ablageort der Datei
	 */
	public void downloadUpdate(
			String quellverzeichnis, 
			String datei,
			String zielverzeichnis) 
	{	
		Task<Double> downloadTask = runDownloadMedysUpdate(quellverzeichnis, datei, zielverzeichnis);
		
		medysFtpOps.loescheVorherigenUpdateVersuch(zielverzeichnis, datei);
		
		medysFtpOps.gibProgressBar().progressProperty().unbind();
		medysFtpOps.gibProgressBar().progressProperty().bind(downloadTask.progressProperty());
		
		// verbinde Text-Komponente in der MedysFTPClientUI-Klasse zur Anzeige
		// des Prozessverlaufs mit der updateText()-Methode dieser TASK-Methode
		//
		medysFtpOps.gibClientUI().gibProgressMessageField().textProperty().unbind();
		medysFtpOps.gibClientUI().gibProgressMessageField().textProperty().bind(downloadTask.messageProperty());
		
		medysFtpOps.gibClientUI().setUpdateMessageFieldText("Update \"" + datei + "\" wird heruntergeladen...");
		
		medysFtpOps.gibClientUI().gibButtonBox().getChildren().remove(medysFtpOps.gibClientUI().gibWeiterButton());
		
		medysFtpOps.gibClientUI().gibUpdateMessageLayout().getChildren().remove(medysFtpOps.gibClientUI().gibUpdateDownloadDauerMessageField());
		
		// ändere Layout-Position - PADDING
		//
		medysFtpOps.gibClientUI().gibUpdateMessageLayout().setId("updateMessageLayout_Finished_Style"); 	// StyleSheet-ID
		
		medysFtpOps.gibClientUI().gibProgressBar().setVisible(true);
		
		Thread t = new Thread(downloadTask);
		
		t.setDaemon(true);
		
		t.setName("UpdateDownload-Task");
		
		t.start();
	}

	/**
	 * Methode zur Berechnung der Download-Dauer (Zeit) für eine beliebige Datei
	 * vom <b><u>verbundenen</u>></b> FTP-Server</b>
	 * 
	 * <br /><br />
	 * 
	 * Diese Methode ist an eine javafx.concurrency.Task (runDownlaodMedysUpdate) gebunden
	 * 
	 * <br /><br />
	 * 
	 * <u><b>Voraussetzung</b></u><br /><br />
	 * 
	 * Ein erfolgreicher Login eines Benutzers in das FTP-Server-Verzeichnis, <br />
	 * wo sich die MedysUpdate-Versionen befinden, sollte vorher erfolgt sein.
	 * 
	 * 
	 * @param quellverzeichnis 
	 * 			Server-Verzeichnis, welches die Datei zum herunterladen enth&auml;lt
	 * @param datei
	 * 			die Datei (Dateiname), die heruntergeladen werden soll
	 * @param preDownloadAnzahlBytes
	 * 			diesen Parameter benutzen, um vorab eine voraussichtliche
	 * 			Download-Zeit der Update-Version zu erhalten
	 */
	public boolean errechneVoraussichtlicheDownloadDauer(
			String quellverzeichnis, 
			String datei,
			byte[] preDownloadAnzahlBytes) 
	{	
		return isLoggedIn() ? errechneDownloadZeit(quellverzeichnis, datei, preDownloadAnzahlBytes) : false;
	}

	/**
	 * liefert den Status, ob die Updateanleitung zum download verf&uuml;gbar ist
	 * 
	 * <br /><br />
	 * 
	 * Man sollte vorher die Methode {@link #downloadFile(String,String,String)} in einer
	 * Programmlogik aufrufen, um den Datei-Download zu erfragen.
	 * 
	 * @return TRUE wenn Datei zum Download bereit steht, sonst FALSE
	 */
	public boolean isDateiAufServerVerfuegbar()
	{
		return dateiAufServerVerfuegbar;
	}
	
	/**
	 * pr&uuml;ft, ob eine angeforderte Datei vom FTP-Server heruntergeladen wurde
	 * 
	 * @param downloadDateiName
	 * 				der Name der Datei, welche heruntergeladen werden sollte
	 * 
	 * @return TRUE wenn die jeweilige Datei heruntergeladen wurde, sonst FALSE
	 */
	public boolean isDownloadDateiHeruntergeladen()
	{
		return downloadDateiHeruntergeladen;
	}
	
	/**
	 * liefert den aktuellen Server-Status
	 * 
	 * 
	 */
	public String gibServerStatus() 
	{
		return serverStatus;
	}

	/**
	 * Liefert die Anzeige der voraussichtlichen Download-Dauer
	 * eines Datei-Downloads
	 * 
	 * <br /><br />
	 * 
	 * Die Download-Dauer sollte vorher durch die Methode <br/>
	 * <i>{@link #errechneVoraussichtlicheDownloadDauer(String,String,byte[])}</i>
	 * ermittlet werden
	 * 
	 * <br />
	 * 
	 * @return die voraussichtliche Download-Dauer im Format <pre><Stunde> <Minute> <Sekunde></pre>
	 * 		   oder NULL, wenn keine Dauer (Zeit) ermittelt werden konnte
	 */
	public String gibVoraussichtlicheDownloadDauerAnzeige()
	{
		return voraussichtlicheDownloadDauerAnzeige;	
	}
	
	/**
	 * liefert alle Verzeichnisnamen aus dem aktuellen Verzeichnis, in dem sich
	 * ein angemeldeter Benutzer GERADE befindet </br></br> Hat ein Verzeichnis
	 * keine weiteren Verzeichnisse, so ist dies ein Root-Verzeichnis, und es
	 * wird dann nur dieser Name ausgeliefert. </br></br> Ansonsten, alle
	 * Unterverzeichnisnamen eines Verzeichnisses, wenn vorhanden </br></br>
	 * 
	 * @return siehe Beschreibung, oben
	 */
	public String[] gibVerzeichnisnamen() 
	{
		
		ArrayList<String> verzeichnisliste = new ArrayList<String>();

		String[] struktur = null;

		if (client.isConnected()) 
		{
			FTPFile[] dirs = null;

			try 
			{
				dirs = client.listDirectories();
				
//				dirs = client.mlistDir();

				for (FTPFile dir : dirs) {
					if (dir.isDirectory()) {
						verzeichnisliste.add(dir.getName());
					}
				}
			}
			catch (IOException ioExec) 
			{
				// DEBUG ONLY
				
				showServerStatus("Es sind kein Update-Verzeichnisse vorhanden !");

				ioExec.printStackTrace();
			}
		}

		// Einträge vorhanden
		//
		if (verzeichnisliste.size() > 0) {
			// übertrage zur Rückgabe
			//
			int pos = 0;

			struktur = new String[verzeichnisliste.size()];

			for (String verzeichnisname : verzeichnisliste) {
				struktur[pos++] = verzeichnisname;
			}
		}

		return struktur;
	}


	/**
	 * pr&uuml;ft, ob eine Verbindung zum FTP-Server besteht und </br> sich ein
	 * Benutzer am Server erfolgreich verbunden hat </br>
	 * 
	 * @return
	 */
	public boolean isLoggedIn() 
	{
		// for DEBUG ONLY
		//
//		System.out.println(getLastServerStatus());
		
		return getLastServerStatus() != null
				?
						
				 (getLastServerStatus().startsWith("230")
				  || getLastServerStatus().startsWith("200")
				  || getLastServerStatus().startsWith("226"))
				  
				: false;
	}

	/*
	 * Login-Prozedur
	 * 
	 * @param hostname FTP-server Host-URL-Name, wie
	 * &quot;ftp.servername.de&quot;
	 * 
	 * @param port FTP-Server Port
	 * 
	 * @param username authentifizierter Benutzername auf dem FTP-Server
	 * 
	 * @param password Password des authentifizierten Benutzers
	 */
	public void login(String username, String password) 
	{
		String actualStatus = "";

		try 
		{
			if (gibServerStatus().toLowerCase().compareTo(VERBUNDEN) == 0) 
			{
				client.login(username, password);

				// FTPClient-Klasse hat keine Abfrage ob logged-in,
				// dies müssen wir abhandlen
				//
				actualStatus = getLastServerStatus();

				if (actualStatus.startsWith("230")) 
				{
					// for DEBUG ONLY
					showServerStatus("Benutzer " + username
							+ " erfolgreich authentifiziert und "
							+ "auf dem Server eingeloggt");
					
					setServerStatus(LOGGED_IN);
					
					client.setFileTransferMode(FTP.BINARY_FILE_TYPE);

					client.setFileType(FTP.BINARY_FILE_TYPE);
				}
				else
				{
					setServerStatus(NOT_LOGGED_IN);
				}
			}
		}
		catch (IOException connectionErr) 
		{
			// DEBUG ONLY
			// connectionErr.printStackTrace();

			showServerStatus("Benutzer "
					+ username
					+ " konnte sich nicht am Server authentifizieren.\n\n"
					+ "Überprüfen Sie bitte, ob der Benutzer auf dem Server registriert ist !");
			
			setServerStatus(NOT_LOGGED_IN);	
		}
	}

	/**
	 * Logout vom Server und schliessen der Verbindung
	 */
	public void logout() 
	{
		if (client.isConnected()) 
		{
			try 
			{
				client.disconnect();
				
				// DEBUG ONLY
				showServerStatus("Logout erfolgreich");
				
				setServerStatus(NOT_LOGGED_IN);
			}
			catch (IOException e) 
			{
				// DEBUG ONLY
				showServerStatus("Fehler beim Logout");
				
				setServerStatus(LOGGED_IN);
			}
		}
		else 
		{
			// for DEBUG ONLY
			showServerStatus("Es besteht keine Verbindung zum FTP-Server"
					+ "\nletzter bekannter Server-Status " + getLastServerStatus());
			
			setServerStatus(NICHT_VERBUNDEN);
			
			// server-status muss nicht erneut NICHT_VERBUNDEN gesetzt werden
			// da Konstruktor dies bereits setzt
		}
	}
	
	/*
	 * setzt die Anzahl an transferierten Bytes, die beim ersten Erfragen
	 * des Download-Vorgangs einer Datei vom FTP-Server übertragen wird
	 */
	private void setzeAnfaenglicheTransferChunkSize(long chunkSize)
	{
		anfaenglichTransferierteAnzahlBytes = chunkSize;
	}
	
	/**
	 * setzt die NOOP-Dateitransfer Timeout-Request Zeit für den FTP-Server
	 * 
	 * @param timeout
	 *            die Dauer der NOOP-Command Zeitdauer
	 */
	public void setzeDateiTransferZeitTimeout(int timeout) 
	{
		if (client != null) 
		{
			client.setControlKeepAliveTimeout(timeout);
			noop_command_gesetzt = true;
		}
	}
	
	/**
	 * versetzt den Verbindungsdialog zwischen Client-Server-Interaction
	 * auf &quot;passiven&quot; oder &quot;aktiven&quot; Modus <br /><br />
	 * 
	 * Wird <b>TRUE</b> &uuml;bergeben, so befindet sich die Interaction auf der
	 * <i>Server-Seite</i> (z.Bspl. um Daten vom Server anzufordern, f&uuml;r einen Upload, etc.)
	 * 
	 * <br /><br />
	 * 
	 * Wird <b>FALSE</b> &uuml;bergeben, so befindet sich die Interaktion auf der
	 * <i>Client-Seite</i> (z.Bspl. um Server-Verzeichnisse zu erfragen, einzusehen, etc.)
	 * 
	 * <br /><br />
	 * 
	 * n&auml;hre Info (Auszug aus der Apache Commons-Net 3.4 FTPCLient-API):<br /><br />
	 * 
	 * f&uuml;r <b>TRUE</b> = PASSIVE MODE <br /><br />
	 * 
	 * <code>
	 *  Use this method only for data transfers between the client and server. <br />
	 *  This method causes a PASV (or EPSV) command to be issued to the server <br />
	 *  before the opening of every data connection, telling the server to open a data port <br />
	 *  to which the client will connect to conduct data transfers. <br />
	 *  The FTPClient will stay in PASSIVE_LOCAL_DATA_CONNECTION_MODE
	 * </code>
	 * 
	 * <br /><br />
	 * 
	 * f&uuml;r <b>FALSE</b> = ACTIVE MODE <br /><br />
	 * 
	 * <code>
	 * No communication with the FTP server is conducted, but this causes all future data transfers <br />
	 * to require the FTP server to connect to the client's data port. Additionally, to accommodate <br />
	 * differences between socket implementations on different platforms, <br />
	 * this method causes the client to issue a PORT command before every data transfer.
	 * </code>
	 * 
	 * <br /><br />
	 * 
	 * @param enter <br /> TRUE = PASSIVE MODE <br /> FALSE = ACTIVE MODE
	 */
	public void setEnterLocalPassiveMode(boolean enter) 
	{
		if (enter) 
		{
			client.enterLocalPassiveMode();
		}
		else
		{
			client.enterLocalActiveMode();
		}
	}

	/**
	 * setzt den Datentransfer-Datentypen f&uuml;r einen Datentransfer <br />
	 * <br />
	 * 
	 * unterst&uuml;tze Datentypen sind: <br />
	 * <br />
	 * 
	 * <b>Klasse</b>: org.apache.commons.net.ftp.FTP <br />
	 * 
	 * <ul>
	 * <li>ASCII_FILE_TYPE - textuelle Objekte wie TXT,XML,CSV,etc.</li>
	 * <li>BINARYY_FILE_TYPE - nicht textuelle Objekte, wie ZIP,JPEG,AVI, etc.</li>
	 * <li>EBCDIC_FILE_TYPE - erweiterter Austauschcode für bin&auml;r kodierte
	 * Dezimalziffern</li>
	 * </ul>
	 * 
	 * <br />
	 * 
	 * Liegt kein g&uuml;tiger Datenttransfer-Typ (Wert) vor, so wird
	 * <i>default</i> BINARY_FILE_TYPE genutzt.
	 * 
	 * <br />
	 * <br />
	 * 
	 * @param type
	 *            der Datentransfer-Typ
	 * @throws IOException
	 */
	public void setFileTransferMode(int type) throws IOException 
	{
		if ((FTP.ASCII_FILE_TYPE == type) || (FTP.BINARY_FILE_TYPE == type)
				|| (FTP.EBCDIC_FILE_TYPE == type)) {
			client.setFileTransferMode(type);
		} else {
			// default
			//
			client.setFileTransferMode(FTP.BINARY_FILE_TYPE);
		}
	}

	/**
	 * druckt eine aktuelle Statusmeldung vom verbundenen FTP-Server aus </br>
	 * 
	 * <br />
	 * 
	 * Diese Methode wird f&uuml;r das DEBUGGING auf der Konsolenausgabe genutzt !!
	 * 
	 * <br />
	 * 
	 * @param status
	 *            ein Server-Status oder eine andere Nachricht
	 */
	public void showServerStatus(String status) 
	{
		System.out.println(status);
	}
	
	/*
	 * liefert die am Anfang eines Datei-Downloads ermittelte ChunkSize
	 * 
	 * (Anzahl übermittelter Bytes von einer Datei, die heruntergeladen werden soll)
	 * 
	 * @return ChunkSize = anfaenglicheTransferierteAnzahlBytes
	 */
	private long gibAnfaenglicheTransferChunkSize()
	{
		return anfaenglichTransferierteAnzahlBytes;
	}
	
	/**
	 * liefert den Dateinamen, welcher als letztes vom FTP-Server heruntergeladen wurde
	 * 
	 * @return Dateiname der letzten Download-Datei
	 */
	public String gibDownloadDateiName()
	{
		return downloadDateiName;
	}
	
	/*
	 * zeigt die textuelle Darstellung der voraussichtlichen Datentransferdauer
	 * f&uuml; einen Datei-Download vom FTP-server an.
	 * 
	 * <br /><br />
	 * 
	 * WICHTIG
	 * --------
	 * 
	 * Diese Methode ZÄHLT NICHT die Transferzeit während des Donwloads herunter !!
	 * 
	 * Nutze dafür "gibRestlicheDownloadzeit(double prozentHeruntergeladen)"
	 * 
	 * 
	 * @param downloadSize
	 * 			die Download-Gr&ouml;ße der Datei
	 * 
	 * @param chunkSize
	 * 			Anzahl der Bytes für einmaligen Schreibprozess -> FileStream.write(byte[])
	 * 
	 * @return die errechnete Download-Zeit eines Datei-Downloads in Stunden:Minuten:Sekunden
	 */
	private String gibVoraussichtlicheDateiDownloadzeit(long downloadSize, long chunkSize)
	{
		String ausgabe = "";
		
		int sekunden = 0;
		
		double downloadSpeed = 1;
		
		// verhindere DivisionByZero-exception !!
		//
		if(chunkSize > 0)
		{
			downloadSpeed = (downloadSize / 1000 ) / chunkSize;
		}
		
		sekunden = (int)downloadSpeed;
		
		// erzeuge die textuelle Zeitdarstellung
		
		int stunden = sekunden / 3600; // 1 Stunde = 3600 Sekunden
		int minuten = sekunden / 60;		 
		sekunden = sekunden % 60;
		
		
		if (stunden > 0) 
		{
			if (minuten > 0) 
			{
				if (sekunden > 0) 
				{
					ausgabe = stunden + " Std. " + minuten + " Min. "
							+ sekunden + " Sek. ";
				} 
				else 
				{
					ausgabe = stunden + " Std. " + minuten + " Min.";
				}
			}
			else 
			{
				ausgabe = stunden + " Std.";
			}
		}
		else 
		{
			if (minuten > 0) 
			{
				if (sekunden > 0) 
				{
					ausgabe = minuten + " Min. " + sekunden + " Sek.";
				}
				else 
				{
					ausgabe = minuten + " Min.";
				}
			}
			else 
			{
				if (sekunden > 0) 
				{
					ausgabe = sekunden + " Sek.";
				}
			}
		}

		return ausgabe;
	}
	
	/*
	 * gibt eine neues java.io.File-Objekt mit der jeweiligen Datei-Pfadangabe
	 * zurück
	 * 
	 * @param path Pfadangabe zu einer Datei oder zu einem Verzeichnis
	 * @return eine neues File-Objekt, welches eine Datei oder ein Verzeichnis darstellt
	 * 			wenn Parameter "path" nicht leer ist
	 */
	private File file(String path) 
	{
		return path.length() > 0 ? new File(path) : null;
	}
	
	/*
	 * setzt den Server-Status
	 * 
	 * <br /><br />
	 * erlaubte Werte f&uuml;r den <b>status</b> sind zur Zeit: <br />
	 * 
	 * <ul>
	 *  <li>LOGGED_IN = zum Server verbudnen und erfolgreich authentifiziert (eingeloggt)</li>
	 *  <li>VERBUNDEN = erfolgreiche Verbindung zum Server aufgebaut</li>
	 *  <li>NICHT_VERBUNDEN = bei Verbindungsfehlern oder falschem Login</li>
	 * </ul>
	 * 
	 * @param status der neue Server-Status
	 */
	private void setServerStatus(String status)
	{
		if((status.compareToIgnoreCase(VERBUNDEN) == 0) 
				|| (status.compareToIgnoreCase(LOGGED_IN) == 0))
		{
			serverStatus = status;
		}
		else 
		{
			serverStatus = status;
		}
	}

	/*
	 * erhalte den aktuellen Status einer Server-Operation
	 */
	private String getLastServerStatus() 
	{
		return client.getReplyString();
	}

}