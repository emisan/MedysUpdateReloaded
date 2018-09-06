package service;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import client.MedysUpdateClientUI;

/**
 * Klasse zum Entpacken einer verpackten Datei in ZIP-Format
 * 
 * <br /><br />
 * <b><u>Info</u></b><br /><br />
 * 
 * Um RAR-formatierte Dateien zu entpacken, sollte die Klasse {@link Unrar} genutzt werden ! 
 * 
 * <br /><br />
 * <u>Zum Verst&auml;ndnis der Zip-Ordnerstruktur</u> <br /><br />
 * 
 * Ordnerstruktur wird hierarchisch indiziert <br /><br />
 * 
 * <b>Beispiel</b> <br /><br />
 * 
 * <pre>
 *  Inhalt einer Zip-Datei 
 *  
 *  /root
 *  |
 * 	|------ Abc.pdf
 *  |
 *  |------ He-man and the Masters of the Universe.avi
 *  |
 *  |------ Hackers Guide (Ordner)
 *  |
 *  |------ hackme.txt
 *  |
 *  |------ how to hack yourself.pdf
 *  |
 *  |------ Hackers Blackbox (Ordner)
 *          |
 *          |------ HackBlack.pdf
 *  |
 *  |------ java_Tuts (ordner)
 *  		|------  SWING (Ordner)
 *  		 		 |
 *  		 		 |------ chap1 (Ordner, empty) 
 *           		 |------ chap2 (Ordner)
 *                    		 |
 *                    		 |------ SwingUtils.zip
 *                    		 |
 *                    		 |------ SwingUtils.java
 * </pre> <br /><br />
 * 
 * dann werden die Zip-Inhalte wie folgt <i>indiziert</i> <br /><br />
 * 
 * <pre>
 * /root
 * /root/Abc.pdf
 * /root/He-man and the Masters of the Universe.avi
 * /root/Hackers Guide
 * /root/hackme.txt
 * /root/how to hack yourself.pdf
 * /root/Hackers Blackbox
 * /root/Hackers Blackbox/HackBlack.pdf
 * /root/java_Tuts
 * /root/java_Tuts/SWING
 * /root/java_Tuts/SWING/chap1
 * /root/java_Tuts/SWING/chap2/SwingUtils.zip
 * /root/java_Tuts/SWING/chap2/SwingUtils.java
 * </pre> <br /><br />
 * 
 * so daß eine rekursive Abarbeitung der Verzeichnisstruktur entfallen kann :)
 * 
 * <br />
 * 
 * @author Hayri Emrah Kayaman, MEDYS GmbH Wülfrath 2016
 *
 */
public class Unzip {

	private String zipDateiOrdnername;
	
	/*
     * Puffergrösse für read/write in Dateien
     */
    private static final byte[] BUFFER = new byte[0xFFF];
    
	private File zipFile;
	
	/**
	 * Erstellt eine neue Instanz von Unzip
	 */
	public Unzip() {
	}

