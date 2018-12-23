package others;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class D2H {
	private static XSSFWorkbook srcFile = null;
	private static XSSFSheet srcSheet = null;
	private String filepath;
	private FileInputStream inputStream;
	private FileOutputStream fos;
	
//	D2H dh = new D2H("data/s_MIPS.xlsx");
//	dh.doTrans();
	public D2H(String file) throws IOException{
		filepath = file;
		inputStream = new FileInputStream(filepath);
		srcFile = new XSSFWorkbook(inputStream);
		srcSheet = srcFile.getSheetAt(0);
	}
	
	public void doTrans() throws IOException {		
		int i, j, no;
		String cell, os, ns;
		i = 0;
		no = 0;
		String[] cells;
		while (!getCell(srcSheet, i, 2).isEmpty()){
			cell = getCell(srcSheet, i, 2);
			os = cell;
			if (cell.contains("IMem[{pid,addr}]=")){
				cell = cell.substring(18, cell.length() - 1);
//				System.out.println(cell);
				ns = cell;
				cells = cell.split(",");
				cell = "";
				Pattern pattern = Pattern.compile("[0-9]*");
				for (j = 0; j < cells.length; j++){
					if(pattern.matcher(cells[j]).matches()){
						int temp = Integer.parseInt(cells[j]);
						if (temp <= 16) {
							cells[j] = "0x0" + Integer.toHexString(temp);
						}
						else {
							cells[j] = "0x" + Integer.toHexString(temp);
						}
					}
					cell = cell + "," + cells[j];
				}
				cell = cell.substring(1);
				cell = os.replace(ns, cell);
				setCell(srcSheet, i, 2, cell);
//				System.out.println(cell);
			}
			else if (cell.contains("ICache[addr]=")){
				cell = cell.substring(15, cell.indexOf('}'));
				ns = cell;
//				System.out.println(cell);
				cells = cell.split(",");
				cell = "";
				Pattern pattern = Pattern.compile("[0-9]*");
				for (j = 0; j < cells.length; j++){
					if(pattern.matcher(cells[j]).matches()){
						int temp = Integer.parseInt(cells[j]);
						if (temp <= 16) {
							cells[j] = "0x0" + Integer.toHexString(temp);
						}
						else {
							cells[j] = "0x" + Integer.toHexString(temp);
						}
					}
					cell = cell + "," + cells[j];
				}
				cell = cell.substring(1);
				cell = os.replace(ns, cell);
				setCell(srcSheet, i, 2, cell);
//				System.out.println(cell);
			}
			i++;
		}
		

		fos = new FileOutputStream(filepath);
		srcFile.write(fos);
		System.out.println("add D2H succeed ...");	
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
