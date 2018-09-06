package client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import server.MedysVersionsnummern;
import service.MedysFTPOperations;
import service.MedysUpdateClientTasks;
import client.extensions.JFXTextLink;


/**
 * Start-Fenster f&uuml;r das Online-Update der Medys-Anwendung
 * 
 * <br /><br />
 * 
 * In diesem Fenster wird zun&auml;chst eine Verbindung zum Medys FTP-Server aufgebaut<br />
 * und auf diesem anhand einer aktuell laufenden (installierten) Medys-Versionsnummer nach 
 * verf&uuml;gbaren Quartals- oder Zwischenupdates gesucht.
 * 
 * <br /><br />
 * @author Hayri Emrah Kayaman, MEDYS GmbH Wülfrath 2016
 *
 */
public class MedysUpdateClientUI extends Application {
	
	// -------- JavaFX UI-Komponenten
	
	// LayoutManager für diese Anwendung
	//
	private BorderPane componentsLayoutManager, updateMessageLayout, sceneLayoutManager;
	
	// Corporate Logo
	//
	private ImageView medysLogo;
	
	private Button btn_cancelUpdate, btn_forwardButton, btn_gotoDownloadScene;
	
	private HBox buttonsHBox;
	
	private VBox progressComponentsVBox, progressMessageButtonsVBox, updateLinksVBox;
	
	private VBox startupSceneTextComponentsVBox;
	
	// Komponenten für die Such-Update-Prozess Anzeige
	//
	private ProgressBar progressBar;
	
	private ProgressIndicator progressIndicator;
	
	// Text-Label für die Statusmeldungen
	//
	private Text updateMessageField, progressMessageField, updateDownloadDauerMessageField;
	
	private Text entryMedysVersionText, entryMedysVersionsSchrittNummerText;
	
	private TextField entryMedysVersionTextField, entryMedysVersionsSchrittNummerTextField;
	
	// ---- Hauptszene und Bühne
	
	private Scene scene;
	
	private Stage stage;
	
	// ---- externe FX-Komponenten (Hilfs-Komponenten)
	//
	private JFXTextLink updateAnleitungLink, updateReportLink;
	
	// ---- Hilfsklassen
	//
	private MedysUpdateClientTasks clientTasks;
	private MedysFTPOperations medFtpOps;
	
	// Thread für den Arbeitsprozess
	// und Task für den Suchprozess/Fortschrittssbalken (ProgressBar)
	//
	private Thread actualThread;
	
	private Task<Void> progressBarTask;
	
	// fester Name für die gezippte Datei (Update-version) vom FTP-Server
	//
	public final String MEDYS_UPDATE_DATEI_NAME = "update_medys.zip";
	
	// fester Inhalt der Update-Info TextDatei (Benachrichtigung / Log-Datei für die Rückkehrprozedur in Medys)
	//
	private final static String UPDATE_NICHT_ENTPACKT = "UPDATE_NICHT_ENTPACKT";
	
	private final static String ZIEL_VERZEICHNIS = System.getProperties().getProperty("user.home") + File.separator + "Desktop";
	
	// aus Medys übergebene Start-Parameter
	//
	private static String[] launchArguments;
	
	private static boolean isStandaloneStartup = false;
	
