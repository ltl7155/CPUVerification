package cpu_model.dlu;

import cpu_model.cpu.CPU;
import cpu_model.element.CtrlPort;
import cpu_model.element.Data;
import cpu_model.element.DataPort;
import cpu_model.element.Reg;
import proving_model.Formula;
import proving_model.Justification;
import proving_model.PortDataFormula;
import proving_model.Procedure;
import proving_model.Proof;
import proving_model.RegContentFormula;
//MIPS
///*
public class IR_IMMU extends DLU {
	
	private Reg reg;
	private DataPort In;
	private DataPort Out;
	private DataPort Out31_26;
	private DataPort Out25;
	private DataPort Out25_0;
	private DataPort Out25_6;
	private DataPort Out25_21;
	private DataPort Out24_6;
	private DataPort Out20_6;
	private DataPort Out20_16;
	private DataPort Out20_18;
	private DataPort Out16;
	private DataPort Out17;
	private DataPort Out15_0;
	private DataPort Out15_6;
	private DataPort Out15_11;
	private DataPort Out10_0;
	private DataPort Out10_3;
	private DataPort Out10_6;
	private DataPort Out5_0; 
	private DataPort Out2_0; 
	private CtrlPort Ctrl;
	
	static private String instructionForm;	
	private Justification j = null;	
		
	public IR_IMMU() {
		
		this.name = "IR_IMMU";
		
		reg = addReg(this, name);
		In = addDataPort(this, name + ".In");
		Out = addDataPort(this, name + ".Out");
		Out31_26 = addDataPort(this, name + ".Out31_26");
		Out25 = addDataPort(this, name + ".Out25");
		Out25_0 = addDataPort(this, name + ".Out25_0");
		Out25_6 = addDataPort(this, name + ".Out25_6");
		Out25_21 = addDataPort(this, name + ".Out25_21");
		Out20_18 = addDataPort(this, name + ".Out20_18");
		Out20_16 = addDataPort(this, name + ".Out20_16");
		Out24_6 = addDataPort(this, name + ".Out24_6");
		Out20_6 = addDataPort(this, name + ".Out20_6");
		Out17 = addDataPort(this, name + ".Out17");
		Out16 = addDataPort(this, name + ".Out16");
		Out15_11 = addDataPort(this, name + ".Out15_11");
		Out15_0 = addDataPort(this, name + ".Out15_0");
		Out15_6 = addDataPort(this, name + ".Out15_6");
		Out10_6 = addDataPort(this, name + ".Out10_6");
		Out10_3 = addDataPort(this, name + ".Out10_3");
		Out10_0 = addDataPort(this, name + ".Out10_0");
		Out5_0 = addDataPort(this, name + ".Out5_0");
		Out2_0 = addDataPort(this, name + ".Out2_0");
		Ctrl = addCtrlPort(this, "Ctrl" + name,  "·");
		
	}		
	
	public static void setInstructionForm(String s) {
		instructionForm = s;		
	}
	
	public void applyTheorems() {
		
		_IR_IMMU_Out();
		_IR_IMMU_Hold();
		_IR_IMMU_Write();
		
	}
		
	private void _IR_IMMU_Hold() {
		
//		IR_IMMU-Hold	
//		_[IR_IMMU]=data, CtrlIR_IMMU=0 |- [IR_IMMU]=data
		
		if (!reg.hasLastContent() || !Ctrl.notActive() || reg.hasCurContent()){
			return;
		}
		
		Procedure a = reg.getLastContent();
		Procedure b = Ctrl.getCtrlSignal();
		
		Data data = a.getData();
		
		Formula f = new RegContentFormula(reg, data);
		Justification j = new Justification("IR_IMMU-Hold", a, b);
		Proof.add(f, j);
	}
	
	private void _IR_IMMU_Write() {
		//IR_IMMU-Write	IR_IMMU.In=inst, CtrlIR_IMMU=1 |- [IR_IMMU]=inst	
		
		if (!In.hasData() || !Ctrl.isActive() || reg.hasCurContent()){
			return;
		}
		Procedure a = In.getPortData();
		Procedure b = Ctrl.getCtrlSignal();
		
		Data data = a.getData();
		
		Formula f = new RegContentFormula(reg, data);
		Justification j = new Justification("IR_IMMU-Write", a, b);
		Proof.add(f, j);		
	}
	
