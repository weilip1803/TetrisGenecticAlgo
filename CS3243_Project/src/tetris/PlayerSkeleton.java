package tetris;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class PlayerSkeleton {
	
	//PlayerSkeleton Parameters
	private Gene g;
	
	/** CONSTRUCTORS **/
	
	/**
	 * Default Constructor for PlayerSkeleton
	 * Description: sets gene parameter with a Default gene
	 * (Refer to Gene class for more information on genes)
	 */
	public PlayerSkeleton(){
		g = new Gene();
		
		//add more heuristics as appropriate
		g.addHeuristic(new FilledLinesHeuristic(), 3.4181268101392694); 	
		g.addHeuristic(new HolesHeuristic(), -7.899265427351652); 	
		g.addHeuristic(new WellSumsHeuristic(), -3.3855972247263626); 	
		g.addHeuristic(new LandingHeightHeuristic(), -4.500158825082766);
		g.addHeuristic(new RowTransitionsHeuristic(), -3.2178882868487753); 
		g.addHeuristic(new ColTransitionsHeuristic(), -9.348695305445199); 
	}
	
	/**
	 * Constructor for PlayerSkeleton
	 * Description: Sets the gene parameter to the specified gene
	 * 
	 * @param g
	 */
	public PlayerSkeleton(Gene g){	
		this.g = g;
	}
	
	
	/** INNER CLASSES **/
	
	/**
	 * TEST STATE CLASS
	 * Description: This class aims to store relevant information for the heuristics to use in their
	 * evaluation of a move. It is a simplified form of the State class that has been given. However, 
	 * changes here do not impact the actual State, and it is used to test the move's fitness.
	 */
	public class TestState {
		
		//TestState Parameters
		private int[][] board;			//equivalent to field in State
		private int[] boardTop;
		private int piece;
		private int[] move;
		private int placedPieceHt;
		private int prevColHt;
		private int rowsCleared;
		
		/**
		 * Constructor for TestState
		 * Description: Takes in the initial field/board, the field/board tops, the piece type and the move
		 * to be tested. It then takes the move given using the the function testingMove(). 
		 * 
		 * @param board
		 * @param boardTop
		 * @param piece
		 * @param move
		 */
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
		 * Description: Takes the move and returns the field/board. Used to check the results of a move without 
		 * actually doing the move. Updates the various parameters like board and boardTop. Returns null if
		 * the move leads to a loss i.e. exceeds the board. Board code modified from State.java
		 * 
		 * @param move
		 * @param s
		 * @return
		 */
		private void testingMove(){
			
			int orient = move[0];
			int slot = move[1];
			int[][][] pBottom = State.getpBottom();
			int[][] pWidth = State.getpWidth();
			int[][][] pTop = State.getpTop();
			int[][] placedPieceHt = State.getpHeight();
			
			//height if the first column makes contact
			int height = boardTop[slot] - pBottom[piece][orient][0];

			//for each column beyond the first in the piece
			for(int c = 1; c < pWidth[piece][orient];c++) {
				height = Math.max(height, boardTop[slot + c] - pBottom[piece][orient][c]);
			}

			//check if game ended
			if(height + placedPieceHt[piece][orient] >= State.ROWS) {		
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
				
				// adjust top
				for (int c = 0; c < pWidth[piece][orient]; c++) {
					boardTop[slot + c] = height + pTop[piece][orient][c];
				}
		
				// check for full rows - starting at the top
				for (int r = height + placedPieceHt[piece][orient] - 1; r >= height; r--) {
					
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

		/**
		 * Description: Gives the current board of the test state
		 * @return
		 */
		public int[][] getBoard() {
			return board;
		}

		/**
		 * Description: Gives the placed piece height, also known as the height of the piece given 
		 * the orientation stated in the move
		 * @return
		 */
		public int getPlacedPieceHt() {
			return placedPieceHt;
		}

		/**
		 * Description: Gives the previous height of the column before the piece was placed there
		 * @return
		 */
		public int getPrevColHt() {
			return prevColHt;
		}

		/**
		 * Description: Gives the number of rows cleared after the move was taken
		 * @return
		 */
		public int getRowsCleared() {
			return rowsCleared;
		}
	}
	
	/**
	 * HEURISTIC ABSTRACT CLASS
	 * Description: Contains several important methods and the parameters for the implementation of actual
	 * heuristic classes. Heuristic Classes' purpose is to evaluate the test state of a move and give a value 
	 * indicating how "good" it is, with regards to the heuristic.
	 */
	abstract public class Heuristic {
		
		//Heuristic Parameters
		protected TestState testState;
		protected int[][] board;
		protected int rows;
		protected int cols;
		
		/**
		 * Constructor for Heuristic Abstract Class
		 * Description: Initializes the parameters
		 */
		public Heuristic(){
			//empty for now
			testState = null;
			board = null;
			rows = -1;
			cols = -1;
		}
		
		/**
		 * Description: Determine number of empty spaces given a row
		 * @param row
		 * @return
		 */
		protected int emptySpaces(int row){
			int count = 0;
			for (int i = 0; i < cols; i++){
				if (board[row][i] == 0){
					count++;
				}
			}
			return count;
		}
		
		/**
		 * Description: Gives the value for the board, with regards to the specified heuristic
		 * @return
		 * @throws Exception
		 */
		abstract public double getValue() throws Exception;

		/**
		 * Description: sets the TestState to be evaluated with the heuristic
		 * @param testState
		 */
		public void setTestState(TestState testState) {
			//able set a null value as the test state --> to ensure new board is set
			if(testState == null){
				this.testState = null;
				this.board = null;
				this.rows = -1;
				this.cols = -1;
			} else {		
				this.testState = testState;
				this.board = testState.getBoard();
				this.rows = board.length;
				this.cols = board[0].length;
			}
		}	
	}

	/**
	 * FILLED LINES HEURISTIC CLASS -- SUBCLASS OF HEURISTIC
	 * Description: Heuristic values the number of lines that a particular move has removed from
	 * the field/board.
	 *
	 */
	public class FilledLinesHeuristic extends Heuristic {
		
		//FilledLinesHeuristic Parameters
		private int rowsCleared;

		@Override
		public double getValue() throws Exception {
			if(testState == null){
				throw new Exception("No State to test");
			}
			return rowsCleared;
		}
	}
	
	/**
	 * HOLES HEURISTIC CLASS -- SUBCLASS OF HEURISTIC
	 * Description: Heuristic values the number of holes on the field/board, 
	 * i.e. those spaces that are not filled but below the highest block in a column
	 */
	public class HolesHeuristic extends Heuristic{
		@Override
		public double getValue() throws Exception {
			if(board == null){
				throw new Exception("No board for Heuristic");
			}
			
			double count = 0;
			
			for(int col = 0; col < cols; col++){
				//position of highest block
				int height = colHeight(col, testState.getBoard());
				
				//Check the rows from bottom up
				//Increment when an empty space that is below the highest block is found
				for(int row = 0; row < height; row++){
					if(board[row][col] == 0){
						count++;
					}
				}
			}
			return count;
		}
	}
	
	/**
	 * WELL SUMS HEURISTIC CLASS -- SUBCLASS OF HEURISTIC
	 * Description: Heuristic values the wells on the field/board. A well is a group of cells in the same column 
	 * that have filled cells to their left and right.
	 */
	public class WellSumsHeuristic extends Heuristic {
		
		@Override
		public double getValue() throws Exception {
			if(board == null){
				throw new Exception("No board for Heuristic");
			}
			
			double wellSum = 0;
			
			for (int i = 0; i < cols; i++){		
				
				//checking the rows for the start of the well
				for(int j = rows - 1; j >= 0; j--){
					
					//both sides of the checked cell are taken to be filled originally
					boolean isLeftFilled = true;
					boolean isRightFilled = true;
					
					//checks the left cell of the checked cell 
					//if the checked cell is the leftmost cell, left is taken to be filled
					if(i != 0 && board[j][i - 1] == 0){		
						isLeftFilled = false;	
					}
					
					//checks the right cell of the checked cell 
					//if the checked cell is the rightmost cell, right is taken to be filled
					if(i != cols - 1 && board[j][i + 1] == 0){
						isRightFilled = false;
					}

					//if checked cell is a well cell, count the number of empty spaces in this col
					//i.e. the length of the well
					if(board[j][i] == 0 && isLeftFilled && isRightFilled){
						for(int k = j; k >= 0; k--){	
							if(board[k][i] == 0){
								wellSum++;
							} else {
								break;
							}
						}
					}
				}
			}
			return wellSum;
		}
		
	}
	
	/**
	 * LANDING HEIGHT HEURISTIC CLASS -- SUBCLASS OF HEURISTIC
	 * Description: Heuristic values the Landing Height 
	 * i.e. the previous height of the column + the 1/2 of the piece height according to orientation
	 *
	 */
	public class LandingHeightHeuristic extends Heuristic {
		
		@Override
		public double getValue() throws Exception {
			if(testState == null){
				throw new Exception("No State to test");
			} 
			
			//landing height formula
			return testState.getPrevColHt() + testState.getPlacedPieceHt()/2;
		}	
	}
	
	/**
	 * ROW TRANSITIONS HEURISTIC CLASS -- SUBCLASS OF HEURISTIC
	 * Description: Heuristic values row transitions
	 * A row transition occurs when an empty cell is adjacent to a filled cell on the same row and vice versa
	 */
	public class RowTransitionsHeuristic extends Heuristic{

		@Override
		public double getValue() throws Exception {
			if(board == null){
				throw new Exception("No board for Heuristic");
			}
			
			double transitions = 0;
			boolean isPrevFilled;		//indicator of fill for previous cell in the same row
			
			//checking at row level
			for (int i = 0; i < rows - 1; i++) {
				
				//previous of leftmost column is taken to be filled i.e. left border taken to be filled  
				isPrevFilled = true;
				
				//checking for transitions in the different column but same row
				for(int j = 0; j < cols; j++){
					
					//check for transition from filled to empty found
					if(board[i][j] == 0 && isPrevFilled){
						transitions++;
						isPrevFilled = false;
						
					//check for transition from empty to filled found
					} else if(board[i][j] != 0 && !isPrevFilled){
						transitions++;
						isPrevFilled = true;
					}
				}
				
				//for rightmost row to be checked for previous, right border is taken to be filled
				if(!isPrevFilled){
					transitions++;
				}
			}
			
			return transitions;
		}
	}
	
	/**
	 * COL TRANSITIONS HEURISTIC CLASS -- SUBCLASS OF HEURISTIC
	 * Description: Heuristic values column transitions
	 * A column transition occurs when an empty cell is adjacent to a filled cell on the same column 
	 * and vice versa.
	 */
	public class ColTransitionsHeuristic extends Heuristic{

		@Override
		public double getValue() throws Exception {
			if(board == null){
				throw new Exception("No board for Heuristic");
			}
			
			double transitions = 0;
			boolean isPrevFilled;		//indicator of fill for previous cell in the same column

			//checking at col level
			for (int i = 0; i < cols; i++) {
				
				//previous of first row is taken to be filled i.e. bottom border is taken to be filled
				isPrevFilled = true;
				
				//checking for transitions in the different row but same col
				for(int j = 0; j < rows; j++){
					
					//check for transition from filled to empty
					if(board[j][i] == 0 && isPrevFilled){	
						transitions++;
						isPrevFilled = false;
						
					//check for transition from empty to filled
					} else if(board[j][i] != 0 && !isPrevFilled){	
						transitions++;
						isPrevFilled = true;
					}
				}
			}
			return transitions;
		}

	}
		
	/**
	 * GENE CLASS
	 * Description: Gene Class contains the heuristics and their respective weights that are needed 
	 * in order to evaluate the fitness of a particular state/field, which is represented by a TestState.
	 * Thus, when a move is made, the resulting fitness of the board can be determined using Gene.
	 */
	public class Gene implements Comparable<Gene>{

		//Gene Parameters
		private ArrayList<Heuristic> heuristics;
		private ArrayList<Double> weights;
		private int fitness;	
		private TestState testState;
		
		/**
		 * Constructor for Gene
		 * Description: Initializes all parameters
		 */
		public Gene(){
			heuristics = new ArrayList<Heuristic>();
			weights = new ArrayList<Double>();
			fitness = 0;
			testState = null;
		}
		
		/**
		 * Description: Adds a heuristic to the Gene, including the heuristic's weight
		 * @param h
		 * @param weight
		 */
		public void addHeuristic(Heuristic h, double weight){
			heuristics.add(h);
			weights.add(weight);
		}
		
		/**
		 * Description: Removes a heuristic and its weight from the Gene
		 * @param pos
		 */
		public void removeHeuristic(int pos){
			heuristics.remove(pos);
			weights.remove(pos);
		}
		
		/**
		 * Description: Returns all the weights of the heuristics
		 * @return
		 */
		public ArrayList<Double> getWeights(){
			return weights;
		}
		
		/**
		 * Description: sets the TestState to be evaluated by all heuristics 
		 * @param ts
		 */
		public void setTestState(TestState ts) {
			this.testState = ts;
		}
		
		/**
		 * Description: Set the fitness of the Gene. To avoid redoing of evaluation in training
		 * @param fit
		 */
		public void setFit(int fit){
			this.fitness = fit;
		}
		
		/**
		 * Description: Get the fitness of the Gene
		 * @return
		 */
		public int getFit(){
			return fitness; 
		}
		
		/**
		 * Description: Gives the score of a move using weights and heuristics. Throws exception if no
		 * state was set for the gene
		 * @return
		 * @throws Exception
		 */
		public double evaluateMove() throws Exception{	
			double score = 0;
			if(testState == null){
				throw new Exception("No State to test");
			} else {
				
				for(int i = 0; i < heuristics.size(); i++){
					Heuristic h = heuristics.get(i);
					double weight = weights.get(i);
					
					h.setTestState(testState);
					
					score += weight * h.getValue();
					
					//precaution to check that new testState is to be set next time heuristic is called
					h.setTestState(null);
				}
				return score;
			}
		}
		
		@Override
		/**
		 * Description: Comparison of two Genes according to their fitness
		 * @return
		 * @param g
		 */
		public int compareTo(Gene g) {

			//ascending order
			//return getFit() - g.getFit();
					
			//descending order
			return g.getFit() - getFit();
		}

		/**
		 * Description: Get the TestState being evaluated by the Gene
		 * @return
		 */
		public TestState getTestState() {
			return testState;
		}

	}
	
	/**
	 * RUNNABLE THREAD CLASS
	 * Description: Implementation for multi-threading
	 */
	public class RunnableThread implements Runnable {
		Thread t;
		private String threadName;
		//m_count is the number of times each thread has to run
		private int m_count;
		// M_base stands for the thread number eg thread 1 runs 0-90 thread 2 runs 91-180
		private int m_base;
		GenePool gp;

		/**
		 * Constructor for thread
		 * @param name
		 */
		RunnableThread(String name) {
			threadName = name;
		}
		
		
		public void run() {
			for (int i = 0; i < m_count; i++) {
				System.out.println("Running: " + (m_base + i));
				
				//set fitness of the gene
				gp.setFitness(gp.getGene(m_base + i));
			}

		}
		
		public void start(int count, int base) {
			m_count = count;
			m_base = base;
			if (t == null) {
				t = new Thread(this, threadName);
				t.start();
			}
		}
	}
	
	/**
	 * GENE POOL CLASS
	 * Description: GenePool creates a list of Genes with weights specified in the file provided to its 
	 * constructor. Uses the Genetic Algorithm in order to obtain the best set of weights for the Tetris
	 * Game 
	 * 
	 * NOTE: When adding new heuristic, add to LIST_HEURISTIC
	 */
	public class GenePool {

		private static final String WEIGHTS_FILEPATH = "weights.txt";
		private static final int NUM_GAMES = 2; // number of games to run before setting 
												// the fitness of the gene
		
		private static final int NUM_GENES_TOURNAMENT = 100; // number of genes needed for each
															 // tournament selection
		
		private static final String WEIGHTS_HEADER = "FilledLines | Holes | WellSums | LandingHeight "
													 + "| RowTransitions | ColTransitions | Fitness";
		private final ArrayList<Heuristic> LIST_HEURISTIC = new ArrayList<Heuristic>(){{
			add(new FilledLinesHeuristic());
			add(new HolesHeuristic());
			add(new WellSumsHeuristic());
			add(new LandingHeightHeuristic());
			add(new RowTransitionsHeuristic());
			add(new ColTransitionsHeuristic());
		}};
		
		private int numOfGenes;	//number of genes with fitness set each run
		
		private ArrayList<Gene> geneList;	//ArrayList to store the genes to test

		/**
		 * Constructor for GenePool
		 * Description: Processes the file specified in the file path and initializes parameters
		 */
		public GenePool() {
			geneList = processFile(WEIGHTS_FILEPATH);
			numOfGenes = 0;
		}
		
		/**
		 * Description: Returns the geneList of the GenePool
		 * @return
		 */
		public ArrayList<Gene> getGenes() {
			return geneList;
		}

		/**
		 * Description: writes the weights of the ArrayList of Genes (list_genes) into file
		 * @param filePath
		 */
		public void writeToFile(String filePath) {
			File f = new File(filePath);
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				bw.write(WEIGHTS_HEADER); // header
				bw.newLine();

				for (int i = 0; i < 1000; i++) {
					
					//get Gene and the weights for the Gene
					Gene g = geneList.get(i);
					ArrayList<Double> weights = g.getWeights();
					
					//write all the weights to the file
					for (int j = 0; j < weights.size(); j++) {
						bw.write(weights.get(j) + " ");
					}
					
					//write the fitness of the Gene to the file
					bw.write(g.getFit() + "");
					bw.newLine();
				}
				
				bw.flush();
				bw.close();
			} catch (IOException e) {
				System.out.println("Sorry but the file failed to save properly");
			}
		}

		/**
		 * Description: Generates a list of Genes according to the weights specified in the file
		 * @param filePath
		 * @return
		 */
		public ArrayList<Gene> processFile(String filePath) {
			ArrayList<Gene> fileGenes = new ArrayList<Gene>();
			
			try {
				File f = new File(filePath);
				Scanner sc = new Scanner(f);
				sc.nextLine();

				//obtaining all weights from the file and creating the corresponding Gene
				while (sc.hasNext()) {
					
					//copying weights into an ArrayList
					ArrayList<Double> weights = new ArrayList<Double>();
					for (int i = 0; i < LIST_HEURISTIC.size(); i++){
						weights.add(Double.parseDouble(sc.next()));
					}

					//get fitness of Gene from file to save recalculating fitness
					int fitness = 0;
					if (sc.hasNextInt()) {
						fitness = sc.nextInt();
					}

					//replicate the Gene with the fitness and weights 
					Gene g = new Gene();
					for (int i = 0; i < LIST_HEURISTIC.size(); i++){
						g.addHeuristic(LIST_HEURISTIC.get(i), weights.get(i));
					}
					g.setFit(fitness);

					fileGenes.add(g);
				}
				
				sc.close();
			} catch (FileNotFoundException e) {
				System.out.println("The weights file is not found");
			}
			return fileGenes;
		}

		/**
		 * Description: Adds the given Gene to geneList
		 * @param g
		 */
		public void addGene(Gene g) {
			geneList.add(g);
		}

		/**
		 * Description: Removes the Gene at the given position from geneList
		 * @param i
		 */
		public void deleteGene(int i) {
			geneList.remove(i);
		}

		/**
		 * Description: Returns the Gene at the given position
		 * @param i
		 * @return
		 */
		public Gene getGene(int i) {
			return geneList.get(i);
		}

		/**
		 * Description: Gives the size of geneList
		 * @return
		 */
		public int getSize() {
			return geneList.size();
		}

		/**
		 * Description: Creates a new Gene from 2 older Genes
		 * @return
		 */
		public Gene newGene() {
			ArrayList<Gene> samplePool = pickGenes(NUM_GENES_TOURNAMENT);
			
			// choose the best two for crossover
			Gene child = crossover(samplePool.get(0), samplePool.get(1));
			child = mutate(child);
			return child;
		}

		/**
		 * Description: Returns an ArrayList of new Genes
		 * @param num
		 * @return
		 */
		public ArrayList<Gene> newGeneArray(int num) {
			ArrayList<Gene> producedGenes = new ArrayList<Gene>();
			for (int i = 0; i < num; i++) {
				System.out.println("New Gene " + i);
				producedGenes.add(newGene());
			}
			return producedGenes;
		}

		/**
		 * Description: Mixes the weights of the 2 Genes provided in order to create a child
		 * with the new weights for each Heuristic
		 * 
		 * @param p1
		 * @param p2
		 * @return
		 */
		public Gene crossover(Gene p1, Gene p2) {
			ArrayList<Double> p1Weights = p1.getWeights();
			ArrayList<Double> p2Weights = p2.getWeights();

			Gene child = new Gene();

			//Ratios of weights to cross according to fitness
			double totalFit = p1.getFit() + p2.getFit();
			double p1Ratio = p1.getFit() / totalFit;
			double p2Ratio = p2.getFit() / totalFit;

			//Adding the new weights to child Gene for each Heuristic
			for (int i = 0; i < LIST_HEURISTIC.size(); i++){
				child.addHeuristic(LIST_HEURISTIC.get(i), 
								   p1Weights.get(i) * p1Ratio + p2Weights.get(i) * p2Ratio);
			}
			
			return child;
		}

		/**
		 * Description: Changes the weight of one Heuristic with a specified probability and returns
		 * the Gene with the new weight
		 * 
		 * @param gene
		 * @return
		 */
		public Gene mutate(Gene gene) {
			Random r = new Random();
			
			if (r.nextDouble() * 100 < 5) {
				// random from 0 to size
				int geneNum = r.nextInt(gene.getWeights().size());
				
				// Mutate weight according to current sign of weights
				double mWeight = gene.getWeights().get(geneNum) < 0 
									? gene.getWeights().get(geneNum) - 0.2 
									: gene.getWeights().get(geneNum) + 0.2;

				// Set new mutated weight
				gene.getWeights().set(geneNum, mWeight);
			}

			return gene;

		}

		
		/**
		 * Description: Randomly selects some Gene from geneList
		 * @param num
		 * @return
		 */
		public ArrayList<Gene> pickGenes(int num) {
			Random rand = new Random();
			
			ArrayList<Gene> samplePool = new ArrayList<Gene>();

			//selection process
			for (int i = 0; i < num; i++) {
				int index = rand.nextInt(geneList.size());
				samplePool.add(geneList.get(index));
			}
			
			Collections.sort(samplePool);
			return samplePool;
		}

		/**
		 * Description: Plays the game in order to set the fitness of the Gene
		 * @param g
		 * @return
		 */
		public int setFitness(Gene g) {
			if (g.getFit() != 0) {
				return g.getFit();
			}
			PlayerSkeleton player = new PlayerSkeleton(g);
			
			int count = 0;
			
			for (int j = 0; j < NUM_GAMES; j++) {
				try {
					//add number of lines cleared to the count
					count += player.playGame(g);
					
				} catch (Exception e) {
					System.out.println("Error occured while simulating the game: " + e);
				}
			}
			
			//Output
			System.out.println("Setting fitness for gene: " + g.getWeights());
			System.out.println( "Fitness: " + count / NUM_GAMES);
			System.out.println("Num of genes with fitness set: " + (++numOfGenes));
			
			//get the average fitness
			g.setFit(count / NUM_GAMES);
			
			return count / NUM_GAMES;
		}

		/**
		 * Description: This is how the GenePool runs and changes the weights using the Genetic Algorithm.
		 * It also makes use of multi-threading (RunnableThread class accomplishes this)
		 */
//		public static void main(String[] args) throws Exception {
//			int generation = 0;
//
//			// initialize start of multiThread
//			int nThreads = Runtime.getRuntime().availableProcessors() * 4;
//			int totalRuns = 1000;
//
//			// calculate how many runs each thread need to run
//			int tRuns = totalRuns / nThreads;
//			int left = totalRuns - (tRuns * nThreads);
//
//			// keep track of the runs
//			RunnableThread[] threadArr = new RunnableThread[nThreads];
//			RunnableThread.gp = new GenePool();
//			
//			while (true) {
//				int currCount = 0;
//				
//				System.out.println("Start of generation " + generation);
//				
//				//Firstly we train remaining Genes using multi-threading
//				//Eg. Number of threads = 11
//				//Eg. 1000 % 11 = 10 we multi-thread 991 - 1000
//				for (int i = 0; i < left; i++) {
//					String tName = Integer.toString(i);
//					RunnableThread threads = new RunnableThread(tName);
//					threadArr[i] = threads;
//					threads.start(tRuns + 1, currCount);
//					currCount += tRuns + 1;
//				}
//				
//				//Then we multi-thread the rest of the genes
//				//Eg. Num of threads = 11
//				//thread 1 trains 0 - 90/ thread 2 trains 0- 180 ... thread 11 trains 900-990
//				for (int i = 0; i < (nThreads - left); i++) {
//					String tName = Integer.toString(i);
//					RunnableThread threads = new RunnableThread(tName);
//					threadArr[left + i] = threads;
//					threads.start(tRuns, currCount);
//					currCount += tRuns;
//				}
//
//				// wait for all threads to finish
//				for (RunnableThread i : threadArr) {
//					try {
//						i.t.join();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//
//				// end of multithreading
//	
//				//Then we add in the new genes produced[selection, mutation, crossover]
//				ArrayList<Gene> newGenes = RunnableThread.gp.newGeneArray(300);
//				for (int i = 999; i >= 700; i--) {
//					RunnableThread.gp.genes.remove(i);
//				}
//				Collections.sort(RunnableThread.gp.genes);			
//				RunnableThread.gp.genes.addAll(newGenes);
//				
//				RunnableThread.gp.writeToFile(WEIGHTS_FILEPATH);
//				System.out.println("End of generation " + generation++);
//				System.out.println("The best set of weights: "
//						+ RunnableThread.gp.getGene(0).getWeights());
//				System.out.println("Num of lines cleared: "
//						+ RunnableThread.gp.getGene(0).getFit());
//				System.out.println("------------------------------------------------------------------------------------");
//				numOfGenes = 0;
//			}
//		}
	}

	/** PLAYERSKELETON METHODS **/
	
	/**
	 * Description: Takes in the current State, the legal moves of the current State and a Gene, and returns
	 * the chosen move as a result of applying the Heuristics of the Gene
	 * 
	 * @param s
	 * @param legalMoves
	 * @param gene
	 * @return
	 * @throws Exception
	 */
	public int pickMove(State s, int[][] legalMoves, Gene gene) throws Exception {
		int move = 0; //move 0 is valid, but will only remain as 0 if all moves lead to the same result
					  //i.e. all cause death; return null board
		
		double moveScore = -1000000; 
		
		for(int i = 0; i < legalMoves.length; i++){
			
			TestState b = new TestState(cloneBoard(s.getField()), s.getTop().clone(),
										s.getNextPiece(), legalMoves[i]);
			
			//To avoid overwritting issues with threads
			Gene trainGene = gene;
			trainGene.setTestState(b);
			
			//A null board means that the move causes death, therefore move is skipped
			if(trainGene.getTestState().getBoard() == null){ 
				continue;
			}
			
			//comparing score of moves to select the move with best score
			if(trainGene.evaluateMove() > moveScore){
				move = i;
				moveScore = trainGene.evaluateMove();
			}
			
			//precaution to check that new testState is to be set for next move
			trainGene.setTestState(null);
		}	
		return move;
	}

	/**
	 * Description: Prints out the 2D board entered (For debugging purposes)
	 * 
	 * @param board
	 */
	public static void printBoard(int[][] board){
		System.out.println("Checking Board:");
		
		for(int j = board.length-1; j >= 0; j--){
			System.out.println(Arrays.toString(board[j]) + "");
		}
	}
	
	/**
	 * Description: Determine height of a given column using the given board
	 * @param col
	 * @return 
	 */
	protected int colHeight(int col, int[][] board){
		for(int row = board.length - 1; row >= 0; row--){
			if(board[row][col] != 0){
				return row + 1;
			}
		}
		return 0;
	}
	
	/**
	 * Description: Gives a 2D array object that has values same as the input. Done in order to handle 
	 * pointer issues
	 *  
	 * @param board
	 * @return
	 */
	private int[][] cloneBoard(int[][] board){
		int [][] myInt = new int[board.length][];
		
		for(int i = 0; i < board.length; i++){
			int[] aMatrix = board[i];
			int aLength = aMatrix.length;
			myInt[i] = new int[aLength];
			System.arraycopy(aMatrix, 0, myInt[i], 0, aLength);
		}
		return myInt;
	}
	
	/**
	 * Description: Allows a tetris game to be played without visualization. For training purposes
	 * 
	 * @param g
	 * @return
	 * @throws Exception
	 */
	public int playGame(Gene g) throws Exception{
		State s = new State();		
		
		while(!s.hasLost()) {
			s.makeMove(pickMove(s,s.legalMoves(), g));
		}
		return s.getRowsCleared();
	}
	
	public static void main(String[] args) throws Exception {
		
		//initialization of new game
		State s = new State();
		new TFrame(s);
		
		PlayerSkeleton p = new PlayerSkeleton();
				
		int iteration = 0;
		
		//playing the game
		while(!s.hasLost()) {
			
			s.makeMove(p.pickMove(s,s.legalMoves(), p.g));
//			s.draw();		
//			s.drawNext(0,0);
			if(iteration % 10000 == 0){
				System.out.println("Rows cleared: " + s.getRowsCleared());
			}
			iteration++;

		}
		System.out.println("You have completed " + s.getRowsCleared() + " rows.");		
	}
	
}
