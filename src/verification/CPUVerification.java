package verification;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PrimitiveIterator.OfDouble;

import javax.swing.plaf.synth.SynthSpinnerUI;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

import cpu_model.cpu.CPU;
import cpu_model.element.CtrlPort;
import cpu_model.element.Data;
import cpu_model.element.DataPort;
import cpu_model.element.Reg;
import others.CPUPath;
import others.InsPath;
import others.ExcelProc;
import proving_model.Conditions;
import proving_model.CtrlSignalFormula;
import proving_model.Formula;
import proving_model.FormulaSet;
import proving_model.PathFormula;
import proving_model.PortDataFormula;
import proving_model.RegContentFormula;

public class CPUVerification {
	
	private static String rootPath = "data/";
	private static XSSFWorkbook srcFile = null; 
	private static XSSFWorkbook cpuSrcFile = null;
	private static XSSFSheet srcSheet = null;
	private static XSSFSheet cpuSrcSheet = null;
	
	private static boolean DCacheFlag = false;
	
	public static int[] stageRange1 = {0, 1, 3, 4, 5, 8, 9};
	public static int[] stageRange2 = {0, 1, 3, 4, 5, 6, 7, 8, 9};
	public static int[] stageRange3 = {0, 1, 2, 3, 4, 5, 8, 9};
	public static int[] stageRange4 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
	
	private static HashMap<String, String> insOpFuncMap = new HashMap<String, String>();
	private static HashMap<String, String> insAndFormMap = new HashMap<String, String>();
	private static HashMap<String, String> insAndIDMap = new HashMap<String, String>();
	private static ArrayList<InsPath> allCpuPath = new ArrayList<InsPath>();
	private static ArrayList<String> insList = new ArrayList<String>();
	private static String[] stageList = {"IF", "IMMU", "ID", "EX", "MEM", "DMMU1", "DMMU2", "WB"};
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
	
	public static void readOpFuncAndFormAndID() throws Exception{
		srcFile = new XSSFWorkbook(rootPath + "OpAndFunc.xlsx");
		srcSheet = srcFile.getSheetAt(0);
		int i = 1;
		while (!getCell(srcSheet, i, 5).isEmpty()){
			insOpFuncMap.put(getCell(srcSheet, i, 5), getCell(srcSheet, i, 6));
			insAndFormMap.put(getCell(srcSheet, i, 5), getCell(srcSheet, i, 7));
			insAndIDMap.put(getCell(srcSheet, i, 5), getCell(srcSheet, i, 0));
			insList.add(getCell(srcSheet, i, 5));
			i++;
		}
		System.out.println("读取opfunc完成");
	}	
	
	public static String getInstructionForm(String insName) {
		return insAndFormMap.get(insName);
	}
	
