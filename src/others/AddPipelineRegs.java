package others;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class AddPipelineRegs {
	private int ColIns = 0;
	private int ColSta = 1;
	private int ColSrc = 2;
	private int ColDest = 3;
	private int ColCond = 4;
	private int ColFlag = 5;
	private int ColSemKey = 6;
	private int ColSemVal = 7;
	private XSSFWorkbook destFile = null;
	private XSSFSheet destSheet = null;
	private String srcPath;
	private String destPath;
	private FileInputStream fis;
	private FileOutputStream fos;
	private String[] dluList = {"IR", "A", "B", "ALUOut", "OVReg", "ConditionReg", "DR"};
	private String[] stageName = {"IF", "ID", "EX", "MEM", "WB"};
	private int stageSum = stageName.length;
	private int[] dluStart = new int[dluList.length];
	private int[] dluEnd = new int[dluList.length];
	private Boolean[] dluHoldResult = new Boolean[dluList.length];
	private ArrayList<ArrayList<String>> stageDLU = new ArrayList<ArrayList<String>>(stageSum * 2);
	private String srcGPR1 = "";
	private String srcGPR2 = "";
	private String destGPR = "";
		
//	AddPipelineRegs apr = new AddPipelineRegs("data/pipeline_gen_input.xlsx");
//	apr.doAdd();
	public AddPipelineRegs(String file) throws IOException{
		srcPath = file;
		destPath = "data/pipeline_gen_output.xlsx";
		Files.copy(Paths.get(srcPath), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING); // 复制旧文件，修改新文件
		fis = new FileInputStream(destPath);
		destFile = new XSSFWorkbook(fis);
		destSheet = destFile.getSheetAt(0);
	}
	
//	添加流水线寄存器
	public void doAdd() throws IOException {		
		int i, j, stage;
		int insStart = 0;
		ArrayList<String> cu = new ArrayList<String>();
//		表示阶段i是否已添加局部CU
		boolean[] cuState = new boolean[stageSum];
		String insName = "";
		String temp;
		i = 0;
		stage = stageSum * 2 - 1;
		while (!ExcelProc.getCell(destSheet, i, ColSta).isEmpty() || !ExcelProc.getCell(destSheet, i, ColSrc).isEmpty()) {
//			若读到一条新指令
			if (!ExcelProc.getCell(destSheet, i, ColIns).isEmpty()) {
//				先摆平上一条指令
				i = getItDone(insStart, i);
//				初始化dluStart、dluEnd
				for (j = 0; j < dluStart.length; j++) {
					dluStart[j] = -1;
					dluEnd[j] = -1;
					dluHoldResult[j] = false;
				}
				cu = new ArrayList<String>();
//				初始化cuState
				for (j = 0; j < stageSum; j++) {
					cuState[j] = false;
				}
//				初始化指令名
				insStart = i;
				insName = ExcelProc.getCell(destSheet, i, ColIns);
				insName = insName.substring(insName.indexOf('.') + 1, insName.length());
//				初始化stageDLU
				stageDLU.clear();
				destGPR = "";
				srcGPR1 = "";
				srcGPR2 = "";
			}
//			若读到指令新的阶段stage
			if (!ExcelProc.getCell(destSheet, i, ColSta).isEmpty()) {
				stage = (stage + 1) % (stageSum * 2);
				stageDLU.add(new ArrayList<String>());
//				若阶段stage未添加局部CU,(ID、IF阶段是改写CU，并不是凭空添加CU，而后面的阶段需要添加CU)
				if (stage % 2 == 0 && stage >= 4 && cuState[stage / 2] == false) {
//					若阶段stage为空，直接添加局部CU
					if (ExcelProc.getCell(destSheet, i, ColSrc).isEmpty()) {
//						CU.Op或CU.IRFunc
						temp = cu.get(0);
						ExcelProc.setCell(destSheet, i, ColSrc, temp.replace("CU.", "CU_" + stageName[stage / 2] + "."));
						temp = cu.get(1);
						ExcelProc.setCell(destSheet, i, ColDest, temp.replace("CU.", "CU_" + stageName[stage / 2] + "."));
						if (cu.size() == 4) {
//							插入新行
							insertRow(destSheet, i + 1, cu.get(2).replace("CU.", "CU_" + stageName[stage / 2] + "."), cu.get(3).replace("CU.", "CU_" + stageName[stage / 2] + "."));
						}
					}
//					若阶段stage不为空，插入新行以添加局部CU
					else {
//						插入新行
						insertRow(destSheet, i, cu.get(0).replace("CU.", "CU_" + stageName[stage / 2] + "."), cu.get(1).replace("CU.", "CU_" + stageName[stage / 2] + "."));
						if (cu.size() == 4) {
							insertRow(destSheet, i + 1, cu.get(2).replace("CU.", "CU_" + stageName[stage / 2] + "."), cu.get(3).replace("CU.", "CU_" + stageName[stage / 2] + "."));
						}
//						把第二列的stage向上挪到应该的位置
						ExcelProc.setCell(destSheet, i, ColSta, ExcelProc.getCell(destSheet, i + cu.size() / 2, ColSta));
						ExcelProc.setCell(destSheet, i + cu.size() / 2, ColSta, "");
					}
					cuState[stage / 2] = true;
//					添加局部CU后，重新处理本行
					stage--;
					continue;
				}
			}
//			source不为空
			if (!ExcelProc.getCell(destSheet, i, ColSrc).isEmpty()) {
				String left = ExcelProc.getCell(destSheet, i, ColSrc);
				String right = ExcelProc.getCell(destSheet, i, ColDest);
				String flag = ExcelProc.getCell(destSheet, i, ColFlag);
//				需要把从GPR中读数据的地址传入到FU中
				if (right.equals("GPR.RReg1") && left.indexOf("IR.") == 0) {
					srcGPR1 = left;
				}
				if (right.equals("GPR.RReg2") && left.indexOf("IR.") == 0) {
					srcGPR2 = left;
				}
//				需要把从要写入GPR的数据传进FU中，FU进行转发
				if (right.equals("GPR.WReg")) {
					destGPR = left;
				}
//				ID阶段记录对CU OP和IRFunc的写，准备将它们复制到别的阶段去
				if (stage == 2 && (right.equals("CU.Op") || right.contains("CU.IRFunc"))) {
					cu.add(left);
					cu.add(right);
				}
//				如果当前阶段本身是有CU.的，那就改写CU为局部CU，
				if (left.contains("CU.")) {
					ExcelProc.setCell(destSheet, i, ColSrc, left.replace("CU.", "CU_" + stageName[stage / 2] + "."));
				}
				if (right.contains("CU.")) {
					ExcelProc.setCell(destSheet, i, ColDest, right.replace("CU.", "CU_" + stageName[stage / 2] + "."));
				}
//				下面就是要从src和des中去找要复制为流水线寄存器的寄存器，并确定位置
				if (!left.contains("'")) {
					if (left.contains(".")) {
						left = left.substring(0, left.indexOf('.'));
					}
					j = indexOfDLU(left);
					if (j >= 0) {
						if (!stageDLU.get(stage).contains(left)) {
							stageDLU.get(stage).add(left);
						}
						if (dluStart[j] == -1) {
							dluStart[j] = 0;
						}
						dluEnd[j] = stage / 2;
					}
				}
//				这里是挑出IF、ID等阶段的，中间IF/ID阶段由于右侧是控制信号，就不需要进行下面的操作
				if (stage % 2 == 0) {
					if (right.contains(".")) {
						right = right.substring(0, right.indexOf('.'));
					}
					j = indexOfDLU(right);
					if (j >= 0) {
						if (!stageDLU.get(stage).contains(right)) {
							stageDLU.get(stage).add(right);
						}
						if (dluStart[j] == -1) {
							dluStart[j] = stage / 2;
						}
						dluEnd[j] = stage / 2;
						if (flag.equals(">")) {
							dluHoldResult[j] = true;
						}
					}
				}
			}
			i++;
		}
		getItDone(insStart, i);
//		合并结果写入目标文件
		fos = new FileOutputStream(destPath);
		destFile.write(fos);
		System.out.println("add pipeline regs succeed ...");	
	}
	
//	处理上一条指令
	int getItDone (int insStart, int nextStart) {
		int j, k;
//		阶段X是否已写入FU.InX_WReg
		boolean hasDestGPR = false;
		boolean readGPR1 = false, readGPR2 = false;
		String temp, dest, writeGPR = "";
//		根据指称语义确定上一条指令各流水线寄存器结束阶段
		genDLUEnd(insStart);
		if (nextStart != 0) {
//			为上一条指令添加流水线寄存器
			for (j = 0; j < dluList.length; j++) {
				if (dluStart[j] != -1) {
					nextStart = addPipelineRegs(insStart, dluList[j], dluStart[j], dluEnd[j]);
				}
			}
//			为上一条指令添加Halt、Bub、FU.IR_
			for (j = insStart, k = 0; j < nextStart; j++) {
//				逐条处理，碰到一个新阶段
				if (!ExcelProc.getCell(destSheet, j, ColSta).isEmpty()) {
					k++;
//					前两个阶段添加FU.Halt_、FU.Bub_ ：确定CU_ID.等等
					if (k <= 4 && k % 2 == 0) {
						insertRow(destSheet, j, "FU.Halt_" + stageName[k / 2 - 1], "CU_" + stageName[k / 2 - 1] + ".Halt");
						insertRow(destSheet, j + 1, "FU.Bub_" + stageName[k / 2 - 1], "CU_" + stageName[k / 2 - 1] + ".Bub");
						j = j + 2;
						nextStart = nextStart + 2;
					}
//					添加FU.IR_ ： 将每个阶段的IR传入FU中
					if (k > 1 && k % 2 == 1) {
						insertRow(destSheet, j, "IR_" + stageName[k / 2] + ".Out", "FU.IR_" + stageName[k / 2]);
						ExcelProc.setCell(destSheet, j, ColSta, stageName[k / 2]);
						ExcelProc.setCell(destSheet, j + 1, ColSta, "");
						j++;
						nextStart++;
					}
//					如果不需要往mem或者gpr里写的话，就往FU.InX_WReg塞一个空数据，InMEM就是写内存，InGPR就是写寄存器
					if (k > 4 && k % 2 == 0 && !hasDestGPR) {
//						如果当前阶段为空
						if (ExcelProc.getCell(destSheet, j - 1, ColSrc).isEmpty()) {
							ExcelProc.setCell(destSheet, j - 1, ColSrc, "5'b00000");
							ExcelProc.setCell(destSheet, j - 1, ColDest, "FU.In" + stageName[k / 2 - 1] + "_WReg");
						}
//						如果当前阶段不为空，就插入
						else {
							insertRow(destSheet, j, "5'b00000", "FU.In" + stageName[k / 2 - 1] + "_WReg");
							j++;
							nextStart++;
						}
					}
//					也是为未填入的FU的RReg填充一个值
					if (k == 4 && readGPR1 == false) {
						insertRow(destSheet, j, "5'b00000", "FU.InID1_RReg");
						j++;
						nextStart++;
					}
					if (k == 4 && readGPR2 == false) {
						insertRow(destSheet, j, "5'b00000", "FU.InID2_RReg");
						j++;
						nextStart++;
					}
					hasDestGPR = false;
				}
//				针对每条指令的一般处理
				dest = ExcelProc.getCell(destSheet, j, ColDest);
//				在IF阶段加上IMem.Out到FU.IR_IF
				if (k == 1 && dest.equals("IR_ID.In")) {
					temp = ExcelProc.getCell(destSheet, j, ColSrc);
					insertRow(destSheet, j, temp, "FU.IR_IF");
					j++;
					nextStart++;
				}
//				标记好readGPR1和readGPR2
				if (k == 3 && dest.equals("GPR.RReg1")) {
					readGPR1 = true;
				}
				if (k == 3 && dest.equals("GPR.RReg2")) {
					readGPR2 = true;
				}
//				修改condition中的CU为CU_
				if (k % 2 == 0) {
					temp = ExcelProc.getCell(destSheet, j, ColCond);
					if (!temp.isEmpty()) {
						ExcelProc.setCell(destSheet, j, ColCond, temp.replace("CU.", "CU_" + stageName[k / 2 - 1] + "."));
					}
				}
//				添加halt和bub
				if (k <= 4 && k % 2 == 0) {
					temp = ExcelProc.getCell(destSheet, j, ColCond);
					if (!temp.isEmpty()) {
						temp += "&";
					}
					temp += "~CU_" + stageName[k / 2 - 1] + ".Halt";
					ExcelProc.setCell(destSheet, j, ColCond, temp);
//					添加Bub
					temp = ExcelProc.getCell(destSheet, j, ColSrc);
					if (temp.indexOf("IR_") == 0) {
//						插入新行
						j++;
						nextStart++;
						insertRow(destSheet, j, temp, "%", "CU_" + stageName[k / 2 - 1] + ".Bub");
					}
				}
//				FU.InID1,FU.InID1_RReg,
				if (ExcelProc.getCell(destSheet, j, ColFlag).equals("<")) {
					temp = ExcelProc.getCell(destSheet, j, ColDest);
					if (ExcelProc.getCell(destSheet, j, ColSrc).equals("GPR.Rdata1")) {
						ExcelProc.setCell(destSheet, j, ColDest, "FU.InID1");
						j++;
						nextStart++;
						insertRow(destSheet, j, srcGPR1.replace(".", "_" + stageName[k / 2] + "."), "FU.InID1_RReg");
						j++;
						nextStart++;
						insertRow(destSheet, j, "FU.OutID1", temp);
					} else if (ExcelProc.getCell(destSheet, j, ColSrc).equals("GPR.Rdata2")) {
						ExcelProc.setCell(destSheet, j, ColDest, "FU.InID2");
						j++;
						nextStart++;
						insertRow(destSheet, j, srcGPR2.replace(".", "_" + stageName[k / 2] + "."), "FU.InID2_RReg");
						j++;
						nextStart++;
						insertRow(destSheet, j, "FU.OutID2", temp);
					}
				} else if (ExcelProc.getCell(destSheet, j, ColFlag).equals(">") && k >= 5) {
					j++;
					nextStart++;
					insertRow(destSheet, j, ExcelProc.getCell(destSheet, j - 1, ColSrc), "FU.In" + stageName[k / 2]);
					if (!destGPR.equals("")) {
						j++;
						nextStart++;
						if (destGPR.contains(".")) {
							insertRow(destSheet, j, destGPR.replace(".", "_" + stageName[k / 2] + "."), "FU.In" + stageName[k / 2] + "_WReg");
						}
						else {
							insertRow(destSheet, j, destGPR, "FU.In" + stageName[k / 2] + "_WReg");
						}
						writeGPR = ExcelProc.getCell(destSheet, j, ColSrc);
						if (writeGPR.contains(".")) {
							writeGPR = writeGPR.substring(writeGPR.indexOf("."), writeGPR.length());
						}
						hasDestGPR = true;
					}
				}
			}
		}
//		我觉得就是MEM和WB阶段基本只有一个阶段会写，所以要统一一下MEM和WB阶段写进FU.InMEM_WReg和FU.InWB_WReg的内容
		if (!writeGPR.isEmpty()) {
			for (j = insStart; j < nextStart; j++) {
				if (ExcelProc.getCell(destSheet, j, ColSrc).equals("5'b00000") && ExcelProc.getCell(destSheet, j, ColDest).contains("_WReg")) {
					temp = ExcelProc.getCell(destSheet, j, ColDest);
					if (writeGPR.contains(".")) {
						ExcelProc.setCell(destSheet, j, ColSrc, "IR_" + temp.substring(5, temp.indexOf("_")) + writeGPR);
					}
					else {
						ExcelProc.setCell(destSheet, j, ColSrc, writeGPR);
					}
				}
			}
		}
		return nextStart;
	}
	
//	确定dluEnd
	void genDLUEnd(int insStart) {
		int i = insStart, t;
		int pos = -1;
		String dluName;
		while (!ExcelProc.getCell(destSheet, i, ColSemVal).isEmpty()) {
			if (!ExcelProc.getCell(destSheet, i, ColSemKey).isEmpty()) {
				pos++;
			}
//			若是后置条件
			if (pos == 3) {
				dluName = ExcelProc.getCell(destSheet, i, ColSemVal);
				if (dluName.charAt(0) == '[') {
					dluName = dluName.substring(dluName.indexOf('[') + 1, dluName.indexOf(']'));
				}
				else{
					dluName = dluName.substring(0, dluName.indexOf("["));
				}
				t = indexOfDLU(dluName);
				if (t >= 0) {
					dluEnd[t] = stageSum - 1;
				}
			}
			i++;
		}
	}
	
	int addPipelineRegs(int insStart, String dluName, int dluStart, int dluEnd) {
		int i, stage = stageSum * 2 - 1, tmp;
		String tail;
		i = insStart;
		while (true) {
//			读到下一条指令即退出
			if ((!ExcelProc.getCell(destSheet, i, ColIns).isEmpty() && i != insStart) || (ExcelProc.getCell(destSheet, i, ColSta).isEmpty() && ExcelProc.getCell(destSheet, i, ColSrc).isEmpty())) {
				break;
			}
//			一条一条读
			else{
//				一条一条读,刚好读到新的阶段时
				if (!ExcelProc.getCell(destSheet, i, ColSta).isEmpty()) {
					if (stage < 8) {
//						本阶段需要添加流水线寄存器
//						在dluStart和dluEnd这个范围内，这里边只解决了当前阶段没有该dlu的问题,进行了添加
						if (stage / 2 >= dluStart && stage / 2 < dluEnd) {
							if (stage % 2 == 0) {
								tmp = stage + 1;
							}
							else {
								tmp = stage;
							}
//							如果当前阶段没有出现该DLU，但还是在dluStart和dluEnd这个范围内
							if (!stageDLU.get(tmp).contains(dluName)) {
//								如果是通路阶段
								if (stage % 2 == 0) {
//									如果整个阶段都是空的，就直接添加一个通路，是直接改当前行，不是插入
									if (ExcelProc.getCell(destSheet, i - 1, ColSrc).isEmpty()) {
										ExcelProc.setCell(destSheet, i - 1, ColSrc, dluName + "_" + stageName[stage / 2] + ".Out");
										ExcelProc.setCell(destSheet, i - 1, ColDest, dluName + "_" + stageName[stage / 2 + 1] + ".In");
										if (dluHoldResult[indexOfDLU(dluName)]) {
											ExcelProc.setCell(destSheet, i - 1, ColFlag, ">");
										}
									}
//									如果不是空的,需要插入一行
									else{
										insertRow(destSheet, i, dluName + "_" + stageName[stage / 2] + ".Out", dluName + "_" + stageName[stage / 2 + 1] + ".In");
										if (dluHoldResult[indexOfDLU(dluName)]) {
											ExcelProc.setCell(destSheet, i, ColFlag, ">");
										}
										i++;
									}
								}
//								如果是控制信号阶段
								else {
//									如果整个阶段是空的
									if (ExcelProc.getCell(destSheet, i - 1, ColSrc).isEmpty()) {
										ExcelProc.setCell(destSheet, i - 1, ColSrc, dluName + "_" + stageName[stage / 2 + 1]);
										ExcelProc.setCell(destSheet, i - 1, ColDest, "·");
									}
									else {
										insertRow(destSheet, i, dluName + "_" + stageName[stage / 2 + 1], "·");
										i++;
									}
								}
								stageDLU.get(stage).add(dluName);
							}
						}
					}
					stage = (stage + 1) % 10;
				}
//				一条一条读，一条一条处理，如果该dlu在当前阶段应该出现了，把左侧和右侧的名字都改了
				if (stage / 2 >= dluStart) {
					if (!ExcelProc.getCell(destSheet, i, ColSrc).isEmpty()) {
						String left = ExcelProc.getCell(destSheet, i, ColSrc);
						String right = ExcelProc.getCell(destSheet, i, ColDest);
						if (!left.contains("'")) {
							if (left.contains(".")) {
								tail = left.substring(left.indexOf('.'), left.length());
								left = left.substring(0, left.indexOf('.'));
							}
							else{
								tail = "";
							}
//							如果在当前阶段出现了要复制的流水线寄存器，就改写名字
							if (left.equals(dluName)) {
								if (stage % 2 == 0) {
									left = left + "_" + stageName[stage / 2] + tail;
								}
								else{
									left = left + "_" + stageName[stage / 2 + 1] + tail;
								}
								ExcelProc.setCell(destSheet, i, ColSrc, left);
							}
						}
//						跟上述left的处理一样，都是处理改流水线寄存器的名字，只不过右侧要改成下一个阶段的
						if (stage % 2 == 0) {
							if (right.contains(".")) {
								tail = right.substring(right.indexOf('.'), right.length());
								right = right.substring(0, right.indexOf('.'));
							}
							else{
								tail = "";
							}
							if (right.equals(dluName)) {
								right = right + "_" + stageName[stage / 2 + 1] + tail;
								ExcelProc.setCell(destSheet, i, ColDest, right);
							}
						}
					}
				}
			}
			i++;
		}
		return i;
	}
	
	int indexOfDLU(String dluName) {
		int i;
		for (i = 0; i < dluList.length; i++) {
			if (dluName.equals(dluList[i])) {
				break;
			}
		}
		if (i >= dluList.length) {
			i = -1;
		}
		return i;
	}
	
	private void insertRow(XSSFSheet destSheet, int row, String src, String dest) {
		int i, j;
		destSheet.shiftRows(row, destSheet.getLastRowNum(), 1, true, false); 
		destSheet.createRow(row);
		ExcelProc.setCell(destSheet, row, ColSrc, src);
		ExcelProc.setCell(destSheet, row, ColDest, dest);
		if (!ExcelProc.getCell(destSheet, row + 1, ColSemVal).isEmpty()) {
			i = row;
			j = row + 1;
			while (!ExcelProc.getCell(destSheet, j, ColSemVal).isEmpty()) {
				ExcelProc.setCell(destSheet, i, ColSemKey, ExcelProc.getCell(destSheet, j, ColSemKey));
				ExcelProc.setCell(destSheet, i, ColSemVal, ExcelProc.getCell(destSheet, j, ColSemVal));
				ExcelProc.setCell(destSheet, j, ColSemKey, "");
				ExcelProc.setCell(destSheet, j, ColSemVal, "");
				i++;
				j++;
			}
		}
	}
	
	private void insertRow(XSSFSheet destSheet, int row, String src, String dest, String cond) {
		int i, j;
		destSheet.shiftRows(row, destSheet.getLastRowNum(), 1, true, false); 
		destSheet.createRow(row);
		ExcelProc.setCell(destSheet, row, ColSrc, src);
		ExcelProc.setCell(destSheet, row, ColDest, dest);
		ExcelProc.setCell(destSheet, row, ColCond, cond);
		if (!ExcelProc.getCell(destSheet, row + 1, ColSemVal).isEmpty()) {
			i = row;
			j = row + 1;
			while (!ExcelProc.getCell(destSheet, j, ColSemVal).isEmpty()) {
				ExcelProc.setCell(destSheet, i, ColSemKey, ExcelProc.getCell(destSheet, j, ColSemKey));
				ExcelProc.setCell(destSheet, i, ColSemVal, ExcelProc.getCell(destSheet, j, ColSemVal));
				ExcelProc.setCell(destSheet, j, ColSemKey, "");
				ExcelProc.setCell(destSheet, j, ColSemVal, "");
				i++;
				j++;
			}
		}
	}
	
	
}
