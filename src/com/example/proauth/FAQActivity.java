

package com.example.proauth;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
		TextView contents  = (TextView) findViewById(R.id.faq_content);
		contents.setMovementMethod(new ScrollingMovementMethod());
	}
}