	public static void readCpuPath() throws Exception{
		cpuSrcFile = new XSSFWorkbook(rootPath + "CPU_list_MIPS_MMU_PPL.xlsx");
		cpuSrcSheet = cpuSrcFile.getSheetAt(0);
		String src = "",  mux = "", des = "", muxCtrlName = "", opfuncStr = "", stage = ""; 	
		int start = 1;
		while (!getCell(cpuSrcSheet, start, 0).isEmpty()){
			InsPath singlePath = new InsPath();
			String[] splittedOpfunc;
			String opf;
			src = getCell(cpuSrcSheet, start, 0);
			mux = getCell(cpuSrcSheet, start, 1);
			des = getCell(cpuSrcSheet, start, 2);
			muxCtrlName = getCell(cpuSrcSheet, start, 3);
			opfuncStr = getCell(cpuSrcSheet, start, 4);
			stage = getCell(cpuSrcSheet, start, 5);
			singlePath.setSrc(src);
			singlePath.setMux(mux);
			singlePath.setDes(des);
			singlePath.setMuxCtrlName(muxCtrlName);
			singlePath.setOpfuncStr(opfuncStr);
			singlePath.setStage(stage);
			if (opfuncStr.equals("1")){
				singlePath.addIntoOpfuncList("1");
			}
			else if (opfuncStr.indexOf("OP") == -1 && opfuncStr.indexOf("IRFunc") == -1) {
				singlePath.addIntoOpfuncList("onlyCU");
			}
			else {
				splittedOpfunc = opfuncStr.split("#");
				for (int i = 0; i < splittedOpfunc.length; i++){
					opf = splittedOpfunc[i];
					if(opf.indexOf("(") == 0){
						opf = opf.substring(1, opf.length() - 1);
						singlePath.addIntoOpfuncList(opf);		
					}
					else {
						singlePath.addIntoOpfuncList(opf);
					}
				}
			}
			allCpuPath.add(singlePath);
			start ++;
		}
		System.out.println("读取CPU通路完成");
	}
	public static String getInsOpFunc(String ins) throws Exception{
		return insOpFuncMap.get(ins);
	}	
	public static String getInsID(String ins) throws Exception{
		return insAndIDMap.get(ins);
	}	
	public static ArrayList<InsPath> getInsPath(String insName) throws Exception{
		String insOpFunc = CPUVerification.getInsOpFunc(insName);
		ArrayList<InsPath> insPathList = new ArrayList<InsPath>();
		for (int i = 0; i < allCpuPath.size(); i++){
			boolean isContained = false;
			InsPath c = (InsPath) allCpuPath.get(i);
			ArrayList<String> l = c.getOpfuncList();
			for (int j = 0; j < l.size(); j++){
				if (l.get(0).equals("1")){
					isContained = true;
					c.setInsWholeCtrl("1");
					c.setCuCtrl("1");
				}
				if (l.get(0).equals("onlyCU")){
					isContained = true;
					String insCtrl = c.getOpfuncStr();
					if (insCtrl.startsWith("("))
						c.setInsWholeCtrl(insCtrl.substring(1, insCtrl.length() - 1));
					else
						c.setInsWholeCtrl(insCtrl);
					c.setCuCtrl(c.getInsWholeCtrl());
				}	
				if (l.get(j).contains(insOpFunc)){
					isContained = true;
					String insCtrl = l.get(j);
					c.setInsWholeCtrl(insCtrl);
					int pos = insCtrl.indexOf(insOpFunc);
					String cuCtrl;
					if (pos > 1)
						if (insCtrl.startsWith("(") && insCtrl.endsWith(")"))
							cuCtrl = insCtrl.substring(1, pos - 2);
						else 
							cuCtrl = insCtrl.substring(0, pos - 1);
					else
						cuCtrl = "1";
					c.setCuCtrl(cuCtrl);
				}			
			}
			if (isContained)
				insPathList.add(c);
		}
		return insPathList;
	}
	public static int indexOfStageList(String stage) {
		for (int i = 0; i < stageList.length; i++){
			if (stageList[i].equals(stage))
				return i;
		}
		System.out.println("没有找到当前阶段");
		return -1;
	}
	private static String getCell(XSSFSheet sheet, int rowNum, int cellNum){
		XSSFRow row = sheet.getRow(rowNum);
		if (row==null)
			return "";
		XSSFCell cell = row.getCell(cellNum);
		if (cell==null)
			return "";
		if (cell.getCellType()==XSSFCell.CELL_TYPE_BLANK)
			return "";
		return cell.toString();
	}
	
//	需要做的是：
//	CPU综合那里在mux_def那里边弄一个op和func联合的表示形式，拷贝到验证这边 : 完成
//	先弄一个instruction的list，弄清楚要验证那些指令，开始逐条验证 ： 完成
//	1，先把所有通路读进来，把条件分成带CU的以及op以及func以及stage等，存储进allPathList，path里边的所有控制信息 设一个controlList表示，
//	2，去找该指令的通路，通过那个op和func的表去找出该指令的的op，func，找出所有包含相应op跟func的通路作为该指令的所有通路，放进一个curPathList里边，最好按照周期存（要存CU的控制信号），至此得到的内容跟简单通路得到的内容应该是完全一样的，			
//	3，以后就对curPathList进行处理，将通路全都turn into formula，放进一个formulaSet里边，写使能信号放进activeMatrix里边
//		把activeMatrix全设为1，这是为了把写使能全部放进formulaSet每个阶段的最后面，
//		顺便把conditions存起来
//	4，进行deduce，弄清楚conditions的值的origin是什么，
//	5，然后generateConds，给conditions赋相应值，再重新把符合条件的弄进formulaSet里边就行了
//	6，再接着进行推导就都一样了
						
