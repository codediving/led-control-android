package at.tripwire.ledcontrol.android;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class LedService extends Service {

	public static final int MSG_SET_COLOR = 90;
	public static final int MSG_SLEEP = 91;
	public static final int MSG_TURN_OFF = 92;
	public static final int MSG_TURN_ON = 93;
	public static final int MSG_FADE = 94;
	public static final int MSG_SUCCESS = 95;
	public static final int MSG_ERROR = 96;
	
	public static final String KEY_SLEEP_SEC = "sleep.seconds";
	public static final String KEY_RED = "red";
	public static final String KEY_GREEN = "green";
	public static final String KEY_BLUE = "blue";
	public static final String KEY_FADE_MS = "fade.ms";
	
	private Messenger messenger = new Messenger(new IncomeHandler());
	private MqttClient mqttClient;
	private MqttMessenger mqttMessenger;

	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Thread thread = new Thread("MqttInitializer"){

			@Override
			public void run() {
				try {
					mqttClient = new MqttClient("tcp://tripwire.at:1883", "android-led-client", new MemoryPersistence());
					mqttClient.connect();
					mqttMessenger = new MqttMessenger(mqttClient);
				} catch (MqttException e) {
					Log.e("", "Unable to connect to MQTT server", e);
				}
			}
		};
		thread.start();
	}
	
	@Override
	public void onDestroy() {
		if (mqttClient != null && mqttClient.isConnected()) {
			Thread thread = new Thread("MqttDisconnecter") {
				@Override
				public void run() {
					try {
						mqttClient.disconnect();
					} catch (MqttException e) {
					}
				}
			};
			thread.start();
		}
		super.onDestroy();
	}

	private class IncomeHandler extends Handler {
		
		public void handleMessage(final android.os.Message msg) {
			Bundle data = msg.getData();
			StringBuilder builder = new StringBuilder();
			switch(msg.what) {
			case MSG_TURN_ON:
				builder.append("turnOn");
				break;
			case MSG_TURN_OFF:
				builder.append("turnOff");
				break;
			case MSG_FADE:
				builder.append("fade;");
				builder.append(data.getInt(KEY_RED));
				builder.append(";");
				builder.append(data.getInt(KEY_GREEN));
				builder.append(";");
				builder.append(data.getInt(KEY_BLUE));
				builder.append(";");
				builder.append(data.getLong(KEY_FADE_MS));
				break;
			case MSG_SET_COLOR:
				builder.append("set;");
				builder.append(data.getInt(KEY_RED));
				builder.append(";");
				builder.append(data.getInt(KEY_GREEN));
				builder.append(";");
				builder.append(data.getInt(KEY_BLUE));
				break;
			case MSG_SLEEP:
				builder.append("sleep;");
				builder.append(data.getLong(KEY_SLEEP_SEC));
				break;
			}
			String stringMsg = builder.toString();
			Log.i("LedService", "Sending MQTT message: " + stringMsg);
			mqttMessenger.sendMessage(stringMsg, new MqttMessengerCallback(msg.replyTo));
		}
	}
	
	private static class MqttMessengerCallback implements MqttMessenger.Listener {

		private Messenger replyTo;
		
		public MqttMessengerCallback(Messenger replyTo) {
			this.replyTo = replyTo;
		}
		
		@Override
		public void success() {
			sendResult(MSG_SUCCESS);
		}

		@Override
		public void error() {
			sendResult(MSG_ERROR);			
		}
		
		private void sendResult(int what) {
			Message resultMsg = new Message();
			resultMsg.what = what;
			try {
				replyTo.send(resultMsg);
			} catch (RemoteException e) {
			}
		}
	}
}
