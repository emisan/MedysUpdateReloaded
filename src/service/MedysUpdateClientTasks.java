package service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javafx.application.Platform;
import javafx.concurrent.Task;
import server.MedysVersionsnummern;
import client.MedysUpdateClientUI;
/**
 * Diese Klasse beinhaltet die Aufgaben (Tasks) f&uuml;r die {@link client.MedysUpdateClientUI}.
 * 
 * <br /><br />
 * 
 * Generell werden die Task f&uuml;r UI-Komponenten in JavaFX verbraucht, <br />
 * die durch ihre setOnAction()-Methoden Aufgaben durchzuf&uuml;hren haben. 
 * 
 * <br /><br />
 * 
 * Tasks k&ouml;nnen nicht nur Aktionen steuern, sondern sie k&ouml;nnen auch <br />
 * selber Prozesse einleiten, so daß diese Prozesse an UI-Komponeten <i>gebunden</i>
 * werden k&ouml;nnen (wie im Fall einer ProgressBar).
 * 
 *  <br />
 *  
 * @author Hayri Emrah Kayaman, MEDYS GmbH Wülfrath 2016
 *
 */
public class MedysUpdateClientTasks {
	
	/*
     * Puffergrösse für read/write in Dateien
     */
    private static final byte[] BUFFER = new byte[0xFFF];
    
	private static boolean updateVerfuegbar = false;
	
	protected double downloadPercentage = 0.0;
	
	private MedysFTPOperations medysFtpOps;
	
	private MedysUpdateClientUI clientApplication;
	
	public MedysUpdateClientTasks(MedysFTPOperations medysFTPOperations)
	{	
		medysFtpOps = medysFTPOperations;
		
		clientApplication = medysFtpOps.gibClientUI();
	}
	
	/**
	 * startet den Task f&uuml;r die voraussichtliche Download-Dauer Messung
	 * 
	 *<br /><br />
	 *<u><b>Voraussetzung</b></u><br /><br />
	 * 
	 * Ein erfolgreicher Login eines Benutzers in das FTP-Server-Verzeichnis, <br />
	 * wo sich die MedysUpdate-Versionen befinden, sollte vorher erfolgt sein.
	 * 
	 *
	 * @param updateVersion 
	 * 			die ermittelte Update-Version, falls vorher <i>verf&uuml;gbar</i> ermittelt wurde
	 */
	public boolean ermitteleVoraussichtlicheDownloadDauer(String updateVersion)
	{
		byte[] preDownloadAnzahlBytes = new byte[4096];
		
		boolean errechnet = false;
		
		if (updateVerfuegbar) 
		{
			if (medysFtpOps.isLoggedIn()) 
			{	
				errechnet = medysFtpOps.errechneDownloadDauer(updateVersion, preDownloadAnzahlBytes);
			}
		}
		
		return errechnet;
	}

	/**
	 * liefert die absolute Pfadangabe zu der Updateanleitung
	 * 
	 * @return Verzeichnispfad + Updateanleitung-Dateiname (mit Dateiendung)
	 */
	public String gibUpdateanleitungPfad()
	{	
		return medysFtpOps.gibDownloads().get(medysFtpOps.gibUpdateAnleitungName());
	}
	
	
	/**
	 * liefert die absolute Pfadangabe zu dem Report zu einer heruntergeladenen Updateanleitung
	 * 
	 * @return der absolute Pfad zum Report, wenn die Updateanleitung heruntergeladen wurde, sonst NULL
	 */
	public String gibUpdateReportPfad()
	{
		return medysFtpOps.gibDownloads().get(medysFtpOps.gibUpdateReportName());
	}
	
	/**
	 * liefert den Status, ob ein Update ver&uuml;gbar ist
	 * 
	 * @return TRUE wenn ver&uuml;gbar, sonst FALSE
	 */
	public boolean istUpdateVerfuegbar()
	{
		return updateVerfuegbar;
	}
	
