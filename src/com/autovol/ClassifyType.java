package com.autovol;

import android.content.Context;

public enum ClassifyType {
	//KNN ("/AutoVolWeb/KnnClassifyServlet", 1), 
	//LOC_KNN ("/AutoVolWeb/LocKnnClassifyServlet", 1),
	//AVG_KNN ("/AutoVolWeb/AvgKnnClassifyServlet", 8), 
	//AVG_LOC_KNN ("/AutoVolWeb/AvgLocKnnClassifyServlet", 8), 
	//CLUSTER_KNN ("/AutoVolWeb/ClusterLocKnnClassifyServlet", 8), 
//	PROB_LOC_KNN ("/AutoVolWeb/EmLocKnnClassifyServlet", 1), 
	RF ("/AutoVolWeb/RfClassifyServlet", 1),
//	AVG_RF("/AutoVolWeb/AvgRfClassifyServlet", 8),
//	PROB_LOC_RF("/AutoVolWeb/EmLocRfClassifyServlet", 1),
	MAIN("/AutoVolWeb/MainClassifyServlet", 4);
	
	private final String reqUrl;
	private final int numStates;
	
	private ClassifyType(String url, int numStates) {
		reqUrl = url;
		this.numStates = numStates;
	}
	
	public String constructUrl(Context c) {
		String target = getTarget();
		if (target == null) {
			return null;
		}
    	String url = AppPrefs.getBaseUrl(c) + reqUrl + "?" + 
    			"target=" + target +
    			"&user=" + AppPrefs.getAccountHash(c);
    	return url;
	}
	
	private String getTarget() {
		if (numStates == 1) {
			return CurrentStateListener.get().currentStateJson();
		} else {
			 return CurrentStateListener.get().recentStatesJson(numStates);
		}
	}
}