	/**
	 * Erstellt einen neuen Ordner f&uuml;r das extrahieren der Inhalte
	 * einer Zip/Rar-Datei.
	 * 
	 * <br /><br />
	 * 
	 * Bei der Erstellung wird auf die Existenz der Zip-Datei gepr&uuml;ft
	 * und beim Vorhandensein der jeweilige Ordnername im Programmfluß festgehalten, <br />
	 * so daß mit {@link #gibZipOrdnername()} der Ordnername abgefragt werden kann
	 * und mit {@link #gibZipDateiName()} der zip-Dateiname oder mit {@link #gibZipDatei()}
	 * die Zip-Datei selbst erfragt werden kann.
	 * 
	 * <br /><br />
	 * Ist <b>keine Zip-Datei vorhanden</b>, so wird keins der oben genannten Optionen abrufbar sein (-> liefern NULL).
	 * 
	 * @param zipDateiVerzeichnispfad die absolute Verzeichnispfadangabe, wo sich die Zip-Datei befindet
	 * @param zipDateiname der Name der Zip/Rar-Datei 
	 */
	public void erstelleZipDateiordner(String zipDateiVerzeichnispfad, String zipDateiname)
	{
		setzeZipDatei(zipDateiVerzeichnispfad, zipDateiname);
		
		if(gibZipDatei() != null)
		{
			setzeZipDateiOrdnername(gibZipDatei().getAbsolutePath());
			
			File file = new File(gibZipDatei().getParent(), gibZipOrdnername());
			
			if(!file.exists())
			{
				file.mkdir();
				
				System.out.println(gibZipOrdnername() + " im Verzeichnis " + zipDateiVerzeichnispfad + " erstellt !");
			}
			else
			{
				System.out.println(gibZipOrdnername() + " existiert bereits im Verzeichnis " + zipDateiVerzeichnispfad);
				
				Unzip.loescheOrdner(zipDateiVerzeichnispfad, gibZipOrdnername());
				
				System.out.println(gibZipOrdnername() + " wurde erfolgreich gelöscht !");
			}
		}
	}
	
	/**
	 * Liefert die aktuell eingesetzte Zip-Datei, <br />
	 * falls vorher mittels {@link #erstelleZipDateiordner(String, String)}
	 * angegeben wurde.
	 * 
	 * @return die Zip-Datei, welche entpackt werden soll wenn angegeben, sonst NULL
	 */
	public File gibZipDatei() {
		return zipFile;
	}

	/**.
	 * Liefert den Dateinamen einer festgelegten Zip-Datei
	 * 
	 * <br /><br />
	 * 
	 * Die Zip-Datei sollte vorher mittels {@link #setzeZipDatei(String, String)}
	 * festgelegt werden.
	 * 
	 * @return der Name der Zip-Datei, falls vorher festgelegt, sonst NULL
	 */
	public String gibZipDateiName() {
		return zipFile.getName();
	}

	/**
	 * Liefert den Verzeichnisnamen der festgelegten Zip-Datei.
	 * 
	 * <br /><br />
	 * 
	 * Die Zip-Datei sollte vorher mittels {@link #setzeZipDatei(String, String)}
	 * festgelegt werden.
	 * 
	 * @return der Verzeichnisname, in der sich die Zip-Datei befindet, falls vorher festgelegt,
	 * 			sonst NULL
	 */
	public String gibZipOrdnername() {
		return zipDateiOrdnername;
	}
	
	/**
	 * Legt die Zip-Datei anhand des Dateinamens fest.
	 * 
	 * <br /><br />
	 * 
	 * Falls der Dateiname <b><u>nicht existiert</u></b>, wird <b><u>keine</u></b> Zip-Datei
	 * erh&auml;tlich sein !!
	 * 
	 * @param dateiPfad 
	 * 			die <b>absolute</b> Verzeichnispfadangabe, in der sich die
	 * 			Zip-Datei befindet
	 * @param zipDateiname
	 * 			der Name der Zip-Datei, die entpackt werden soll
	 */
	public void setzeZipDatei(String dateiPfad, String zipDateiname) 
	{
		// re-definiere den absoluten Dateipfad, falls dieser nicht
		// ordentlich abschliesst 
		//
		if(!dateiPfad.toLowerCase().endsWith(File.separator))
		{
			dateiPfad = dateiPfad + File.separator;
		}
		
		File zipFile = new File(dateiPfad + zipDateiname);
	
		if (zipFile.exists()) 
		{
			this.zipFile = zipFile;
		}
	}

