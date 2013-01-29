package at.tripwire.ledcontrol.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import at.tripwire.ledcontrol.android.ColorPickerDialog.OnColorChangedListener;

public class MainActivity extends Activity {

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
		}
	};

	private Messenger serviceMessenger;
	private Messenger localMessenger = new Messenger(new IncomingHandler());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button turnOn = (Button) findViewById(R.id.btnTurnOn);
		turnOn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				turnOn();
			}
		});

		Button turnOff = (Button) findViewById(R.id.btnTurnOff);
		turnOff.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				turnOff();
			}
		});

		Button sleep = (Button) findViewById(R.id.btnSleep);
		sleep.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sleep();
			}
		});

		Button setColor = (Button) findViewById(R.id.btnSetColor);
		setColor.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setColor();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		bindService(new Intent(this, LedService.class), serviceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		unbindService(serviceConnection);
		super.onStop();
	}

	private void turnOff() {
		sendServiceMessage(LedService.MSG_TURN_OFF, null);
	}

	private void turnOn() {
		sendServiceMessage(LedService.MSG_TURN_ON, null);
	}

	private void sleep() {

	}

	private void setColor() {
		ColorPickerDialog pickerDialog = new ColorPickerDialog(this, new OnColorChangedListener() {

			@Override
			public void colorChanged(int color) {
				int r = (color >> 16) & 0xFF;
				int g = (color >> 8) & 0xFF;
				int b = (color >> 0) & 0xFF;
				Bundle data = new Bundle();
				data.putInt(LedService.KEY_RED, r);
				data.putInt(LedService.KEY_GREEN, g);
				data.putInt(LedService.KEY_BLUE, b);
				sendServiceMessage(LedService.MSG_SET_COLOR, data);
			}
		}, Color.WHITE);
		pickerDialog.show();
	}

	private void sendServiceMessage(int what, Bundle data) {
		Message msg = new Message();
		msg.what = what;
		if (data != null) {
			msg.setData(data);
		}
		msg.replyTo = localMessenger;
		try {
			serviceMessenger.send(msg);
		} catch (RemoteException e) {
			Log.e("MainActivity", "Unable to send message.", e);
		}
	}

	private class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LedService.MSG_SUCCESS:
				Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
				break;
			case LedService.MSG_ERROR:
				Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}
}
