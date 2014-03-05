package fr.cmoatoto.multishare.receiver;

import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class RemotedKeyboard extends InputMethodService {
	
	private static final String TAG = RemotedKeyboard.class.getName();

	private static RemotedKeyboard keyboard = null;
	private static KeyboardCreatedListener keyboardCreatedListener = null;

	@Override
	public void onCreate() {
		super.onCreate();
		keyboard = this;
		if (keyboardCreatedListener != null) {
			keyboardCreatedListener.keyboardCreated();
			keyboardCreatedListener = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		keyboard = null;
	}

	@Override
	public View onCreateInputView() {
		return new View(this);
	}

	@Override
	public View onCreateCandidatesView() {
		return new View(this);
	}

	/**
	 * Helper to send a key down / key up pair to the current editor.
	 */
	public void keyDownUp(int keyEventCode) {
		if (getCurrentInputConnection() != null) {
			getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
			getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
		}
	}

	private interface KeyboardCreatedListener {
		public void keyboardCreated();
	}

	private static void startKeyboardIfNeeded(Context c) {
		if (keyboard == null) {
			c.startService(new Intent(c, RemotedKeyboard.class));
		} else if (keyboardCreatedListener != null) {
			keyboardCreatedListener.keyboardCreated();
			keyboardCreatedListener = null;
		}
	}

	public static void sendKeyEvent(Context c, final int keyEventCode) {
		Log.d(TAG, "Received KeyCode " + KeyEvent.keyCodeToString(keyEventCode));
		if (keyEventCode == KeyEvent.KEYCODE_HOME) {
			// An App can't send KEYCODE_HOME...
			Intent i = new Intent();
			i.setAction(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			c.startActivity(i);
			return;
		}
		keyboardCreatedListener = new KeyboardCreatedListener() {

			@Override
			public void keyboardCreated() {
				keyboard.keyDownUp(keyEventCode);
			}
		};
		startKeyboardIfNeeded(c);
	}
}