	/**
	 * entpackt das Zip-File in einen angegebenen Zip-Ordner eines
	 * Verzeichnisses
	 * 
	 * <br />
	 * <br />
	 * 
	 * existiert der Ordner nicht, so wird er im angegebenen Verzeichnis
	 * erstellt
	 * 
	 * <br />
	 * <br />
	 * Wurde der Konstruktor {@link #Unzip(ProgressBar, TextField)} verwendet, 
	 * so wird die entsprechende Entpack-Information an das Textfield 
	 * übermittelt und die Progressbar dem Entpack-Status entsprechend
	 * aktualisiert
	 * 
	 * <br />
	 * 
	 * @param zipDatei
	 *            die gezippte Datei
	 * @param zielVerzeichnis
	 *            Zielverzeichnis, wo der zipOrdner sich befindet oder neu
	 *            erstellt wird
	 * @param zipOrdnerName
	 *            der Name des Ordners, welcher die entpackten Inhalte der
	 *            zipDatei beinhalten soll
	 */
	public void unzip(
				final File zipDatei, 
				String zielVerzeichnis,
				String zipOrdnername)			
	{
		
		// erzeuge Zip-Ordner, falls nicht existiert
		//
		if (!zielVerzeichnis.toLowerCase().endsWith(File.separator)) 
		{
			zielVerzeichnis = zielVerzeichnis + File.separator;
		}
		
		final File zipOutputFolder = 
				new File(zielVerzeichnis + File.separator + zipOrdnername);

		if (!zipOutputFolder.exists()) 
		{
			zipOutputFolder.mkdir();
		}
		
		try
		{
			// mittels java.utils.zip.ZipFile-Variante (aktuelle routine)
			//
			ZipFile zipFile = new ZipFile(zipDatei);
			
			StringBuilder sb = new StringBuilder();
			
			sb
			.append("Das Update ")
			.append(zipDatei.getName())
			.append(" wird in den Ordner ")
			.append(zipOutputFolder)
			.append(" entpackt....");
				
			System.out.println(sb.toString());
			
			for(final ZipEntry zipInhalt : Collections.list(zipFile.entries()))
			{			
				String entpackProzessText = 
									"Entpacke zipFile : " + zipInhalt.getName();
				
				// for DEBUG ONLY
				System.out.println(entpackProzessText);
				
				// Ausnahme:
				// beim entpacken wird in MAC ein Subordner "__MACOSX" erstellt,
				// dieser muss ignoriert werden
				//
				if(!(zipInhalt.getName().toLowerCase().contains("__macosx")))
				{	
					String zielAblage =
									zipOutputFolder.getAbsolutePath() 
									+ File.separator;
					
					extrahiereZipDateiinhalt(zipInhalt, zipFile, zielAblage);
				}
			}
			
			System.out.println("Fertig entpackt");
			
			zipFile.close();
		}
		catch(IOException ioExcep)
		{
			ioExcep.printStackTrace();
		}
	}
	
