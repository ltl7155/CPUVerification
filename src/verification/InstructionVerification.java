package verification;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import others.AddNo;
import others.AddPipelineRegs;
import others.D2H;
import others.DLUCount;
import others.EliminateDuplicate;
import others.ExcelProc;
import others.GeneSem;
import others.PathMerge;
import proving_model.ConditionValue;
import proving_model.Conditions;
import proving_model.CtrlSignalFormula;
import proving_model.Formula;
import proving_model.FormulaSet;
import proving_model.PathFormula;
import proving_model.PortDataFormula;
import proving_model.RegContentFormula;
import cpu_model.cpu.CPU;
import cpu_model.element.CtrlPort;
import cpu_model.element.Data;
import cpu_model.element.DataPort;
import cpu_model.element.Reg;

//CPU验证
//main方法入口

//1.读取文件，生成公式集
//2.证明，生成证明序列
//3.输出到文件

public class InstructionVerification {
	
	private static String rootPath = "data/";
	private static String instructionListFilepath = rootPath + "instruction_list.txt";	
	private static boolean DCacheFlag = false;
	
	public static int[] stageRange5 = {0, 1, 2, 3, 4, 5, 6};
	public static int[] stageRange1 = {0, 1, 3, 4, 5, 8, 9};
	public static int[] stageRange2 = {0, 1, 3, 4, 5, 6, 7, 8, 9};
	public static int[] stageRange3 = {0, 1, 2, 3, 4, 5, 8, 9};
	public static int[] stageRange4 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
	
	private static void init() throws Exception {
		//initialize system
		CPU.init();
		
		System.out.print(String.format("%-25s","Initializing system: "));
		Timer.start();
		
		String testFilepath = rootPath + "test.xlsx";		
		Input.startTest(testFilepath);
		
		Timer.stop();
		System.out.println(Timer.getRunTime() + "ms");
	}
	
