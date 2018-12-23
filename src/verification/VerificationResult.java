package verification;

public class VerificationResult {
	public String insName;
	public boolean veriRst;
	public int formulaNum;
	public int stepNum;
	public long time;
	
	public VerificationResult(boolean b, int c, int d, long e) {
		veriRst = b;
		formulaNum = c;
		stepNum = d;
		time = e;
	}
	
	public void setName(String a) {
		insName = a;
	}
}
