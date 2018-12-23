package verification;

import java.io.BufferedWriter;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.RestoreAction;

import cpu_model.cpu.CPU;
import proving_model.Conditions;
import proving_model.Formula;
import proving_model.FormulaSet;
import proving_model.NumberedFormula;
import proving_model.Procedure;
import proving_model.Proof;

public class CheckAndOutput {

	private static boolean result = false;
	public static int[] stageRange;
	public static int[] stageRange1 = {0, 1, 3, 4, 5, 8, 9};
	public static int[] stageRange2 = {0, 1, 3, 4, 5, 6, 7, 8, 9};
	public static int[] stageRange3 = {0, 1, 2, 3, 4, 5, 8, 9};
	public static int[] stageRange4 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
	public static int[] stageRange5 = {0, 1, 2, 3, 4, 5, 6};
	private static String filepath = null;
	private static String dirpath = null;
	private static File dir;
	private static File EXCELfile;
	private static XSSFSheet formulaSetSheet;
	private static XSSFSheet fullProofSheet;
	private static XSSFSheet proofSheet;
	private static XSSFWorkbook wb;
	
	public static VerificationResult start(String s) throws Exception {
		filepath = s;
		dirpath = s.substring(0, s.lastIndexOf('/'));
		dir = new File(dirpath);
		EXCELfile = new File(filepath + " output.xlsx");
		wb = new XSSFWorkbook();
		formulaSetSheet = wb.createSheet("FormulaSet");
		fullProofSheet = wb.createSheet("FullProof");
		proofSheet = wb.createSheet("Proof");
		
		if(CPU.ICacheH == 1 && CPU.DCacheH == 1)
			stageRange = stageRange1;
		if(CPU.ICacheH == 1 && CPU.DCacheH == 0)
			stageRange = stageRange2;
		if(CPU.ICacheH == 0 && CPU.DCacheH == 1)
			stageRange = stageRange3;
		if(CPU.ICacheH == 0 && CPU.DCacheH == 0)
			stageRange = stageRange4;
//		stageRange = stageRange4;
		if (CPU.StageSum == 5)
			stageRange = stageRange5;
							
		if (!dir.exists() && !dir.isDirectory()){      
		    dir.mkdir();    
		}
		printFullProof();
		check();		
		printFormulaSet();
		printProof();
		
		FileOutputStream output = new FileOutputStream(EXCELfile);
        wb.write(output);
        output.flush();
        output.close();
        
		return printResult();
		
	}
	
	public static void completeConds(){
		boolean findAll = false;
		int no = 0;
		for (int j = 0; !findAll && j < CPU.stageRange.length; j++) {
			int i = CPU.stageRange[j];
			for (int num = 0; !findAll && num < Proof.size(i); num++) {
				Procedure pd = Proof.get(i,num);
				String fStr = pd.getFormula().getStr();
//				如果不是通路的公式的话
				if (!fStr.contains("=>")){
					String left = fStr.split("=")[0];
//					如果这个公式的左边是conditions中的一员的话，在conditions里边把它的值设置好
					if (Conditions.indexOf(left) != -1){
						no = Conditions.indexOf(left);
						if (Conditions.getConds().get(no).getOriginal().equals("")){
							Conditions.getConds().get(no).setOriginal(fStr.split("=")[1]);
							no = Conditions.incReadyNum();
							if (no >= Conditions.getConds().size()){
								findAll = true;
							}
						}
					}
				}
			}
		}
//		for (no = 0; no < Conditions.getConds().size(); no++){
//			System.out.println(Conditions.getConds().get(no).getPort() + "=" + Conditions.getConds().get(no).getOriginal());
//		}
	}
	
	public static String getResult() {
		return (result ? "correctness" : "incorrectness");
	}
	
	public static void fullproofNumbered(){
		for (int i = 0; i < CPU.StageSum + 2; i++) {
			for (int num = 0; num < Proof.size(i); num++) {
				Procedure pd = Proof.get(i,num);
				pd.setUsed();
//				System.out.println("*ok");
			}
		}
		Proof.number();	
	}
	
	public static void fullproofRestore(){
		for (int i = 0; i < CPU.StageSum + 2; i++) {
			for (int num = 0; num < Proof.size(i); num++) {
				Procedure pd = Proof.get(i,num);
				pd.unsetUsed();
				pd.setNumber(0);
//				System.out.println("*ok");
			}
		}
	}
	
	
	private static void check() {
		
		//检查FormulaSet[POST]中每一条公式是否都在Proof[WB/]中
		//如果某条公式包含，则把这个公式的所有直接或间接条件打上“已引用”的记号
		//如果所有公式都包含，则验证正确
		
		result = true;
		for (int i = 0; i < FormulaSet.size(CPU.StageSum + 1); i++) {
			NumberedFormula nf = FormulaSet.get(CPU.StageSum + 1, i);
			Formula f = nf.getFormula();
			Procedure pd = Proof.lookFor(CPU.StageSum + 1, f);
			if (pd != null){
				Proof.mark(pd);
			}
			else{
				result = false;
			}
		}
		//将所有带有“已引用”标记的步骤编号
		Proof.number();
		
	}	
		
