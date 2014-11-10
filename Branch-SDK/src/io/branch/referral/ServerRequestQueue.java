package io.branch.referral;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

public class ServerRequestQueue {
	
	// This is thread-safe because static member variables initialized are guaranteed to be
	// created the first time they are accessed, hence lazy instantiation, too!
	private static final ServerRequestQueue SharedInstance = new ServerRequestQueue();
	private static final String PREF_KEY = "BNCServerRequestQueue";
	
	private static SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
	private LinkedList<ServerRequest> queue;

    public static ServerRequestQueue getInstance(Context c) {
   		SharedInstance.initSharedPrefs(c);
    	return SharedInstance;
    }
    
    private ServerRequestQueue () {
    	queue = retrieve();
    }
    
    private void initSharedPrefs(Context c) {
    	if (sharedPref == null) {
    		sharedPref = c.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE);
    		editor = sharedPref.edit();
    	}
    }
    
    private void persist() {
    	new Thread(new Runnable() {
			@Override
			public void run() {
				LinkedList<ServerRequest> copyQueue = new LinkedList<ServerRequest>(queue);
				
				JSONArray jsonArr = new JSONArray();
				Iterator<ServerRequest> iter = copyQueue.iterator();
				while (iter.hasNext()) {
					jsonArr.put(iter.next().toJSON());
				}
				
				editor.putString(PREF_KEY, jsonArr.toString());
			}
		}).start();
    }
    
    private static LinkedList<ServerRequest> retrieve() {
    	LinkedList<ServerRequest> result = (LinkedList<ServerRequest>)Collections.synchronizedList(new LinkedList<ServerRequest>());
    	String jsonStr = sharedPref.getString(PREF_KEY, null);
    	
    	if (jsonStr != null) {
    		try {
    			JSONArray jsonArr = new JSONArray(jsonStr);
    			for (int i = 0; i < jsonArr.length(); i++) {
    				JSONObject json = jsonArr.getJSONObject(i);
    				ServerRequest req = ServerRequest.fromJSON(json);
    				result.add(req);
    			}
    		} catch (JSONException e) {
    		}
    	}
    	
    	return result;
    }
    
	public int getSize() {
		return queue.size();
	}
	
	public void enqueue(ServerRequest request) {
		if (request != null) {
			queue.add(request);
			persist();
		}
	}
    
	public ServerRequest dequeue() {
		ServerRequest req = null;
		try {
			req = queue.removeFirst();
			persist();
		} catch (NoSuchElementException ex) {
		}
		return req;
	}

	public ServerRequest peek() {
		ServerRequest req = null;
		try {
			req = queue.getFirst();
		} catch (NoSuchElementException ex) {
		}
		return req;
	}
	
	public ServerRequest peekAt(int index) {
		ServerRequest req = null;
		try {
			req = queue.get(index);
		} catch (NoSuchElementException ex) {
		}
		return req;
	}
	
	public void insert(ServerRequest request, int index) {
		try {
			queue.add(index, request);
			persist();
		} catch (IndexOutOfBoundsException ex) {
		}
	}
	
	public ServerRequest removeAt(int index) {
		ServerRequest req = null;
		try {
			req = queue.remove(index);
			persist();
		} catch (IndexOutOfBoundsException ex) {
		}
		return req;
	}

	private boolean containsInstallOrOpen() {
		synchronized(queue) {
			Iterator<ServerRequest> iter = queue.iterator();
			while (iter.hasNext()) {
				ServerRequest req = iter.next();
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsClose() {
		synchronized(queue) {
			Iterator<ServerRequest> iter = queue.iterator();
			while (iter.hasNext()) {
				ServerRequest req = iter.next();
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void moveInstallOrOpenToFront(String tag) {
		
	}
}