	public static ArrayList<VerificationResult> startInstructionMode(String insFile, String semFileStr) throws Exception{
		init();		
		XSSFWorkbook srcFile = new XSSFWorkbook(rootPath + insFile);
		XSSFSheet srcSheet = srcFile.getSheetAt(0);
		XSSFWorkbook semFile = new XSSFWorkbook(rootPath + semFileStr);
		XSSFSheet semSheet = semFile.getSheetAt(0);
		String instructionName;
		int insStartLine = -1, insEndLine = -1, insNo = 0;
		int semStartLine = -1, semEndLine = -1;
		int i, j, stage;
		int si, sj;
		double times = 0;
		String insID;
		//所有控制端口
		ArrayList<CtrlPort> ctrlPortList;
		//有效控制端口矩阵。stageSum个阶段，每个阶段一个列表，包含该阶段所有有效的控制端口。
		ArrayList<ArrayList<CtrlPort>> activeCtrlPortMatrix;
		ArrayList<VerificationResult> rstList = new ArrayList<VerificationResult>();

		ArrayList<String> insNames = new ArrayList<String>();
		ArrayList<Integer> insTimes = new ArrayList<Integer>();
		while (!ExcelProc.getCell(srcSheet, insEndLine + 1, 0).isEmpty()){
			insNo++;
			instructionName = ExcelProc.getCell(srcSheet, insEndLine + 1, 0);
			insID = ExcelProc.getCell(srcSheet, insEndLine + 1, 0);
			System.out.println(insID + ": ");
			String filepath = rootPath + CPU.testType + "/" + insID + "/" + instructionName + " -";
			Timer.start();
			CPU.init();
			//读取指令通路
			ctrlPortList = null;
			activeCtrlPortMatrix = new ArrayList<ArrayList<CtrlPort>>();
			for (j = 0; j < CPU.StageSum; j++){
				activeCtrlPortMatrix.add(new ArrayList<CtrlPort>());
			}
			insStartLine = insEndLine + 1;
			i = insStartLine;
			stage = -1;
			FormulaSet.clear();
			do{
				String s1 = ExcelProc.getCell(srcSheet, i, 1);
				String s2 = ExcelProc.getCell(srcSheet, i, 2);
				String s3 = ExcelProc.getCell(srcSheet, i, 3);
				if (!s1.isEmpty()) {
					stage++;
					if (s2.isEmpty() && s3.isEmpty()){
						i++;
						continue;		
					}
				}
				else if (s2.isEmpty() && s3.isEmpty()){
					break;
				}
				//处理通路，将其转化成formula
				if (stage % 2 == 0) {
//					处理6'b000000
					if (s2.indexOf("'") != -1){
						Data data = new Data(s2);
						DataPort dataPort = CPU.getDataPort(s3);
						Formula f = portDataToFormula(dataPort, data);
						FormulaSet.add((stage + 2) / 2, f);
					}
					else{
						try{						
							Formula f = pathToFormula(s2, s3);
							for (j = 1; j <= CPU.StageSum; j++)
								FormulaSet.add(j, f);
						}
						catch (Exception e){
							System.out.println(e.getMessage());
						}
					}
				}
				//写使能信号
				else {
					CtrlPort port = CPU.getCtrlPort(s2, s3);
					activeCtrlPortMatrix.get(stage / 2).add(port);
				}
				i++;
			}while(ExcelProc.getCell(srcSheet, i, 0).isEmpty() && (!ExcelProc.getCell(srcSheet, i, 1).isEmpty() 
					|| !ExcelProc.getCell(srcSheet, i, 2).isEmpty() || !ExcelProc.getCell(srcSheet, i, 3).isEmpty()));
			//获得CPU加载的DLU中，所有的所有寄存器和多路选择器控制端口
			ctrlPortList = CPU.getCtrlPortList();
			//添加写使能公式到公式集
			Formula f = null;
			for (j = 1; j < CPU.stageRange.length - 1; j++) {
				
				int k = CPU.stageRange[j-1];
				for (CtrlPort port : ctrlPortList) {
//					如果在该指令中用到写使能，先给这个formula赋值一个1，如果没有用到，赋一个0
					if (activeCtrlPortMatrix.get(k).contains(port))
						f = ctrlSignalToFormula(port, 1);
					else
						f = ctrlSignalToFormula(port, 0);
					FormulaSet.add(k + 1, f);
				}
			}
			insEndLine = i - 1;
			//读取指称语义
			semStartLine = si = semEndLine + 1;
			//指令名称
			instructionName = ExcelProc.getCell(semSheet, si++, 2);
			insNames.add(instructionName);
			//指令格式
			String instructionForm = ExcelProc.getCell(semSheet, si++, 2);
			CPU.setInstructionForm(instructionForm);
			//前置条件
			do {
				String s = ExcelProc.getCell(semSheet, si++, 2);
				int staNo = 0;
				if (s.contains("@")) {
					String[] st = s.split("@");
					s = st[0];
					staNo = CPU.getStageNo(st[1]);
				}
				
				f = regDataToFormula(s);
				FormulaSet.add(staNo, f);			
			}while (ExcelProc.getCell(semSheet, si, 1).isEmpty() && !ExcelProc.getCell(semSheet, si, 2).isEmpty());
			//后置条件
			do {
				String s = ExcelProc.getCell(semSheet, si++, 2);
				
				int staNo = CPU.StageSum + 1;
				if (s.contains("@")) {
					String[] st = s.split("@");
					s = st[0];
					/* staNo = CPU.getStageNo(st[1]); */
				}
				
				f = regDataToFormula(s);
				FormulaSet.add(staNo, f);
			}while (ExcelProc.getCell(semSheet, si, 1).isEmpty() && !ExcelProc.getCell(semSheet, si, 2).isEmpty());
			semEndLine = si - 1;
//			读取条件
			Conditions.clearConds();
			for (i = insStartLine; i <= insEndLine; i++){
				if (!ExcelProc.getCell(srcSheet, i, 4).isEmpty()){
					if (ExcelProc.getCell(srcSheet, i, 4).contains("DCacheHit"))
						InstructionVerification.DCacheFlag = true;
					Conditions.add(ExcelProc.getCell(srcSheet, i, 4));
				}
			}
			FormulaSet.number();
			Deduce.start();
//			把Conditions里边所有的值给好
			CheckAndOutput.completeConds();
//			对除了那几个值确定的条件所有的其他条件值的所有组合分别验证
			ArrayList<int[]> combinations = Conditions.generateConds();
			int c;
			for (c = 0; c < combinations.size(); c++){
				Conditions.setAll(combinations.get(c));
				if (Conditions.judge("CU.ICacheHit") || Conditions.judge("CU_IF.ICacheHit")|| Conditions.judge("CU_IMMU.ICacheHit"))
					CPU.ICacheH = 1;
				else
					CPU.ICacheH = 0;
				
				if (Conditions.judge("CU.DCacheHit") || Conditions.judge("CU_DMMU1.DCacheHit"))
					CPU.DCacheH = 1;
				else
					CPU.DCacheH = 0;
//				如果指令里边根本没有出现DCache，那就根本用不到DCache，那就直接写成1，后面将不会打印DCache的证据
				if (!InstructionVerification.DCacheFlag)
					CPU.DCacheH = 1;
				
//				确定好DCacheH和ICacheH之后，确定好要利用哪些流水线阶段去推导证明
				if(CPU.ICacheH == 1 && CPU.DCacheH == 1)
					CPU.stageRange = stageRange1;
				if(CPU.ICacheH == 1 && CPU.DCacheH == 0)
					CPU.stageRange = stageRange2;
				if(CPU.ICacheH == 0 && CPU.DCacheH == 1)
					CPU.stageRange = stageRange3;
				if(CPU.ICacheH == 0 && CPU.DCacheH == 0)
					CPU.stageRange = stageRange4;
				
				if(CPU.StageSum == 5)
					CPU.stageRange = stageRange5;
				
//				开始验证
				ctrlPortList = null;
				activeCtrlPortMatrix = new ArrayList<ArrayList<CtrlPort>>();
				for (j = 0; j < CPU.StageSum; j++){
					activeCtrlPortMatrix.add(new ArrayList<CtrlPort>());
				}
				stage = -1;
				FormulaSet.clear();
				i = insStartLine;
				do{
					String s1 = ExcelProc.getCell(srcSheet, i, 1);
					String s2 = ExcelProc.getCell(srcSheet, i, 2);
					String s3 = ExcelProc.getCell(srcSheet, i, 3);
					String s4 = ExcelProc.getCell(srcSheet, i, 4);
					if (!s1.isEmpty()) {
						stage++;
						if (s2.isEmpty() && s3.isEmpty()){
							i++;
							continue;		
						}
					}
					else if (s2.isEmpty() && s3.isEmpty()){
						break;
					}
					if (s4.isEmpty() || Conditions.judge(s4)){
//						if (!s4.isEmpty()){
//							System.out.println(i + ":" + s4 + "," + Conditions.judge(s4));
//						}
//						通路
						if (stage % 2 == 0) {
//							处理6'b000000
							if (s2.indexOf("'") != -1){
								Data data = new Data(s2);
								DataPort dataPort = CPU.getDataPort(s3);
								f = portDataToFormula(dataPort, data);
								FormulaSet.add((stage + 2) / 2, f);
							}
							else{
								try{
									f = pathToFormula(s2, s3);
//									for (j = 1; j <= CPU.StageSum; j++){
//										FormulaSet.add(j, f);
//									}
									FormulaSet.add((stage + 2) / 2, f);
								}
								catch (Exception e){
									System.out.println(e.getMessage());
								}
							}
						}
//						写使能信号
						else {
							CtrlPort port = CPU.getCtrlPort(s2, s3);
							activeCtrlPortMatrix.get(stage / 2).add(port);
						}
					}
					i++;
				}while(i <= insEndLine);
				//获得CPU激活的所有寄存器和多路选择器控制端口
				ctrlPortList = CPU.getCtrlPortList();
				//添加控制信号公式到公式集
				f = null;
				for (j = 0; j < CPU.StageSum; j++) {
					for (CtrlPort port : ctrlPortList) {
						if (activeCtrlPortMatrix.get(j).contains(port)){
							f = ctrlSignalToFormula(port, 1);
						}
						else{
							f = ctrlSignalToFormula(port, 0);
						}
						FormulaSet.add(j + 1, f);
					}
				}
				insEndLine = i - 1;
				//读取指称语义
				si = semStartLine;
				//指令名称
				instructionName = ExcelProc.getCell(semSheet, si++, 2);
				//指令格式
				instructionForm = ExcelProc.getCell(semSheet, si++, 2);
				CPU.setInstructionForm(instructionForm);
				//前置条件
				do {
					String s = ExcelProc.getCell(semSheet, si++, 2);
//					if (s.indexOf(" | ") != -1){
//						System.out.println(s.substring(s.indexOf(" | ") + 3, s.length() - 1));
//					}			
					int staNo = 0;
					if (s.contains("@")) {
						String[] st = s.split("@");
						s = st[0];
						staNo = CPU.getStageNo(st[1]);
					}
					if (s.indexOf(" | ") == -1){
						f = regDataToFormula(s);
						FormulaSet.add(staNo, f);
					}
					else if (Conditions.judgeOriginal(s.substring(s.indexOf(" | ") + 3, s.length() - 1))){
						s = s.substring(0, s.indexOf(" | "));
						s = s.substring(0, s.indexOf('=') + 1) + s.substring(s.indexOf('=') + 2, s.length());
						f = regDataToFormula(s);
						FormulaSet.add(staNo, f);
					}
				}while (ExcelProc.getCell(semSheet, si, 1).isEmpty() && !ExcelProc.getCell(semSheet, si, 2).isEmpty());
				//后置条件
				do {
					String s = ExcelProc.getCell(semSheet, si++, 2);
					
					int staNo = CPU.StageSum + 1;
					if (s.contains("@")) {
						String[] st = s.split("@");
						s = st[0];
						/* staNo = CPU.getStageNo(st[1]);*/
					}			
//					System.out.println(s);
					if (s.indexOf(" | ") != -1){
						String st = s.substring(s.indexOf(" | ") + 3, s.length() - 1);
//						System.out.println(st);
					}
					if (s.indexOf(" | ") == -1){
						f = regDataToFormula(s);
						FormulaSet.add(CPU.StageSum + 1, f);
					}
					else if (Conditions.judgeOriginal(s.substring(s.indexOf(" | ") + 3, s.length() - 1))){
						s = s.substring(0, s.indexOf(" | "));
						s = s.substring(0, s.indexOf('=') + 1) + s.substring(s.indexOf('=') + 2, s.length());
						f = regDataToFormula(s);
						FormulaSet.add(CPU.StageSum + 1, f);
					}
				}while (ExcelProc.getCell(semSheet, si, 1).isEmpty() && !ExcelProc.getCell(semSheet, si, 2).isEmpty());
				
				FormulaSet.number();
				Deduce.start();
				VerificationResult vr = CheckAndOutput.start(filepath + c);
				vr.setName(insID);
				rstList.add(vr);
				System.out.print(CheckAndOutput.getResult() + ":  ");
				for (j = 0; j < Conditions.getConds().size(); j++){
					System.out.print(String.format("%-15s", Conditions.getConds().get(j).getPort() + "=" + Conditions.getConds().get(j).getValue() + " "));
				}
				
				System.out.print("\r\n");
			}
			insTimes.add((int)Timer.getRunTime());
			System.out.println(String.format("%-10s", Timer.getRunTime() + "ms"));
			times += Timer.getRunTime();
			i = insEndLine + 1;
		}
		System.out.println(insNo + " instructions : " + times + "ms");
		System.out.println("average time : " + (times / insNo) + "ms");
		return rstList;
	} 
	
