package io.branch.referral;

/**
 * Returns a general error if the server back-end is down.
 */
public class BranchError {
	
	/**
	 * Returns the message explaining the error.
	 * 
	 * @return A {@link String} value that can be used in error logging or for dialog display to the user.
	 */
	public String getMessage() {
		return "Trouble communicating with server. Please try again";
	}
	
	/**
	 * Overridden toString method for this object; returns the error message rather than the object's address.
	 * 
	 * @return A {@link String} value representing the object's current state.
	 */
	@Override
	public String toString() {
		return getMessage();
	}
}
