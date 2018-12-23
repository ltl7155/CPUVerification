package cpu_model.dlu;

import java.util.ArrayList;


import java.util.HashMap;

import proving_model.Axiom;
import proving_model.Conditions;
import proving_model.Formula;
import proving_model.Justification;
import proving_model.PortDataFormula;
import proving_model.Procedure;
import proving_model.Proof;
import proving_model.RegContentFormula;
import cpu_model.cpu.CPU;
import cpu_model.element.CtrlPort;
import cpu_model.element.Data;
import cpu_model.element.DataPort;
import cpu_model.element.Element;
import cpu_model.element.Reg;

// 数字逻辑部件
// 抽象类
public abstract class DLU {

//	 部件名称
	protected String name;
//	寄存器类DLU值
	protected Reg reg;
	
//	 元素集合
	private ArrayList<Element> elementList = new ArrayList<Element>();
	
	public ArrayList<Axiom> axioms;
	
//	 每个阶段都要初始化，清空数据
	public void clear() {
		for(Element e : elementList)
			e.clear();
	}
	
//	 部件功能
	public void applyTheorems() {
		int i, j;
		for (i = 0; i < axioms.size(); i++){
			for (j = 0; j < axioms.get(i).getList().size(); j++){
				doApply(axioms.get(i).getName(), axioms.get(i).getList().get(j));
			}
		}
	}
	
	
	public String getName() {
		return name;
	}
	
	public ArrayList<Element> getElementList() {
		return elementList;
	}
	
//	 添加一个DataPort
	protected DataPort addDataPort(DLU dlu, String name) {
		DataPort dp = new DataPort(dlu, name);
		Element e = (Element) dp;
		elementList.add(e);
		return dp;		
	}
		
//	 添加一个CtrlPort（Mux）
	protected CtrlPort addCtrlPort(DLU dlu, String name) {
		CtrlPort cp = new CtrlPort(dlu, name);
		Element e = (Element) cp;
		elementList.add(e);
		return cp;		
	}
	
//	 添加一个CtrlPort（寄存器）
	protected CtrlPort addCtrlPort(DLU dlu, String name, String sign) {
		CtrlPort cp = new CtrlPort(dlu, name, sign);
		Element e = (Element) cp;
		elementList.add(e);
		return cp;		
	}
	
//	 添加一个Reg
	protected Reg addReg(DLU dlu, String name) {
		Reg r = new Reg(dlu, name);
		Element e = (Element) r;
		elementList.add(e);
		return r;		
	}	
	
	protected Element getPort(String portName){
		int i;
		for (i = 0; i < elementList.size(); i++){
			if (elementList.get(i).nameIs(portName)){
				break;
			}
		}
		if (i < elementList.size()){
			return elementList.get(i);
		}
		else{
			return null;
		}
	}
	