	/**
	 * wie {@link #unzip(File, String, String)}, nur das man hier noch eine
	 * errechnete Startzeit >= 0 angeben kann
	 * 
	 * <br/>
	 * <br/>
	 * Wird eine Sartzeit vorgegeben, so wird in der Konsole die Entpackzeit
	 * im Minuten ausgegeben
	 * <br/> 
	 * @see {@link #unzip(File, String, String)}
	 * @param zipDatei
	 * @param zielVerzeichnis
	 * @param zipOrdnername
	 */
	public void unzip(
			File zipDatei, String zielVerzeichnis,
			String zipOrdnername, final MedysUpdateClientUI clientApplication) 
	{		
		if(clientApplication != null)
		{
			long startzeit = System.currentTimeMillis();
			
			// DEUG ONLY für die Konsolenausgabe
			System.out.println("start-zeit " + startzeit);
			long dauer = 0L;
			
			// erzeuge Zip-Ordner, falls nicht existiert
			//
			if (!zielVerzeichnis.toLowerCase().endsWith(File.separator)) {
				zielVerzeichnis = zielVerzeichnis + File.separator;
			}

			File zipOutputFolder = new File(zielVerzeichnis + File.separator
					+ zipOrdnername);

			if (!zipOutputFolder.exists()) {
				zipOutputFolder.mkdir();
			}

			try {
				// mittels java.utils.zip.ZipFile-Variante (aktuelle routine)
				//
				ZipFile zipFile = new ZipFile(zipDatei);

				int anzahlEntpackterInhalte = 0;
				
				final double maxAnzahlAnInhalten = 
						Collections.list(zipFile.entries()).size();

				StringBuilder sb = new StringBuilder();
				
				if (clientApplication != null) 
				{
					sb
					.append("Das Update ").append(zipDatei.getName())
					.append(" wird in den Ordner \"").append(zipOutputFolder)
					.append("\" entpackt....");
					
					clientApplication.setUpdateMessageFieldText(sb.toString());
				}
				
				for (final ZipEntry zipInhalt : Collections.list(zipFile.entries())) 
				{
					final String entpackProzessText = 
							"Entpacke zipFile :\n" + zipInhalt.getName();

					// DEBUG ONLY
					System.out.println(entpackProzessText);

					// Ausnahme:
					// beim entpacken wird in MAC ein Subordner "__MACOSX" erstellt,
					// dieser muss ignoriert werden
					//
					if (!(zipInhalt.getName().toLowerCase().contains("__macosx")))
					{
						String zielAblage = 
								zipOutputFolder.getAbsolutePath()
								+ File.separator;

						if (anzahlEntpackterInhalte < maxAnzahlAnInhalten)
						{	
							anzahlEntpackterInhalte++;
							
							if(clientApplication != null)
							{
								
								double percentage = anzahlEntpackterInhalte / maxAnzahlAnInhalten;
							
								// DEBUG ONLY
								System.out.println("aktueller Text in Download-Dauer-MessageField = " + clientApplication.gibProgressMessageField().getText());
								
								if(Platform.isFxApplicationThread())
								{
									clientApplication.setUpdateMessageFieldText(entpackProzessText);
									Platform.runLater(() -> clientApplication.gibProgressBar().setProgress(percentage));
								}
							}

						}
						extrahiereZipDateiinhalt(zipInhalt, zipFile, zielAblage);
					}
				}

				dauer = System.currentTimeMillis() - startzeit;
				
				
				System.out.println("end-zeit " + (dauer + startzeit));
				
				System.out.println("Fertig entpackt");
				
				zipFile.close();
				
				Platform.runLater(() -> clientApplication.gibProgressBar().setProgress(1));
			}
			catch (IOException ioExcep) 
			{
				ioExcep.printStackTrace();
			}
		}
	}
	
	/*
	 * setzt den Verzeichnisnamen, in der sich die Zip-Datei befindet
	 * 
	 * @param dateiPfad absolute Verzeichnispfadangabe zum Verzeichnis der Zip-Datei
	 */
	private void setzeZipDateiOrdnername(String zipDateipfad)
	{
		// erhalte aus zipDateipfad den Namen des Verzeichnisses
		//
		if(zipDateipfad.endsWith(File.separator))
		{
			// abschneiden falls absolute Pfadangabe mit Verzeichnistrenner endet
			
			zipDateipfad = zipDateipfad.substring(0, zipDateipfad.lastIndexOf(File.separator));
		}
		
		// letzten Verzeichnistrenner ermitteln und ab diesem Index abschneiden => der Dateiname
		//
		zipDateiOrdnername = zipDateipfad.substring(zipDateipfad.lastIndexOf(File.separator) + 1, zipDateipfad.length());
		
		zipDateiOrdnername = zipDateiOrdnername.substring(0, zipDateiOrdnername.indexOf(".zip"));
	}

	/*
	 * extrahiert ein Objekt ({@ link java.util.zip.ZipEntry}) aus einer Zip-Datei in das angegebene Zielverzeichnis
	 * 
     * @param zis 
     * 			ZipInputStream auf ein aktuell referenziertes Objekt einer geöffneten Zip-Datei
     * @param data 
     * 			Daten in Bytes die geschrieben werden sollen
     * @param bos
     * 			Byte-Ausgabeskanal verbunden zur Zieldatei
     * 
     * @throws IOException
     */
	@SuppressWarnings("unused")
    private void extrahiereZipDateiinhalt(ZipInputStream zis, byte[] data, BufferedOutputStream bos)  throws IOException 
    {
        int read = 0;
        
        int bufferSize = Unzip.BUFFER.length;
        
        if(data.length > bufferSize)
        {
        	bufferSize = data.length;
        }
        
        while ((read = zis.read(data, 0, bufferSize)) != -1) 
        {
            bos.write(data, 0, read);
        }
        bos.flush();
        bos.close();
    }

