package others;

import java.util.ArrayList;

public class CPUPath {
	
	public String src = "";
	public String mux = "";
	public String muxCtrlName = "";
	public String des = "";
	public String opfuncStr = "";
	public String stage = "";
	public ArrayList<String> opfuncList = new ArrayList<String>();
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getMux() {
		return mux;
	}
	public void setMux(String mux) {
		this.mux = mux;
	}
	public String getMuxCtrlName() {
		return muxCtrlName;
	}
	public void setMuxCtrlName(String muxCtrlName) {
		this.muxCtrlName = muxCtrlName;
	}
	public String getDes() {
		return des;
	}
	public void setDes(String des) {
		this.des = des;
	}
	public String getOpfuncStr() {
		return opfuncStr;
	}
	public void setOpfuncStr(String opfuncStr) {
		this.opfuncStr = opfuncStr;
	}
	public String getStage() {
		return stage;
	}
	public void setStage(String stage) {
		this.stage = stage;
	}
	public void addIntoOpfuncList(String singleOpfunc){
		this.opfuncList.add(singleOpfunc);
	}
	public ArrayList<String> getOpfuncList() {
		return opfuncList;
	}
	public void setOpfuncList(ArrayList<String> opfuncList) {
		this.opfuncList = opfuncList;
	}
	
	
}
