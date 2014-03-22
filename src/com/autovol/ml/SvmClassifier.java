package com.autovol.ml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

import weka.classifiers.functions.SMO;
import weka.core.Instance;
import android.util.Log;

public class SvmClassifier {
	
	private SMO smo;
/*	
	public static SvmClassifier createSvmClassifier(Instances data) {
		SvmClassifier classifier = new SvmClassifier();
		try {
			classifier.trainClassifier(data);
			return classifier;
		} catch (Exception e) {
			Log.e("SvmClassifier", "Error training SVM");
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	private void trainClassifier(Instances data) throws Exception {
		smo = new SMO();
		smo.buildClassifier(data);
	}
	*/
	
	public static SvmClassifier loadSvmClassifier(InputStream is) {
		SvmClassifier classifier = new SvmClassifier();
		try {
			classifier.loadClassifier(is);
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return classifier;
	}
	
	private void loadClassifier(InputStream is) throws StreamCorruptedException, FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(is);
		smo = (SMO) ois.readObject();
		ois.close();
	}
	
	public double classify(Instance target) {
		try {
			double score = smo.classifyInstance(target);
			return score;
		} catch (Exception e) {
			Log.e("SvmClassifier", "error classifying");
			e.printStackTrace();
			return -1;
		}
	}

}