	private void _IR_IMMU_Out() {
		//IR_IMMU-Out
		//_[IR]={..} |- IR.Outxx=.., IR.Outxx==.., ...
		
		//寄存器里没东西
		if (!reg.hasLastContent()){
			return;
		}
		Procedure a = reg.getLastContent();
		j = new Justification("IR_IMMU-Out", a);

		//取得指令码，去掉大括号，拆分成字段，装到fields中
		String str_inst = a.getData().getName();
		generateIROut(Out, str_inst);
		str_inst = str_inst.substring(1, str_inst.length()-1);
		String[] fields = str_inst.split(",");
//						System.out.println(instructionForm + " " + fields.length);
		// R-Form
		// _[IR]={op, rs, rt, rd, shamt, func} |- 
		// IR[31:26]=op, IR[25:21]=rs, IR[20:16]=rt, IR[15:11]=rd, IR[5:0]=func
		if (instructionForm.equals("R-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_16, fields[2]);
			generateIROut(Out15_11, fields[3]);
			generateIROut(Out10_6, fields[4]);
			generateIROut(Out5_0, fields[5]);
		}
		// I-Form
		// _[IR]={op, rs, rt, imm} |- 
		// IR[31:26]=op, IR[25:21]=rs, IR[20:16]=rt, IR[15:0]=imm 
		else if (instructionForm.equals("I-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_16, fields[2]);
			generateIROut(Out15_0, fields[3]);
		}
		// J-Form
		// _[IR]={op, imm} |- 
		// IR[31:26]=op, IR[25:0]=imm
		else if (instructionForm.equals("J-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_0, fields[1]);
		}
		// B-Form
		// _[IR]={op, func1, cc, nd, tf, offset} |- 
		// IR[31:26]=op, IR[25:21]=func1, IR[20:18]=cc, IR[17]=nd, IR[16]=tf, IR[15:0]=offset
		else if (instructionForm.equals("B-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_18, fields[2]);
			generateIROut(Out17, fields[3]);
			generateIROut(Out16, fields[4]);
			generateIROut(Out15_0, fields[5]);
		}
		// M0-Form
		// _[IR]={cop0, func, rt, rd, 0, sel} |- 
		// IR[31:26]=cop0, IR[25:21]=func, IR[20:16]=rt, IR[15:11]=rd, IR[2:0]=sel
		else if (instructionForm.equals("M0-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_16, fields[2]);
			generateIROut(Out15_11, fields[3]);
			generateIROut(Out10_3, fields[4]);
			generateIROut(Out2_0, fields[5]);
		}
		// M1-Form
		// _[IR]={cop0, func, rt, rd, 0} |- 
		// IR[31:26]=cop0, IR[25:21]=func, IR[20:16]=rt, IR[15:11]=rd
		else if (instructionForm.equals("M1-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_16, fields[2]);
			generateIROut(Out15_11, fields[3]);
			generateIROut(Out10_0, fields[4]);
		}
		// M2-Form
		// _[IR]={op, rs, cc, 0, tf, rd, 0, func} |- 
		// IR[31:26]=op, IR[25:21]=rs, IR[20:18]=cc, IR[16]=tf, IR[15:11]=rd, IR[5:0]=func
		else if (instructionForm.equals("M2-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_18, fields[2]);
			generateIROut(Out17, fields[3]);
			generateIROut(Out16, fields[4]);
			generateIROut(Out15_11, fields[5]);
			generateIROut(Out10_6, fields[6]);
			generateIROut(Out5_0, fields[7]);
		}
		// M3-Form
		// _[IR]={op, rs, 0, func} |- 
		// IR[31:26]=op, IR[25:21]=rs, IR[5:0]=func
		else if (instructionForm.equals("M3-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_6, fields[2]);
			generateIROut(Out5_0, fields[3]);
		}
		// SC-Form
		// _[IR]={op, code, func} |- 
		// IR[31:26]=op, IR[25:6]=code, IR[5:0]=func
		else if (instructionForm.equals("SC-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_6, fields[1]);
			generateIROut(Out5_0, fields[2]);
		}
		// T-Form
		// _[IR]={op, rs, rt, code, func} |- 
		// IR[31:26]=op, IR[25:21]=rs, IR[20:16]=rt, IR[15:6]=code, IR[5:0]=func
		else if (instructionForm.equals("T-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_16, fields[2]);
			generateIROut(Out15_6, fields[3]);
			generateIROut(Out5_0, fields[4]);
		}
		// T2-Form
		// _[IR]={op, rs, func, imm} |- 
		// IR[31:26]=op, IR[25:21]=rs, IR[20:16]=func, IR[15:0]=imm 
		else if (instructionForm.equals("T2-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25_21, fields[1]);
			generateIROut(Out20_16, fields[2]);
			generateIROut(Out15_0, fields[3]);
		}
		// E-Form
		// _[IR]={op, co, 0, func} |- 
		// IR[31:26]=op, IR[25]=co, IR[24:6]=0, IR[5:0]=func
		else if (instructionForm.equals("E-Form")){
			generateIROut(Out31_26, fields[0]);
			generateIROut(Out25, fields[1]);
			generateIROut(Out24_6, fields[2]);
			generateIROut(Out5_0, fields[3]);
		}
	}
		
	private void generateIROut(DataPort port, String s) {
		if(port.hasData()){
			return;
		}
		Data data =  CPU.addData(s);	
		Formula f = new PortDataFormula(port, data);		
		Proof.add(f, j);
	}

}
//*/
