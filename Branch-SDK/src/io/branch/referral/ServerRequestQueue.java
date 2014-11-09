package io.branch.referral;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import android.content.Context;
import android.content.SharedPreferences;

public class ServerRequestQueue {
	
	// This is thread-safe because static member variables initialized are guaranteed to be
	// created the first time they are accessed, hence lazy instantiation, too!
	private static final ServerRequestQueue SharedInstance = new ServerRequestQueue();
	
	private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
	private LinkedList<ServerRequest> queue;

    public static ServerRequestQueue getInstance(Context c) {
   		SharedInstance.initSharedPrefs(c);
    	return SharedInstance;
    }
    
    private ServerRequestQueue () {
    	queue = (LinkedList<ServerRequest>)Collections.synchronizedList(new LinkedList<ServerRequest>());
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
				synchronized (queue) {
					Iterator<ServerRequest> iter = queue.iterator();
					while (iter.hasNext()) {
//						editor.put
						System.out.println("item: " + iter.next());
					}
				}
			}
		}).start();
    }
    
//    /** Read the object from Base64 string. */
//    private static Object fromString( String s ) throws IOException ,
//                                                        ClassNotFoundException {
//         byte [] data = Base64Coder.decode( s );
//         ObjectInputStream ois = new ObjectInputStream( 
//                                         new ByteArrayInputStream(  data ) );
//         Object o  = ois.readObject();
//         ois.close();
//         return o;
//    }
//
//     /** Write the object to a Base64 string. */
//     private static String toString( Serializable o ) throws IOException {
//         ByteArrayOutputStream baos = new ByteArrayOutputStream();
//         ObjectOutputStream oos = new ObjectOutputStream( baos );
//         oos.writeObject( o );
//         oos.close();
//         return new String( Base64Coder.encode( baos.toByteArray() ) );
//     }
    
	public int getSize() {
		return queue.size();
	}
	
	public void enqueue(ServerRequest request) {
		if (request != null) {
			queue.add(request);
			persist();
		}
	}
    
//	- (void)enqueue:(ServerRequest *)request;
//	- (ServerRequest *)dequeue;
//	- (ServerRequest *)peek;
//	- (ServerRequest *)peekAt:(unsigned int)index;
//	- (void)insert:(ServerRequest *)request at:(unsigned int)index;
//	- (ServerRequest *)removeAt:(unsigned int)index;
//	- (void)persist;
//
//	- (BOOL)containsInstallOrOpen;
//	- (BOOL)containsClose;
//	- (void)moveInstallOrOpenToFront:(NSString *)tag;
//
//	+ (id)getInstance;
}
