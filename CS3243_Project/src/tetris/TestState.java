package tetris;

public class TestState {
	private int[][] board;
	private int[] boardTop;
	private int piece;
	private int[] move;
	private int placedPieceHt;
	private int prevColHt;
	private int rowsCleared;
	
	public TestState(int[][] board, int[] boardTop, int piece, int[] move){
		//initial board
		this.board = board;
		this.boardTop = boardTop;
		
		//other params needed
		this.piece = piece;
		this.move = move;
		this.placedPieceHt = State.getpHeight()[piece][move[0]];
		this.prevColHt = colHeight(move[1], board);
		this.rowsCleared = 0;	//increased according to move set (below)
		
		//change board according to move
		testingMove();
	}
	
	/**
	 * Description: Determine height of a given column using the given board
	 * @param col
	 * @return 
	 */
	private int colHeight(int col, int[][] board){
		for(int row = board.length - 1; row >= 0; row--){
			if(board[row][col] != 0){
				return row + 1;
			}
		}
		return 0;
	}
	
	/**
	 * Description: Sets the move and returns the board. Used to check the results of a move without actually
	 * doing the move. Board code modified from State.java
	 * 
	 * @param move
	 * @param s
	 * @return
	 */
	private void testingMove(){
		
		int orient = move[0];
		int slot = move[1];
		int[][][] pBottom = State.getpBottom();
		int [][] pWidth = State.getpWidth();
		int [][][] pTop = State.getpTop();
		int [][] pHeight = State.getpHeight();
		
		//height if the first column makes contact
		int height = boardTop[slot] - pBottom[piece][orient][0];

		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[piece][orient];c++) {
			height = Math.max(height, boardTop[slot + c] - pBottom[piece][orient][c]);
		}

		//check if game ended
		if(height + pHeight[piece][orient] >= State.ROWS) {
			//System.out.println("DEAD");
			board = null;
			boardTop = null;
			
		} else {
			
			//for each column in the piece - fill in the appropriate blocks
			for(int i = 0; i < pWidth[piece][orient]; i++) {
				
				//from bottom to top of brick
				for(int h = height + pBottom[piece][orient][i]; h < height+pTop[piece][orient][i]; h++) {
					board[h][i + slot] = 1;
				}
			}
			//System.out.println("LEGIT MOVE");
			
			// adjust top
			for (int c = 0; c < pWidth[piece][orient]; c++) {
				boardTop[slot + c] = height + pTop[piece][orient][c];
			}
	
			// check for full rows - starting at the top
			for (int r = height + pHeight[piece][orient] - 1; r >= height; r--) {
				// check all columns in the row
				boolean full = true;
				for (int c = 0; c < State.COLS; c++) {
					if (board[r][c] == 0) {
						full = false;
						break;
					}
				}
				// if the row was full - remove it and slide above stuff down
				if (full) {
					rowsCleared++;
					// for each column
					for (int c = 0; c < State.COLS; c++) {
	
						// slide down all bricks
						for (int i = r; i < boardTop[c]; i++) {
							board[i][c] = board[i + 1][c];
						}
						// lower the top
						boardTop[c]--;
						while (boardTop[c] >= 1 && board[boardTop[c] - 1][c] == 0)
							boardTop[c]--;
					}
				}
			}
		}		
	}

	public int[][] getBoard() {
		return board;
	}

	public int getPlacedPieceHt() {
		return placedPieceHt;
	}

	public int getPrevColHt() {
		return prevColHt;
	}

	public int getRowsCleared() {
		return rowsCleared;
	}
	
}
