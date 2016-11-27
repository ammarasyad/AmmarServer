package ammar.mod.plugin.exception;

@SuppressWarnings("serial")
public class WrongUsageException extends Exception {
	
	public WrongUsageException(String s) {
		super(s);
	}
	
	public WrongUsageException() {
		super();
	}
}
