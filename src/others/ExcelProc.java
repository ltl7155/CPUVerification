package others;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class ExcelProc {
	public static void setCell(XSSFSheet sheet, int rowNum, int cellNum, String s) {
		XSSFRow row = sheet.getRow(rowNum);
		if(row == null) {
			row = sheet.createRow(rowNum);
		}
		XSSFCell cell = row.createCell((short)cellNum);
		cell.setCellType(XSSFCell.CELL_TYPE_BOOLEAN);
		cell.setCellValue(s);
	}

	
	//取出一个Cell并转为String，含空Cell判断
	public static String getCell(XSSFSheet sheet, int rowNum, int cellNum) {
		XSSFRow row = sheet.getRow(rowNum);
		if (row==null) {
			return "";
		}
		XSSFCell cell = row.getCell(cellNum);
		if (cell==null) {
			return "";
		}
		if (cell.getCellType()==XSSFCell.CELL_TYPE_BLANK) {
			return "";
		}
		return cell.toString();
	}
}