	public static ArrayList<VerificationResult> startInstructionMode(String insFile, String semFileStr) throws Exception{
		init();	
		XSSFWorkbook srcFile = new XSSFWorkbook(rootPath + insFile);
		XSSFSheet srcSheet = srcFile.getSheetAt(0);
		XSSFWorkbook semFile = new XSSFWorkbook(rootPath + semFileStr);
		XSSFSheet semSheet = semFile.getSheetAt(0);
		String instructionName, instructionForm;
		int insStartLine = -1, insEndLine = -1, insNo = 0;
		int semStartLine = -1, semEndLine = -1;
		int i, j;
		int si, sj;
		double times = 0;
		//所有控制端口
		ArrayList<CtrlPort> ctrlPortList;
		//有效控制端口矩阵。stageSum个阶段，每个阶段一个列表，包含该阶段所有有效的控制端口。
		ArrayList<ArrayList<CtrlPort>> activeCtrlPortMatrix;
		ArrayList<VerificationResult> rstList = new ArrayList<VerificationResult>();

		ArrayList<String> insNames = new ArrayList<String>();
		ArrayList<Integer> insTimes = new ArrayList<Integer>();
		CPUVerification.readOpFuncAndFormAndID();
		CPUVerification.readCpuPath();
		for(int ins = 0; ins < insList.size(); ins++){
			ArrayList<InsPath> insPathList = new ArrayList<InsPath>();			
			String insName = insList.get(ins);
			String insID = getInsID(insName) + "." + insName;
			System.out.println("当前验证指令" + insID);
			String filepath = rootPath + CPU.testType + "/" + insID + "/" + insID + " -";
//			这里已经把当前指令的通路给取出来了
			insPathList = CPUVerification.getInsPath(insName);
			activeCtrlPortMatrix = new ArrayList<ArrayList<CtrlPort>>();
			Timer.start();
			CPU.init();
			for (j = 0; j < 8; j++){
				activeCtrlPortMatrix.add(new ArrayList<CtrlPort>());
			}
			FormulaSet.clear();
			for (i = 0; i < insPathList.size(); i++){
				InsPath tempPath = insPathList.get(i);
				String src = tempPath.getSrc();
				String des = tempPath.getDes();
				String stage = tempPath.getStage();
				int stageN = indexOfStageList(stage);
//				如果是写使能通路
				if (des.equals("·") || des.equals("%") || des.equals("ASID") || des.equals("EPC") || des.equals("ExCode") || des.equals("8Word") || des.equals("++")){
					CtrlPort port = CPU.getCtrlPort(src, des);
					activeCtrlPortMatrix.get(stageN).add(port);
				}
//				如果是端口连接通路
				else {
//					处理6'b000000
					if (src.indexOf("'") != -1){
						Data data = new Data(src);
						DataPort dataPort = CPU.getDataPort(des);
						Formula f = portDataToFormula(dataPort, data);
						FormulaSet.add(stageN + 1, f);
					}
					else{
						try{
							Formula f = pathToFormula(src, des);
							for (j = 1; j <= CPU.StageSum; j++)
								FormulaSet.add(j, f);
						}
						catch (Exception e){
							System.out.println(e.getMessage());
						}
					}
				}			
			}
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
			// 读取指令指称语义
			semStartLine = si = semEndLine + 1;
			//指令名称
			instructionName = ExcelProc.getCell(semSheet, si++, 2);
			insNames.add(instructionName);
			//指令格式
			instructionForm = ExcelProc.getCell(semSheet, si++, 2);
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
			
			Conditions.clearConds();
			for (i = 0; i < insPathList.size(); i++){
				InsPath p = insPathList.get(i);
				if (p.getCuCtrl().isEmpty() || p.getCuCtrl() == "1")
					;
				else {
					Conditions.add(p.getCuCtrl());
					if(p.getCuCtrl().contains("DCacheHit"))
						CPUVerification.DCacheFlag = true;
				}
								
			}
			
			FormulaSet.number();
			Deduce.start();
//			把Conditions里边所有的值给好
			CheckAndOutput.completeConds();
//			对除了那几个值确定的条件所有的其他条件值的所有组合分别验证
			ArrayList<int[]> combinations = Conditions.generateConds();
			for (int c = 0; c < combinations.size(); c++){
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
				if (!CPUVerification.DCacheFlag)
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
				
//				开始验证
				ctrlPortList = null;
				activeCtrlPortMatrix = new ArrayList<ArrayList<CtrlPort>>();
				for (j = 0; j < CPU.StageSum; j++){
					activeCtrlPortMatrix.add(new ArrayList<CtrlPort>());
				}
				FormulaSet.clear();
				
				for (i = 0; i < insPathList.size(); i++){
					InsPath tempPath = insPathList.get(i);
					String src = tempPath.getSrc();
					String des = tempPath.getDes();
					String stage = tempPath.getStage();
					String cuCtrl = tempPath.getCuCtrl();
					int stageN = indexOfStageList(stage);
					if (cuCtrl.isEmpty() || cuCtrl.equals("1") || Conditions.judge(cuCtrl)){
						if (des.equals("·") || des.equals("%") || des.equals("ASID") || des.equals("EPC") || des.equals("ExCode") || des.equals("8Word") || des.equals("++")){
							CtrlPort port = CPU.getCtrlPort(src, des);
							activeCtrlPortMatrix.get(stageN).add(port);
						}
	//					如果是端口连接通路
						else {
	//						处理6'b000000
							if (src.indexOf("'") != -1){
								Data data = new Data(src);
								DataPort dataPort = CPU.getDataPort(des);
								Formula formula = portDataToFormula(dataPort, data);
								FormulaSet.add(stageN + 1, formula);
							}
							else{
								try{
									Formula formula = pathToFormula(src, des);
									for (j = 1; j <= CPU.StageSum; j++)
										FormulaSet.add(j, formula);
								}
								catch (Exception e){
									System.out.println(e.getMessage());
								}
							}
						}
					}									
				}				
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
				// 读取指令指称语义
				si = semStartLine;
				instructionName = ExcelProc.getCell(semSheet, si++, 2);
				instructionForm = ExcelProc.getCell(semSheet, si++, 2);
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
		insNo = insList.size();
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
//			处理[:]
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
	
	public static ArrayList<VerificationResult> verification(String file1, String file2, String file3, int type) throws Exception{
		ArrayList<VerificationResult> vr = null;
		if (type == 7){
//			CPU级验证
			String[] stageName = {"PRE", "IF", "IMMU", "ID", "EX", "MEM", "DMMU1", "DMMU2", "WB", "POST"};
			CPU.init(8, stageName);
		}
		ReadTheorems rt = new ReadTheorems(file3);
		rt.doRead();
		vr = CPUVerification.startInstructionMode(file1, file2);
//		int i;
//		for (i = 0; i < vr.size(); i++) {
//			System.out.println(vr.get(i).insName + "," + vr.get(i).time);
//		}
		return vr;
	} 
}