	/*
	 * lade die entsprechende Update-Anleitung zur Version herunter
	 * und lege Sie im Zielverzeichnis ab
	 */
	private void runUpdateanleitungDownload(String updateVersion, String zielverzeichnis)
	{
		if (updateVerfuegbar) 
		{
			if (medysFtpOps.isLoggedIn()) 
			{	
				// Beispiel:
				//
				// updateVersion kommt als Dezimalzahl , wie 4080 für v40_80
				//
				// formatiere 4080 zu 40_80
				//
				// Wichtig
				// -------
				//
				// JEDE UPDATEANLEITUNG sollte wie folgt benannt sein
				//
				// <Updateanleitung><_><Zahl><Zahl><_><Zahl><Zahl><.pdf>
				//
				// für 4080 --> "Updateanleitung_40_80.pdf"
				//
				// ansonsten, kann man nicht genau spezifizieren welches
				// PDF-Dokument die Updateanleitung ist, außer man 
				// legt Standard-mäßig NUR EIN PDF in den jeweiligen
				// Update-Versionsordner neben die "update_medys.zip"-Datei
				//
				// dann muß man jedoch wiederrum zuerst das Update-Versionsverzeichnis
				// nach einer PDF-Datei suchen (vorzeitiges LOOKUP) ,was wiederrum
				// eine separate Abfrage auf dem FTP-Server zu Folge hätte
				//
				String anleitungVersion = 
						MedysVersionsnummern
						.stelleUrspruenglichenOrdnernamenWiederHer(updateVersion);
				
				anleitungVersion = anleitungVersion.replace('v', '_');
				
				String updateAnleitung = "Updateanleitung" + anleitungVersion + ".pdf";
				
				File updateanleitung = new File(zielverzeichnis, updateAnleitung);
				
				if(updateanleitung.exists())
				{
					updateanleitung.delete();
				}
				
				// die Methode DownloadFile benötigt wiederrum die ursprüngliche Versions-Darstellung
				// -> z.Bspl 4090 und NICHT v40_80
				//
				// lade erneut runter
				//
				medysFtpOps.downloadFile(updateVersion, updateAnleitung, zielverzeichnis);
				
				if(medysFtpOps.isUpdateanleitungHeruntergeladen())
				{
					medysFtpOps.setzeUpdateVersion(anleitungVersion);
				}
			}
			else
			{
				Platform.runLater(new Runnable() 
				{
					@Override
					public void run() 
					{
						medysFtpOps.sendeFehlerhafterLoginNachricht();
					}
				});
			}
		}
	}
	
	/*
	 * lade den Report zur entsprechenden Update-Anleitung einer Version herunter
	 * und lege Sie im Zielverzeichnis ab
	 */
	private void runUpdatreportDownload(String updateVersion, String zielverzeichnis)
	{
		if (updateVerfuegbar) 
		{
			if (medysFtpOps.isLoggedIn()) 
			{	
				// Beispiel:
				//
				// siehe hierzu : runUpdateanleitunDownload(...)
				
				String reportVersion = 
						MedysVersionsnummern
						.stelleUrspruenglichenOrdnernamenWiederHer(updateVersion);
				
				reportVersion = reportVersion.replace('v', '_');
				
				String report = "Updatereport" + reportVersion + ".pdf";
				
				// lösche die Datei, falls Sie vorher heruntergeladen wurde
				//
				File updatereport = new File(zielverzeichnis, report);
				
				if(updatereport.exists())
				{
					updatereport.delete();
				}
				
				// die Methode DownloadFile benötigt wiederrum die ursprüngliche Versions-Darstellung
				// -> z.Bspl 4090 und NICHT v40_80
				//
				// lade erneut runter
				//
				medysFtpOps.downloadFile(updateVersion, report, zielverzeichnis);
				
				if(medysFtpOps.isUpdateReportHeruntergeladen())
				{
					medysFtpOps.setzeUpdateReportVersion(reportVersion);
				}
			}
			else
			{
				Platform.runLater(new Runnable() 
				{
					@Override
					public void run() 
					{
						medysFtpOps.sendeFehlerhafterLoginNachricht();
					}
				});
			}
		}
	}
	
	
	/**
	 * Task f&uuml;r den Downlaod-Prozess der ermittelten Update-Version
	 * 
	 * @param updateVersion 
	 *			die ermittelte Update-Version, falls vorher <i>verf&uuml;gbar</i> ermittelt wurde
	 */
	public void runDownloadTask(String updateVersion)
	{
		if (updateVerfuegbar) 
		{
			if (medysFtpOps.isLoggedIn()) 
			{	
				medysFtpOps.downloadUpdate(updateVersion);
			}
			else
			{
				Platform.runLater(new Runnable() 
				{
					@Override
					public void run() 
					{
						medysFtpOps.sendeFehlerhafterLoginNachricht();
					}
				});
			}
		}
	}

