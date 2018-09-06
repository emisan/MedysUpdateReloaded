package client.extensions;
/**
 * Example of a jLabel Hyperlink and a jLabel Mailto
 */

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

//import javafx.event.EventHandler;
import javafx.scene.Cursor;
//import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;


/**
 * Diese Klasse repr&auml;sentiert eine {@link javafx.scene.control.Label}-Komponente
 * welche mit Hyperlinks versehen ist und diese &quot;<i>je nach Hyperlink-Art</i>&quot; auf dem
 * lokalen Betriebssystem aufzurufen versucht.
 * 
 * <br /><br />
 * 
 * Eine Hyperlink-Art kann folgende Syntax haben <br />
 * 
 * <br>--&gt;startet mit<b>
 * 
 * <ul>
 * 	<li>file://</li>
 *  <li>http://</li>
 *  <li>https://</li>
 *  <li>ftp://</li>
 *  <li>mailto:</li>
 * </ul>
 * 
 * siehe hierzu auch: <a href="http://stackoverflow.com/users/1600562/ibrabelware">StackOverflow-Blogseite ibrabelware</a>
 * 
 * <br />
 * @author Hayri Emrah Kayaman, Medys GmbH W&uuml;lfrath 2016
 */
public class JFXTextLink extends Text { //implements EventHandler<MouseEvent>
//{
    private EmailValidator emailValidator = new EmailValidator();
    
    private Color textColor;
    private String linkText;
    private String mailToAddress;
    private String mailToSubject;
    private URL hyperlinkURL;
    
    public JFXTextLink()
    {
    	setUnderline(true);
    	setCursor(Cursor.HAND);
    }
    
//    public JFXTextLink (String text)
//    {
//    	setText(text);
//    	setUnderline(true);
//    	
//    	cursorProperty().set(Cursor.HAND);
//    }
    
    public void setTextColor(Color color)
    {
    	textColor = color;
    	setFill(color);
    }
    
    public Color getTextColor()
    {
    	return textColor;
    }
    
    public void setHyperlinkText(String linkText)
    {
    	if(linkText != null)
    	{
    		this.linkText = linkText;
    		setText(linkText);
    	}
    }
    
    public String getHyperlinkText()
    {
    	return linkText;
    }
    
    
    public void setHyperlinkURL(URL hyperlinkURL)
    {
    	this.hyperlinkURL = hyperlinkURL;
    }
    
    public URL getHyperlinkURL()
    {
    	return hyperlinkURL;
    }
    
    public void setMailToAddress(String emailAddress)
    {
    	mailToAddress = emailAddress;
    }
    
    public String getMailToAddress()
    {
    	return mailToAddress;
    }
    
    public void setMailToSubject(String mailToSubject)
    {
    	this.mailToSubject= mailToSubject;
    }
    
    public String getMailToSubject()
    {
    	return mailToSubject;
    }
    
    /**
     * erm&ouml;glicht es f&uuml;r eine LabelLink-Instanz als Internet-Hyperlink
     * zu funktionieren
     * 
     * @param descriptionOrText &Uuml;berschrift oder beschreibender Text des Hyperlink
     * @param hyperlink ein Hyperlink-Objekt
     */
    public void doInternetHyperlink(String descriptionOrText, URL hyperlink)
    {
    	if(hyperlink != null);
    	{
    		if(descriptionOrText != null)
    		{
    			if(descriptionOrText.length() > 0)
    			{
					if (hyperlink.getPath().length() > 0)
					{
						String protocol = hyperlink.getProtocol();
						
						if(protocol.toLowerCase().equals("http")
						   || protocol.toLowerCase().equals("telnet")
						   || protocol.toLowerCase().equals("ftp")
						   || protocol.toLowerCase().equals("gopher"))
						{
							try
							{
								setText(descriptionOrText);
								
								// durchsuche das System nach einem installierten Internet-Browser
								// welche dieses URL öffnen kann
								//
			                    
							    Desktop.getDesktop().browse(hyperlink.toURI());
			                }
							catch (URISyntaxException urlExcep)
							{
								System.out.println("URLException in JFXLabelLink.doHyperlink(..)");
								urlExcep.printStackTrace();
							}
							catch(IOException ioExcep) 
							{
			                   System.out.println("IOException in JFXLabelLink.doHyperlink(..)");
			                   ioExcep.printStackTrace();
			                }
						}
					}
    			}
    		}
    	}
    }
    
