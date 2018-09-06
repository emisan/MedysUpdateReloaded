package service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import server.MedysFTPConnectionClient;
import server.MedysVersionsnummern;
import client.MedysUpdateClientUI;

/**
 * Die Klasse MedysFTPOperations bildet eine <u><i>logische</i></u> Schnittstelle zu
 * dem {@link server.MedysFTPConnectionClient}, wor&uuml;ber sich die
 * FTP-Server relevanten Funktionen abrufen lassen
 * 
 * <br /><br />
 * 
 * @author Hayri Emrah Kayaman, MEDYS Gmbh Wülfrath 2016
 *
 */
public class MedysFTPOperations {

	private boolean ftpVerbunden;
	private boolean clientLoggedIn;
			
	// verfügbare Medys Update-Version
	//
	private String verfuegbareVersion;
	
	// der Dateiname einer heruntergeladenen Datei
	//
	private String downloadDateiName;

	private String hostname, port, username, password;
	
	private String updateVersion, updateReportVersion;
	
	private final String MEDYS_SUPPORT_TEL = " Telefon 02058 - 92 11 20";
	
	private final String UPDATE_FILE_NAME = "update_medys.zip";
	
	private final String ZIEL_VERZEICHNIS = System.getProperties().getProperty("user.home") + File.separator + "Desktop";
	
	// Namen der Download-Dateien und das Dwonlaodverzeichnis festhalten
	private Map<String, String> downloads;
		
	private MedysFTPConnectionClient ftpClient;
	
	private MedysUpdateClientUI clientUI;
	
	public MedysFTPOperations(MedysUpdateClientUI clientUI)
	{
		
		downloads = new HashMap<String, String>();
		
		ftpVerbunden = false;
		clientLoggedIn = false;
		
		this.clientUI = clientUI;
		
		ftpClient = new MedysFTPConnectionClient(this);
	}

	/**
	 * Speichert den Namen der Downlaoddatei und den dazugehörigen Downloadverzeichnispfad
	 * 
	 *  @param downloadDateiName
	 *  		der Name der heruntergeladenen Datei
	 *  
	 *  @param donwloadVerzeichnisPfad das Pfad zum Downloadverzeichnis, wo die Download-Datei sich befinden soll
	 */
	public void addDownload(String downloadDateiName, String donwloadVerzeichnisPfad)
	{
		downloads.put(downloadDateiName, donwloadVerzeichnisPfad);
	}
	
	/**
	 * * verbindet sich zum Medys FTP-Server mit den spezifischen
	 * Verbindungsparametern 
	 * 
	 * @param hostname 
	 * 			FTP-Server-URL in WWW-Form (wie ftp.organistaion.de) oder IP des FTP-Servers
	 * @param port
	 * 			der Verbindungsport (Socket der Verbindung) 
	 * @param username
	 * 			anmeldender Benutzername f&uuml;r die Login-Prozedur
	 * @param password
	 * 			Password des anmeldenden Benutzernamen
	 */
	public void connect(String hostname,
			String port,
			String username,
			String password)
	{	
		// verbinde dich
		//
		// WICHTIG
		//
		// entfernter Aufruf der Benachrichtigung einer fehlenden Internetverbindung in
		// MedysFTPConnectionClient.connect()
		//
		// siehe internen Kommentar in MedysUPdateClientTasks.runSearchForNewUpdatesTask(..)-Methode für
		// den Grund
		//
		ftpClient.connect(hostname,
				port,
				username,
				password);
		
		if(ftpClient.gibServerStatus().toLowerCase().compareTo("verbunden") == 0)
		{
			setFTPVerbunden(true);
			
			ftpClient.login(username, password);
			
			if(ftpClient.isLoggedIn())
			{
				clientLoggedIn = true;
				
				// merke dir die Verbindungsparameter
				// für einen eventuellen Aufruf der Methode "reConnectAndLogin(..)"
				//
				this.hostname = hostname;
				this.port = port;
				this.username = username;
				this.password = password;
				
				ftpClient.setEnterLocalPassiveMode(true);
			}
		}
	}
		
