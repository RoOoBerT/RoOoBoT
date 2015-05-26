package fr.rooobert.energy.rooobot.comm;

/** List of messages sent by the bot */
public enum Messages {
	GREETINGS("Bonsoir, humains."),
	QUESTION("== QUESTION == {0}"),
	RIGHT_ANSWER("Oui {0} ! Bonne reponse : \"{1}\" ! "
			+ "Tout ceci te fait un score de {2} points !"),
	ENABLED("Ouaaiis {0} je vais spammer mes questions !"),
	DISABLED("Bon ok {0} j'arrete le spam !"),
	ANSWER("Bande de noobs ! La reponse etait \"{0}\" !"),
	KICKED("R.I.P {0}. Paix à son âme."),
	GOODBYE("Adieu, humains."),
	;
	
	// --- Constants
	
	// --- Attributes
	private final String text;
	
	// --- Methods
	private Messages(String text) {
		this.text = text;
	}
	
	/** @param params Parameters for the message  
	 * @return The text of the message */
	public String getText(String ... params) {
		String text = this.text;
		for (int i = 0; i != params.length; i++) {
			text = text.replace("{" + i + "}", params[i]);
		}
		return text;
	}
}
