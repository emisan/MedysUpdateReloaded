package server;

import java.util.ArrayList;
import java.util.Iterator;


public class MedysVersionsnummern {
	
	private static int version;
	
	private static int[] versionen;

	private static String updateVersionVomServer;
	
	/**
	 * zeigt Medys-Versionen
	 * 
	 * @param versionen die Medys-Versionsnummern vom FTP-Server
	 */
	public static void druckeVersionen(int[] versionen)
	{
		System.out.println("\nVersionen: \n");
		
		if(versionen != null)
		{
			for(int version : versionen)
			{
				System.out.println(version);
			}
		}
		else
		{
			System.out.println("\nKeine Versionen auf dem FTP-Server vorhanden");
		}
	}

	/**
	 * zeigt Medys-Versionen in der Konsolenausgabe
	 * 
	 * @param versionen die Medys-Versionsnummern vom FTP-Server
	 */
	public static void druckeVersionen(String[] versionen)
	{
		System.out.println("\nVersionen: \n");
		
		if(versionen != null)
		{
			for(String version : versionen)
			{
				System.out.println(version);
			}
		}
		else
		{
			System.out.println("\nKeine Versionen auf dem FTP-Server vorhanden");
		}
	}
	
	/**
	 * listet die Verzeichnissnamen auf 
	 * 
	 * @param verzeichnisstruktur die Verzeichnisnamen
	 */
	public static void druckeVerzeichnisnamen(String[] verzeichnisnamen)
	{
		System.out.println("Update-Verzeichnisse auf dem Server: \n");
		druckeVersionen(verzeichnisnamen);
	}
	
	/**
	 * liefert die zuletzt gespeicherte aktuelle Versionsnummer </br></br>
	 * 
	 * Eine Versionsnummer kann ein Quartalsupdate oder ein
	 * Zischenupdate aus einem Quartal sein. </br>
	 * 
	 * @return die aktuell, laufende Medys-Versionsnummer
	 */
	public static int gibAktuelleVersionsnummer()
	{
		return version;
	}
	
	/**
	 * liefert die Quartalsversion-Darstellung einer Update-Version <br />
	 * welche als Dezimalzahl vorliegt
	 * 
	 * <br /><br />
	 * 
	 * Die Formatierung sieht wie folgt aus <br /><br />
	 * 
	 * &Uuml;bergebener Parameter liegt in der Form &lt;4-stellige&gt;-Zahl vor <br /><br />
	 * 
	 * Dann wird daraus eine <br /><br />
	 * 
	 * <b>v</b>2-stellige-Zahl<b>_</b>2-stellige-Zahl formatiert
	 * 
	 * <br />
	 * 
	 * @param version die Update-Version, von der eine Quartalsversion formatiert werden soll
	 */
	public static String gibQuartalsversionDarstellungVon(int version)
	{
		String neueVersion = null;
		
		if(version > 0)
		{
			int quartal = version / 100;
			
			if(quartal > 0)
			{
				int quartalVersion = version - (quartal * 100);
				
				if((quartalVersion >= 50) && (quartalVersion <= 99))
				{
					neueVersion = "v" + quartal + "_" + quartalVersion;
				}
			}
		}
		
		return neueVersion;
	}
	
	/**
	 * liefert die verf&uuml;gbaren Medys-Versionen als Dezimalzahlen
	 * 
	 * @param versionen die Medys-Versionsnummern vom FTP-Server
	 * 
	 */
	public static int[] gibVersionen(String[] versionen)
	{
		int einfuegePos = 0;
		
		int[] rueckgabe = null;
		
		if(versionen != null)
		{
			rueckgabe = new int[versionen.length];
			
			for(String version : versionen)
			{
				rueckgabe[einfuegePos++] = gibVersionsnummer(version);
			}
		}
		
		return rueckgabe;
	}
	
	/**
	 * liefert die verf&uuml;gbaren Medys-Versionen
	 * </br>
	 * @param versionen die Medys-Versionsnummern vom FTP-Server
	 */
	public static String[] gibVersionen(int[] versionen)
	{
		int einfuegePos = 0;
		
		String[] rueckgabe = new String[versionen.length];
		
		for(int version : versionen)
		{
			rueckgabe[einfuegePos++] = "" + version;
		}
		
		return rueckgabe;
	}
	
	/**
	 * liefert die gesetzten Medys-Versionen
	 * 
	 * @return die gesetzten Medys-Versionen, wenn nicht leer, sonst NULL
	 */
	public static int[] gibVersionsnummern() 
	{
		return versionen;
	}