	/**
	 * L&auml;dt die angeforderte Update-Version vom jeweiligen Update-Ordner
	 * vom FTP-Server herunter
	 *
	 *<br /><br />
	 *<u><b>Voraussetzung</b></u><br /><br />
	 * 
	 * Ein erfolgreicher Login eines Benutzers in das FTP-Server-Verzeichnis, <br />
	 * wo sich die MedysUpdate-Versionen befinden, sollte vorher erfolgt sein.
	 * 
	 *
	 * @param serverVerzeichnis
	 * 			das Verzeichnis auf dem Server, indem sich die Datei zum Donwload befindet
	 * 
	 * @param version
	 * 			die jeweilige angeforderte UpdateVersion (entsprechend Ordnernamen auf dem FTP-Server)
	 * 
	 * @param preDownloadAnzahlBytes
	 * 			diesen Parameter benutzen, um vorab eine voraussichtliche
	 * 			Download-Zeit der Update-Version zu erhalten
	 * 
	 */
	public boolean errechneDownloadDauer(String serverVerzeichnis, byte[] preDownloadAnzahlBytes)
	{
		boolean errechnet = false;
		
		if (clientUI != null) {

			if (serverVerzeichnis != null) 
			{
				serverVerzeichnis = 
						MedysVersionsnummern
						.stelleUrspruenglichenOrdnernamenWiederHer(serverVerzeichnis);
				
				System.out.println("Quartals-Version zum download: " + serverVerzeichnis);
				
				
				if((preDownloadAnzahlBytes != null) && (preDownloadAnzahlBytes.length > 0))
				{
					// da diese Methode public ist (wegen der anwendungsarchitektur)
					// prüfe, ob Server-Verbindung vorhanden
					//
					if(isLoggedIn())
					{
						errechnet = 
								ftpClient.errechneVoraussichtlicheDownloadDauer(
										serverVerzeichnis, 
										UPDATE_FILE_NAME, 
										preDownloadAnzahlBytes);
					}
				}
			}
		}
		
		return errechnet;
	}
	
	/**
	 * Bricht die Verbidnung zum FTP-Server ab
	 * 
	 * @throws IOException - falls es zu einem Verbindungsfehler kommt
	 */
	public void disconnect() throws IOException
	{
		ftpClient.disconnect();
	}
	
	/**
	 * L&auml;dt eine Datei vom FTP-Server herunter
	 * 
	  * @param quellverzeichnis
	 * 			Server-Verzeichnis, welches die Datei zum herunterladen enth&auml;lt
	 * 
	 * @param dateiName
	 * 			die Datei (Dateiname), die heruntergeladen werden soll
	 * 
	 * @param zielVerzeichnis
	 * 			Ablageort der Datei
	 */
	public void downloadFile(String quellverzeichnis, String dateiName, String zielVerzeichnis)
	{
		if (dateiName != null)
		{
			if (isLoggedIn())
			{
				downloadDateiName = dateiName;
				
				ftpClient.downloadFile(quellverzeichnis, dateiName, zielVerzeichnis);
				
				if(ftpClient.isDownloadDateiHeruntergeladen())
				{
					String pfad = "";
					
					if(zielVerzeichnis.endsWith(File.separator))
					{
						pfad = zielVerzeichnis + dateiName;
					}
					else
					{
						pfad = zielVerzeichnis + File.separator + dateiName;
					}
					
					addDownload(dateiName, pfad);
				}
			}
		}
	}
	
