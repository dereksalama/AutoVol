package com.autovol.ml;

import weka.classifiers.functions.SMO;
import weka.core.Instance;
import weka.core.Instances;
import android.util.Log;

public class SvmClassifier {
	
	private SMO smo;
	
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
	
	private SvmClassifier() {}
	
	private void trainClassifier(Instances data) throws Exception {
		smo = new SMO();
		smo.buildClassifier(data);
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
