package org.nadine.ori.stc;

import android.util.Log;

public class Stc {
	
	public Direction currentDirection;
	public Orien faceOrien;
	public Coord currCoord;
	public SpanningTree treeTable;
	public Detector detector;
	public RoboDirection roboDirection_rotation;
	public RoboDirection roboDirection;
	private boolean moveRobo = false;
	/***************** Methods ************************/
	public  void setCurrentDirection(Direction dir)
	{
			if (dir == Direction.NONE)
				return;
			currentDirection = dir;
	}	
	private Orien setOrientation(Direction nextDir)
	{
		switch(faceOrien){
		case NORTH:
			switch (nextDir)
			{
			case UP:
				break;
			case DOWN:
				faceOrien = Orien.SOUTH;
				break;
			case RIGHT:
				faceOrien = Orien.EAST;
				break;
			case LEFT:
				faceOrien = Orien.WEST;
				break;
			case NONE:
				break;
			}
			break;
		case SOUTH:
			switch (nextDir)
			{
			case UP:
				break;
			case DOWN:
				faceOrien = Orien.NORTH;
				break;
			case RIGHT:
				faceOrien = Orien.WEST;
				break;
			case LEFT:
				faceOrien = Orien.EAST;
				break;
			case NONE:
				break;
			}
		break;
		case EAST:
			switch (nextDir)
			{
			case UP:
				break;
			case DOWN:
				faceOrien = Orien.WEST;
				break;
			case RIGHT:
				faceOrien = Orien.SOUTH;
				break;
			case LEFT:
				faceOrien = Orien.NORTH;
				break;
			case NONE:
				break;
			}
		break;
		case WEST:
			switch (nextDir)
			{
			case UP:
				break;
			case DOWN:
				faceOrien = Orien.EAST;
				break;
			case RIGHT:
				faceOrien = Orien.NORTH;
				break;
			case LEFT:
				faceOrien = Orien.SOUTH;
				break;
			case NONE:
				break;
			}
		break;
		}
		return faceOrien;
	}
	public  void setCurrentCoord(int r, int c) 
	{
			currCoord.row = r;
			currCoord.col = c;
	}	
	
	/*private  void setCurrentCoord(Coord coord) 
	{
		currCoord.row = coord.row;
		currCoord.col = coord.col;
	}*/
	private  void setCurrentCoord(Cell c) 
	{
		currCoord.row = c.pos.row;
		currCoord.col = c.pos.col;
	}
		
	public  Cell setNextCell(Coord currP, Direction dir) {
		Coord fcoord = new Coord(currCoord.row , currCoord.col);
		Coord neigCoord = new Coord(currP);
		detector.coordByDirectAndOrien(neigCoord, dir, faceOrien);
		if(treeTable.contains(neigCoord))
			return(treeTable.getValue(neigCoord));
		return treeTable.putValue(neigCoord, fcoord,false);
	}
	/* updateMoveRobo - keep moveRobo synchronized.
	 * updateMoveRoboTrue - turn move to TRUE , and locked until the move is end.
	 * 						assume this function will be called only from stcAlgorithem.
	 * updateMoveRoboFalse - turn move to FALSE, and free the notify that the move ended.
	 * 						assume this function will be called only from the IOI looper.
	 */
	private synchronized void updateMoveRoboTrue() throws InterruptedException
	{
		moveRobo = true;
		while(moveRobo == true){
			wait();
		}
		
	}
	
	public synchronized void updateMoveRoboFalse()
	{
		moveRobo = false;
		notify();
	}
	
	public boolean  toMove()
	{
		return moveRobo;
	}
	
	public void stcAlgorithm(Cell currCell) throws InterruptedException {
		Thread.sleep(1000);
		Coord startCoord = new Coord(0,0);
		Log.d("Movement","Starting at cell: " + currCell.getPos().row + "," + currCell.getPos().col );
		Direction nextDirection;
		boolean toContine = true;
		detector._bisFirst = true; //3.9
		while( detector._bisFirst);// the next camera frame will be processed and the samples array && neighStatus will be updated. 
		while (toContine) {
			nextDirection = detector.getMoveDirection(currCell,faceOrien); //the son\neighbor to move to
			if (nextDirection == Direction.NONE){
				if(currCell.getPos().equals(startCoord)){
					//END OF ALGO
					toContine = false;
				} else 
					assert(false):"Direction NONE when we are not in the StartCell-StcAlgorithm";
			}else{
				currCell = setNextCell(currCell.getPos(), nextDirection); //update the cell and currCoord we go to, insert to the Tree & update mambers
				Log.d("Movement","Moving to cell: " + currCell.getPos().row + "," + currCell.getPos().col );
				currentDirection = nextDirection;
				setCurrentCoord(currCell);
				updateMoveRoboTrue();
				detector._btakePicture = true; //3.9
				setOrientation(nextDirection);
			}
		}
	}

		public void startStc() throws InterruptedException
		{
			treeTable = SpanningTree.getInstance();
			detector = Detector.getInstance();
			faceOrien = Orien.NORTH;
			roboDirection = RoboDirection.STOP;
			roboDirection_rotation = RoboDirection.STOP;
			currCoord = new Coord(0,0);
			Cell p = new Cell(new Coord(0,0),null,false);
			treeTable.putValue(0,0,p);
			stcAlgorithm(p);	
		}

	/*	
		public static void main(String[] args) {
			Stc stcProssesor = new Stc();
			stcProssesor.startStc();
		}
		*/			
}