	/**
	 * L&auml;dt die angeforderte Update-Version vom jeweiligen Update-Ordner
	 * vom FTP-Server herunter
	 * 
	 * @param version
	 * 			die jeweilige angeforderte UpdateVersion
	 * 
	 * @param preDownloadAnzahlBytes
	 * 			diesen Parameter benutzen, um vorab eine voraussichtliche
	 * 			Download-Zeit der Update-Version zu erhalten
	 * 
	 */
	public void downloadUpdate(String version)
	{
		if (clientUI != null) {

			if (version != null) 
			{
				version = 
						MedysVersionsnummern
						.stelleUrspruenglichenOrdnernamenWiederHer(version);
				
				System.out.println("Quartals-Version zum download: " + version);
				
				if(isLoggedIn())
				{
					// die gesamte Version herunterladen
					//
					ftpClient.downloadUpdate(
							version,
							UPDATE_FILE_NAME,
							ZIEL_VERZEICHNIS);
				}
				else
				{
					reConnectAndLogin(gibHostname(), gibVerbindungsport(), gibUsername(), gibPassword());
					
					downloadUpdate(version);
				}
			}
		}
	}
	
	/**
	 * erhält zu einer gegebene aktuelle MEDYS-Version alle verfügbaren Quartals- und
	 * Zwischenupdate vom MEDYS FTP-Server 
	 * 
	 * 
	 * @param aktuelleVersion die aktuell, installierte laufende MEDYS-Version
	 * @return in Zahlen formatierte Ansichten der Updateversionen zu der aktuellen Version
	 */
	public String erhalteLetzteUpdateVersionenVomServer(String aktuelleVersion)
	{	
		System.out.println("\ngesuchte Version " + aktuelleVersion);
		
		if(isLoggedIn())
		{		
			MedysVersionsnummern.setzeAktuelleVersionsnummer(aktuelleVersion);
			
			MedysVersionsnummern.setzeVerfuegbareVersionsnummern(gibVerzeichnisnamen());
			
			// for DEBUG ONLY
			//
			MedysVersionsnummern.druckeVersionen(MedysVersionsnummern.gibVersionsnummern());
		
			verfuegbareVersion = gibLetzteVerfuegbareVersionZu(aktuelleVersion, MedysVersionsnummern.gibVersionsnummern());
			
			System.out.println("erhaltene verfügbare Version vom FTP-Server " + verfuegbareVersion);
		}
		return verfuegbareVersion;
	}

	public MedysUpdateClientUI gibClientUI()
	{
		return clientUI;
	}
	
	/**
	 * Liefert Verbindungsparameter : Hostname (alias FTP-Server-URL)
	 * 
	 * @return die FTP-Server-URL, wie sie beim Aufruf der {@link #connect(String, String, String, String)}
	 * 			-Methode angegeben wurde, sonst NULL
	 */
	public String gibHostname()
	{
		return hostname;
	}

	/**
	 * liefert die Referenz auf die intern genutzte {@link server.MedysFTPConnectionCLient} Klasse
	 * 
	 * @return MedysFTPConnectionClient - ftpClient
	 */
	public MedysFTPConnectionClient gibFTPConnectionClient()
	{
		return ftpClient;
	}

	/**
	 * Liefert Verbindungsparameter : das Password für die Benutzerauthentifizierung des anmeldenden Benutzers
	 * 
	 * @return das Benutzerpasswort wie sie beim Aufruf der {@link #connect(String, String, String, String)}
	 * 			-Methode angegeben wurde, sonst NULL
	 */
	public String gibPassword()
	{
		return password;
	}

	/**
	 * liefert die Referenz auf die ProgressBar der MedysUpdateClientUI
	 * 
	 * @return ProgressBar - client.MedysUpdateClientTasks.rogressBar
	 */
	public ProgressBar gibProgressBar()
	{
		return clientUI != null ? clientUI.gibProgressBar() : null;
	}
	
	/**
	 * liefert den aktuellen Server-Status zur&uuml;ck
	 * 
	 * @return serverStatus - String
	 */
	public String gibServerStatus()
	{
		return ftpClient.gibServerStatus();
	}
	
