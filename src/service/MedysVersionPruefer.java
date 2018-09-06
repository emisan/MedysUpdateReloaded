package service;

import server.MedysVersionsnummern;

public class MedysVersionPruefer {
	
	private static int anzahlZwischenupdates = 0;
	private static int quartalsUeberprungVersion = 0;
	private static int verfuegbareQuartalsUpdate = 0;
	private static int verfuegbaresZwischenUpdate = 0;
	
	/**
	 * prüft, ob es ein verf&uuml;gbares Quartal- oder Zwischenupdate zu
	 * der laufenden (gesetzten) Medys-Versionsnummer gibt
	 * 
	 *  @return TRUE wenn kein Quartals&uuml;bersprung stattgefunden hat und
	 *  		g&uuml;ltige Update-Versionen vorher ermittelt wurden, sonst FALSE
	 */
	public static boolean hatVerfuegbaresUpdate()
	{
		return (quartalsUeberprungVersion == 0) 
				&& 
				( (verfuegbareQuartalsUpdate != 0) || (verfuegbaresZwischenUpdate != 0) );
	}
	
	/**
	 * speichert das Quartalsupdate zu einer aktuellen Medys-Version aus einer
	 * Hand von verf&uuml;gbaren Medys-Versionen, </br>
	 * so daß die Version mit der Methode <br /><br />
	 * 
	 *<i><code>MedysVersionsnummern.gibQuartalsversionDarstellungVon(MedysVersionsPruefer.gibVerfuegbaresUpdate())</code></i>
	 * 
	 * <br /><br/>
	 * 
	 * angezeigt werden kann. <br />
	 * 
	 * @param aktuelleVersion 
	 * 			die aktuelle Medys-Versionsnummer, die verwendet wird
	 * 
	 * @param versionen
	 * 			die verf&uuml;gbaren Medys-Versionen
	 */
	/**
	public static void erfasseQuartalsupdateVersionZuAus(
			int aktuelleVersion,
			int[] versionen) 
	{

		int aktVersNum = aktuelleVersion / 100;

		if (aktVersNum > 0) 
		{
			// aktuelle Zwischenupdate Versionsnummer
			//
			int aktZW = aktuelleVersion	- (aktVersNum * 100);

			// bspl.: (bis hier)
			//
			// aktuelleVersion = 3750
			//
			// dann ist
			//
			// aktuelleVersionsnummer = 37
			//
			// und der REST von aktuelleVersion (=50) wird in
			// aktuelleZwischenupdateVersionsnummer gespeichert
			//
			// ein Zwischenupdate liegt regulär dann vor,
			// wenn die letzte Zahl modulo 10 einen Restwert hat
			//
			// ist der Restwert eine 0, dann ist die aktuelle Version
			// ein Quartalsupdate
			
			if (versionen != null) 
			{
				for (int version : versionen) 
				{
					if ((version > 0) && (version >= aktuelleVersion)) 
					{
						int nextVersNum = version / 100;

						if (aktVersNum <= nextVersNum) 
						{
							int differenz = nextVersNum
									- aktVersNum;

							// kein Quartal überspringen, schrittweite bleibt
							// erhalten
							//
							if ((differenz >= 0) && (differenz < 2)) 
							{
								// naechste Zwischenupdate Versionsnummer
								//
								int nextZW = version - (nextVersNum * 100);

								if ((nextZW > 50)	&& (nextZW <= 99)) 
								{
									// Fall: 31_80 bis 32_90
									//
									if (aktZW < nextZW) 
									{
										// ist nächste Versionsnummer EIN
										// QUARTALSUPDATE
										//
										if ((nextZW % 10) == 0) 
										{
											if (aktZW < nextZW) 
											{
												differenz = (nextZW - aktZW) / 10;

												// kein Quartal übersprungen
												//
												if (differenz < 2) 
												{
													// Quartalsupdate verfügbar,
													// sofort
													// anbieten, wenn es keine
													// vorherigen
													// Zwischenupdates gibt
													//
													 verfuegbareQuartalsUpdate
													 = version;

													if (anzahlZwischenupdates == 0) 
													{
														verfuegbareQuartalsUpdate = version;
														break;
													}

													break;
												}
												else 
												{
													if (aktZW < nextZW) 
													{
														differenz = (nextZW - aktZW) / 10;

														// kein Quartal
														// übersprungen
														//
														if (differenz < 2) 
														{
															// Quartalsupdate
															// verfügbar, sofort
															// anbieten, wenn es
															// keine vorherigen
															// Zwischenupdates
															// gibt
															//
															 verfuegbareQuartalsUpdate
															 = version;

															if (anzahlZwischenupdates == 0) 
															{
																verfuegbareQuartalsUpdate = version;
																break;
															}

															break;
														}
//														// Quartal
//														// übersprüngen,
//														// merke dieses
//														// Quartalsupdate
//														//
														quartalsUeberprungVersion = version;
						
														break;
													}
													quartalsUeberprungVersion = version;
						
													break;
												}
											}
										}
										else 
										{
											// Zwischenupdate verfügbar, merke
											// stets
											// letztes Update und zähle Anzahl
											// hoch
											//
											verfuegbaresZwischenUpdate = version;
											anzahlZwischenupdates++;
										}
									}
								}
								if(nextZW == 50)
								{
									verfuegbareQuartalsUpdate = version;
									
									break;
								}
							}
							else
							{
								// Quartal
								// übersprüngen,
								// merke dieses
								// Quartalsupdate
								//
								quartalsUeberprungVersion = version;

								break;
							}
						}
					}
				}
			}
		}
	}
	*/
	public static void erfasseQuartalsupdateVersionZuAus(
			int aktuelleVersion,
			int[] versionen) 
	{

		int aktVersNum = aktuelleVersion / 100;

		if (aktVersNum > 0) 
		{
			// aktuelle Zwischenupdate Versionsnummer
			//
			int aktZW = aktuelleVersion	- (aktVersNum * 100);

			// bspl.: (bis hier)
			//
			// aktuelleVersion = 3750
			//
			// dann ist
			//
			// aktuelleVersionsnummer = 37
			//
			// und der REST von aktuelleVersion (=50) wird in
			// aktuelleZwischenupdateVersionsnummer gespeichert
			//
			// ein Zwischenupdate liegt regulär dann vor,
			// wenn die letzte Zahl modulo 10 einen Restwert hat
			//
			// ist der Restwert eine 0, dann ist die aktuelle Version
			// ein Quartalsupdate
			
			if (versionen != null) 
			{
				for (int version : versionen) 
				{
					if ((version > 0) && (version >= aktuelleVersion)) 
					{
						int nextVersNum = version / 100;

						if (aktVersNum <= nextVersNum) 
						{
							int differenz = nextVersNum
									- aktVersNum;

							// kein Quartal überspringen, schrittweite bleibt
							// erhalten
							//
							if ((differenz >= 0) && (differenz < 2)) 
							{
								// naechste Zwischenupdate Versionsnummer
								//
								int nextZW = version - (nextVersNum * 100);

								if ((nextZW > 50)	&& (nextZW <= 99)) 
								{
									// Fall: 31_80 bis 32_90
									//
									if (aktZW < nextZW) 
									{
										// ist nächste Versionsnummer EIN
										// QUARTALSUPDATE
										//
										if ((nextZW % 10) == 0) 
										{
											if (aktZW < nextZW) 
											{
												differenz = (nextZW - aktZW) / 10;

												// kein Quartal übersprungen
												//
												if (differenz < 2) 
												{
													// Quartalsupdate verfügbar,
													// sofort
													// anbieten, wenn es keine
													// vorherigen
													// Zwischenupdates gibt
													//
													 verfuegbareQuartalsUpdate
													 = version;

													if (anzahlZwischenupdates == 0) 
													{
														verfuegbareQuartalsUpdate = version;

														break;
													}
												}
												else 
												{
													quartalsUeberprungVersion = version;
													
													break;
												}
											}
										}
										else 
										{
											// Zwischenupdate verfügbar, merke
											// stets
											// letztes Update und zähle Anzahl
											// hoch
											//
											verfuegbaresZwischenUpdate = version;
											anzahlZwischenupdates++;
											break;
										}
									}
								}
								if((nextZW == 50)&&(nextVersNum>aktVersNum))
								{
									verfuegbareQuartalsUpdate = version;
									
									break;
								}
							}
							else
							{
								// Quartal
								// übersprungen,
								// merke dieses
								// Quartalsupdate
								//
								quartalsUeberprungVersion = version;

								break;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * liefert das n&auml;chste, verf&uuml;gbare Medys-Update <br />
	 * zu einer laufenden, aktuell installierten Medys-Version an
	 * 
	 * @return wenn vorhanden, das n&auml;chste Medys-Update vom Medys FTP-Server, sonst NULL
	 */
	public static String zeigMedysUpdates()
	{
		String ausgabe = null;
		
		int version = 0;
		
		if(MedysVersionsnummern.hatAktuelleVersionsnummerErhalten())
		{
			version = MedysVersionsnummern.gibAktuelleVersionsnummer();
		
			if(version > 0)
			{
				if(MedysVersionsnummern.hatVerfuegbareVersionsnummernErhalten())
				{
					// for DEBUG ONLY
//					int[] versionen = MedysVersionsnummern.gibVersionsnummern();
					
//					erfasseQuartalsupdateVersionZuAus(version, versionen);
					
					System.out.println();
					
					if (quartalsUeberprungVersion > 0) 
					{
						ausgabe = "Ihre Medys-Version " + version + " ist abgelaufen !!\n"
								+ "\nSie dürfen nur noch Zwischenupdates herunterladen !\n\n"
								+ "\nVerfügbare Zwischenupdates zu dieser Medys-Version: ";
						
					
						ausgabe = ausgabe + 
								(verfuegbaresZwischenUpdate > 0 
								 ? gibVerfuegbaresUpdate()
								 : "keine vorhanden\n\n"
								 	+ "Sie benutzen bereits die letzte mögliche Version "
								 	+ "für Ihren Lizenzvertrag !");
					}
					else 
					{
						if ((verfuegbareQuartalsUpdate == 0)
								&& (verfuegbaresZwischenUpdate == 0)) 
						{
							ausgabe = "Ihre Medys-Version ist bereits auf dem neuesten Stand.\n"
									+ "Es liegen keine weitere Updates vor.";
						}
						else
						{
							ausgabe = (verfuegbareQuartalsUpdate > 0 
									
										? ("Quartalsupdate zu "
												+ version + " : " + 
												MedysVersionsnummern
												.gibQuartalsversionDarstellungVon(verfuegbareQuartalsUpdate))
												
										: ("Quartalsupdate zu " + version + " : keins vorhanden"));
						
							ausgabe = ausgabe + "\n\n" 
									  + (verfuegbaresZwischenUpdate > 0 
											  
										? ("Zwischenupdate zu "
											+ version + " : " + 
											MedysVersionsnummern
											.gibQuartalsversionDarstellungVon(verfuegbaresZwischenUpdate))
											
										: ("Zwischenupdate zu " + version + " : keins vorhanden"));
							
							// für Produktivsystem relevante Information
							//
							String update = MedysVersionsnummern
											.gibQuartalsversionDarstellungVon(gibVerfuegbaresUpdate());

							if (update != null)
							{
								ausgabe = ausgabe
										+ "\n\nverfügbares Update zum download: "
										+ update;
							}
						}
					}
					
					// for DEBUG ONLY
//					System.out.println(ausgabe);
				}
			}
		}
		
		return ausgabe;
	}
	
	protected static int gibVerfuegbaresUpdate()
	{	
		return verfuegbaresZwischenUpdate > 0 
				? verfuegbaresZwischenUpdate 
				  : verfuegbareQuartalsUpdate > 0 
				    ? verfuegbareQuartalsUpdate 
				: 0;
	}
}
