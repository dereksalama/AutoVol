package com.autovol.ml;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.neighboursearch.KDTree;

public class KnnClassifier {
	
	private KDTree tree;
	private static final int DEFAULT_K = 3;
	
	public static KnnClassifier getKnnClassifier(String csvFilePath) throws IOException {
		KnnClassifier classifier = new KnnClassifier();
		File csvFile = new File(csvFilePath);
		classifier.loadCsvFile(csvFile);
		return classifier;
	}
	
	private KnnClassifier() {}
	
	private void loadCsvFile(File f) throws IOException {
		CSVLoader loader = new CSVLoader();
		loader.setSource(f);
		Instances data = loader.getDataSet();
		data.setClassIndex(data.numAttributes() - 1); // last attr = class
		tree = new KDTree(data);
	}
	
	public Instances classify(Instance target, int k) throws Exception {
		return tree.kNearestNeighbours(target, k);
	}
	
	public Integer defaultClassify(Instance target) throws Exception {
		Instances is = classify(target, DEFAULT_K);
		Map<Integer, Integer> classLabelCount = new HashMap<Integer, Integer>();
		for (int i = 0; i < is.numInstances(); i++) {
			Instance instance = is.instance(i);
			Integer classValue = Double.valueOf(instance.value(instance.classAttribute())).intValue();
			Integer count = classLabelCount.get(classValue);
			if (count == null) {
				count = Integer.valueOf(0);
				classLabelCount.put(classValue, count);
			}
			count += 1;
		}
		
		int maxCount = Collections.max(classLabelCount.keySet());
		for (Entry<Integer, Integer> e : classLabelCount.entrySet()) {
			if (e.getValue() == maxCount) {
				return e.getKey();
			}
		}
		return -1; // no max???
	}

}
