package org.nadine.ori.stc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StcActivity extends IOIOActivity implements CvCameraViewListener2, SensorEventListener {

	private static final String TAG = "StcActivity";

	private ToggleButton button_;
	private ToggleButton startButton_;
	private Detector detector_;
	private Stc stc_;
	private int width_, height_;
	private boolean isStcEnabled = false;
	private boolean pause = false;
	private CameraBridgeViewBase cameraView_;
	
	//Sensors
	private boolean isFirst;
	private SensorManager mSensorManager;
	private Sensor mAccelSensor;
	private Sensor mMagnetSensor;
	private float[] mAccelVector;
	private float[] mMagnetVector;
	private float[] mRotationMatrix;
	private boolean isAccelUpdated;
	private boolean isMagnetUpdated;
	private static final ScheduledExecutorService worker = 
			  Executors.newSingleThreadScheduledExecutor();

	/********* IOIO Params - from majd *********/
	//private SensorManager sensorManager;
	private TextView textView_;
	//private SeekBar seekBar_;
	//private ToggleButton toggleButton_;
	private int STBY = 11;
	private int PWMB = 14;
	private int PWMA = 7;
	private int BIN1 = 12;
	private int BIN2 = 13;
	private int AIN1 = 10;
	private int AIN2 = 9;
	/*
	public DIRECTION direction_rotation = StcActivity.DIRECTION.STOP;
	public DIRECTION direction = StcActivity.DIRECTION.STOP;


	public enum DIRECTION {
		FORWARD, BACKWARD, CW, CCW, STOP
	};
	 */
	/*******************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stc);
		button_ = (ToggleButton) findViewById(R.id.ioio_btn);
		startButton_ = (ToggleButton) findViewById(R.id.ToggleButton1);

		cameraView_ = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view);
		cameraView_.setVisibility(SurfaceView.VISIBLE);

		cameraView_.setCvCameraViewListener(this);
		cameraView_.setCameraIndex(1); // camera id 0 is back and camera id 1 is front
		isFirst = true;
		//Sensors
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		mAccelVector = new float[3];
		mMagnetVector = new float[3];
		mRotationMatrix = new float[9];
		
		stc_ = new Stc();
		detector_ = Detector.getInstance();
	}

	public void onStartButtonClicked(View view) throws InterruptedException {
		pause = !((ToggleButton) view).isChecked();
		if (((ToggleButton) view).isChecked() && !isStcEnabled) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					isStcEnabled = true;
					try {
						stc_.startStc();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	/*
	 * enable opencv library
	 */
	private BaseLoaderCallback openCvLoader_ = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				cameraView_.enableView();
			}
			break;
			default: {
				super.onManagerConnected(status);
			}
			break;
			}
		}
	};



	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (cameraView_ != null){
			cameraView_.disableView();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (cameraView_ != null){
			cameraView_.disableView();
		}
		//Sensors
		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, openCvLoader_);
		//Sensors
		mSensorManager.registerListener(this,
		        mAccelSensor,
		        SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this,
		        mMagnetSensor,
		        SensorManager.SENSOR_DELAY_NORMAL);

	}

	/*
	 * enable ioio looper
	 */
	class Looper extends BaseIOIOLooper {
		private DigitalInput encoderLeftInput_;
		private DigitalInput encoderLeftInput1_;
		private DigitalInput encoderLeftInput2_;
		private DigitalInput encoderRightInput_;
		private DigitalInput encoderRightInput1_;
		private DigitalInput encoderRightInput2_;
		private PwmOutput pwmLeftMotors_;
		private PwmOutput pwmRightMotors_;
		private DigitalOutput led_;
		private DigitalOutput stby;
		private DigitalOutput ain1;
		private DigitalOutput ain2;
		private DigitalOutput bin1;
		private DigitalOutput bin2;
		private StcActivity app_;
		int count = 0;

		public Looper(StcActivity app) {
			app_ = app;

		}
		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			try{
				encoderLeftInput_  = ioio_.openDigitalInput(37);
				encoderLeftInput1_  = ioio_.openDigitalInput(35);
				encoderLeftInput2_  = ioio_.openDigitalInput(36);

				encoderRightInput_ = ioio_.openDigitalInput(47);
				encoderRightInput1_ = ioio_.openDigitalInput(45);
				encoderRightInput2_ = ioio_.openDigitalInput(46);

				led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
				Log.d("Ori&Nadine", "Connection Started");
				pwmRightMotors_ = ioio_.openPwmOutput(PWMA, 50); //speed?
				pwmLeftMotors_ = ioio_.openPwmOutput(PWMB, 50);

				stby = ioio_.openDigitalOutput(STBY, false);
				ain1 = ioio_.openDigitalOutput(AIN1, false);
				ain2 = ioio_.openDigitalOutput(AIN2, true);
				bin1 = ioio_.openDigitalOutput(BIN1, true);
				bin2 = ioio_.openDigitalOutput(BIN2, false);
				//enableUi(true);

			} catch (ConnectionLostException e) {
				setText(e.toString());
				//enableUi(false);
				throw e;
			}
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			led_.write(!button_.isChecked());
			if(startButton_.isChecked() && app_.stc_.toMove() == true && !pause){
				moveToDirection(app_.stc_.currentDirection);
				stby.write(false); //not necessary
				app_.stc_.updateMoveRoboFalse();
			}
			/************************ Ori **********************
			
			if (button_.isChecked() && isFirst) {
				isFirst = false;
				//moveFW();
				ain1.write(false);
				ain2.write(true);
				bin1.write(false);
				bin2.write(true);

				pwmLeftMotors_.setPulseWidth(19000);
				pwmRightMotors_.setPulseWidth(17000);
				String s = Environment.DIRECTORY_MUSIC + "/../";
				String state = Environment.getExternalStorageState();
				if (Environment.MEDIA_MOUNTED.equals(state)) {
					File file = new File(Environment.getExternalStoragePublicDirectory(
							s), "ori.txt");

					try {
						FileWriter filewriter = new FileWriter(file,true);
						stby.write(true);
						while (button_.isChecked()) {
							filewriter.write("35: " + Boolean.toString(encoderLeftInput1_.read())  + " TimeStamp: " + String.valueOf(System.nanoTime()) + "\n");
							filewriter.write("45: " + Boolean.toString(encoderRightInput1_.read()) + " TimeStamp: " + String.valueOf(System.nanoTime()) + "\n");
						}
						stby.write(false);
						filewriter.flush();
						filewriter.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}



				}

			}
			if(startButton_.isChecked()) {
				isFirst = true;
			}
			
			/**************************************************/
			/*if (button_.isChecked() && isFirst) {
				isFirst = false;
				moveCW();
			}
			if(startButton_.isChecked()) {
				isFirst = true;
			}*/
		}

		public void moveToDirection(Direction dir) throws ConnectionLostException, InterruptedException {
			try{
				switch (dir) {
				case UP:
					moveFW();
					moveFW();
					break;
				case DOWN:
					moveFW();
					moveCCW();
					moveFW();
					moveCCW();
					moveFW();
					moveFW();
					break;
				case LEFT:
					moveFW();
					moveCCW();
					moveFW();
					moveFW();
					break;
				case RIGHT:
					moveCW();
					moveFW();
					break;
				case NONE:
					break;
				}
			}catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch(ConnectionLostException e) {
				setText(e.toString());
				//enableUi(false);
				throw e;
			}
		}		
		
		private void startMovement2() throws ConnectionLostException, InterruptedException {
			long start = System.nanoTime();
			long end = (long) (start + (Math.pow(10, 9) * 0.45));
			stby.write(true);
			while (System.nanoTime() < end);
			stby.write(false);
		}
		
		private void Rotate2() throws ConnectionLostException, InterruptedException {
			long start = System.nanoTime();
			long end = (long) (start + (Math.pow(10, 9) * 1.1));
			stby.write(true);
			while (System.nanoTime() < end);
			stby.write(false);
		}
		
		
		private void startMovement() throws ConnectionLostException, InterruptedException {
			//double wheelAngle = 200.0, progressAngle = 0.0;
			/*boolean sig;
			int count = 0;
			sig = encoderLeftInput_.read();
			stby.write(true);
			
			while (count < 1) {
				encoderLeftInput_.waitForValue(!sig);
				count++;
				//progressAngle += 5.625;
				sig = !sig;
			}
			stby.write(false);*/
			
			
			
			waitForSensors();
			float x = mAccelVector[0];
			float y = mAccelVector[1];
			float z = mAccelVector[2];
			double val1 = Math.sqrt(x*x+y*y+z*z);
			stby.write(true);
			while (button_.isChecked()) {	
				/*stby.write(true);
				Thread.sleep(100);
				stby.write(false);
				
				waitForSensors();
				SensorManager.getRotationMatrix(progressRotationMatrix, null, mAccelVector, mMagnetVector);
				SensorManager.getAngleChange(angle, progressRotationMatrix, mRotationMatrix);
				progress_angle = Math.abs(angle[0]);*/ 
			}
			stby.write(false);
			
			waitForSensors();
			float newX = mAccelVector[0];
			float newY = mAccelVector[1];
			float newZ = mAccelVector[2];
			
			double val2 = Math.sqrt(newX*newX+newY*newY+newZ*newZ);
			
			float diffX = newX - x;
			float diffY = newY - y;
			float diffZ = newZ - z;
		}
		
		private void waitForSensors() {
			isAccelUpdated = false;
			isMagnetUpdated = false;
			while (!isAccelUpdated || !isMagnetUpdated);
		}
		
		private void Rotate(boolean isCW) throws ConnectionLostException, InterruptedException {
			float[] progressRotationMatrix = new float[9];
			float[] angle = new float[3];
			final float half_pi = ((float) Math.PI / (float) 2.0);  
			float progress_angle = (float) 0.0;
			
			waitForSensors();
			SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelVector, mMagnetVector);

			while (progress_angle < 1.0) {	
				stby.write(true);
				Thread.sleep(300);
				stby.write(false);
				
				waitForSensors();
				SensorManager.getRotationMatrix(progressRotationMatrix, null, mAccelVector, mMagnetVector);
				SensorManager.getAngleChange(angle, progressRotationMatrix, mRotationMatrix);
				progress_angle = Math.abs(angle[0]); 
			}
			
			progress_angle -= half_pi;
			float newProgAng = Math.abs(progress_angle);
			if (progress_angle > 0 && isCW) {
				ain1.write(false);
				ain2.write(true);
				bin1.write(true);
				bin2.write(false);
				progress_angle = 0;
				while (progress_angle < newProgAng) {	
					stby.write(true);
					Thread.sleep(50);
					stby.write(false);
					
					waitForSensors();
					SensorManager.getRotationMatrix(progressRotationMatrix, null, mAccelVector, mMagnetVector);
					SensorManager.getAngleChange(angle, progressRotationMatrix, mRotationMatrix);
					progress_angle = Math.abs(angle[0]); 
				}
			} else if (progress_angle > 0 && !isCW) {
				ain1.write(true);
				ain2.write(false);
				bin1.write(false);
				bin2.write(true);
				progress_angle = 0;
				while (progress_angle < newProgAng) {	
					stby.write(true);
					Thread.sleep(50);
					stby.write(false);
					
					waitForSensors();
					SensorManager.getRotationMatrix(progressRotationMatrix, null, mAccelVector, mMagnetVector);
					SensorManager.getAngleChange(angle, progressRotationMatrix, mRotationMatrix);
					progress_angle = Math.abs(angle[0]); 
				}
			} else {
				
				progress_angle = 0;
				while (progress_angle < newProgAng) {	
					stby.write(true);
					Thread.sleep(50);
					stby.write(false);
					
					waitForSensors();
					SensorManager.getRotationMatrix(progressRotationMatrix, null, mAccelVector, mMagnetVector);
					SensorManager.getAngleChange(angle, progressRotationMatrix, mRotationMatrix);
					progress_angle = Math.abs(angle[0]); 
				}
			}
			
		}
				
		
		
		public void moveFW() throws ConnectionLostException, InterruptedException {
			ain1.write(false);
			ain2.write(true);
			bin1.write(false);
			bin2.write(true);

			pwmLeftMotors_.setPulseWidth(19000);
			pwmRightMotors_.setPulseWidth(17000);
			
			startMovement2();

			//This is for making some time off between atomic steps
			Thread.sleep(500);
		}
		
		public void moveBW() throws ConnectionLostException, InterruptedException {
			//This function is not used as the robot never run in reverse
			ain1.write(true);
			ain2.write(false);
			bin1.write(true);
			bin2.write(false);
			
			pwmLeftMotors_.setPulseWidth(17900);
			pwmRightMotors_.setPulseWidth(20000);
			
			stby.write(true);
			Thread.sleep(1050);
			stby.write(false);
			
			//This is for making some time off between atomic steps
			Thread.sleep(500);
		}
		
		public void moveCCW() throws ConnectionLostException, InterruptedException {
			ain1.write(false);
			ain2.write(true);
			bin1.write(true);
			bin2.write(false);
			
			pwmLeftMotors_.setPulseWidth(45000);
			pwmRightMotors_.setPulseWidth(50000);
			
			//Rotate(false);
			Rotate2();
			
			//This is for making some time off between atomic steps
			Thread.sleep(500);
		}
		
		public void moveCW() throws ConnectionLostException, InterruptedException {
			ain1.write(true);
			ain2.write(false);
			bin1.write(false);
			bin2.write(true); 
			
			pwmLeftMotors_.setPulseWidth(45000);
			pwmRightMotors_.setPulseWidth(50000);
		
			//Rotate(true);
			Rotate2();
			
			//This is for making some time off between atomic steps
			Thread.sleep(500);
		}
		
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper(this);
	}
	/*	
	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				 seekBar_.setEnabled(enable);
				 toggleButton_.setEnabled(enable);
			}
		});
	}
	 */

	private void setText(final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView_.setText(str);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stc, menu);
		return true;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		width_ = width;
		height_ = height;
		Log.i(TAG,"onCameraViewStarted done.");
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat grayImage = inputFrame.gray();		
		Core.flip(grayImage, grayImage, 1);
		Mat outputImage = detector_.process(grayImage);	
		return outputImage;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mAccelVector = event.values.clone();
			isAccelUpdated = true;
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mMagnetVector = event.values.clone();
			isMagnetUpdated = true;
		}
	}

}