	/**
	 * Liefert Verbindugnsparameter : der Name des anmeldenden (sich authentifizierenden) Benutzers/Benutzeraccounts
	 * 
	 * @return der anmeldende Benutzername eines Accoutns (username) wie sie beim Aufruf der {@link #connect(String, String, String, String)}
	 * 			-Methode angegeben wurde, sonst NULL
	 */
	public String gibUsername()
	{
		return username;
	}

	/**
	 * Liefert die Anzeige der voraussichtlichen Download-Dauer
	 * eines Datei-Downloads
	 * 
	 * <br /><br />
	 * 
	 * Die Download-Dauer sollte vorher durch die Methode <br/>
	 * <i>{@link server.MedysFTPConnectionClient#errechneVoraussichtlicheDownloadDauer(String,String,byte[])}</i>
	 * ermittlet werden
	 * 
	 * <br />
	 * 
	 * @return die voraussichtliche Download-Dauer im Format &lt; Stunde &gt; &lt; Minute &gt; &lt; Sekunde &gt; <br />
	 * 		   oder NULL, wenn keine Dauer (Zeit) ermittelt werden konnte
	 */
	public String gibVoraussichtlicheDownloadDauerAnzeige()
	{
		return ftpClient.gibVoraussichtlicheDownloadDauerAnzeige();	
	}
	
	/**
	 * liefert alle Verzeichnisnamen aus dem aktuellen Verzeichnis,
	 * in den sich ein angemeldeter Benutzer GERADE befindet
	 * <br /><br />
	 * Hat ein Verzeichnis keine weiteren Verzeichnisse,
	 * so ist dies ein Root-Verzeichnis, und es wird dann nur dieser Name
	 * ausgeliefert.
	 * <br /><br />
	 * Ansonsten, alle Unterverzeichnisnamen eine Verzeichnisses, wenn vorhanden
	 * <br /><br />
	 * @return siehe Beschreibung, oben
	 */
	public String[] gibVerzeichnisnamen()
	{	
		return clientLoggedIn ? gibFormatiereUpdateOrdnerNamen(ftpClient.gibVerzeichnisnamen()) : null;
	}

	/**
	 * Liefert Verbindungsparameter : Port (Verbindungsport / Socket-Port) des FTP-Hostnamen/FTP-IP
	 * 
	 * @return der Verbindungsports, wie er beim Aufruf der {@link #connect(String, String, String, String)}
	 * 			-Methode angegeben wurde, sonst NULL
	 */
	public String gibVerbindungsport()
	{
		return port;
	}
	
	/**
	 * pr&uuml;ft, ob eine Verbindung zum FTP-Server hergestellt wurde
	 * 
	 * @return TRUE wenn verbunden, sonst FALSE
	 */
	public boolean isFTPVerbunden()
	{
		return ftpVerbunden;
	}
	
	/**
	 * pr&uumlft, ob sich ein Benutzer mit den spezifischen Verbindungsparametern
	 * über die connect()-Methode am Medys FTP-Server erfolgreich angemeldet
	 * hat
	 * <br />
	 * @return TRUE für eine erfolgreichen Login, sonst FALSE
	 */
	public boolean isLoggedIn()
	{
		return clientLoggedIn;
	}

	/**
	 * pr&uum:ft, ob die Updateanleitung aus dem Versionsverzeichnis vom FTP-Server
	 * heruntergeladen werden konnte.
	 * 
	 * @return TRUE wenn die Updateanleitung heruntergeladen worden ist, FALSE wenn nicht
	 */
	public boolean isUpdateanleitungHeruntergeladen()
	{
		boolean status = false;
		
		Iterator<Entry<String, String>> iterator = gibDownloads().entrySet().iterator();
		
		while(iterator.hasNext())
		{
			if(iterator.next().getKey().toLowerCase().contains("updateanleitung"))
			{
				status = true;
				break;
			}
		}
		return status;
	}
	
