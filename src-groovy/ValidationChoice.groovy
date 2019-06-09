enum ValidationChoice {
	VALID("Valid"), INVALID("Invalid"), REFUSED("Refused")
	
	private String choice
	
	def ValidationChoice(String choice) {
		this.choice = choice	
	}
	
	@Override
	public String toString() {
		choice
	}
	
}