	//将寄存器状态转换成公式
	private static Formula regDataToFormula(String str) {
		
		//按[]和=分隔开	
		int i1 = str.indexOf("[");
		int i2 = str.indexOf("=");
		String s1 = str.substring(0, i1);
		String s2 = str.substring(i1+1, i2-1);
		String s3 = str.substring(i2+1);
		
		Reg reg = null;
		Data addr = null;
		Data data = null;
		
		//不带地址的单寄存器结构
		if (s1.isEmpty()) {
			reg = CPU.getReg(s2);
			data = CPU.addData(s3); 
		}
		//带地址的复合寄存器结构
		else {
			reg = CPU.getReg(s1);
			addr = CPU.addData(s2);
			data = CPU.addData(s3);
		}
			
		return new RegContentFormula(reg, addr, data);
		
	}
	
	//将控制信号转换成公式
	private static Formula ctrlSignalToFormula(CtrlPort port, int value) {
		if (value == 0)
			return new CtrlSignalFormula(port, CPU.addData("0"));
		else if (value == 1)
			return new CtrlSignalFormula(port, CPU.addData("1"));
		return null;
	}
	
	//将通路结构转换成公式
	private static Formula pathToFormula(String port1, String port2) throws Exception {
//		处理[:]
		String port = port1;
		if (port.indexOf(":") != -1){
			port = port.replace(":", "_");
		}
		if (port.indexOf("[") != -1){
			port = port.replace("[", "");
			port = port.replace("]", "");
		}
		if (CPU.getDataPort(port) == null){
			throw(new Exception(port1 + "不存在：" + port1 + "," + port2));
		}
		else if (CPU.getDataPort(port2) == null){
			throw(new Exception(port2 + " 不存在：" + port1 + "," + port2));
		}
		return (Formula) new PathFormula(CPU.getDataPort(port), CPU.getDataPort(port2));
	}
	