	/**
	 * liefert eine Medys-Versionsnummern <br /><br />
	 * 
	 * Die &uuml;bergebene Versionsnummer sollte im folgenden Format vorliegen <br /><br />
	 * 
	 * <code>v<2-stellige Dezimalzahl>_<2-stellige Dezimalzahl></code> <br /><br />
	 * 
	 * <b>Beispiel:</b> <br /><br />
	 * 
	 * Quartalsupdatenummer = 37 <br /><br />
	 * 
	 * Zwischenupdatenummer = 50 <br /><br />
	 * 
	 * Medys-Versionsnummer = v37_50 <br /><br />
	 * 
	 * Ergebnis dieser Methode = 3750 <br /><br />
	 * 
	 * Wird von dieser Vorgabe abgewichen, so ist das Ergebnis dieser Methode eine Dezimale-null <br />
	 * und ist somit keine <i>valide</i> Medys-Versionsnummer
	 * 
	 * @param versionsnummer die Medys-Versionsnummer
	 * @return die Versionsnummer als Dezimalzahl oder dezimale-null wenn nicht valide
	 */
	public static int gibVersionsnummer(String versionsnummer)
	{
		int version = 0;
		
		int quartalsUpdate = 0;
		
		int zwischenUpdate = 0;
		
		int posUnterzeichen = versionsnummer.length();
		
		if(versionsnummer.length() > 0)
		{
			if(versionsnummer.startsWith("v"))
			{
				posUnterzeichen = versionsnummer.indexOf('_');
				
				if(posUnterzeichen < versionsnummer.length())
				{
					quartalsUpdate = Integer.parseInt(
										versionsnummer
										.substring(1, posUnterzeichen)) * 100;
	
					zwischenUpdate = Integer.parseInt(
										versionsnummer
										.substring(
												posUnterzeichen + 1, 
												versionsnummer.length()));
					
					version = quartalsUpdate + zwischenUpdate;
				}
			}
			else
			{
				// versionsnummern liegen bereits in Deziamldarstellung,
				// 
				version = Integer.parseInt(versionsnummer);
			}
		}
		
		return version;
	}

	
	/**
	 * speichert die übergebenen Medys-Versionen als Dezimalzahlen</br>
	 * und gibt diese in einem Array zur&uuml;ck
	 * <br /><br />
	 * 
	 * Die &uuml;bergebene Versionsnummer sollte im folgenden Format vorliegen <br /><br />
	 * 
	 * <code>v<2-stellige Dezimalzahl>_<2-stellige Dezimalzahl></code> <br /><br />
	 * 
	 * <b>Beispiel:</b> <br /><br />
	 * 
	 * Quartalsupdatenummer = 37 <br /><br />
	 * 
	 * Zwischenupdatenummer = 50 <br /><br />
	 * 
	 * Medys-Versionsnummer = v37_50 <br /><br />
	 * 
	 * Ergebnis dieser Methode = 3750 <br /><br />
	 * 
	 * Wird von dieser Vorgabe abgewichen, so ist das Ergebnis dieser Methode eine Dezimale-null <br />
	 * und ist somit keine <i>valide</i> Medys-Versionsnummer
	 * 
	 * @param versionsnummern die Medys-Versionsnummern
	 * @return die Medys-Versionsnummern als Dezimalzahlen wenn sie
	 * 		   dem Format entsprechen, sonst NULL
	 */
	public static int[] gibVersionsnummern(String[] versionsnummern) 
	{
		int pos = 0;
		
		int[] versionen = null;
		
		if(versionsnummern != null)
		{
			versionen = new int[versionsnummern.length];
			
			for(String versionsnummer : versionsnummern)	
			{
				versionen[pos++] = gibVersionsnummer(versionsnummer);
			}
		}
		
		return versionen;
	}

	/**
	 * gibt diejenige Version zur&uuml;ck, welche als Update zur Verf&uuml;gung ermittelt gestellt
	 * wurde
	 * 
	 * @return die letzte, neueste Udpate-Version, sonst NULL
	 */
	public static String gibVersionZumDownload()
	{
		return updateVersionVomServer;
	}

	/**
	 * pr&uuml;ft, ob eine aktuelle Versionsnummer gesetzt wurde
	 * 
	 * @return TRUE wenn gesetzt wurde, sonst FALSE
	 */
	public static boolean hatAktuelleVersionsnummerErhalten()
	{
		return version > 0;
	}

	/**
	 * pr&uuml;ft, ob eine Update-Version zum Download ermittelt und
	 * bereit für das Herunterladen zur Verf&uuml;gung steht
	 * 
	 * @retrun TRUE wenn festgelegt wurdde, welche Version vom Server erhalten werden soll
	 * 			und eine g&uuml;tige Update-Version ist, sonst FALSE
	 */
	public static boolean hatUpdateVersionVomServerErmitteltUndGesetzt() 
	{
		/*
		 * updateVersionVomServer kann nur im Foglenden Methodenaufruf gesetzt werden
		 * 
		 * 1) ermitteln der zulässigen Update-Version vom Server
		 * 
		 *   -> MedysVersionPruefer.gibLetzteVerfuegbareVersionZu(version, versionsnummern)
		 *   
		 *   wobei "version" die laufende Medys-Version ist und "versionen"
		 *   die ermittelten, verfügbaren Updates vom Server sind (Updaet-Ordnernamen)
		 *   
		 * 2)
		 * 
		 *  die Methode "MedysVersionsnummern.setzeVersionVomServerZumDownload( version : String)
		 *  mindestens einmal aufgerufen wurde
		 *  
		 *  in allen anderen Fällen bleibt der ursprüngliche Zustand von "updateVersionVomServer"
		 *  erhalten !!!
		 *  
		 */
		return  updateVersionVomServer != null;
	}
	