	/**
	 * Dieser Task dient nur zum hochz&auml;hlen des aktuellen
	 * Prozessverlaufs für den ProgressIndicator beim Programmstart
	 * und für den Start der Suche nach neuen Updates
	 * 
	 * @return der Task, zum <i>binden</i> an eine UI-Komponente
	 */
	public final Task<Void> runProgressBarTask()
	{	
		Task<Void> task = new Task<Void>() 
		{
		    @Override protected Void call() throws Exception 
		    {	
		    	if(clientApplication != null)
		    	{
					Task<Void> searchVersion = runSearchForNewUpdatesTask(MedysUpdateClientUI.gibStartparameter());
					
					Thread searcher = new Thread(searchVersion);

					searcher.setName("JavaMedysUpdateSearcher");
					
					searcher.setDaemon(true);

					searcher.start();
					
					searcher.join();
					
					System.out.println("raus aus runSearchForNewUpdatesTask");
					
					System.out.println("searchVersion-task in runProgressBarTask Status=done");
					
					updateProgress(100, 100);
					
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() 
						{
							clientApplication.gibProgressIndicator().setVisible(false);

							String info = "";
							String nextUpdateVersion = "";

							if(medysFtpOps.isFTPVerbunden())
							{
								if(medysFtpOps.isLoggedIn())
								{
									if (updateVerfuegbar) 
									{
										info = "Ein neues Update steht zur Verfügung: ";

										nextUpdateVersion = 
												MedysVersionsnummern
												.stelleUrspruenglichenOrdnernamenWiederHer(MedysVersionsnummern
														.gibVersionZumDownload());

										System.out.println("nächstes Update "
														+ nextUpdateVersion
														+ " ermittelt in Task : runProgressBarTask()");

										MedysVersionsnummern.setzeVersionVomServerZumDownload(nextUpdateVersion);
										
										boolean downloadDauerErmittlet = ermitteleVoraussichtlicheDownloadDauer(nextUpdateVersion);
										
										if(downloadDauerErmittlet)
										{
											if (nextUpdateVersion.length() >= 6) 
											{	
												clientApplication
												.setUpdateDownloadDauerMessageFieldText(
														medysFtpOps.gibVoraussichtlicheDownloadDauerAnzeige()
														+ "\n\n\n"
														+ "Bitte drücken Sie auf WEITER, um den Download zu starten !");
												
												if(medysFtpOps.isUpdateanleitungHeruntergeladen())
												{
													clientApplication
													.setUpdateanleitungLinkText(medysFtpOps.gibUpdateAnleitungName());
												}
												if(medysFtpOps.isUpdateReportHeruntergeladen())
												{
													clientApplication
													.setUpdateReportLinkText(medysFtpOps.gibUpdateReportName());
												}
												
												if(medysFtpOps.isUpdateanleitungHeruntergeladen() || medysFtpOps.isUpdateReportHeruntergeladen())
												{	
													clientApplication.gibUpdateLinksHBox().setVisible(true);
													
													clientApplication
													.setUpdateMessageFieldText(
															info
															+ "Update-Version \"" + nextUpdateVersion + "\"\n\n"
															+ "Anleitungen oder Reports (anklicken für die Vorschau)");
												}
												else
												{	
													clientApplication
													.setUpdateMessageFieldText(
															info
															+ "Update-Version \"" + nextUpdateVersion + "\"\n\n"
															+ "(Anleitungen oder Reports liegen zu diesem Update nicht vor)");
													
													clientApplication.gibUpdateLinksHBox().getChildren().remove(
															clientApplication.getUpdateAnleitungLink());
													
													clientApplication.gibUpdateLinksHBox().getChildren().remove(
															clientApplication.getUpdateReportLink());
													
													clientApplication.gibUpdateMessageLayout().getChildren().remove(
															clientApplication.gibUpdateDownloadDauerMessageField());
												}
												clientApplication.gibWeiterButton().setDisable(false);
												
												// reconnect durchführen
												//
												// diesmal brauchen wir es um später die "update_medys.zip"
												// aus dem currentWorkingDirectory herunterzuladen
												//
												medysFtpOps.reConnectAndLogin(
														medysFtpOps.gibHostname(), 
														medysFtpOps.gibVerbindungsport(), 
														medysFtpOps.gibUsername(), 
														medysFtpOps.gibPassword());
											}
										}
									}
//									else
//									{
//										medysFtpOps.sendeFehlendeUpdateDateiNachricht();
//									}
								}
//								else
//								{
//									medysFtpOps.sendeFehlerhafterLoginNachricht();
//								}
							}
//							else
//							{
//								medysFtpOps.sendeFtpServerInternetverbindungsfehlerNachricht();
//							}
						}
					});
				}
				
		    	return null;
		    }
		};
		
		return task;
	}

	/**
	 * Task, das nach Quartals- und Zwischenupdates auf dem Server f&uuml;r
	 * eine laufende Medys-Versionsnummer sucht
	 * 
	 * <br />
	 * 
	 * @param args
	 *            Parameter für die FTP-Serververbindung und Benutzerauthentifizierung, sowie 
	 *            die Angabe über die aktuell installierte (laufende) Medys-Version
	 * @return der Task, zum <i>binden</i> an eine UI-Komponente
	 */
	public final Task<Void> runSearchForNewUpdatesTask(final String[] args)
	{
		Task<Void> task = new Task<Void>()
		{		
			@Override
			protected Void call() throws Exception
			{	
				//
				// WICHTIG:
				//
				// Aufruf der Methode MedysFTPOperations.connect() 
				// enthält Benachrichtigung für eine fehlende Internetverbindung
				//
				// da wir im laufenden TASK, bzw. Thread sind, muss
				// die Benachrichtigung aus der entfernt laufenden Klasse
				// MedysFTPOperations heraus erfolgen :)
				//
				
				// for DEBUG ONLY
				//
				System.out.println("\n\nVerbindungsparameter\n\nFTP-Server " + args[0]
						+ "\nPort " + args[1]
						+ "\nUser " + args[2]
						+ "\nPassword " + args[3]
						+ "\nMedys-Verrsion " + args[4]
						+ "\nZielverzeichnis " + args[5] + "\n\n"
						);
				
				medysFtpOps.connect(args[0], args[1], args[2], args[3]);
				
				if(medysFtpOps.isFTPVerbunden())
				{
					if (medysFtpOps.isLoggedIn()) 
					{
						String version = medysFtpOps.erhalteLetzteUpdateVersionenVomServer(args[4]);

						System.out.println(MedysVersionPruefer.zeigMedysUpdates());
						
						if ((version.length() > 0) && (!version.equalsIgnoreCase(args[4])))
						{
							System.out.println(MedysVersionPruefer.zeigMedysUpdates());
							
							updateVerfuegbar = true;

							System.out.println("setze Version vom Server zum Download in Task : runSearchForNewUpdatesTask");

							MedysVersionsnummern.setzeVersionVomServerZumDownload(version);

							System.out.println("Versuche Updateanleitung herunterzuladen");

							long time = System.currentTimeMillis();

							// param: Versionsordner auf FTP-Server,
							// Downlaod-Zielverzeichnis (lokaler Rechner)
							//
							runUpdateanleitungDownload(
									MedysVersionsnummern
									.gibQuartalsversionDarstellungVon(
											Integer.parseInt(version)), args[5]);

							time = System.currentTimeMillis() - time;

							System.out.println("Updateanleitung heruntergeladen in "
												+ time + " Millisek.");

							time = System.currentTimeMillis();

							runUpdatreportDownload(
									MedysVersionsnummern.gibQuartalsversionDarstellungVon(
											Integer.parseInt(version)), args[5]);

							time = System.currentTimeMillis() - time;

							System.out.println("Report heruntergeladen in "
												+ time + " Millisek.");
						}
						else
						{
							updateVerfuegbar = false;

							// außerhalb des JavaFX-Application Threads
							//
							Platform.runLater(new Runnable() 
							{
								@Override
								public void run() 
								{
									medysFtpOps.sendeFehlendeUpdateDateiNachricht();
								}
							});
						}
					}
					else
					{
						// außerhalb des JavaFX-Application Threads
						//
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								medysFtpOps.sendeFehlerhafterLoginNachricht();
							}
						});
					}
				}
				else
				{
					// außerhalb des JavaFX-Application Threads
					//
					Platform.runLater(new Runnable()
					{
						@Override
						public void run()
						{
							medysFtpOps.sendeFtpServerVerbindungsAbbruchNachricht();
						}
					});
				}
				
				return null;
			}
		};
		
		return task;
	}
	
	/**
	 * Task zum Entpacken der heruntergeladenen Update-Version (aktuell: im Zip-Format)
	 * 
	 * @param downloadVerzeichnis
	 * 			die absolute Verzeichnispfadangabe zum jeweiligen Verzeichnis,
	 * 			wo die Update-Version heruntergeladen wurde
	 * 
	 * @param heruntergeladeneDatei
	 * 			die Update-Version im ZIP-Format 
	 * @return
	 */
	public final Task<Double> runUnzipTask(
			final String downloadVerzeichnis,
			final String heruntergeladeneDatei) 
	{
		final Task<Double> task = new Task<Double>() 
		{
			@Override
			public Double call() 
			{
				double percentage = 0.0;

				long time = System.currentTimeMillis();

				System.out.println("start-zeit " + time);

				long dauer = 0L;

				updateMessage("Das Update " + heruntergeladeneDatei
						+ " wird in den Ordner " + downloadVerzeichnis
						+ " entpackt....");
				try 
				{
					File downloadedZipFile = new File(downloadVerzeichnis,
							heruntergeladeneDatei);

					if (downloadedZipFile.exists()) 
					{
						File zipOutputFolder = new File(downloadVerzeichnis
								+ File.separator + "update_medys");

						if (!zipOutputFolder.exists()) {
							zipOutputFolder.mkdir();
						}

						ZipFile zipFile = new ZipFile(downloadedZipFile);

						int anzahlEntpackterInhalte = 0;

						int maxAnzahlAnInhalten = Collections.list(
								zipFile.entries()).size();

						for (final ZipEntry zipInhalt : Collections.list(zipFile.entries())) 
						{
							final String entpackProzessText = "Entpacke zipFile :\n"
									+ zipInhalt.getName();

							// DEBUG ONLY
							System.out.println(entpackProzessText);

							// Ausnahme:
							// beim entpacken wird in MAC ein Subordner
							// "__MACOSX" erstellt,
							// dieser muss ignoriert werden
							//
							if (!(zipInhalt.getName().toLowerCase().contains("__macosx"))) 
							{
								if (anzahlEntpackterInhalte < maxAnzahlAnInhalten) 
								{
									anzahlEntpackterInhalte++;

									percentage = anzahlEntpackterInhalte
											/ maxAnzahlAnInhalten;

									updateMessage(entpackProzessText);
									updateProgress(anzahlEntpackterInhalte,
											maxAnzahlAnInhalten);
								}

								File datei = new File(zipOutputFolder,
										zipInhalt.getName());

								if (zipInhalt.isDirectory()) 
								{
									datei.mkdirs();
								}
								else 
								{
									new File(datei.getParent()).mkdirs();

									BufferedInputStream bis = new BufferedInputStream(
											zipFile.getInputStream(zipInhalt));

									BufferedOutputStream bos = new BufferedOutputStream(
											new FileOutputStream(datei));

									for (int read; (read = bis.read(BUFFER)) != -1;) 
									{
										bos.write(BUFFER, 0, read);
									}

									if (bis != null) 
									{
										bis.close();
									}
									if (bos != null) 
									{
										bos.close();

									}
								}
							}
						} // END FOR

						zipFile.close();
					}
					else
					{
						updateMessage("Kann die Datei \""
								+ downloadedZipFile.getName() + "\" im Ordner "
								+ downloadVerzeichnis + " nicht finden!");
					}

					dauer = System.currentTimeMillis() - time;

					System.out.println("end-zeit " + (dauer + time));

					System.out.println("entpackt in "
							+ (float) (dauer / 60.0f * 60) + " Minuten ");

					File downloadFile = new File(downloadVerzeichnis,
							"update_medys.zip");

					File downloadVerz = new File(downloadVerzeichnis,
							"update_medys");

					if (downloadFile.exists() && downloadVerz.exists())
					{
						if ((downloadVerz.length() > 0)
								&& (downloadFile.length() > 0)) 
						{
							// Update-Datei heruntergeladen und entpackt
							//
							// setze Info in "update_medys.txt" im
							// Medys-Zusatzordner
							// "med_zusatz/med_update" für die weitere
							// Abarbeitung
							// in MEDYS
							//
							String medUpdateOrdner = MedysUpdateClientUI
									.gibStartparameter()[5];

							// prüfe ob Verzeichnisangabe korrekt ist
							// (existiert)
							//
							File updateOrdner = new File(medUpdateOrdner);

							if (updateOrdner.isDirectory()) 
							{
								if (!medUpdateOrdner.endsWith(File.separator)) 
								{
									medUpdateOrdner += File.separator;
								}

								// erhalte die Ausgabedatei für das setzen der
								// Update-Information
								//
								String medUpdateTxtDatei = MedysUpdateClientUI
										.gibStartparameter()[6];

								if ((medUpdateTxtDatei != null)
										&& (medUpdateTxtDatei.toLowerCase()
												.endsWith(".txt"))) 
								{
									File file = new File(medUpdateOrdner
											+ medUpdateTxtDatei);

									if (file.exists()) 
									{
										file.delete();
									}

									FileWriter fw = new FileWriter(file);

									fw.write("UPDATE_ENTPACKT,"
											+ MedysVersionsnummern
													.gibVersionZumDownload());

									fw.close();
								}
							}
						}
					}
				} 
				catch (IOException ioExcep) 
				{
					ioExcep.printStackTrace();
				}

				Platform.runLater(new Runnable()
				{
					public void run() 
					{
						clientApplication.gibAbbruchButton().setText("Schliessen");

						// zeige die Updateanleitung an
						//
						clientApplication.gibUpdateLinksHBox().setVisible(true);

						clientApplication
								.gibComponentsLayoutManager()
								.getChildren()
								.remove(clientApplication.gibProgressComponentsBox());
						
						clientApplication.setUpdateMessageFieldText(heruntergeladeneDatei + " entpackt und fertig zur Installation"
						+ "\n\nDrücken Sie bitte auf \"Schliessen\", um das Update wie in der Updatenleitung beschrieben zu installieren !");
					}
				});

				return percentage;
			}
		};
		return task;
	}
}