	/**
	 * Executive-Methode für MedysUpdateClientUI
	 * 
	 * <br /><br />
	 * @param args erwartete Parameter = 6 </br>
	 * 		  <ul>
	 * 			<li>Server URL-name ohne Http-Angabe</li>
	 * 		    <li>Verbindungsportnummer</li>
	 * 			<li>anmeldender Benutzername</li>
	 * 			<li>Password des Benutzers</li>
	 * 			<li>in DezimalZahl-Darstellung, die aktuelle installierte Medys-Version</li>
	 * 			<li>Verzeichnispfad-Angabe auf Zusatzordner &quot;Updates&quot; auf dem lokalen Medys-Server
	 */
	public static void main(String[] args) {
		
		String medysZusatzVerzeichnis = MedysUpdateClientUI.ZIEL_VERZEICHNIS;
		String medysUpdateTxtDateiName = "medys_update_info.txt";
		
		if(args.length==7)
		{
			medysZusatzVerzeichnis = args[5];
			medysUpdateTxtDateiName = args[6];
		}
		if(args.length == 0)
		{
			MedysUpdateClientUI.isStandaloneStartup = true;
			
			args = new String[7];
			
			args[0] = "ftp.medys.de";
			
			args[1] = "21";
			
			args[2] = "u7943014-medysupdate";
			
			args[3] = "NPnB1gPf";
			
			args[4] = ""; // MEDYS Version über ein Textfield erfragen !
			
			args[5] = medysZusatzVerzeichnis ;
			
			args[6] = medysUpdateTxtDateiName;
		}
		
		if(medysZusatzVerzeichnis != null)
		{
			File medZusatzDir = new File(medysZusatzVerzeichnis);
			
			if(medZusatzDir.exists() && !medZusatzDir.isFile())
			{
				if(medysUpdateTxtDateiName.endsWith(".txt"))
				{
					File medUpdateInfoTxt = new File(medZusatzDir, medysUpdateTxtDateiName);
					
					if(medUpdateInfoTxt.exists() && !medUpdateInfoTxt.isDirectory())
					{
						medUpdateInfoTxt.delete();
						
						try
						{
							FileWriter fw = new FileWriter(medUpdateInfoTxt);
							
							fw.write(UPDATE_NICHT_ENTPACKT);
							
							fw.close();
						}
						catch(IOException fwExcep)
						{
							// for DEBUG ONLY
							//
							System.out.println("IOException aus MedysUpdateClientUI.main");
							fwExcep.printStackTrace();
						}
					}
					else
					{
						try
						{
							FileWriter fw = new FileWriter(medUpdateInfoTxt);
							
							fw.write(UPDATE_NICHT_ENTPACKT);
							
							fw.close();
						}
						catch(IOException fwExcep)
						{
							// for DEBUG ONLY
							//
							System.out.println("IOException aus MedysUpdateClientUI.main");
							fwExcep.printStackTrace();
						}
					}
				} 
			}
		}
		
		// sichere die Startup-Parameter aus der OMNIS-Übergabe
		//
		MedysUpdateClientUI.launchArguments = args;
		
		// starte die JavaFx-Anwendung
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception 
	{	
		initSceneAndStage(primaryStage);
	}

	/*
	 * erstellt alle UI-Komponenten
	 * 
	 * WICHTIG
	 * -------
	 * 
	 * Rufe diese Methode VOR der Initialisierung
	 * eines Scene-Objekts UND VOR der initLayout()-Methode auf !!
	 * 
	 */
	private void initComponents()
	{
		initInteractiveComponents();
		
		buttonsHBox = new HBox();
		buttonsHBox.setSpacing(30);
		buttonsHBox.setAlignment(Pos.CENTER);
		buttonsHBox.getChildren().addAll(btn_cancelUpdate, btn_forwardButton);
		
		updateLinksVBox = new VBox();
		updateLinksVBox.setId("updateHyperlinksVBox_Style");  // StyleSheet-ID
		updateLinksVBox.setSpacing(10);
		updateLinksVBox.getChildren().addAll(updateAnleitungLink, updateReportLink);
		updateLinksVBox.setVisible(false);
		
		// ----------- Text-Label Angaben
		
		updateMessageField = new Text("Suche nach neuen Updates....");
		updateMessageField.setId("downloadableVersion");	// StyleSheet-ID
	
		updateDownloadDauerMessageField = new Text();
		updateDownloadDauerMessageField.setId("downloadableVersion");	// StyleSheet-ID
		
		progressMessageField = new Text("");
		progressMessageField.setId("progressText_Style");	// StyleSheet-ID
		
		entryMedysVersionText = new Text("Medys-Version : v");
		entryMedysVersionText.setId("progressText_Style");
		
		entryMedysVersionTextField = new TextField();
		entryMedysVersionTextField.setId("medysVersionsNummernTextField");
		
		entryMedysVersionsSchrittNummerText = new Text("_");
		entryMedysVersionsSchrittNummerText.setId("progressText_Style");
		
		entryMedysVersionsSchrittNummerTextField = new TextField();
		entryMedysVersionsSchrittNummerTextField.setId("medysVersionsNummernTextField");
		
		startupSceneTextComponentsVBox = new VBox();
		startupSceneTextComponentsVBox.setAlignment(Pos.CENTER);
		startupSceneTextComponentsVBox
			.getChildren()
			.addAll(entryMedysVersionText, 
					entryMedysVersionTextField, 
					entryMedysVersionsSchrittNummerText, 
					entryMedysVersionsSchrittNummerTextField);
		
	}

	/*
	 * erstellt die interaktiven UI-Komponenten und
	 * legt deren Action-Handling Verhalten fest
	 * 
	 * WICHTIG
	 * -------
	 * 
	 * Rufe diese Methode NIE ALLEINE AUF !!!
	 * 
	 * Diese Methode dient nur zur Definition der interaktiven
	 * Komponenten -> Layout-Komposition und Positionierung
	 * erfolgen in der initComponents() / initLayout() Methoden
	 * 
	 */
	private void initInteractiveComponents()
	{	
		// ---------- anklickbarer Link A
		
		updateAnleitungLink = new JFXTextLink();
		updateAnleitungLink.setId("JFXTextLink_Style");	// StyleSheet-ID
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() 
			{
				updateAnleitungLink.setOnMouseClicked(new EventHandler<MouseEvent>() 
				{
					@Override
					public void handle(MouseEvent arg0) {
						try 
						{
							if(clientTasks != null)
							{
								if(medFtpOps.isUpdateanleitungHeruntergeladen())
								{
									File updateFile = new File(clientTasks.gibUpdateanleitungPfad());
									
									if(updateFile.exists())
									{
										updateAnleitungLink.doFileHyperlink(
												medFtpOps.gibUpdateAnleitungName(), 
												new URL("file", null, updateFile.getAbsolutePath()));
									}
									else
									{
										gibUpdateLinksHBox().getChildren().remove(updateAnleitungLink);
									}
								}
							}
						}
						catch (MalformedURLException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		});
		
		// ---------- anklickbarer Link B
		
		updateReportLink = new JFXTextLink();
		updateReportLink.setId("JFXTextLink_Style");	// StyleSheet-ID
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() 
			{
				updateReportLink.setOnMouseClicked(new EventHandler<MouseEvent>() 
				{
					@Override
					public void handle(MouseEvent arg0) {
						try 
						{
							if(clientTasks != null)
							{
								if(medFtpOps.isUpdateReportHeruntergeladen())
								{
									File updateFile = new File(clientTasks.gibUpdateReportPfad());
									
									if(updateFile.exists())
									{
										updateReportLink.doFileHyperlink(
												medFtpOps.gibUpdateReportName(), 
												new URL("file", null, updateFile.getAbsolutePath()));
									}
									else
									{
										gibUpdateLinksHBox().getChildren().remove(updateReportLink);
									}
								}
							}
						}
						catch (MalformedURLException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			}
		});
		
		// ---------- ProgressBar und ProgressIndicator
		
		progressBar = new ProgressBar(0);
		progressBar.setMaxWidth(250);
		
		// setze unsichtbar, wird sichtbar, wenn Download startet
		//
		progressBar.setVisible(false);
		progressBar.setId("updateInProgressText_Style");
		
		progressIndicator = new ProgressIndicator(0);
	    progressIndicator.setMaxSize(200, 100);
	
	    // ---------- Schaltflächen
	    
	    btn_forwardButton = new Button("Weiter >>");
		btn_forwardButton.setDisable(true);
		btn_forwardButton.setOnAction(
				new EventHandler<ActionEvent>() 
				{
					// eventuelle Server-Fehler oder Statusmeldungen werden
					// in MedysUpdateCleintTasts oder MedysFTPOperations
					// statisch an diese GUI auf das Anzeigefeld
					// "downloadVersionMessageField" weitergeleitet
					
					@Override
					public void handle(ActionEvent acevt)
					{
						if(MedysVersionsnummern.hatUpdateVersionVomServerErmitteltUndGesetzt())
						{
							String updateVerzeichnis = 
									MedysVersionsnummern.stelleUrspruenglichenOrdnernamenWiederHer(
											MedysVersionsnummern.gibVersionZumDownload());
							
							// verstecke die updateLinksVBox bis das Update heruntergeladen
							// wurde
							//
							gibUpdateLinksHBox().setVisible(false);
							
							// zeige es danach wieder an
							// (siehe -> MedysUpdateClientTasks.runUnzipTask)
							
							clientTasks.runDownloadTask(updateVerzeichnis);
						}
					}
				});
		
		btn_cancelUpdate = new Button("Abbruch");
		btn_cancelUpdate.setOnAction(
				new EventHandler<ActionEvent>() 
				{
					@Override
					public void handle(ActionEvent acevt)
					{
						if(medFtpOps.isLoggedIn())
						{
							medFtpOps.logout();
						}
						
						try
						{
							File file = 
									new File(
											medFtpOps.gibUpdateVersionZielVerzeichnis(), 
											medFtpOps.gibUpdateZipDateiName());
							
							if(file.exists())
							{
								file.delete();
							}
						}
						catch(Exception e)
						{
							// nur fürs DEGUBBEN sichtbar
							System.out.println("Exception nach Betätigung des Abbruch-Button"
									+ "\nwenn teilweise heruntergeladene Zip-Datei gelöscht werden soll\n"
									+ e.getMessage());
						}
						stage.close();
					}
				});
		
		btn_gotoDownloadScene = new Button("Weiter");
		btn_gotoDownloadScene.setOnAction(new EventHandler<ActionEvent>() 
				{
					@Override
					public void handle(ActionEvent acevt)
					{
						if(entryMedysVersionTextField != null)
						{
							String medysVersion = entryMedysVersionTextField.getText();
							
							if(medysVersion != null)
							{
								if(stage != null)
								{
									initDownloadSceneAndStage(stage);
								}
							}
						}
					}
				});
	}

	public void initStartupStandaloneScene(Stage stage)
	{
		sceneLayoutManager = new BorderPane();
		sceneLayoutManager.setTop(startupSceneTextComponentsVBox);
		sceneLayoutManager.setCenter(btn_gotoDownloadScene);
		
		scene  = new Scene(sceneLayoutManager, 560, 440);
		
		// lege die SytelSheet-Datei fest (hier kommt sie aus dem Resource-Folder "resources"
		//
		// der "resources"-Folder wird in der RunConfiguration als "Source"-Folder angegeben !!
		
		scene.getStylesheets().clear();
		
		scene.getStylesheets().add("Styles.css");
		
		stage.setTitle("MEDYS Updates");
		
		stage.setScene(scene);
		
		stage.setResizable(false);
		
		stage.show();
		
		// speichere die aktuelle Scene ab
		// falls Sie anderswo verändert werden sollte
		// oder implementiert wird
		//
		this.stage = stage;
	}
	
	/*
	 * rufe diese Methode auf, NACHDEM ein Scene-Object initialisiert wurde
	 */
	private void initDownloadSceneLayout()
	{	
		updateMessageLayout = new BorderPane();
		updateMessageLayout.setTop(updateMessageField);
		updateMessageLayout.setCenter(updateLinksVBox);
		updateMessageLayout.setBottom(updateDownloadDauerMessageField);
		updateMessageLayout.setId("updateMessageLayout_Startup_Style");
		
		progressComponentsVBox = new VBox();
		progressComponentsVBox.setAlignment(Pos.CENTER);
		VBox.setVgrow(progressIndicator, Priority.ALWAYS);
		progressComponentsVBox.setSpacing(10);
		progressComponentsVBox.getChildren().addAll(progressIndicator, progressBar, progressMessageField);

		progressMessageButtonsVBox = new VBox();
		progressMessageButtonsVBox.setSpacing(20);
		progressMessageButtonsVBox.setAlignment(Pos.CENTER);
		progressMessageButtonsVBox.getChildren().add(buttonsHBox);
//		progressMessageButtonsVBox.setId("movePosition_progressButtonBox_beforeDownload");	// StyleSheet-ID

		componentsLayoutManager = new BorderPane();

		// --- Ausrichtungen

		BorderPane.setAlignment(progressComponentsVBox, Pos.CENTER);
		BorderPane.setAlignment(progressMessageButtonsVBox, Pos.CENTER);

		BorderPane.setMargin(updateMessageField, new Insets(5, 20, 20, 20));
		BorderPane.setMargin(updateDownloadDauerMessageField, new Insets(30, 30, 20, 20));

		BorderPane.setMargin(progressComponentsVBox, new Insets(-50, 10, 10, -10));
		BorderPane.setMargin(progressMessageButtonsVBox, new Insets(-10, 10, 10,
				-10));

		// --- auf STAGE festlegen
		
		componentsLayoutManager.setTop(updateMessageLayout);
		componentsLayoutManager.setCenter(progressComponentsVBox);
		componentsLayoutManager.setBottom(progressMessageButtonsVBox);

		// ------------ Corporate Logo
		
		medysLogo = new ImageView(new Image("Logo.png"));
		medysLogo.setSmooth(true); // schärfer
		medysLogo.setCache(true);  // schnelles laden
		
		HBox logoBox  = new HBox();
		logoBox.getChildren().add(medysLogo);
		logoBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		logoBox.setStyle("-fx-padding: 5 0 5 10;");
		
		sceneLayoutManager = new BorderPane();
		sceneLayoutManager.setTop(logoBox);
		sceneLayoutManager.setCenter(componentsLayoutManager);
	}
	
	private void initDownloadSceneAndStage(Stage stage)
	{	
		medFtpOps = new MedysFTPOperations(this);
		
		clientTasks = new MedysUpdateClientTasks(medFtpOps);
		
		progressBarTask = clientTasks.runProgressBarTask();
	    
		progressIndicator.progressProperty().unbind();
	    progressIndicator.progressProperty().bind(progressBarTask.progressProperty());
	    
	    initDownloadSceneLayout();
	    
		scene  = new Scene(sceneLayoutManager, 560, 440);
		
		// lege die SytelSheet-Datei fest (hier kommt sie aus dem Resource-Folder "resources"
		//
		// der "resources"-Folder wird in der RunConfiguration als "Source"-Folder angegeben !!
		
		scene.getStylesheets().clear();
		
		scene.getStylesheets().add("Styles.css");
		
		stage.setTitle("MEDYS Updates");
		
		stage.setScene(scene);
		
		stage.setResizable(false);
		
		// Scene-Object ist gesetzt, 
		// ändere wenn notwendig Properties der UI-Komponenten
		//		
		// automatischer Textumbruch 
		//
		updateMessageField.setWrappingWidth(scene.getWidth() - 60);
		updateDownloadDauerMessageField.setWrappingWidth(scene.getWidth() - 60);
		
		stage.show();
		
		// benötigen wir, da ansonsten die JFXTextLink-Objekte doppelt angeklickt werden müssen !!
		//
		stage.requestFocus();
		
		// speichere die aktuelle Scene ab
		// falls Sie anderswo verändert werden sollte
		// oder implementiert wird
		//
		this.stage = stage;
		
		// ---- JavaFX Application-Thread
		//
		// starte die Suche nach einer neuen Version und
		// die Zeitmessung für die voraussichtliche Download-Dauer
		//
		actualThread = new Thread(progressBarTask);
		
		actualThread.setDaemon(true);
		
		// Name vergeben, so daß man diesen Thread in der
		// Aktivitätsanzeige/Geräte-Manager beobachten kann
		//
		actualThread.setName("JavaMedysUpdateClientActualThread");
		
		actualThread.start();
	}
	/*
	 * definiere die Szene der Stage
	 */
	private void initSceneAndStage(Stage stage)
	{
		initComponents();
		
		if(!MedysUpdateClientUI.isStandaloneStartup)
		{
			// starte die MEDYS integrierte Online-Version
			//
			initDownloadSceneAndStage(stage);
		}
		else
		{
			// starte die standalone Lösung
			//
			initStartupStandaloneScene(stage);
		}
		
//		medFtpOps = new MedysFTPOperations(this);
//		
//		clientTasks = new MedysUpdateClientTasks(medFtpOps);
//		
//		progressBarTask = clientTasks.runProgressBarTask();
//	    
//		progressIndicator.progressProperty().unbind();
//	    progressIndicator.progressProperty().bind(progressBarTask.progressProperty());
//	    
//	    initDownloadSceneLayout();
//	    
//		scene  = new Scene(sceneLayoutManager, 560, 440);
//		
//		// lege die SytelSheet-Datei fest (hier kommt sie aus dem Resource-Folder "resources"
//		//
//		// der "resources"-Folder wird in der RunConfiguration als "Source"-Folder angegeben !!
//		
//		scene.getStylesheets().clear();
//		
//		scene.getStylesheets().add("Styles.css");
//		
//		stage.setTitle("MEDYS Updates");
//		
//		stage.setScene(scene);
//		
//		stage.setResizable(false);
//		
//		// Scene-Object ist gesetzt, 
//		// ändere wenn notwendig Properties der UI-Komponenten
//		//		
//		// automatischer Textumbruch 
//		//
//		updateMessageField.setWrappingWidth(scene.getWidth() - 60);
//		updateDownloadDauerMessageField.setWrappingWidth(scene.getWidth() - 60);
//		
//		stage.show();
//		
//		// benötigen wir, da ansonsten die JFXTextLink-Objekte doppelt angeklickt werden müssen !!
//		//
//		stage.requestFocus();
//		
//		// speichere die aktuelle Scene ab
//		// falls Sie anderswo verändert werden sollte
//		// oder implementiert wird
//		//
//		this.stage = stage;
	}
	
	/**
	 * liefert die Referenz auf den &quot;Abbruch&quot;-Button dieser
	 * Anwendung
	 * 
	 * @return Weiter-Button <b>btn_forwardButton</b>-Komponente
	 */
	public Button gibAbbruchButton()
	{
		return btn_cancelUpdate;
	}

	/**
	 * liefert die HBox für die Buttons &quot;Abbruch&quot; und &quot;Weiter&quot;
	 * 
	 * @return HBox - buttonsHBox
	 */
	public HBox gibButtonBox()
	{
		return buttonsHBox;
	}
	
	/**
	 * liefert die Referenz uf die Client-Tasks von Medys
	 * 
	 * @return MedysUpdateClientTasks - clientTasks
	 */
	public MedysUpdateClientTasks gibClientTasks()
	{
		return clientTasks;
	}
	
	/**
	 * liefert die Referenz zum BorderPane-LayoutManager
	 * f&uuml;r die Anzeige der Nachrichten des Updateprozesses oder
	 * anderen Update-Nachrichten
	 * 
	 * @return BorderPane - updateMessageLayout
	 */
	public BorderPane gibUpdateMessageLayout()
	{
		return updateMessageLayout;
	}
	
	/**
	 * liefert die Referenz zum BorderPane-LayoutManager von
	 * MedysUpdateClientUI
	 * 
	 * @return BroderPane - componentsLayoutManager
	 */
	public BorderPane gibComponentsLayoutManager()
	{
		return componentsLayoutManager;
	}
	
	/**
	 * liefert die Referenz auf die VBox-Laoyut Komponente f&uuml;r
	 * das Auslegen der ProgressBar und des ProgressIndicators
	 * von MedysUpdateClientUI
	 * 
	 * @return VBox - progressComponentsVBox
	 */
	public VBox gibProgressComponentsBox()
	{
		return progressComponentsVBox;
	}
	
	/**
	 * liefert die Referenz auf die VBox-Laoyut Komponente f&uuml;r
	 * das Auslegen der Text-Message Komponenten und Buttons
	 * von MedysUpdateClientUI
	 * 
	 * @return VBox - progressMessageButtonsVBox
	 */
	public VBox gibProgressButtonsBox()
	{
		return progressMessageButtonsVBox;
	}
	
	/**
	 * liefert die Referenz auf die ProgressBar (Verlaufsanzeige)
	 * dieser Anwendung
	 * 
	 * @return ProgressBar <b>progressBar</b>-Komponente
	 */
	public ProgressBar gibProgressBar()
	{
		return progressBar;
	}
	
	/**
	 * liefert die Referenz auf den ProgressIndicator (Verlaufsanzeige)
	 * dieser Anwendung
	 * 
	 * @return ProgressIndicator <b>progressIndicator</b>-Komponente
	 */
	public ProgressIndicator gibProgressIndicator()
	{
		return progressIndicator;
	}

	/**
	 * liefert die Referenz auf die Text-Komponente zur Anzeige des
	 * Prozessverlaufs
	 * 
	 * @return Text - progressMessageField
	 */
	public Text gibProgressMessageField() 
	{
		return progressMessageField;
	}

	/**
	 * liefert eine Referenz auf das aktuelle Stage-Objekt von
	 * MedysUpdateClientUI
	 * 
	 * @return das aktuelle Stage-Objekt - stage
	 */
	public Stage gibStage()
	{
		return stage;
	}
	
	/**
	 * liefert die Startup-Parameter (launch-arguments) dieser Anwendung
	 * 
	 * @return FTP-Server-HOSTNAME, PORT, Username, Password, laufende Medys-Version, Pfad zur Medys-Updateordner Ablage
	 */
	public static String[] gibStartparameter()
	{
		return launchArguments;	
	}
	
	/**
	 * liefert die Referenz auf die {@link JFXLabelLink} - updateAnleitungLink
	 * 
	 * @return anklickbarer javafx-Label zur Anzeige der Updateanleitung zu einer entprechenden
	 * 			Medys Update-Version
	 */
	public JFXTextLink getUpdateAnleitungLink()
	{
		return updateAnleitungLink;
	}
	
	/**
	 * liefert die Referenz auf den {@link JFXLabelLink} - updateReportLink
	 * 
	 * @return ancklickbarer javafx-Label zur Anzeige der UpdateReport_&lt;Update-Version&gt;.pdf Datei
	 */
	public JFXTextLink getUpdateReportLink()
	{
		return updateReportLink;
	}
	
	/**
	 * liefert die Referenz auf die Text-Komponente zur Anzeige
	 * der Update-Statusmeldungen
	 * 
	 * @return Text - updateMessageField
	 */
	public Text gibUpdateMessageField()
	{
		return updateMessageField;
	}
	
	/**
	 * liefert die Referenz auf die Text-Komponente zur Anzeige
	 * der Download-Dauer
	 * 
	 * @return Text - updateDownloadDauerMessage
	 */
	public Text gibUpdateDownloadDauerMessageField()
	{
		return updateDownloadDauerMessageField;
	}
	
	/**
	 * liefert die Refrenz auf die HBox, welche die Hyperlinks
	 * zu der UpdateAnleitung und der UpdateReport Datei beinhaltet
	 * 
	 * @return HBox - updateLinksVBox
	 */
	public VBox gibUpdateLinksHBox()
	{
		return updateLinksVBox;
	}
	
	/**
	 * liefert die Referenz auf den &quot;Weiter&quot;-Button dieser
	 * Anwendung
	 * 
	 * @return Weiter-Button <b>btn_forwardButton</b>-Komponente
	 */
	public Button gibWeiterButton()
	{
		return btn_forwardButton;
	}

	/**
	 * setzt eine neue &Uuml;berschrfit oder einen beschreibenden Text
	 * f&uuml;r die JFXTextLink-Komponente &quot;updateAnleitungLink&quot;
	 * 
	 * @param newLinkDescriptionOrText
	 * 			neue &Uuml;berschrfit oder einen beschreibenden Text
	 * 			f&uuml;r die JFXTextLink-Komponente 
	 */
	public void setUpdateanleitungLinkText(String newLinkDescriptionOrText)
	{
		if(!((newLinkDescriptionOrText == null) || (newLinkDescriptionOrText.isEmpty())))
		{
			updateAnleitungLink.setText(newLinkDescriptionOrText);
		}
	}
	
	/**
	 * setzt eine neue &Uuml;berschrfit oder einen beschreibenden Text
	 * f&uuml;r die JFXTextLink-Komponente &quot;updateReportLink&quot;
	 * 
	 * @param newLinkDescriptionOrText
	 * 			neue &Uuml;berschrfit oder einen beschreibenden Text
	 * 			f&uuml;r die JFXTextLink-Komponente 
	 */
	public void setUpdateReportLinkText(String newLinkDescriptionOrText)
	{
		if(!((newLinkDescriptionOrText == null) || (newLinkDescriptionOrText.isEmpty())))
		{
			updateReportLink.setText(newLinkDescriptionOrText);
		}
	}
	
	/**
	 * setzt einen neuen Textinhalt auf die interne Text-Komponente
	 * zur Anzeige des Update-Prozessverlaufsnachrichten
	 * 
	 * @param newText der neue Textinhalt
	 */
	public void setUpdateMessageFieldText(String newText)
	{
		updateMessageField.setText(newText);
	}
	
	/**
	 * setzt einen neuen Textinhalt auf die interne Text-Komponente
	 * zur Anzeige des Update-Downloaddauer
	 * 
	 * @param newText der neue Textinhalt
	 */
	public void setUpdateDownloadDauerMessageFieldText(String newText)
	{
		updateDownloadDauerMessageField.setText(newText);
	}
}