    /**
     * erm&ouml;glicht es f&uuml;r eine LabelLink-Instanz als Datei-Hyperlink 
     * (auf dem lokalen System oder im lokalen Netzwerk) zu funktionieren
     * 
     * <br /><br />
     * 
     * <u><b>INFO</b></u><br />
     * Das &uuml;bergebene URL-Objekt sollte idealerweise mit &quot;file://&quot; beginnen
     * und dahinter die <b>absolute Verzeichnispfadangabe</b> bis zur Datei.
     * 
     * @param url die Verkn&uuml;pfung zu einer Datei auf dem lokalen System oder im lokalen Netzwerk
     * @param descriptionOrText &Uuml;berschrift oder beschreibender Text f&uuml;r diese Verkn&uuml;pfung (Hyperlink-Text)
     */
    public void doFileHyperlink(String descriptionOrText, URL url)
    {
    	if(url != null);
    	{
    		if(descriptionOrText != null)
    		{
    			if(descriptionOrText.length() > 0)
    			{
					if (url.getPath().length() > 0)
					{	
						if(url.getProtocol().toLowerCase().equals("file"))
						{
//							setText(descriptionOrText);
							
							File file  = new File(url.getFile());
								
							if (file.exists() && !file.isDirectory()) 
							{
								try 
								{
									Desktop.getDesktop().open(file);
								}
								catch (IOException e)
								{
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
//								// lass die Datei durch ein Standardprogramm
//								// das über das aktuelle System erreichbar wäre,
//								// öffnen
//								// wenn der mauszeiger diesen Link drückt
//								//
//								setOnMouseClicked(new EventHandler<MouseEvent>() 
//								{
//
//									@Override
//									public void handle(MouseEvent arg0) 
//									{
//										try 
//										{
//											Desktop.getDesktop().open(file);
//										}
//										catch (IOException e)
//										{
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										}
//									}
//								});
							}
							else
							{
//								setText("Keine " + descriptionOrText + " verfügbar");
								setText("");
//								setCursor(Cursor.DISAPPEAR);
							}
						}
					}
    			}
    		}
    	}
    }
    
    /**
     * erm&ouml;glicht einer LabelLink-Instanz in einer &quot;mailto&quot;-Funktion
     * zu wirken
     * 
     * @param descriptionOrText &Uuml;berschrift oder beschreibender Text des Mail-Hyperlink
     * @param mailSubject Betreff der E-Mail
     * @param mailAddress eine valide E-Mail Adresse
     */
    public void doMailto(String descriptionOrText, String mailSubject, String mailAddress)
    {
    	if(mailAddress != null)
    	{
    		if(mailAddress.startsWith("mailto:"))
    		{
    			if(emailValidator.validate(mailAddress.substring("mailto:".length(), mailAddress.length())))
    			{
    				if(mailSubject != null)
    				{
    					setHyperlinkText(descriptionOrText);
    					
    					try 
	                    {
	                    	if(mailSubject.length() > 0)
	                    	{
	                    		Desktop.getDesktop().mail(new URI("mailto:" + mailAddress + "?subject=" + mailSubject));
	                    	}
	                    	else
	                    	{
	                    		Desktop.getDesktop().mail(new URI("mailto:" + mailAddress));
	                    	}
	                    }
						catch (URISyntaxException urlExcep)
						{
							System.out.println("URISyntaxException in JFXLabelLink.doMailTo");
	                        urlExcep.printStackTrace();
						}
						catch(IOException ioExcep) 
						{
	                        System.out.println("IOException in JFXLabelLink.doMailTo");
	                        ioExcep.printStackTrace();
	                    }
    					
//    					setOnMouseEntered(new EventHandler<MouseEvent>() {
//    						
//    						@Override
//    						public void handle(MouseEvent mevt)
//    						{
//    							try 
//    		                    {
//    		                    	if(mailSubject.length() > 0)
//    		                    	{
//    		                    		Desktop.getDesktop().mail(new URI("mailto:" + mailAddress + "?subject=" + mailSubject));
//    		                    	}
//    		                    	else
//    		                    	{
//    		                    		Desktop.getDesktop().mail(new URI("mailto:" + mailAddress));
//    		                    	}
//    		                    }
//    							catch (URISyntaxException urlExcep)
//    							{
//    								System.out.println("URISyntaxException in JFXLabelLink.doMailTo");
//    		                        urlExcep.printStackTrace();
//    							}
//    							catch(IOException ioExcep) 
//    							{
//    		                        System.out.println("IOException in JFXLabelLink.doMailTo");
//    		                        ioExcep.printStackTrace();
//    		                    }
//    						}
//						});
    				}
    			}
    		}
    	}
    }

//	@Override
//	public void handle(MouseEvent arg0) 
//	{
//		String mailToAddress = getMailToAddress();
//		String mailToSubject = getMailToSubject();
//		String linkText = getHyperlinkText();
//		
//		URL link = getHyperlinkURL();
//			
//		if((link != null) && (linkText != null))
//		{
//			if(linkText.length() > 0)
//			{
//				if (link.getProtocol().toLowerCase().equals("file")) 
//				{
//					doFileHyperlink(getHyperlinkText(), getHyperlinkURL());
//				}
//			
//				if (link.getProtocol().toLowerCase().equals("http")
//					|| link.getProtocol().toLowerCase().equals("https")
//					|| link.getProtocol().toLowerCase().equals("ftp")
//					|| link.getProtocol().toLowerCase().equals("gopher")) 
//				{
//					doInternetHyperlink(getHyperlinkText(), getHyperlinkURL());
//				}
//				
//				if(link.getProtocol().toLowerCase().equals("mailto"))
//				{
//					doMailto(linkText, mailToSubject, mailToAddress);
//				}
//			}
//		}	
//	}
}