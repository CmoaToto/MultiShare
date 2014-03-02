package fr.cmoatoto.multishare.receiver;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;
import fr.cmoatoto.multishare.R;
import fr.cmoatoto.multishare.sender.HttpServiceSender;

public class ReceiverActivity extends Activity {

	private ReceiverActivity mActivity = this;

	private ImageSwitcher mImageSwitcher;

	private TextView mBackgroundAuthorTextView;

	private TextView mBackgroundNameTextView;

	private Handler mBackgroundHandler;

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
		
		startService(new Intent(this, HttpServiceReceiver.class));
	}
}
