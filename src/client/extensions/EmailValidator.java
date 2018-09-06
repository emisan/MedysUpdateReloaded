package client.extensions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Klasse zum validieren einer E-Mail Adresse
 * 
 * @author Hayri Emrah Kayaman, Medys GmbH W&uuml;lfrath 2016
 *
 */
public class EmailValidator {

	private Pattern pattern;
	private Matcher matcher;

	// regex-pattern
	//
	private static final String EMAIL_PATTERN = 
		"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
		+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	/**
	 * Erzeugt eine neue Instanz von EmailValidator
	 */
	public EmailValidator() 
	{
		// erzeuge regulären Ausdruck zum Prüfen in
		// validate(String)-Methode
		//
		pattern = Pattern.compile(EMAIL_PATTERN);
	}

	/**
	 * Validiere E-Mail Adresse mit regul&auml;ren Ausdruck
	 * 
	 * @param email
	 * 			die E-Mail Adresse
	 * @return true valid hex, false invalid hex
	 */
	public boolean validate(String email)
	{
		matcher = pattern.matcher(email);
		return matcher.matches();

	}
}