package com.plesba.ioio_logger;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

/* This will write files to the "external storage area", which may or may not be 
 * on removeable media. You'll need to mount the phone on a computer to retrieve
 * the files, or have a FileManager app installed on the phone. 
 */
public class FileWriter {
	private String TAG = "pickledata";
	private final int TEMP_MAX = 5; // batch writes into this size
	private File dir; 
	private File syslogFile, dataFile;
	private int syslogTmpCt = 0;
	private int dataTmpCt = 0;
	private String syslogTmpStr = "";
	private String dataTmpStr = "";
	private boolean storageAvailable, storageWritable;
	private SimpleDateFormat filenameFormat, logFormat;
	// this class is intended to be a Singleton
	private static FileWriter INSTANCE;

	public static FileWriter getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new FileWriter();
		}
		return INSTANCE;
	}

	@SuppressLint("SimpleDateFormat")
	private FileWriter() {
		// see if we can write to the "external storage" -- if
		// it's currently mounted to a computer, we probably can't.
		try {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				// We can read and write the media
				storageAvailable = storageWritable = true;
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
				// We can only read the media
				storageAvailable = true;
				storageWritable = false;
				Log.w(TAG, "can't write to storage");
			} else {
				// Something else is wrong.
				storageAvailable = storageWritable = false;
				Log.w(TAG, "can't access storage in its current state: "
						+ state);
			}
			// set a log timestamp format only for syslog entries;
			logFormat = new SimpleDateFormat("HH:mm:ss.SSS");

			// set up the directory where we'll write
			File sdCard = Environment.getExternalStorageDirectory();
			dir = new File(sdCard.getAbsolutePath() + "/" + TAG);
			dir.mkdirs();

			// build the filename timestamp suffixes
			filenameFormat = new SimpleDateFormat("_yyyy-MM-dd_HHmmss");
			// NO COLONS!! VERBOTTEN!!
            rollFiles();
		} catch (Exception e) {
			Log.i(TAG, "SHIT: " + e.getMessage());
		}
	}

	public void rollFiles() {
		if (syslogFile != null) {
			this.writeToSyslogFile();
			Log.i(TAG, "rolling files; old syslog file closed out");
		}
		if (dataFile != null) { 
			this.writeToDataFile();
			Log.i(TAG, "rolling files; old data file closed out");
		}

		// NO COLONS!! VERBOTTEN!!
		String fileTS = filenameFormat.format(new Date());
		// create the files
		syslogFile = new File(dir, "syslog" + fileTS + ".txt");
		dataFile = new File(dir, "Data" + fileTS + ".txt");
		Log.i(TAG, "new files in place at " + fileTS);
	}
	
	// we add a timestamp to each syslog entry
	// & we batch up the entries before we write
	public void syslog(String s) {
		Log.i(TAG, "syslogging: " + s);
		String ts = logFormat.format(new Date());
		this.syslogTmpStr += "\n" + ts + ": " + s;
		this.syslogTmpCt++;
		if (this.syslogTmpCt >= this.TEMP_MAX) {
			Boolean done = false;
			while (!done) {
				done = this.writeToSyslogFile();
			}
		}
	}

	// data coming in should be CSV, one line at a time
	// if you want a timestamp, include it in the data
	// & we batch up the entries before we write
	public void data(String csv) {
		this.dataTmpStr += "\n" + csv;
		this.dataTmpCt++;
		if (this.dataTmpCt >= this.TEMP_MAX) {
			Boolean done = false;
			while (!done) {
				done = this.writeToDataFile();
			}
		}
	}

	// the actual writing
	private Boolean writeToSyslogFile() {
		if (this.storageAvailable && this.storageWritable) {
			byte[] data = this.syslogTmpStr.getBytes();
			try {
				FileOutputStream stream = new FileOutputStream(syslogFile, true);
				stream.write(data);
				stream.flush();
				stream.close();

				this.syslogTmpStr = "";
				this.syslogTmpCt = 0;

				//Log.i(TAG, "syslog succesfully written");
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			Log.w(TAG, "syslog storage problem");
		}
		return false;
	}

	private Boolean writeToDataFile() {
		if (this.storageAvailable && this.storageWritable) {
			byte[] data = this.dataTmpStr.getBytes();
			try {
				FileOutputStream stream = new FileOutputStream(dataFile, true);
				stream.write(data);
				stream.flush();
				stream.close();

				this.dataTmpStr = "";
				this.dataTmpCt = 0;

				//Log.i(TAG, "data succesfully written");
				return true;

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		} else {
			Log.w(TAG, "data storage problem");
		}
		return false;
	}

	public void finish() {
		this.writeToDataFile();
		this.writeToSyslogFile();
	}

}
