package com.karate.api;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class JavaUtil {

	public static int stringToInt(String myString) {
		int myInt = Integer.parseInt(myString);
		return myInt;
	}

	public static String convString(String myString) {
		String myNewString = myString.toString();
		System.out.println("response.completed"+myString);
		return myNewString;
	}
	
	
	public static String fileChoose() throws Exception {

		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

		int returnValue = jfc.showOpenDialog(null);
		// int returnValue = jfc.showSaveDialog(null);

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			System.out.println(selectedFile.getAbsolutePath());
			returnValue = 0;
			return selectedFile.getAbsolutePath();
			
		}
		return null;

	}


}