	/**
	 * pr&uum:ft, ob der Report zu der Updateanleitung aus dem Versionsverzeichnis vom FTP-Server
	 * heruntergeladen werden konnte.
	 * 
	 * @return TRUE wenn die Updateanleitung heruntergeladen worden ist, FALSE wenn nicht
	 */
	public boolean isUpdateReportHeruntergeladen()
	{
		boolean status = false;
		
		Iterator<Entry<String, String>> iterator = gibDownloads().entrySet().iterator();
		
		while(iterator.hasNext())
		{
			if(iterator.next().getKey().toLowerCase().contains("updatereport"))
			{
				status = true;
				break;
			}
		}
		
		return status;
	}
	
	/**
	 * L&ouml;scht vorherige Download-/Entpack-Versuche, falls ein Abbruch
	 * erfolgte oder<br />
	 * ein Fehler aufgetreten ist
	 * 
	 * @param
	 */
	public void loescheVorherigenUpdateVersuch(
			String zielverzeichnis,
			String updateDateiName)
	{
		Unzip.loescheDatei(zielverzeichnis, updateDateiName);

		String zipDateiOrdnerName = 
				updateDateiName.substring(0, updateDateiName.toLowerCase().lastIndexOf(".zip"));

		Unzip.loescheOrdner(zielverzeichnis, zipDateiOrdnerName);
	}

	/**
	 * Logout-Prozedur vom Medys FTP-Server
	 */
	public void logout()
	{
		ftpClient.logout();
		
		// merke Logout
		//
		clientLoggedIn = false;
	}
	
	/**
	 * liefert die Versionsnummer oder den Versionsbezeichner(vXX_XX) der aktuellen UpdateVersion
	 * 
	 * @return die Versionsnummer, wenn ermittelt, sonst NULL
	 */
	public String gibUdpateVersionsnummer()
	{
		return updateVersion;
	}
	
	/**
	 * liefert die Versionsnummer oder den Versionsbezeichner(vXX_XX) der aktuellen UpdateReport-Version
	 * 
	 * @return die Versionsnummer, wenn ermittelt, sonst NULL
	 */
	public String gibUpdateReportVersionsnummer()
	{
		return updateReportVersion;
	}
	
	/**
	 * liefert den festen Namen der Zip-Datei welche die Updatedateien
	 * beinhalten wird
	 * 
	 * @return String - update_medys.zip
	 */
	public final String gibUpdateZipDateiName()
	{
		return UPDATE_FILE_NAME;
	}
	
	/**
	 * liefert die fest definierte absolute Verzeichnispfadangabe zum Download-Verzeichnis zur&uuml;ck
	 * 
	 *  @return je nach System: Desktop-Verzeichnis des aktuellen Systembenutzers
	 */
	public final String gibUpdateVersionZielVerzeichnis()
	{
		return ZIEL_VERZEICHNIS;
	}
	
	/**
	 * leitet die Unzip-Routine zu der heruntergeladenen Update-Version ein
	 * 
	 * @param downloadedUpdateVersion
	 * 			die Versionsnummer oder der Ordnername der heruntergeladenen Update-Version
	 */
	public void leiteUnzipRoutineEin()
	{	
		final String downloadVerzeichnis = gibUpdateVersionZielVerzeichnis();
		
		final String heruntergeladeneDatei = gibClientUI().MEDYS_UPDATE_DATEI_NAME;	
		
		Task<Double> task = gibClientUI().gibClientTasks().runUnzipTask(downloadVerzeichnis, heruntergeladeneDatei);
		
		gibClientUI().gibProgressMessageField().setWrappingWidth(gibClientUI().gibStage().getWidth() - 50);
		
		gibClientUI().gibUpdateMessageLayout().getChildren().remove(gibClientUI().gibUpdateDownloadDauerMessageField());

		gibClientUI().gibProgressBar().progressProperty().unbind();
		gibClientUI().gibProgressBar().progressProperty().bind(task.progressProperty());
		
		gibClientUI().gibProgressMessageField().autosize();
		gibClientUI().gibProgressMessageField().textProperty().unbind();
		gibClientUI().gibProgressMessageField().textProperty().bind(task.messageProperty());
	
//		gibClientUI().gibUpdateMessageField().textProperty().unbind();
		
		Thread taskThread = new Thread(task);
		
		taskThread.setDaemon(true);
		
		taskThread.setName("MedysUpdate-UnzipRoutineTask");
		
		taskThread.start();
	}
	
