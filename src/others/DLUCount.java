package others;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DLUCount {
	private int ColIns = 0;
	private int ColSta = 1;
	private int ColSrc = 2;
	private int ColDest = 3;
	private XSSFWorkbook srcFile = null;
	private XSSFSheet srcSheet = null;
	private String filepath;
	private FileInputStream inputStream;
	private ArrayList<String> dluNameList;
	private ArrayList<ArrayList<ArrayList<String>>> dluCountList = null;
	private int stageSum = 5;
	
//	DLUCount dc = new DLUCount("data/pipeline_list.xlsx");
//	dc.doCount();
	public DLUCount(String file) throws IOException {
		filepath = file;
		inputStream = new FileInputStream(filepath);
		srcFile = new XSSFWorkbook(inputStream);
		srcSheet = srcFile.getSheetAt(0);
		dluNameList = new ArrayList<String>();
		dluCountList = new ArrayList<ArrayList<ArrayList<String>>>();
	}
	
	public void doCount() throws IOException {		
		int i, stage;
		String insName = "";
		i = 0;
		stage = stageSum * 2 - 1;
		while (!getCell(srcSheet, i, ColSta).isEmpty() || !getCell(srcSheet, i, ColSrc).isEmpty()) {
			if (!getCell(srcSheet, i, ColIns).isEmpty()) {
				insName = getCell(srcSheet, i, ColIns);
				insName = insName.substring(insName.indexOf('.') + 1, insName.length());
			}
			if (!getCell(srcSheet, i, ColSta).isEmpty()){
				stage = (stage + 1) % 10;
			}
			if (!getCell(srcSheet, i, ColSrc).isEmpty()){
				String left = getCell(srcSheet, i, ColSrc);
				String right = getCell(srcSheet, i, ColDest);
				if (!left.contains("'")){
					if (left.contains(".")){
						left = left.substring(0, left.indexOf('.'));
					}
					addItem(left, insName, stage);
				}
				if (stage % 2 == 0){
					if (right.contains(".")){
						right = right.substring(0, right.indexOf('.'));
					}
					addItem(right, insName, stage);
				}
			}
			i++;
		}
		
		for (String dluName : dluNameList){
			System.out.println(dluName + ":");
			int no = dluNameList.indexOf(dluName);
			for (ArrayList<String> dluItem : dluCountList.get(no)){
				System.out.println(dluItem.toString());
			}
		}
//		合并结果写入目标文件
//		fos = new FileOutputStream(filepath);
//		srcFile.write(fos);
//		System.out.println("eliminate duplicate succeed ...");	
	}
	
	private void addItem(String dluName, String insName, int stage){
		int i, no;
		if (!dluNameList.contains(dluName)){
			dluNameList.add(dluName);
			ArrayList<ArrayList<String>> dluItem = new ArrayList<ArrayList<String>>();
			for (i = 0; i < stageSum; i++){
				dluItem.add(i, new ArrayList<String>());
			}
			dluItem.get(stage / 2).add(insName);
			dluCountList.add(dluItem);
		}
		else{
			no = dluNameList.indexOf(dluName);
			if (!dluCountList.get(no).get(stage / 2).contains(insName)){
				dluCountList.get(no).get(stage / 2).add(insName);
			}
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
		if (row==null){
			return "";
		}
		XSSFCell cell = row.getCell(cellNum);
		if (cell==null){
			return "";
		}
		if (cell.getCellType()==XSSFCell.CELL_TYPE_BLANK){
			return "";
		}
		return cell.toString();
	}
}
