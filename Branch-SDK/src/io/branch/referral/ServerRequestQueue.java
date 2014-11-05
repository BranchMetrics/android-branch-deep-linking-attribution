package io.branch.referral;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ServerRequestQueue {
	
	private ConcurrentLinkedDeque<ServerRequest> queue;
	private SharedPreferences mSharedQueue;
    private SharedPreferences.Editor mEditor;
    private int size;

    public ServerRequestQueue (Context c) {
        mSharedQueue = c.getSharedPreferences("BNC_Server_Request_Queue", Context.MODE_PRIVATE);
        mEditor = mSharedQueue.edit();
    }
    
	public int getSize() {
		return size;
	}
	
	public void enqueue(ServerRequest request) {
		
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
