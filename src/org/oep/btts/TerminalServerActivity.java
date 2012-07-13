package org.oep.btts;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class TerminalServerActivity extends Activity {
	private static final boolean D = true;
	private static final String TAG = "TerminalServerActivity";
	
	protected BluetoothAdapter mBluetoothAdapter;
	protected BluetoothSocket mConnectSocket;
	protected TerminalServer mTerminalServer;
	
	protected ArrayList<Byte> mByteQueue = new ArrayList<Byte>();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	
    	if(mBluetoothAdapter == null) {
    		Toast.makeText(this, R.string.msg_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
    		showMessage(R.string.msg_demo);
    		showDebugBytes();
    	}
    }
    
    public void onStart() {
    	super.onStart();
    	
    	if(mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
    		Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    		startActivityForResult(i, REQUEST_ENABLE_BT);
    	}
    	else if(mTerminalServer == null) {
    		setupTerminal();
    	}
    }
    
    public void onResume() {
    	super.onResume();
    	startListening();
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        // Stop the Bluetooth chat services
        if (mTerminalServer != null) mTerminalServer.stop();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch(requestCode) {
    	case REQUEST_ENABLE_BT:
    		if(resultCode == Activity.RESULT_OK) {
    			startListening();
    		}
    		else {
    			Log.d(TAG, "BT not enabled");
    			Toast.makeText(this, R.string.msg_bluetooth_not_enabled, Toast.LENGTH_LONG).show();
    			finish();
    		}
    		break;
    	}
    }
    
    public Dialog onCreateDialog(int id, Bundle args) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	switch(id){
    	case DIALOG_ACCEPT_CONNECTION:
    		return builder
    			.setTitle(R.string.title_accept_connection)
    			.setMessage(R.string.msg_accepted_connection)
    			.setCancelable(false)
    			.setPositiveButton(R.string.label_yes, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
//						spawnConnectionThread();
					}
    			})
    			.setNegativeButton(R.string.label_no, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startListening();
					}
    			})
    			.show();
    	}
    	
    	return builder
    		.setTitle("Invalid dialog ID")
    		.setMessage("Invalid dialog ID received.")
    		.setCancelable(true)
    		.show();
    }
    
    private void startListening() {
    	if(mBluetoothAdapter != null
    			&& mTerminalServer != null
    			&& mTerminalServer.getState() == TerminalServer.STATE_NONE) {
    		mTerminalServer.start();
    	}
    }
    
    private void setupTerminal() {
    	mTerminalServer = new TerminalServer(this, mHandler);
    }
    
    private void showDebugBytes() {
    	ArrayList<Byte> bytes = new ArrayList<Byte>();
    	for(int i = 0; i < 256; i++) {
    		bytes.add((byte) (i % 256));
    	}
    	((TerminalView)findViewById(R.id.terminal_output)).setBytes(bytes);
    }
     
    protected void showMessage(int rid) {
    	TextView tv = (TextView) findViewById(R.id.msg_output);
    	tv.setText(rid);
    }
    
    protected void receiveBytes(byte bytes[], int num) {
    	for(int i = 0; i < num; i++) {
    		mByteQueue.add(bytes[i]);
    	}
    }
    
    protected void onDisconnect() {
    	
    }
    
    protected void onStateChanged(int newState) {
    	switch(newState) {
    	case TerminalServer.STATE_NONE:
    		break;
    		
    	case TerminalServer.STATE_LISTENING:
    		showMessage(R.string.msg_listening);
    		break;
    		
    	case TerminalServer.STATE_CONNECTED:
    		showMessage(R.string.msg_accepted_connection);
    		break;
    	}
    }
    
    protected final Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    		case TerminalServer.MSG_CONNECTION_LOST:
    			onDisconnect();
    			break;
    			
    		case TerminalServer.MSG_READ:
    			byte readBuf[] = (byte[]) msg.obj;
    			receiveBytes(readBuf, msg.arg1);
    			break;
    			
    		case TerminalServer.MSG_STATE_CHANGED:
    			onStateChanged(mTerminalServer.getState());
    			break;
    		}
    	}
    };
    
    public static final int
    	DIALOG_ACCEPT_CONNECTION = 0x1337FACE,
    	REQUEST_ENABLE_BT = 0x1337;
}