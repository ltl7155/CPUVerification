package others;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import cpu_model.cpu.CPU;
import proving_model.Axiom;

public class GeneSem {
	private static XSSFWorkbook srcFile = null;
	private static XSSFWorkbook destFile = null;
	private static XSSFSheet srcSheet = null;
	private static XSSFSheet destSheet = null;
	private File resultFile;
	
	// GeneSem gs = new GeneSem("data/semantics_MIPS.xlsx");
	public GeneSem(String srcPath) throws IOException{
		srcFile = new XSSFWorkbook(srcPath);
		srcSheet = srcFile.getSheetAt(0);
	}
	
	public void doGene() throws IOException {
		int i = 0, j = 0;
		int state = -1;
		while (!getCell(srcSheet, i, 2).isEmpty()) {
			String cell1 = getCell(srcSheet, i, 1);
			String cell2 = getCell(srcSheet, i, 2);
			if (!getCell(srcSheet, i, 0).isEmpty()) {
				if (i != 0) {
					FileOutputStream fos = new FileOutputStream(resultFile);
					destFile.write(fos);
					fos.flush();
					fos.close();
				}
				String dstPath = "data/semantics/semantics_MIPS - " + cell2 + ".xlsx";
				destFile = new XSSFWorkbook();
				destSheet = destFile.createSheet("semantics_MIPS - " + cell2);
				resultFile = new File(dstPath);
				j = 0;
				setCell(destSheet, j, 0, cell1);
				setCell(destSheet, j++, 1, cell2);
			}
			else {
				setCell(destSheet, j, 0, cell1);
				setCell(destSheet, j++, 1, cell2);
			}
			i++;
		}
	}
	
	private static void setCell(XSSFSheet sheet, int rowNum, int cellNum, String s) {
		XSSFRow row = sheet.getRow(rowNum);
		if(row == null)
			row = sheet.createRow(rowNum);
		XSSFCell cell = row.createCell((short)cellNum);
		
		cell.setCellType(XSSFCell.CELL_TYPE_BOOLEAN);
		cell.setCellValue(s);
		
	}
	
	//取出一个Cell并转为String，含空Cell判断
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
}