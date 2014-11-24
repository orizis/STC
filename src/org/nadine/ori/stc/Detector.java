package org.nadine.ori.stc;

import java.io.File;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Environment;

public class Detector {
	/*Fields*/
	private SpanningTree treeTable;
	private static Detector _instance = null;
	private Scalar[] _samples; 
	private boolean[] _neighStatus;
	private int dmaxB = 50;
	private int dmaxG = 50;
	private int dmaxR = 50;
	private int dminB = 50;
	private int dminG = 50;
	private int dminR = 50;
	private static final Rect RightRoi = new Rect(725, 200, 160, 60); //Green
	private static final Rect LeftRoi  = new Rect(720, 595, 130, 70); //Red
	private static final Rect UpRoi    = new Rect(570, 380, 80, 110); //Yellow
	private static final Rect DownRoi  = new Rect(960, 470, 80, 70); //Pink
	private static final int RIGHT = 0;
	private static final int UP = 1;
	private static final int LEFT = 2;
	private static final int DOWN = 3;
	public boolean _btakePicture = false; //mybe to change to couter and remmber the last number. inc when tacking picture. that way the ret move direction can be sure the data is updated
	public boolean _bisFirst = false;
	private boolean _isDraw = true;
	public int _counter = 0;
	public int _counter2 = 0;
	/*Methods*/
	protected Detector(){
		treeTable = SpanningTree.getInstance();
		_bisFirst = false;
		_btakePicture = false;
		_samples = new Scalar[7];
		_neighStatus = new boolean[4];
	}
	 public static Detector getInstance(){
		 if (_instance == null){
			 _instance = new Detector();
		 }
		 return _instance;
	 }
	 
	/* according to the Mat frame Detect witch  is the first new neighbor Counterclockwise
	 * returns his direction. NONE if there is NO neighbor to move to.
	 * ** in case one of the neighbor is border will update the TreeTable.       
	 */
	public Direction getMoveDirection(Cell currCell, Orien faceOrien )
	{
		boolean isFree;
		//boolean toContinue = true;
		while(_btakePicture);//the current location frame is not processed yet.
		Coord neigh = new Coord(currCell.getPos().row ,currCell.getPos().col);
		/** RIGHT**/
		neigh = coordByDirectAndOrien(neigh , Direction.RIGHT , faceOrien);
		isFree = _neighStatus[0]; //here should check the camera Mat if the right neigh is free.
		if(isFree){
			if(treeTable.getStatus(neigh) == CellStatus.NEW)
				return Direction.RIGHT;	
			else{
				if (currCell.fatherPos != null &&  neigh.equals(currCell.fatherPos))
					return Direction.RIGHT;
			}
		}
		/** UP **/
		neigh.row = currCell.getPos().row;
		neigh.col = currCell.getPos().col;
		neigh = coordByDirectAndOrien(neigh , Direction.UP , faceOrien);
		isFree = _neighStatus[1]; //here should check the camera Mat if the right neigh is free.
		if(isFree){
			if(treeTable.getStatus(neigh) == CellStatus.NEW)
				return Direction.UP;	
			else{
				if (currCell.fatherPos != null &&  neigh.equals(currCell.fatherPos))
					return Direction.UP;
			}
		}
		/** LEFT **/
		neigh.row = currCell.getPos().row;
		neigh.col = currCell.getPos().col;
		neigh = coordByDirectAndOrien(neigh , Direction.LEFT , faceOrien);
		isFree = _neighStatus[2]; //here should check the camera Mat if the right neigh is free.
		if(isFree){
			if(treeTable.getStatus(neigh) == CellStatus.NEW)
				return Direction.LEFT;	
			else{
				if (currCell.fatherPos != null &&  neigh.equals(currCell.fatherPos))
					return Direction.LEFT;
			}
		}
		/** DOWN **/
		neigh.row = currCell.getPos().row;
		neigh.col = currCell.getPos().col;
		neigh = coordByDirectAndOrien(neigh , Direction.DOWN , faceOrien);
		isFree = _neighStatus[3]; //here should check the camera Mat if the right neigh is free.
		if(isFree){
			if(treeTable.getStatus(neigh) == CellStatus.NEW)
				return Direction.DOWN;	
			else{
				if (currCell.fatherPos != null &&  neigh.equals(currCell.fatherPos))
					return Direction.DOWN;
			}
		}
		assert(currCell.fatherPos == null) : "Direction is NONE when Father exist";
			return Direction.NONE;
	}
	public Coord coordByDirectAndOrien(Coord curr, Direction direc, Orien ori )
	{
		switch(ori){
			case NORTH:
				switch(direc){
				case UP:
					curr.row -=1; 
					break;
				case DOWN:
					curr.row +=1;
					break;
				case LEFT:
					curr.col -=1;
					break;
				case RIGHT:
					curr.col +=1;
					break;
				case NONE:
					break;
			}
			break;
			case SOUTH:
				switch(direc){
				case UP:
					curr.row +=1; 
					break;
				case DOWN:
					curr.row -=1;
					break;
				case LEFT:
					curr.col +=1;
					break;
				case RIGHT:
					curr.col -=1;
					break;
				case NONE:
					break;
			}
			break;
			case EAST:
				switch(direc){
				case UP:
					curr.col +=1; 
					break;
				case DOWN:
					curr.col -=1;
					break;
				case LEFT:
					curr.row -=1;
					break;
				case RIGHT:
					curr.row +=1;
					break;
				case NONE:
					break;
			}
			break;
			case WEST:
				switch(direc){
				case UP:
					curr.col -=1; 
					break;
				case DOWN:
					curr.col +=1;
					break;
				case LEFT:
					curr.row +=1;
					break;
				case RIGHT:
					curr.row -=1;
					break;
				case NONE:
					break;
			}
			break;
		}
		return curr;
	}
	