	/**
	 * Sendet eine angemessene Fehlernachricht an die MedysFTPClientUI zur Anzeige <br />
	 * in der jeweiligen Text-Komponente "updateMessageField" und entfernt unn&ouml;tige Schaltfl&auml;chen bis auf den Schliessen-Button
	 * 
	 * @deprecated wird nicht mehr genutzt, nutze {@link #sendeFtpServerVerbindungsAbbruchNachricht()}
	 */
	@Deprecated
	public void sendeFtpServerInternetverbindungsfehlerNachricht()
	{
		updateClientUIBeimSchliessenMitFehlermeldung();
		
		gibClientUI()
		.setUpdateMessageFieldText("Fehlende Internetverbindung !"
					+ "\n\nPrüfen Sie Ihre Internetverbindung und versuchen Sie es erneut."
					+ "\n\nWenden Sie sich bitte an den Medys-Support" 
					+ ", falls Sie diese Meldung wiederholt erhalten."
					+ "\n\n" + MEDYS_SUPPORT_TEL);
	}
	
	/**
	 * Sendet eine Fehlernachricht an das "updateMessageField" bez&uuml;glich des Verbindungsabbruchs <br />
	 * und entfernt unn&ouml;tige Schaltfl&auml;chen bis auf den Abbruch-Button
	 */
	public void sendeFtpServerVerbindungsAbbruchNachricht()
	{
		updateClientUIBeimSchliessenMitFehlermeldung();

		gibClientUI()
		.setUpdateMessageFieldText(
				"Verbindung zum FTP-Server nicht möglich !"
						+ "\n\nDas Herunterladen des Updates wurde unterbrochen, "
						+ "da der FTP-server nicht erreichbar ist."
						+ "\n\nVersuchen Sie es zu einem späteren Zeitpunkt erneut oder "
						+ "rufen Sie den Medys-Support an, falls Sie diese Meldung wiederholt erhalten."
						+ "\n\n" + MEDYS_SUPPORT_TEL);
	}
	
	/**
	 * Sendet eine Fehlernachricht an das "updateMessageField" bez&uuml;glich des Verbindungsabbruchs <br />
	 * und entfernt unn&ouml;tige Schaltfl&auml;chen bis auf den Abbruch-Button
	 */
	public void sendeFehlerhafterLoginNachricht()
	{
		updateClientUIBeimSchliessenMitFehlermeldung();

		gibClientUI()
				.gibUpdateMessageField()
				.setText(
						"Falscher Benutzer-Login\n\n"
								+ "Sie sind nicht berechtigt "
								+ "eine Update-Version vom Server zu bekommen."
								+ "\n\nWenden Sie sich bitte an den Medys-Support " 
								+ "und drücken Sie auf \"Schliessen\" um das Programm zu beenden!"
								+ "\n\n" + MEDYS_SUPPORT_TEL);
	}
	
	/**
	 * setzt den Verbindungsstatus zu dem FTP-Server
	 * 
	 * @param status der Verbindungsstatus
	 */
	public void setFTPVerbunden(boolean status)
	{
		ftpVerbunden = status;
	}
	
	/**
	 * setze die Version der erfragten medys-Version
	 * 
	 * @param version die Versionsnummer oder den Versionsbezeichner (vXX_XX)
	 */
	public void setzeUpdateVersion(String version)
	{
		updateVersion = version;
	}
	
	/**
	 * setze die Version der Updatereport-Datei
	 * 
	 * @param version die Versionsnummer oder den Versionsbezeichner (vXX_XX)
	 */
	public void setzeUpdateReportVersion(String version)
	{
		updateReportVersion = version;
	}
	
