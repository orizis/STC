package org.nadine.ori.stc;
public class Coord {
	int row;
	int col;
	public Coord(){}
	public Coord(int i,int j){
		row = i;
		col = j;
	}
	
	public Coord(Coord c)
	{
		row = c.row;
		col = c.col;
	}
	
	@Override
	public boolean equals(Object arg0) {
		Coord pt=(Coord) arg0;
		return(row == pt.row && col == pt.col);
	}	
}