	public Mat process(Mat inputImage) {
		//Mat invcolor = new Mat (inputImage.rows(),inputImage.cols(),inputImage.type(),new Scalar(255,255,255));
		//Core.subtract(invcolor, inputImage, inputImage);
		//invcolor.release();

		if (_isDraw) {
			Core.rectangle(inputImage,RightRoi.tl(),RightRoi.br(), new Scalar(255,255,0));//Green
			Core.rectangle(inputImage,LeftRoi.tl(),LeftRoi.br(), new Scalar(255,255,0));//Red
			Core.rectangle(inputImage,UpRoi.tl(),UpRoi.br(), new Scalar(255,255,0));//Yellow
			Core.rectangle(inputImage,DownRoi.tl(),DownRoi.br(), new Scalar(255,255,0));//Pink
		}
		if(_bisFirst){
			/* Take the First sample of the floor and update the samples array 3.9
			 * this flag is updated in the algo method before starting the loop - in the init section.
			 */		
			_isDraw = false;
			UpdateFirstSamples(inputImage);
			detectObst(inputImage);
			_bisFirst = false;
			_btakePicture = false;
		}
		
		if (_btakePicture){
			/*
			 *update the neighborhood status.
			 *1.call the proccsing algo of madjd
			 *2.cut the Mat according to prisave (Left,Right,FF,BW) rectangle location
			 *3.for each direction update the status array. (0 = free !0 = Border)
			 */
			detectObst(inputImage);
			_btakePicture = false;
		}		
		return inputImage;
	}
	