	/**
	 * Sendet eine Fehlernachricht an das "updateMessageField" bez&uuml;glich der fehlenden
	 * Datei "update_medys.zip" (wenn der Downlaod nicht möglich ist)
	 */
	public void sendeFehlendeUpdateDateiNachricht()
	{
		updateClientUIBeimSchliessenMitFehlermeldung();
		
		gibClientUI()
				.gibUpdateMessageField()
				.setText(
						"Ein neues Medys-Update ist nicht verfügbar\n\n"
						+ "Drücken Sie auf \"Schliessen\" um das Programm zu beenden!");
	}
	
	private void updateClientUIBeimSchliessenMitFehlermeldung()
	{	
		gibClientUI()
		.gibComponentsLayoutManager()
		.getChildren()
		.removeAll(gibClientUI()
					.gibComponentsLayoutManager()
					.getChildren());
		
		gibClientUI()
		.gibComponentsLayoutManager()
		.setTop(gibClientUI().gibUpdateMessageField());
		
		gibClientUI()
		.gibComponentsLayoutManager()
		.setCenter(gibClientUI().gibAbbruchButton());
		
		BorderPane
		.setMargin(gibClientUI().gibAbbruchButton(), new Insets(80, 0, 0, 0));
		
		gibClientUI().gibAbbruchButton().setText(
				"Schliessen");
	}
	
	/**
	 * Versucht eine erneute Verbindung aus einer vorher gehenden FTP-Verbindung aufzubauen
	 * 
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 */
	public void reConnectAndLogin(
			String hostname,
			String port,
			String username,
			String password)
	{
		if(isLoggedIn())
		{
			logout();
		}
		System.out.println(ftpClient.gibServerStatus());
		
		connect(hostname, port, username, password);
	}
	
	/**
	 * Speichert den Dateinamen und das Downloadverzeichnis der heruntergeladenen Datei
	 * 
	 * @param downladDateiName
	 * 			der Name der heruntergeladenen Datei
	 * 
	 * @param donwloadDateiPpfad
	 * 			Verzeichnispfadangabe zum Zielverzeichnis oder Downloadverzeichnis, in 
	 * 			der die heruntergeladenen Datei sich befinden soll
	 */
	public void speicherDownloadInfo(String downladDateiName, String downloadDateiPfad)
	{
		downloads.put(downloadDateiName, downloadDateiPfad);
	}
	
	public Map<String, String> gibDownloads()
	{
		return downloads;
	}
	
	
	public String gibUpdateAnleitungName() 
	{
		String name = null;
		String version = gibUdpateVersionsnummer();
		
		if(version != null)
		{
			Set<Entry<String, String>> downloadEintraege = gibDownloads().entrySet();
		
			Iterator<Entry<String, String>> iter = downloadEintraege.iterator();
		
			while(iter.hasNext())
			{
				String key = iter.next().getKey();
				
				if (key.toLowerCase().contains("updateanleitung")
						&& key.contains(version))
				{
					name = key;
					break;
				}
			}
		}
		return name;
	}
	