	static void printFormulaSet() throws Exception {
		
		String s = filepath + " FormulaSet.txt";
		File file = new File(s);
		BufferedWriter bw = new BufferedWriter(new PrintWriter(file));
		int row = 0;
		//print formula set		
		bw.append("\r\n---------------------------------- Formula Set ----------------------------------\r\n\r\n");
		String[] a = CPU.StageName;
		for (int part: CheckAndOutput.stageRange) {
			bw.append(a[part]);
			CheckAndOutput.setCell(formulaSetSheet, row, 0, a[part]);
			for (int num = 0; num < FormulaSet.size(part); num++){
				bw.append("\t" + FormulaSet.get(part,num).getStr() + "\r\n");
				CheckAndOutput.setCell(formulaSetSheet, row, 1, FormulaSet.get(part,num).getStr());
				row ++;
			}
			bw.append("\r\n");
		}
		
		bw.close();

	}	
	
	private static void printFullProof() throws Exception {
		
		fullproofNumbered();
		
		String s = filepath + " FullProof.txt";
		File file = new File(s);
		BufferedWriter bw = new BufferedWriter(new PrintWriter(file));
		//print full proof
		bw.append("\r\n----------------------------------- FullProof -----------------------------------\r\n\r\n");
		int row = 0;
		String[] b = CPU.StageName;
		for (int i: CheckAndOutput.stageRange) {
			bw.append(b[i]);
			CheckAndOutput.setCell(fullProofSheet, row, 0, b[i]);
			for (int num = 0; num < Proof.size(i); num++) {
				Procedure pd = Proof.get(i,num);
//				System.out.println("*" + pd.getFormula().getStr());
				bw.append("\t" + pd.getStr() + "\r\n");
//				System.out.println("*ok");
				CheckAndOutput.setCell(fullProofSheet, row, 1, pd.getNumber() + "= " + pd.getFormula().getStr());
				CheckAndOutput.setCell(fullProofSheet, row, 2, pd.getJst().getStr());
				row ++;
			}
			bw.append("\r\n");
		}
		
		bw.close();
		
		fullproofRestore();
		
	}
	
	private static void printProof() throws Exception {
		
		String s = filepath + " Proof.txt";
		File file = new File(s);
		BufferedWriter bw = new BufferedWriter(new PrintWriter(file));
		int row = 0;	
		//print proof
		bw.append("\r\n------------------------------------- Proof -------------------------------------\r\n\r\n");
		String[] b = CPU.StageName;
		for (int i: CheckAndOutput.stageRange) {
			bw.append(b[i]);
			CheckAndOutput.setCell(proofSheet, row, 0, b[i]);
			for (int num = 0; num < Proof.size(i); num++) {
				Procedure pd = Proof.get(i,num);
				if(pd.isUsed()){
					bw.append("\t" + pd.getStr() + "\r\n");
					CheckAndOutput.setCell(proofSheet, row, 1, pd.getNumber() + "= " + pd.getFormula().getStr());
					CheckAndOutput.setCell(proofSheet, row, 2, pd.getJst().getStr());
					row ++;
				}
			}
			bw.append("\r\n");
		}
		
		bw.close();
		
	}
	
	private static VerificationResult printResult() throws Exception {
		VerificationResult vr = null;
		String s = filepath + " Result.txt";
		File file = new File(s);
		BufferedWriter bw = new BufferedWriter(new PrintWriter(file));
			
		//print verification result
		bw.append("\r\n------------------------------ Verification Result ------------------------------\r\n\r\n");
		
		bw.append("Result: " + (result?"Correctness":"Incorrectness") + "\r\n");
		
		Timer.stop();
		vr = new VerificationResult(result, FormulaSet.getSize(), Proof.getSize(), Timer.getRunTime());
		bw.append("Used Time: " + Timer.getRunTime() + "ms\r\n");
		
		bw.append("Extracted Formulas: " + FormulaSet.getSize() + "\r\n");
		
		bw.append("Procedures: " + Proof.getSize() + "\r\n");
				
		bw.close();
		return vr;
	}
	
	 //创建Excel文件
    public void createExcel(String path) throws Exception {
        //创建Excel文件对象
        XSSFWorkbook wb = new XSSFWorkbook();
        //用文件对象创建sheet对象  
    	formulaSetSheet = wb.createSheet("FormulaSet");;
    	fullProofSheet = wb.createSheet("FullProof");;
    	proofSheet = wb.createSheet("ProofFile");;
        //输出Excel文件
        FileOutputStream output = new FileOutputStream(path);
        wb.write(output);
        output.flush();
    }

    
    private static void setCell(XSSFSheet sheet, int rowNum, int cellNum, String s) {
		XSSFRow row = sheet.getRow(rowNum);
		if(row == null)
			row = sheet.createRow(rowNum);
		XSSFCell cell = row.createCell((short)cellNum);	
		cell.setCellType(XSSFCell.CELL_TYPE_BOOLEAN);
		cell.setCellValue(s);
		
	}
	
}