	private void UpdateFirstSamples(Mat frame){
		//Scalar s = new Scalar(255,0,0);
		Rect[] sampRect = new Rect[7];
		Mat tmp;		
		
		sampRect[0] = new Rect(730, 440, 30, 30);
		sampRect[1] = new Rect(710, 535, 30, 30);
		sampRect[2] = new Rect(700, 384, 30, 30);
		sampRect[3] = new Rect(705, 490, 30, 30);
		sampRect[4] = new Rect(790, 500, 30, 30);
		sampRect[5] = new Rect(830, 540, 30, 30);
		sampRect[6] = new Rect(880, 490, 30, 30);
		
		//for (int i =0; i<4; i++)
		/*Core.rectangle(frame, sampRect[0].tl(), sampRect[0].br(), new Scalar(255,0,0)); //Red
		Core.rectangle(frame, sampRect[1].tl(), sampRect[1].br(), new Scalar(0,255,0)); //Green
		Core.rectangle(frame, sampRect[2].tl(), sampRect[2].br(), new Scalar(0,0,255)); //Blue
		Core.rectangle(frame, sampRect[3].tl(), sampRect[3].br(), new Scalar(255,255,0)); //Yellow
		Core.rectangle(frame, sampRect[4].tl(), sampRect[4].br(), new Scalar(255,255,0)); //Yellow
		Core.rectangle(frame, sampRect[5].tl(), sampRect[5].br(), new Scalar(0,255,0)); //Green
		Core.rectangle(frame, sampRect[6].tl(), sampRect[6].br(), new Scalar(0,0,255)); //Blue
		*/
		/*drawing the neigh Rectangle*/
		/*Core.rectangle(frame,RightRoi.tl(),RightRoi.br(), new Scalar(0,255,0));//Green
		Core.rectangle(frame,LeftRoi.tl(),LeftRoi.br(), new Scalar(255,0,0));//Red
		Core.rectangle(frame,UpRoi.tl(),UpRoi.br(), new Scalar(255,255,0));//Yellow
		Core.rectangle(frame,DownRoi.tl(),DownRoi.br(), new Scalar(0,0,255));//Pink
		*/
		for (int i=0; i<7;i++){
			tmp = frame.submat(sampRect[i]);
			_samples[i] = Core.mean(tmp);
		}
	}
		
	private void detectObst(Mat frame) {
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


		String filename = "img" + _counter2++ + ".png";
		File file = new File(path, filename);
		filename = file.toString();
		boolean b = Highgui.imwrite(filename, frame);
		
		Scalar lowBound , upBound;
		Mat dst = new Mat(), tmp = new Mat();
		double free = 200.0;
		//lowBound = new Scalar(max(_samples[0].val[0] - dminB,0.0),max( _samples[0].val[1] - dminG,0.0),max( _samples[0].val[2] - dminR,0.0) );
		//upBound  = new Scalar(min(_samples[0].val[0] + dmaxB,255.0), min(_samples[0].val[1] + dmaxG,255.0), min(_samples[0].val[2] + dmaxR,255.0) );
		
		//There is a range of 40 since the reflection of the robot and phone on the plastic
		//bars are translated as obstacle.
		lowBound = new Scalar(max(_samples[0].val[0] - 30.0, 0.0), 0, 0);
		upBound  = new Scalar(min(_samples[0].val[0] + 30.0, 255.0), 0, 0);
		Core.inRange(frame, lowBound, upBound, dst);
		
		for(int i = 1; i<7; i++)
		{
			//OpenCV BGR = Scalar(Blue,Green,Red)
			//lowBound = new Scalar(_samples[i].val[0] - dminB, _samples[i].val[1] - dminG, _samples[i].val[2] - dminR );
			//upBound  = new Scalar(_samples[i].val[0] + dmaxB, _samples[i].val[1] + dmaxG, _samples[i].val[2] + dmaxR );
			lowBound = new Scalar(max(_samples[i].val[0] - 30.0, 0.0), 0, 0);
			upBound  = new Scalar(min(_samples[i].val[0] + 30.0, 255.0), 0, 0);
			Core.inRange(frame, lowBound, upBound, tmp);
		}
		
		Imgproc.medianBlur(dst, dst, 7);
		
		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		filename = "med" + _counter++ + ".png";
		file = new File(path, filename);
		filename = file.toString();
		b = Highgui.imwrite(filename, dst);
		
		/*update neighStatus*/
		_neighStatus[RIGHT] = (Core.mean(dst.submat(RightRoi)).val[0] >= free);
		_neighStatus[LEFT]  = (Core.mean(dst.submat(LeftRoi)).val[0]  >= free);
		_neighStatus[UP]    = (Core.mean(dst.submat(UpRoi)).val[0]    >= free);
		_neighStatus[DOWN]  = (Core.mean(dst.submat(DownRoi)).val[0]  >= free);
	}
	
	private double min(double d, double e) {
		return (d < e) ? d : e;
	}
	private double max(double d, double e) {
		return (d > e) ? d : e;
	}
	
}
