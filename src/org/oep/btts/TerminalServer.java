package org.oep.btts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TerminalServer {
	
	private static final String TAG = "TerminalServer";
	private static final boolean D = true;
	
	
	protected Context mContext;
	protected Handler mHandler;
	protected int mState;
	
	private final BluetoothAdapter mAdapter;
	
	protected ListenThread mSecureListenThread;
	protected ListenThread mInsecureListenThread;
	protected ConnectedThread mConnectedThread;
	
	public TerminalServer(Context ctx, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mContext = ctx;
		mHandler = handler;
		mState = STATE_NONE;
	}
	
	private synchronized void setState(int state) {
		mState = state;
		mHandler.obtainMessage(MSG_STATE_CHANGED, mState, -1).sendToTarget();
	}
	
	public synchronized int getState() {
		return mState;
	}
	
	public synchronized void start() {
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		setState(STATE_LISTENING);
		
		if(mSecureListenThread == null) {
			mSecureListenThread = new ListenThread(true);
			mSecureListenThread.start();
		}
		
		if(mInsecureListenThread == null) {
			mInsecureListenThread = new ListenThread(false);
			mInsecureListenThread.start();
		}
	}
	
	public synchronized void stop() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureListenThread != null) {
        	mSecureListenThread.cancel();
        	mSecureListenThread = null;
        }

        if (mInsecureListenThread != null) {
        	mInsecureListenThread.cancel();
        	mInsecureListenThread = null;
        }
	}
	
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String type) {
		if(mSecureListenThread != null) {
			mSecureListenThread.cancel();
			mSecureListenThread = null;
		}
		
		if(mInsecureListenThread != null) {
			mInsecureListenThread.cancel();
			mInsecureListenThread = null;
		}
		
		if(mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		mConnectedThread = new ConnectedThread(socket, type);
		mConnectedThread.start();
		
		// TODO: Connection verification?
	}
	
	private void connectionLost() {
		mHandler.obtainMessage(MSG_CONNECTION_LOST).sendToTarget();
		TerminalServer.this.start();
	}
	
	private class ListenThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;
		private String mSocketType;
		
		public ListenThread(boolean secure) {
			BluetoothServerSocket tmp = null;
			mSocketType = (secure) ? "Secure":"Insecure";
			
			try {
				if(secure) {
					tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, UUID_SECURE);
				}
				else {
					// TODO: Insecure one?
					tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_INSECURE, UUID_INSECURE);
				}
			}
			catch(IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + " listen() failed", e);
			}
			
			mmServerSocket = tmp;
		}
		
		public void run() {
			if(D) Log.d(TAG, "ListenThread" + mSocketType + " BEGINS");
			setName("ListenThread" + mSocketType);
			
			BluetoothSocket socket = null;
			
			while(mState != STATE_CONNECTED) {
				try {
					Log.d(TAG, "ListenThread" + mSocketType + " is awaiting connections");
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "Socket Type " + mSocketType + " accept() failed", e);
					TerminalServer.this.start();
					break;
				}
				
				if(socket != null) {
					synchronized(TerminalServer.this) {
						switch(mState) {
						case STATE_LISTENING:
							connected(socket, socket.getRemoteDevice(), mSocketType);
							break;
							
						case STATE_NONE:
						case STATE_CONNECTED:
							try {
								socket.close();
							}
							catch(IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			
			if(D) Log.d(TAG, "ListenThread" + mSocketType + " ENDS");
		}
		
		public void cancel() {
			try {
				mmServerSocket.close();
				Log.i(TAG, "Closed listen " + mSocketType + " socket successfully");
			}
			catch(IOException e) {
				Log.e(TAG, "Socket Type " + mSocketType + " close() failed", e);
			}
		}
	}
	
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInputStream;
		private final OutputStream mmOutputStream;
		
		public ConnectedThread(BluetoothSocket socket, String type) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Temp input/output streams not created", e);
			}
			
			mmInputStream = tmpIn;
			mmOutputStream = tmpOut;
		}
		
		public void run() {
			if(D) Log.d(TAG, "ConnectedThread BEGINS");
			
			byte buffer[] = new byte[1024];
			int bytes;
			
			while(true) {
				try {
					bytes = mmInputStream.read(buffer);
					mHandler.obtainMessage(MSG_READ, bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "Disconnected.", e);
					connectionLost();
					TerminalServer.this.start();
					break;
				}
				
			}
			
			if(D) Log.d(TAG, "ConnectedThread ENDS");
		}
		
		public void cancel() {
			try {
				mmSocket.close();
				Log.i(TAG, "Closed connection socket successfully");
			}
			catch(IOException e) {
				Log.e(TAG, "Connected socket close() failed", e);
			}
		}
	}
	
    public static final String
    	NAME_INSECURE = "Insecure BTTS",
    	NAME_SECURE = "Secure BTTS";
    
    public static final UUID UUID_SECURE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID UUID_INSECURE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    
	public static final int
		STATE_NONE = 0,
		STATE_LISTENING = 1,
		STATE_AUTHORIZING = 2,
		STATE_CONNECTED = 3;

	public static final int
		MSG_READ = 1,
		MSG_CONNECTION_LOST = 2,
		MSG_STATE_CHANGED = 500;
}
