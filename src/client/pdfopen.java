package client;
import java.io.File;

public class pdfopen  
{ 
	public static void main(String args[]) {
		
		String pdfDatei =
				System.getProperty("user.home")
				+ File.separator 
				+ "Desktop"
				+ File.separator 
				+ "pdfs" 
				+ File.separator
				+ "Updateanleitung 39_70.pdf";
		try 
		{ 
			String betriebssystem = System.getProperty("os.name").toLowerCase();
			
			if(betriebssystem.contains("win"))
			{
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + pdfDatei);
			}
			if(betriebssystem.contains("mac"))
			{
				Runtime.getRuntime().exec(new String[]{"/usr/bin/open", pdfDatei});
			}
		} 
		catch (Exception e)
		{ 
			System.out.println("Error" + e ); 
		}
	}
}