	public String gibUpdateReportName()
	{
		String name = null;
		String version = gibUpdateReportVersionsnummer();
		
		if(version != null)
		{
			Set<Entry<String ,String>> donwloadEintraege = gibDownloads().entrySet();
			
			Iterator<Entry<String ,String>> iter = donwloadEintraege.iterator();
			
			while(iter.hasNext())
			{
				String key = iter.next().getKey();
				
				if(key.toLowerCase().contains("updatereport")
					&& key.contains(version))
				{
					name = key;
					break;
				}
			}
		}
		return name;
	}
	/**
	 * liefert die, f&uuml;r die Programmierung notwendige Ansicht der
	 * Update-Ordnernamen, </br> die sich auf dem Medys FTP-Server binden
	 * 
	 * <br /><br />
	 * 
	 * die Ordner liegen in der Regel wie folgt benannt auf dem Server
	 * 
	 * <br /><br />
	 * 
	 * Beispiel: </br>
	 * <ul>
	 * <li>v37_50</li>
	 * <li>v38_60</li>
	 * <li>v39_70</li>
	 * etc. ..
	 * </ul>
	 * 
	 * 
	 * die an den MedysUpdateClientUI übergebenen Startparameter beinhalten aber
	 * folgende Benennung aus dem JavaJar-Medysaufruf
	 * 
	 * <br /><br />
	 * 
	 * Medys-interne Benennung<br /><br />
	 * 
	 * <ul>
	 * <li>3750</li>
	 * <li>3860</li>
	 * <li>3970</li>
	 * etc. ..
	 * </ul>
	 * 
	 * Auf dem Server müssen die Ordnernamen daher mit dieser Methode auf die
	 * Medys-interne Benennung der Versionen für den weiteren Programmierablauf
	 * vorher formatiert werden
	 * <br /><br />
	 * 
	 * @return die formatierten Ordnernamen
	 */
	private String[] gibFormatiereUpdateOrdnerNamen(String[] ordnerNamen) 
	{
		int einfuegePos = 0;
	
		String[] rueckgabe = null;
	
		ArrayList<String> formatierteNamen = new ArrayList<String>();
	
		if (ordnerNamen != null) 
		{
			for (String name : ordnerNamen)
			{
				String format = "";
	
				if (name.startsWith("v")) 
				{
					format = name.substring(name.indexOf("v") + 1,
							name.length());
	
					int sub_tile = name.indexOf("_");
	
					if (sub_tile > 0) 
					{
						String save = format;
	
						format = format.substring(0, sub_tile - 1)
								+ save.substring(sub_tile, save.length());
	
						formatierteNamen.add(format);
	
						einfuegePos++;
					}
				}
			}
	
			int anzahlEinfuegungen = formatierteNamen.size();
	
			if (anzahlEinfuegungen > 0) 
			{
				einfuegePos = 0;
	
				rueckgabe = new String[anzahlEinfuegungen];
	
				Iterator<String> iterator = formatierteNamen.iterator();
	
				while (iterator.hasNext()) 
				{
					rueckgabe[einfuegePos++] = iterator.next();
				}
			}
		}
		return rueckgabe;
	}

	/*
	 * ermittelt anhand der Update-Verzeichnisse auf dem MEDYS FTP-Server,
	 * welche aktuell verf&uuml;gbare Update-Version zu einer gegebenen
	 * laufenden (aktuelle installierten) Medys-Version vorhanden ist.
	 * 
	 * <br /><br />
	 * 
	 * @param aktuelleVersion
	 * @return
	 */
	private String gibLetzteVerfuegbareVersionZu(String aktuelleVersion, int[] versionen) 
	{
		String letzteVersion = aktuelleVersion;
		
		int laufendeVersion = MedysVersionsnummern.gibVersionsnummer(aktuelleVersion);
		
		if(clientLoggedIn)
		{				
			MedysVersionPruefer
				.erfasseQuartalsupdateVersionZuAus(laufendeVersion, versionen);

			if(MedysVersionPruefer.hatVerfuegbaresUpdate())
			{
				// KEINE Formatierung von XXXX zu vXX_XX durchführen !!
				//
				// einfach die Version in XXXX erhalten
				//
				// jeweilige Formatierung mit gibFormatiereUpdateOrdnerNamen(..)
				// besser extern aufrufen
				
				// Grund: die Formatierungsmethode kann den Zustand 
				//
				// formatiere: vXX_XX zu vXX_XX 
				//
				// nicht HANDHABEN --> es gilt nur XXXX zu vXX_XX  !!!!
				//
				
				MedysVersionsnummern.setzeVersionVomServerZumDownload("" + MedysVersionPruefer.gibVerfuegbaresUpdate());
				
				if(MedysVersionsnummern.hatUpdateVersionVomServerErmitteltUndGesetzt())
				{
					letzteVersion = MedysVersionsnummern.gibVersionZumDownload();
				}
			}
		}
		
		return letzteVersion;
	}
}

