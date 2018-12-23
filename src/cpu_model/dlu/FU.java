package cpu_model.dlu;

import cpu_model.cpu.CPU;
import cpu_model.element.Data;
import cpu_model.element.DataPort;
import proving_model.Formula;
import proving_model.Justification;
import proving_model.PortDataFormula;
import proving_model.Procedure;
import proving_model.Proof;

public class FU extends DLU {
		
	private DataPort IR_IF;
	private DataPort IR_IMMU;
	private DataPort IR_ID;
	private DataPort IR_EX;
	private DataPort IR_MEM;
	private DataPort IR_DMMU1;
	private DataPort IR_DMMU2;
	private DataPort IR_WB;
	private DataPort InID1;
	private DataPort InID1_RReg;
	private DataPort OutID1;
	private DataPort InID2;
	private DataPort InID2_RReg;
	private DataPort OutID2;
	private DataPort InEX;
	private DataPort InEX_WReg;
	private DataPort InMEM;
	private DataPort InMEM_WReg;
	private DataPort InDMMU1;
	private DataPort InDMMU1_WReg;
	private DataPort InDMMU2;
	private DataPort InDMMU2_WReg;
	private DataPort InWB;
	private DataPort InWB_WReg;
	private DataPort Halt_IF;
	private DataPort Halt_IMMU;
	private DataPort Halt_ID;
	private DataPort Bub_IF;
	private DataPort Bub_IMMU;
	private DataPort Bub_ID;
	private DataPort ICacheHit;
	private DataPort DCacheHit;
	private DataPort DCacheHit2;
		
	public FU() {		
		
		this.name = "FU";
		
		IR_IF = addDataPort(this, name + ".IR_IF");
		IR_IMMU = addDataPort(this, name + ".IR_IMMU");
		IR_ID = addDataPort(this, name + ".IR_ID");
		IR_EX = addDataPort(this, name + ".IR_EX");
		IR_MEM = addDataPort(this, name + ".IR_MEM");
		IR_DMMU1 = addDataPort(this, name + ".IR_DMMU1");
		IR_DMMU2 = addDataPort(this, name + ".IR_DMMU2");
		IR_WB = addDataPort(this, name + ".IR_WB");
		InID1 = addDataPort(this, name + ".InID1");
		InID1_RReg = addDataPort(this, name + ".InID1_RReg");
		OutID1 = addDataPort(this, name + ".OutID1");
		InID2 = addDataPort(this, name + ".InID2");
		InID2_RReg = addDataPort(this, name + ".InID2_RReg");
		OutID2 = addDataPort(this, name + ".OutID2");
		InEX = addDataPort(this, name + ".InEX");
		InEX_WReg = addDataPort(this, name + ".InEX_WReg");
		InMEM = addDataPort(this, name + ".InMEM");
		InMEM_WReg = addDataPort(this, name + ".InMEM_WReg");
		InDMMU1 = addDataPort(this, name + ".InDMMU1");
		InDMMU1_WReg = addDataPort(this, name + ".InDMMU1_WReg");
		InDMMU2 = addDataPort(this, name + ".InDMMU2");
		InDMMU2_WReg = addDataPort(this, name + ".InDMMU2_WReg");
		InWB = addDataPort(this, name + ".InWB");
		InWB_WReg = addDataPort(this, name + ".InWB_WReg");
		Halt_IF = addDataPort(this, name + ".Halt_IF");
		Halt_IMMU = addDataPort(this, name + ".Halt_IMMU");
		Halt_ID = addDataPort(this, name + ".Halt_ID");
		Bub_IF = addDataPort(this, name + ".Bub_IF");
		Bub_IMMU = addDataPort(this, name + ".Bub_IMMU");
		Bub_ID = addDataPort(this, name + ".Bub_ID");
		ICacheHit = addDataPort(this, name + ".ICacheHit");
		DCacheHit = addDataPort(this, name + ".DCacheHit");
		DCacheHit = addDataPort(this, name + ".DCacheHit2");
		
	}

}
