package io.branch.referral;

public class BranchError {
	public String getMessage() {
		return "Trouble communicating with server. Please try again";
	}
	
	@Override
	public String toString() {
		return getMessage();
	}
}