	protected void doApply(String name, String s){
		try{
//			allTh中存放的是公理所转变成的公式，可以直接进行推导的公式 
			ArrayList<String> allTh = new ArrayList<String>();
			if (s.contains("[line_#*#]")){
				if (!reg.hasLastContent()){
					return;
				}
				else{
//					就是把比如Icache里边存的地址和值，一条条地转换成公式放进allTh中，实际上是公理转化成公式的过程
					for (Procedure a : reg.getLastContentList()) {
						String addr = a.getAddr().getName();
						if (addr.contains("line_")){
							String strAddr = s.replaceAll("line_#\\*#", addr);
	//						System.out.println(strAddr);
							allTh.add(s.replaceAll("line_#\\*#", addr));
						}
					}
				}
			}
			else if (s.contains("[#*#]")){
				if (!reg.hasLastContent()){
					return;
				}
				else{
					for (Procedure a : reg.getLastContentList()) {
						String addr = a.getAddr().getName();
						if (!addr.contains("line_")){
							allTh.add(s.replaceAll("#\\*#", addr));
						}
					}
				}
			}
			else{
				allTh.add(s);
			}
//			下面是利用dlu的公理和现有的寄存器值推导出当前阶段的寄存器值放进当前阶段的Proof中
			int no;
			for (no = 0; no < allTh.size(); no++){
				int i;
				String[] theorems;
				String[] pre;
				String[] post;
				ArrayList<Procedure> conditionList = new ArrayList<Procedure>();
	//			<#No#, value>
				HashMap<String, String> valMap = new HashMap<String, String>(); 
				theorems = allTh.get(no).split(" \\|- ");
	//			条件列表
				pre = theorems[0].split(", ");
				
	//			检查条件
				for (i = 0; i < pre.length; i++){
					String[] strPort = pre[i].split("=");
	//				端口数据公式、控制信号公式:dataPort和ctrlPort
//					需要做的是 把#1#跟寄存器实际的值对应起来放在valMap里，以及判断控制信号是否符合要求，放到conditionList里
					if (strPort[0].indexOf('[') < 0){
//						CU.ICacheHit和CU.DCacheHit
						if (strPort[0].contains("CU") && strPort[0].contains("Hit")){
	//						System.out.println(name + "," + s);
//							因为这两个是CU的条件值,其结果在上个阶段已经产生了
//							这个条件的赋值如果已经给定是1了，但公理要求是0，则停止推导该公理
//							这个条件的赋值如果给定是0，但公理要求是1，也直接退回去不推导
//							遗留一个疑惑是这俩值在哪部分代码中被set的,应该是在根据通路进行推导的那里
							if ((Conditions.judge(strPort[0]) && strPort[1].equals("0"))){
								return;
							}
							else if ((!Conditions.judge(strPort[0]) && strPort[1].equals("1"))){
								return;
							}
						}
						else{
							Element t = getPort(strPort[0]);
//							如果是数据端口，将值和表示放到valMap里
							if (t.isDataPort()){
								DataPort dp = (DataPort)t;
								if (dp.hasData()){
									if (strPort[1].charAt(0) == '#'){
										valMap.put(strPort[1], dp.getData().getName());
										conditionList.add(dp.getPortData());
									}
									else if (strPort[1].charAt(0) == '{' && strPort[1].contains("#")){
										String tmp = strPort[1].substring(1, strPort[1].length() - 1);
										String[] des = tmp.split(",");
										tmp = dp.getData().getName();
										tmp = tmp.substring(1, tmp.length() - 1);
										String[] bytes = tmp.split(",");
										if (des.length == bytes.length){
											int it;
											for (it= 0; it < des.length; it++){
												valMap.put(des[it], bytes[it]);
											}
										}
										else{
											return;
										}
									}
//									如果是硬性要求一个具体的值，发现寄存器现在的值不符合的话就直接返回
									else{
										if (!dp.getData().getName().equals(strPort[1])){
											return;
										}
									}
								}
								else{
									return;
								}
							}
//							如果是控制端口，如果符合条件，加到conditionList列表中
							else if (t.isCtrlPort()){
								CtrlPort cp = (CtrlPort)t;
								if (strPort[1].equals("0") && cp.notActive()){
									conditionList.add(cp.getCtrlSignal());
								}
								else if (strPort[1].equals("1") && cp.isActive()){
									conditionList.add(cp.getCtrlSignal());
								}
								else{
									return;
								}
							}
						}
					}
	//				寄存器公式 _[R]=#No#
					else if (strPort[0].indexOf("_[") == 0){
						if (!reg.hasLastContent()){
							return;
						}
						Procedure a = reg.getLastContent();
						
						conditionList.add(a);
						valMap.put(strPort[1], a.getData().getName());
					}
	//				寄存器组公式 _R[addr]=#No#
					else if (strPort[0].indexOf('[') > 0){
						if (!reg.hasLastContent()){
							return;
						}
						String regName = strPort[0].substring(strPort[0].indexOf('[') + 1, strPort[0].length() - 1);
						boolean test = true;
						while (regName.contains("#")){
							int start = regName.indexOf('#'); 
							int end = regName.indexOf("#", start + 1);
							String key = regName.substring(start, end + 1);
							if (valMap.containsKey(key)){
								regName = regName.replaceAll(key, valMap.get(key));
							}
							else{
								test = false;
								break;
							}
							
						}
//						如果"# #"没有找到对应的值，就把他挪到最后面去最后处理,是不是应该加个if (pre.length - 1 != i )才换
						if (!test){
							String temp = pre[pre.length - 1];
							pre[pre.length - 1] = pre[i];
							pre[i] = temp;
							i--;
							continue;
						}
						if (regName.charAt(0) == '#'){
							if (valMap.containsKey(regName)){
								regName = valMap.get(regName);
							}
							else{
								String temp = pre[pre.length - 1];
								pre[pre.length - 1] = pre[i];
								pre[i] = temp;
								i--;
								continue;
							}
						}
						boolean find = false;
//						对付不带#的，像_CP0[ASID]这种
						for (Procedure a : reg.getLastContentList()) {
							if (!a.getAddr().nameIs(regName)){
								continue;
							}
							conditionList.add(a);
							valMap.put(strPort[1], a.getData().getName());
							find = true;
							break;
						}
						if (!find){
							return;
						}
					}
				}
				while (theorems[1].contains("#")){
					int start = theorems[1].indexOf('#');
					int end = theorems[1].indexOf("#", start + 1);
					String key = theorems[1].substring(start, end + 1);
					theorems[1] = theorems[1].replaceAll(key, valMap.get(key));
				}
	//			结论列表
				post = theorems[1].split(", ");
				for (i = 0; i < post.length; i++){
					String[] strPort = post[i].split("=");
					if (strPort[0].indexOf('[') < 0){
						Element t = getPort(strPort[0]);
						DataPort dp = (DataPort)t;
						if (dp.hasData()){
							return;
						}
					}
					else if (strPort[0].indexOf("[") == 0){ 
						if (reg.hasCurContent() == true){
							return;
						}
					}
					else if (strPort[0].indexOf("[") > 0){
						String regName = strPort[0].substring(strPort[0].indexOf('[') + 1, strPort[0].length() - 1);
						for (Procedure pd : reg.getCurContentList()){
							if (pd.getAddr().nameIs(regName)){
								return;
							}
						}
					}
				}
				Justification j = new Justification(name, conditionList);
				for (i = 0; i < post.length; i++){
					Formula f = null;
					String[] strPort = post[i].split("=");
	//				端口数据公式、控制信号公式
					if (strPort[0].indexOf('[') < 0){
						Element t = getPort(strPort[0]);
						Data data = CPU.addData(strPort[1]);
						f = new PortDataFormula((DataPort)t, data);
					}
	//				寄存器公式 [R]=#No#
					else if (strPort[0].indexOf("[") == 0){
						Data data = CPU.addData(strPort[1]);
						f = new RegContentFormula(reg, data);
					}
	//				寄存器组公式 R[addr]=#No#
					else if (strPort[0].indexOf("[") > 0){
						String regName = strPort[0].substring(strPort[0].indexOf('[') + 1, strPort[0].length() - 1);
						Data dWReg = CPU.addData(regName);
						Data dWData = CPU.addData(strPort[1]);
						f = new RegContentFormula(reg, dWReg, dWData);
					}
					Proof.add(f, j);
				}
			}
		}
		catch (Exception e){
			System.out.println(name + ":" + s);
			e.printStackTrace();
			
		}
	}
	
}
