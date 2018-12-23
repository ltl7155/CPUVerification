package verification;

import java.util.ArrayList;
import java.util.Scanner;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import cpu_model.cpu.CPU;
import cpu_model.element.CtrlPort;
import cpu_model.element.Data;
import cpu_model.element.DataPort;
import others.AddPipelineRegs;
import others.ExcelProc;
import proving_model.Conditions;
import proving_model.Formula;
import proving_model.FormulaSet;

public class Verification {
	public static ArrayList<VerificationResult> verification(String file1, String file2, String file3, int type) throws Exception{
		ArrayList<VerificationResult> vr = null;
		if (type == 1){
//			指令验证
			String[] stageName = {"PRE", "IF", "ID", "EX", "MEM", "WB", "POST"};
			CPU.init(5, stageName);
		}
		else if (type == 2){
			String[] stageName = {"PRE", "IF", "IF(IMMU)", "ID", "EX", "MEM", "MEM(DMMU1)", "MEM(DMMU2)", "WB", "POST"};
			CPU.init(8, stageName);
		}
		else if (type == 3){
//			指令验证
			String[] stageName = {"PRE", "IF", "ID", "EX", "MEM", "WB", "POST"};
			CPU.init(5, stageName);
		}
		else if (type == 4){
//			指令验证
			String[] stageName = {"PRE", "IF", "IF(IMMU)", "ID", "EX", "MEM", "MEM(DMMU1)", "MEM(DMMU2)", "WB", "POST"};
			CPU.init(8, stageName);
		}
		else if (type == 5){
//			指令验证
			String[] stageName = {"PRE", "IF", "ID", "EX", "MEM", "WB", "POST"};
			CPU.init(5, stageName);
		}
		else if (type == 6){
//			指令验证
			String[] stageName = {"PRE", "IF", "IF(IMMU)", "ID", "EX", "MEM", "MEM(DMMU1)", "MEM(DMMU2)", "WB", "POST"};
			CPU.init(8, stageName);
		}
		else if (type == 7){
//			CPU级验证
			String[] stageName = {"PRE", "IF", "IF(IMMU)", "ID", "EX", "MEM", "MEM(DMMU1)", "MEM(DMMU2)", "WB", "POST"};
			CPU.init(8, stageName);
		}
		ReadTheorems rt = new ReadTheorems(file3);
		rt.doRead();
		vr = InstructionVerification.startInstructionMode(file1, file2);
//		int i;
//		for (i = 0; i < vr.size(); i++) {
//			System.out.println(vr.get(i).insName + "," + vr.get(i).time);
//		}
		return vr;
	}
public static void main(String args[]) throws Exception {
		
		Scanner in = new Scanner(System.in);
		String testPath = in.nextLine();
		if (testPath.equals("CPU"))
			testPath = "CPU MIPS MMU PPL";
		if (testPath.equals("test"))
			testPath = "TEST MIPS MMU PPL";
		CPU.testType = testPath;
		Verification iv = new Verification(); 
		if (testPath.equals("MIPS")) {
//			MIPS基本通路验证
			iv.verification("instruction_list_MIPS.xlsx", "semantics_MIPS.xlsx", "data/theorems_MIPS.xlsx", 1);
		}
		else if (testPath.equals("MIPS MMU")) {
//			MIPS MMU通路验证
			iv.verification("instruction_list_MIPS_MMU.xlsx", "semantics_MIPS_MMU.xlsx", "data/theorems_MIPS_MMU_PPL.xlsx", 2);
		}
		else if (testPath.equals("PPC")) {
//			PPC基本通路验证
			iv.verification("instruction_list_PPC.xlsx", "semantics_PPC.xlsx", "data/theorems_PPC.xlsx", 3);
		}
		else if (testPath.equals("PPC MMU")) {
//			PPC MMU通路验证
			iv.verification("instruction_list_PPC_MMU.xlsx", "semantics_PPC_MMU.xlsx", "data/theorems_PPC_MMU.xlsx", 4);
		}
		else if (testPath.equals("MIPS PPL")) {
//			MIPS PPL通路验证
			iv.verification("instruction_list_MIPS_PPL.xlsx", "semantics_MIPS_PPL.xlsx", "data/theorems_MIPS_PPL.xlsx", 5);
		}
		else if (testPath.equals("MIPS MMU PPL")) {
//			MIPS MMU PPL通路验证
			iv.verification("instruction_list_MIPS_MMU_PPL.xlsx", "semantics_MIPS_MMU_PPL.xlsx", "data/theorems_MIPS_MMU_PPL.xlsx", 6);
		}
		else if (testPath.equals("CPU MIPS MMU PPL")) {
			CPUVerification cv = new CPUVerification(); 
//			CPU完整通路验证
			cv.verification("CPU_list_MIPS_MMU_PPL.xlsx", "semantics_MIPS_MMU_PPL.xlsx", "data/theorems_MIPS_MMU_PPL.xlsx", 7);
		}
		else if (testPath.equals("TEST MIPS MMU PPL")) {
//			错误通路验证
			iv.verification("instruction_list_MIPS_MMU_PPL.xlsx", "semantics_MIPS_MMU_PPL.xlsx", "data/theorems_MIPS_MMU_PPL.xlsx", 6);
		}
				
//		
//		D2H dh = new D2H("data/s_PPC_MMU.xlsx");
//		dh.doTrans();

//		
//		CPU验证
//		Verification.startCPUMode();
//		输入指令通路合并
//		PathMerge pm = new PathMerge("data/new.xlsx");
//		pm.doPathMerge();
//		EliminateDuplicate ed = new EliminateDuplicate("data/pre_list.xlsx");
//		ed.doEliminate();
//		AddNo an = new AddNo("data/mipsCPUGen12.xlsx");
//		an.doAddNo();
//		Conditions.clearConds();
//		Conditions.add("(CU.IMemHit|CU.ICacheHit)");
//		Conditions.add("(CU.IMemHit&~CU.ICacheHit)");
//		Conditions.add("((CU.DMemHit&~CU.DCacheHit))&(CU.IMemHit|CU.ICacheHit)");
//		ArrayList<ConditionValue> cv = Conditions.getConds();
//		for (int i = 0; i < cv.size(); i++){
//			System.out.println(cv.get(i).getPort());
//			cv.get(i).setValue(1);
//		}
//		System.out.println(Conditions.judge("((CU.DMemHit&CU.DCacheHit))&(CU.IMemHit|CU.ICacheHit)"));
//		System.out.println(Conditions.judge("((CU.DMemHit&~CU.DCacheHit))&(CU.IMemHit|CU.ICacheHit)"));
//		Conditions.generateConds();
//		指称语义文件分离
//		GeneSem gs = new GeneSem("data/semantics_MIPS.xlsx");
//		gs.doGene();
//		流水线冲突检测
//		DLUCount dc = new DLUCount("data/instruction_list_MIPS_pipeline.xlsx");
//		dc.doCount();
	}
}

