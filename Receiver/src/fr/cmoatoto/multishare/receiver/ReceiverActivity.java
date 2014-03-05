package fr.cmoatoto.multishare.receiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class ReceiverActivity extends Activity {

	private static final String TAG = ReceiverActivity.class.getName();

	public static final String INTENT_SHOW_DIALOG = TAG + ".msgShowDialog";

	public static final String INTENT_SHOW_DIALOG_TXT = TAG + ".msgShowDialogTxt";

	private ReceiverActivity mActivity = this;

	private ImageSwitcher mImageSwitcher;

	private TextView mBackgroundAuthorTextView;

	private TextView mBackgroundNameTextView;

	private Handler mBackgroundHandler;

	private BroadcastReceiver mReceiver;

	private int mCurrentBackgroundIndex = 0;
	private TypedArray backgroundImages;
	private String[] backgroundNames;
	private final static int DELAY_BACKGROUND_SWITCH = 10 * 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_receiver);

		mImageSwitcher = (ImageSwitcher) findViewById(R.id.receiver_imageswitcher);
		mBackgroundAuthorTextView = (TextView) findViewById(R.id.receiver_backgroundauthor_textview);
		mBackgroundNameTextView = (TextView) findViewById(R.id.receiver_backgroundname_textview);

		mImageSwitcher.setFactory(new ViewFactory() {

			@Override
			public View makeView() {
				ImageView imageView = new ImageView(mActivity);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				return imageView;
			}

		});

		mBackgroundHandler = new Handler();
		backgroundImages = getResources().obtainTypedArray(R.array.background_images);
		backgroundNames = getResources().getStringArray(R.array.background_images_names);

		mBackgroundHandler.post(new Runnable() {

			@Override
			public void run() {
				mImageSwitcher.setImageDrawable(backgroundImages.getDrawable(mCurrentBackgroundIndex));
				mBackgroundNameTextView.setText(backgroundNames[mCurrentBackgroundIndex]);
				if (mCurrentBackgroundIndex == backgroundNames.length - 1) {
					mCurrentBackgroundIndex = 0;
				} else {
					mCurrentBackgroundIndex++;
				}
				mBackgroundHandler.postDelayed(this, DELAY_BACKGROUND_SWITCH);
			}

		});

		mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();
				final String txt = bundle.getString(INTENT_SHOW_DIALOG_TXT);
				if (bundle != null) {
					Builder builder = new AlertDialog.Builder(mActivity).setTitle("A message has been received").setMessage(txt)
							.setPositiveButton("Open in an Application", new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int id) {
									Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
									sharingIntent.setType("text/plain");
									sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, txt);
									startActivity(Intent.createChooser(sharingIntent, "Select Application"));
								}
							}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int id) {
								}
							});
					if (txt.contains("http")) {
						String tmp = txt.substring(txt.indexOf("http"));
						final String url = tmp.contains(" ") ? tmp.substring(0,  tmp.indexOf(" ")) : tmp;
						builder.setNeutralButton("Open url", new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int id) {
								Intent sharingIntent = new Intent(android.content.Intent.ACTION_VIEW);
								sharingIntent.setData(Uri.parse(url));
								startActivity(Intent.createChooser(sharingIntent, "Select Application"));
							}
						});
					}
					builder.show();
				}
			}
		};

		startService(new Intent(this, HttpServiceReceiver.class));

		registerReceiver(mReceiver, new IntentFilter(INTENT_SHOW_DIALOG));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
}