    /*
	 * extrahiert ein Objekt ({@ link java.util.zip.ZipEntry}) aus einer Zip-Datei in das angegebene Zielverzeichnis
	 * 
     * @param zipFileEntry 
     * 			die aktuell referenzierte Datei (ZipEntry) im Zip-Archive (ZipFile)
     * @param zipFile
     * 			das Zip-Archive, das die <b>zipFileEntry</b> beinhaltet
     * @param zielAblage 
     * 			wohin die Datei <b>zipFileEntry</b> abgelegt werden soll
     * 
     * @throws IOException
     */
    private void extrahiereZipDateiinhalt(ZipEntry zipFileEntry, ZipFile zipFile, String zielAblage)  throws IOException 
    {
    	if((zipFileEntry != null) && (zipFile != null))
    	{
    		File datei = new File(zielAblage, zipFileEntry.getName());
            
            if(zipFileEntry.isDirectory())
            {
            	datei.mkdirs();
            }
            else
            {
            	new File(datei.getParent()).mkdirs();
                    
    			BufferedInputStream bis = 
    					new BufferedInputStream(
    							zipFile.getInputStream(zipFileEntry));
    			
    			BufferedOutputStream bos = 
    					new BufferedOutputStream(
    							new FileOutputStream(datei));
    			    			
    			for(int read; (read = bis.read(Unzip.BUFFER)) != -1;) 
    			{
    				bos.write(Unzip.BUFFER, 0, read);
    			}
    			
    			if(bis != null)
    			{
    				bis.close();
    			}
    			if(bos != null)
    			{
    				bos.close();
    			}
            }
    	}
    }
    
    /**
     * L&ouml;scht eine Datei aus dem angegebene Verzeichnis
     * 
     * @param parentFolderPath
	 * 			absolute Verzeichnispfadangabe zum übergeordneten Verzeichnis
	 * 
	 * @param fileName
	 * 			der Name der Datei, die gelöscht werden soll
	 */
    public static void loescheDatei(String parentFolderPath, String fileName)
    {
    	File file = new File(parentFolderPath, fileName);
    	
    	if(file.exists())
    	{
    		file.delete();
    	}
    }
    
	/**
	 * L&ouml;scht einen Ordner, auch wenn der Ordner nicht leer ist (rekursives Löschen)
	 * 
	 * @param parentFolderPath
	 * 			absolute Verzeichnispfadangabe zum übergeordneten Verzeichnis
	 * @param folderName
	 * 			der Name des Verzeichnisses, das gelöscht werden soll
	 */
    public static void loescheOrdner(String parentFolderPath, String folderName)
    {
    	File file = new File(parentFolderPath, folderName);
    	
    	while(file.exists())
    	{
    		Unzip.loescheOrdner(file);
    	}
    }
    
	private static void loescheOrdner(File folder)
	{
		if(folder != null)
		{
			File[] files = folder.listFiles();
			
			if(files != null)
			{
				if(folder.listFiles().length == 0)
				{
					folder.delete();
				}
				else
				{
					for(File file : files) 
					{
						if(file.isDirectory())
						{
							if(file.listFiles().length == 0)
							{
								System.out.println("Ordner gelöscht " + file.getAbsolutePath() + File.separator + file.getName());
							
								file.delete();
							}
							else
							{
								Unzip.loescheOrdner(file);
							}
						}
						else
						{
							System.out.println("Datei gelöscht " + file.getAbsolutePath() + File.separator + file.getName());
						
							file.delete();
						}
					}
				}
			}
		}
	}
	
}
