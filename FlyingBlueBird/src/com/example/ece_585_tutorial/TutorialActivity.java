package com.example.ece_585_tutorial;

import java.io.InputStream;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class TutorialActivity extends Activity {

	String TAGV = "Tutorial";
	boolean mInitialized = false;
	public long beginTime, currentTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu); // activity_thread_example
		return true;
	}

	public void buttonClick(final View view) {
		CustomView cv = new CustomView(view.getContext());
		setContentView(cv);
	}

	public class CustomView extends View implements SensorEventListener {

		private Movie mMovie;
		String TAGX = "TutAct";
		private long mMoviestart = 1000;

		Bitmap mBckgroundImage;
		Bitmap mBirdImage;
		Resources mRes;

		DisplayMetrics metrics;
		private int mCanvasWidth;
		private int mCanvasHeight;
		private int mArrTarLimitY;
		private int mPixelMoveX = 3;
		private Vector<Bird> mBirdVec;
		private boolean CreateNextBird = true;
		private int mBirdHeight;

		private SensorManager mSensorManager;
		private Sensor mAccelerometer;
		private Sensor mMagnetometer;
		float[] mAccelVal;
		float[] mMagneticField;
		float[] mOrientation;

		Handler m_handler;
		Runnable m_handlerTask;

		public CustomView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			mRes = getResources();
			mBckgroundImage = BitmapFactory.decodeResource(mRes,
					R.drawable.for_butterfly);
			mBirdImage = BitmapFactory.decodeResource(mRes,
					R.drawable.blue_flying_bird);

			metrics = getResources().getDisplayMetrics();
			mCanvasWidth = metrics.widthPixels;
			mCanvasHeight = metrics.heightPixels;
			mArrTarLimitY = mCanvasHeight / 4;
			mBirdHeight = mBirdImage.getHeight();

			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			mAccelerometer = mSensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mMagnetometer = mSensorManager
					.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			mOrientation = new float[3];
			mSensorManager.registerListener((SensorEventListener) this,
					mAccelerometer, SensorManager.SENSOR_DELAY_UI);
			mSensorManager.registerListener((SensorEventListener) this,
					mMagnetometer, SensorManager.SENSOR_DELAY_UI);

			m_handler = new Handler();
		}

		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			canvas.drawColor(Color.TRANSPARENT);
			canvas.drawBitmap(mBckgroundImage, 0, 0, null);
			move(canvas);
			this.invalidate();// Very important for animation
		}

		public void move(final Canvas canvas) {

			m_handlerTask = new Runnable() {
				@Override
				public void run() {

					// canvas.drawBitmap(mBckgroundImage, 0, 0, null);
					InputStream stream1 = null;
					try {
						stream1 = getContext().getResources().openRawResource(
								R.drawable.blue_flying_bird);
					} catch (Exception e) {
						e.printStackTrace();
						Log.d(TAGV, " error: : " + e.getMessage());
					}
					if (!mInitialized) {
						setInitialGameState();
						mInitialized = true;
					}
					currentTime = SystemClock.uptimeMillis();
					setGifAnimation(currentTime, stream1);
					doBirdAnimation(canvas);
					updateGameState();
				}// run
			};// runnable
			m_handlerTask.run();
		}

		public void setGifAnimation(long now, InputStream stream) {
			Log.d(TAGX, " setGifAnimation: : ");

			try {
				mMovie = Movie.decodeStream(stream);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (mMoviestart == 0) {
				mMoviestart = now;
			}
			int relTime = (int) ((now - mMoviestart) % mMovie.duration());
			mMovie.setTime(relTime);
		}

		public void setInitialGameState() {

			mBirdVec = new Vector<Bird>();
			doBirdCreation();
		}

		public void updateGameState() {

			if (CreateNextBird) {
				doBirdCreation();
			}
			updateBird();
		}

		private void doBirdCreation() {

			Bird _bird = new Bird();
			_bird.mDrawY = mCanvasHeight / 2;
			_bird.mDrawX = 0;
			_bird.mStartTime = System.currentTimeMillis();
			mBirdVec.add(_bird);
		}

		protected void updateBird() {

			if (mBirdVec == null | mBirdVec.size() == 0)
				return;

			Bird curBird = mBirdVec.elementAt(0);
			double birdTiltAngle = (90 * mOrientation[1]) / 1.1;
			// mBirdImage.getHeight()
			double newYINcr = mBirdHeight
					* Math.tan(Math.toRadians(birdTiltAngle));
			int newYVal = curBird.mDrawY + (int) newYINcr;

			currentTime = SystemClock.uptimeMillis();
			if ((currentTime - beginTime) > 300) {

				beginTime = SystemClock.uptimeMillis();

				if (newYVal < mArrTarLimitY) {
					curBird.mDrawY = mArrTarLimitY;
				} else if (newYVal > 3 * mArrTarLimitY) {
					curBird.mDrawY = 3 * mArrTarLimitY;
				} else {
					curBird.mDrawY = newYVal;
				}
			}
			// Update the asteroids position, even missed ones keep moving
			curBird.mDrawX += mPixelMoveX; // -=

			if (curBird.mDrawX > (mCanvasWidth - 100)) {
				mBirdVec.removeElementAt(0);
				CreateNextBird = true;
			} else {
				CreateNextBird = false;
			}
		}

		private void doBirdAnimation(Canvas canvas) {

			if ((mBirdVec == null | mBirdVec.size() == 0))
				return;
			Bird curBird = mBirdVec.elementAt(0);
			mMovie.draw(canvas, curBird.mDrawX, curBird.mDrawY);
		}

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
		}

		// sensor methods
		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
				mAccelVal = event.values;
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
				mMagneticField = event.values;
			if (mAccelVal != null && mMagneticField != null) {
				float RotMat[] = new float[9];
				float InclMat[] = new float[9];

				boolean success = SensorManager.getRotationMatrix(RotMat,
						InclMat, mAccelVal, mMagneticField);
				if (success) {
					SensorManager.getOrientation(RotMat, mOrientation);
				}
			}
		}

		public void unregisterListener(SensorEventListener listener) {
			// Unregisters a listener for the sensors with which it is
			// registered.
			mSensorManager.unregisterListener(listener);
		}

		protected void onDestroy() {
			unregisterListener(this);
		}

	}
}