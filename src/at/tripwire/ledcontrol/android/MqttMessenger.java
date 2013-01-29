package at.tripwire.ledcontrol.android;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import android.os.AsyncTask;
import android.util.Log;

public class MqttMessenger {

	public interface Listener {
		void success();
		
		void error();
	}
	
	private MqttClient client;
	
	public MqttMessenger(MqttClient client) {
		this.client = client;
	}
	
	public void sendMessage(final String msg, final Listener listener) {
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				if(client == null || !client.isConnected()) {
					return false;
				}
				
				MqttTopic topic = client.getTopic("led-control");
				MqttMessage message = new MqttMessage();
				message.setPayload(msg.getBytes());
				try {
					topic.publish(message);
					return true;
				} catch (MqttException e) {
					Log.w("MqttMessenger", "Unable to send message", e);
					return false;
				}
			}
			
			@Override
			protected void onPostExecute(Boolean success) {
				if(success) {
					listener.success();
				} else {
					listener.error();
				}
			}
		};
		
		task.execute();
	}
}