	/**
	 * pr&uuml;ft, ob verf&uuml;gbare Versionsnummern gesetzt wurden
	 * 
	 * @return TRUE wenn gesetzt wurden, sonst FALSE
	 */
	public static boolean hatVerfuegbareVersionsnummernErhalten()
	{
		return (versionen != null) && (versionen.length > 0);
	}

	public static void setzeAktuelleVersionsnummer(String versionsnummer)
	{
		version = gibVersionsnummer(versionsnummer);
	}
	
	public static void setzeAktuelleVersionsnummer(int versionsnummer)
	{
		version = versionsnummer;
	}
	
	/**
	 * legt die verf&uuml;gbaren (idealerweise vom Server aus dem Medys-Updateordner ermittelten)</br>
	 * Medys-Versionsnummern fest
	 * </br> 
	 * @param versionsnummern die Medys Quartals-Update und Quartals-Zwischenupdate Versionsnummern
	 */
	public static void setzeVerfuegbareVersionsnummern(int[] versionsnummern)
	{
		versionen = versionsnummern;
	}
	
	/**
	 * legt die verf&uuml;gbaren (idealerweise vom Server aus dem Medys-Updateordner ermittelten)</br>
	 * Medys-Versionsnummern fest
	 * </br> 
	 * @param versionsnummern die Medys Quartals-Update und Quartals-Zwischenupdate Versionsnummern
	 */
	public static void setzeVerfuegbareVersionsnummern(String[] versionsnummern)
	{
		versionen = gibVersionsnummern(versionsnummern);
	}
	
	/**
	 * legt diejenige Versionsnummer fest, welche zum Download angeboten werden soll
	 * <br />
	 * @param medysUpdateVersionVomServer
	 */
	public static void setzeVersionVomServerZumDownload(String medysUpdateVersionVomServer)
	{
		updateVersionVomServer = medysUpdateVersionVomServer;
	}
	
	/**
	 * stellt die urspr&uuml;gliche Bennenung eines Updateordner-Namens
	 * her
	 * 
	 * Die Formatierung sieht wie folgt aus <br /><br />
	 * 
	 * &Uuml;bergebener Parameter leigt in der Form &lt;4-stellige&gt;-Zahl vor <br /><br />
	 * 
	 * Dann wird daraus eine <br /><br />
	 * 
	 * <b>v</b>2-stellige-Zahl<b>_</b>2-stellige-Zahl formatiert
	 * 
	 * <br />
	 * 
	 * @param aktuellerUpdateOrdnerName Update-Ordnername als Zahl
	 * @return der urspr&uuml;gliche Orndername der Update-Bezeichnung
	 */
	public static String stelleUrspruenglichenOrdnernamenWiederHer(String aktuellerUpdateOrdnerName)
	{
		return gibQuartalsversionDarstellungVon(
				gibVersionsnummer(aktuellerUpdateOrdnerName));
	}
	
	/**
	 * stellt die urspr&uuml;gliche Bennenung der Updateordner-Namen des
	 * FTP-Servers
	 * 
	 * Die Formatierung sieht wie folgt aus <br /><br />
	 * 
	 * &Uuml;bergebener Parameter leigt in der Form &lt;4-stellige&gt;-Zahl vor <br /><br />
	 * 
	 * Dann wird daraus eine <br /><br />
	 * 
	 * <b>v</b>2-stellige-Zahl<b>_</b>2-stellige-Zahl formatiert
	 * 
	 * <br />
	 * 
	 * @param Verzeichnisnamen, die als 4-stellige Zahl vorliegen
	 * @return Verzeichnisnamen, in der oben genannten Form
	 */
	public static String[] stelleUrspruenglichenOrdnernamenWiederHer(String[] verzeichnisnamen)
	{
		String[] namen = null;
		
		ArrayList<String> liste = new ArrayList<String>();
		
		if(verzeichnisnamen != null)
		{
			
			for(String name : verzeichnisnamen)
			{
				// prüfe ob auch in Dezimaldarstellungvorliegt
				//
				try
				{
					int zahl = Integer.parseInt(name);
					
					if(zahl > 0)
					{
						liste.add(stelleUrspruenglichenOrdnernamenWiederHer(name));
					}
				}
				catch(NumberFormatException wrongDigitFormat)
				{
					System.out.println(name + " liegt nicht in Dezimalzahldarstellung vor !\n");
					wrongDigitFormat.printStackTrace();
				}
			}
		}
		
		int anzahlErmittelteNamen = liste.size();
		
		if(anzahlErmittelteNamen > 0)
		{
			int pos = 0;
			
			namen = new String[anzahlErmittelteNamen];
			
			Iterator<String> listIterator = liste.iterator();
			
			while(listIterator.hasNext())
			{
				namen[pos++] = listIterator.next();
			}
		}
		return namen;
	}
}
