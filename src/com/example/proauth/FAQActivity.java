

package com.example.proauth;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;


public class FAQActivity extends Activity {

	static FileInputStream log_file;
	public static String LOG_FILE = "proauth_log.txt";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_faq);

		File log_file_object = new File(this.getApplicationContext().getFilesDir(), LOG_FILE);
		/*
		TextView contents  = (TextView) findViewById(R.id.faq_content);
		try {
			log_file = new FileInputStream(log_file_object);
			FileReader fr=new FileReader(log_file_object);
	        BufferedReader br=new BufferedReader(fr);
	        String line = null;
	        try {
	            line = br.readLine();
	            while (null != line) {
	                contents.append(line);
	                contents.append("\n");
	                line = br.readLine();
	            }
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}			
		*/
	}
}
