package fr.cmoatoto.multishare.sender;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

public class KeyboardActivity extends Activity implements OnClickListener {

	private static final String TAG = KeyboardActivity.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View contentView = getLayoutInflater().inflate(R.layout.keyboard_activity, null);

		setContentView(contentView);

		contentView.findViewById(R.id.keyboard_button1_1).setOnClickListener(this);
		contentView.findViewById(R.id.keyboard_button1_2).setOnClickListener(this);
		contentView.findViewById(R.id.keyboard_button1_3).setOnClickListener(this);

		contentView.findViewById(R.id.keyboard_button2_1).setOnClickListener(this);
		contentView.findViewById(R.id.keyboard_button2_2).setOnClickListener(this);
		contentView.findViewById(R.id.keyboard_button2_3).setOnClickListener(this);

		contentView.findViewById(R.id.keyboard_button3_1).setOnClickListener(this);
		contentView.findViewById(R.id.keyboard_button3_2).setOnClickListener(this);
		contentView.findViewById(R.id.keyboard_button3_3).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.keyboard_button1_1:
			sendKeyEvent(KeyEvent.KEYCODE_HOME);
			break;
		case R.id.keyboard_button1_2:
			sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
			break;
		case R.id.keyboard_button1_3:
			sendKeyEvent(KeyEvent.KEYCODE_BACK);
			break;
		case R.id.keyboard_button2_1:
			sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);
			break;
		case R.id.keyboard_button2_2:
			sendKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER);
			break;
		case R.id.keyboard_button2_3:
			sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
			break;
		case R.id.keyboard_button3_1:
			sendKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN);
			break;
		case R.id.keyboard_button3_2:
			sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
			break;
		case R.id.keyboard_button3_3:
			sendKeyEvent(KeyEvent.KEYCODE_VOLUME_UP);
			break;
		}
	}

	private void sendKeyEvent(int keyEventCode) {
		//new BroadcastTask().execute(BroadcastTask.SEND_KEYBOARD_KEY, String.valueOf(keyEventCode));
	}

}
