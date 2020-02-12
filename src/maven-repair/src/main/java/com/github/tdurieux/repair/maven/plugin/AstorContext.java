package com.github.tdurieux.repair.maven.plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AstorContext {

	public  String mode;

	public String location;

	public List<String> dependencies = new ArrayList<>();

	public List<String> srcJavaFolder = new ArrayList<>();

	public List<String> srcTestFolder = new ArrayList<>();

	public List<String> binJavaFolder = new ArrayList<>();

	public List<String> binTestFolder = new ArrayList<>();

	public List<String> failing = new ArrayList<>();

	public String out;

	public String Package;

	public  String scope;

	public  double flThreshold;

	public  int seed;

	public  int maxGen;

	public  int maxTime;

	public  boolean stopFirst = false;

	public int javaComplianceLevel;

	public boolean skipfaultlocalization = false;



	public String[] getAstorArgs() {
		List<String> output = new ArrayList<>();
		Field[] declaredFields = this.getClass().getDeclaredFields();
		for (int i = 0; i < declaredFields.length; i++) {
			Field declaredField = declaredFields[i];
			try {
				Object value = declaredField.get(this);
				if (value == null) {
					continue;
				}
				StringBuilder strValue = new StringBuilder(value.toString());
				if (value instanceof Boolean) {
					if (Boolean.TRUE.equals(value)) {
						output.add("-" + declaredField.getName().toLowerCase());
						output.add("true");
					}
					continue;
				} else if (value instanceof Collection) {
					if (((Collection) value).isEmpty()) {
						continue;
					}
					strValue = new StringBuilder();
					for (Iterator iterator = ((Collection) value).iterator(); iterator.hasNext(); ) {
						Object v =  iterator.next();
						strValue.append(v);
						if (iterator.hasNext()) {
							strValue.append(":");
						}
					}
				}
				output.add("-" + declaredField.getName().toLowerCase());
				output.add(strValue.toString());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return output.toArray(new String[output.size()]);
	}
}