	//将端口数据转换成公式
	private static Formula portDataToFormula(DataPort port, Data data) {
		return new PortDataFormula(port, data);
	}
	
	public static void startCPUMode() throws Exception{
		
		init();		
		
		String cpuRootPath = rootPath + "CPU/";
		String cpuPathFilepath = cpuRootPath + "CPU_structure.xlsx";
		
		File f = new File(instructionListFilepath);
		BufferedReader br = new BufferedReader(new FileReader(f));
		
		int totalTime = 0;
		String s = null;
		while((s = br.readLine())!=null) {
			//debug
			System.out.print(String.format("%-25s",s+": "));
			
			String name = s.substring(s.indexOf(" ") + 1);
			String semanticsFilepath = rootPath + s + "/" + name + ".xlsx";
			String filepath = cpuRootPath + s;
			
			Timer.start();
			
			CPU.init();
			Input.startCPUMode(cpuPathFilepath,semanticsFilepath);
			Deduce.start();
			CheckAndOutput.start(filepath + "/" + name);
			System.out.print(String.format("%-10s",Timer.getRunTime()+"ms"));
			totalTime += Timer.getRunTime();
			
			System.out.print(String.format("%-15s",CheckAndOutput.getResult()));
			
			System.out.print("\r\n");			
							
		}
			
		System.out.print(String.format("%-25s", "Total Time:"));	
		System.out.print(String.format("%-10s", totalTime + "ms"));
		System.out.print("\r\n");
		
		br.close();	


	}
	
		
}
