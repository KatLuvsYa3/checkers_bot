package checkers;

import bot.Bot;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.ArrayList;

public class Board {

    private int board_size = 8; // must be even
    private Piece[] pieces = new Piece[board_size*3];
        private int piece_count = 0;
    private int[] board = new int[board_size*board_size]; // id for piece, -1 for empty, -2 for out of bounds
        public void override_board(int[] _b, Piece[] _p){
            board = _b.clone();
            pieces = new Piece[_p.length];
            for(int i = 0; i < _p.length; i++){
                pieces[i] = _p[i].copy();
            }
        }
    private String error_buffer = "";
    private Scanner scan = new Scanner(System.in);

    private int new_piece(Piece.Color c, int rank, int file){
        pieces[piece_count] = new Piece(c, new Point(rank, file), piece_count);
        return piece_count++;
    }

    public Board(){
        //fill out board
        for(int rank = 0; rank < board_size; rank++){
            for(int file = 0; file < board_size; file++){
                int index = get_board_index(rank, file);
                board[index] = (file+rank*(board_size+1)) % 2 == 0 ? 
                    //on the diagonals
                    rank < 3 ? new_piece(Piece.Color.Red, rank, file) : ( rank > 4) ? new_piece(Piece.Color.Black, rank, file) : -1
                :
                    //off the diagonals
                    -1;
            }
        }
    }

    public boolean move(Move m){
        if(!m.is_legal(this)){ 
            m.print();
            display();
            throw new RuntimeException("\nIllegal move: "+m.error_buffer);
            //m.error_buffer="";
            //System.out.println(index_board(m.pairs[3].p.rank, m.pairs[3].p.file)); 
            //System.exit(-1);
            //return false;
        }
        m.apply_to(this);
        return true;
    }

    public boolean dev_move(String raw){
        return move(new Move(raw));
    }

    private Board(boolean x){}

    public Board copy(){
        Board b = new Board(true);
        b.override_board(board, pieces);
        return b;
    }

    //bot game
    public void bot_game(){
        Bot black = new Bot(Piece.Color.Black);
        Bot red = new Bot(Piece.Color.Red);
        boolean game_running = true;
        while(game_running){
            System.out.println("RED MOVING...");
            if(!poll_bot_move(red)) break;
            display();
            System.out.println("BLACK MOVING...");
            if(!poll_bot_move(black)) break;
            display();
        }
        display();
        int score = count_iterate((index) -> {
            return index == -1 ? 0 : 
            switch(index_piece(index).color){
                case Piece.Color.Red -> 1;
                case Piece.Color.Black -> -1;
                default -> -50;
            };
        });
        if(score == 0) System.out.println("DRAW!");
        else if(score < 0) System.out.println("BLACK WINS!");
        else System.out.println("RED WINS!");
    }

    //run game
    public void run(Piece.Color player_color, Bot bot){
        display();
        boolean game_running = true;
        while(game_running){
            //turn order
            switch(player_color){
                case Piece.Color.Red:
                    poll_player_move(player_color);
                    display();
                    if(piece_count < 2){ game_running = false; continue; }
                    poll_bot_move(bot);
                    display();
                    if(piece_count < 2) game_running = false;
                    break;
                case Piece.Color.Black:
                    poll_bot_move(bot);
                    display();
                    if(piece_count < 2){ game_running = false; continue; }
                    poll_player_move(player_color);
                    display();
                    if(piece_count < 2) game_running = false;
                    break;
                default: System.exit(-1);
            }
        }
    }

    //player game
    public void player_game(){
        display();
        boolean game_running = true;
        while(game_running){
            //turn order
            poll_player_move(Piece.Color.Red);
            display();
            if(piece_count < 2){ game_running = false; continue; }
            poll_player_move(Piece.Color.Black);
            display();
            if(piece_count < 2) game_running = false;
        }
    }

    //___INDEXING___ 
        public int get_board_index(int rank, char file){
            return get_board_index(rank, (int)(file-'a'));
        }
        public int get_board_index(int rank, int file){
            return file+rank*board_size;
        }

        //grabbing piece id of coords
        public int index_board(int rank, char file){
            return index_board(rank, file-'a');
        }
        public int index_board(int rank, int file){
            if ((rank<0||rank>board_size-1) || (file<0||file>board_size-1))
                return -2;
            return board[get_board_index(rank, file)];
        }

        //grabbing color of coords
        public Piece.Color index_color(int rank, char file){
            return index_color(rank, file-'a');
        }
        public Piece.Color index_color(int rank, int file){
            int index = index_board(rank, file);
            if ( index < 0 ) return Piece.Color.ERROR;
            return pieces[index].color;
        }

        //!!UNSAFE!! grabbing piece of coords
        public Piece get_piece(int rank, char file){
            return get_piece(rank, file-'a');
        }
        public Piece get_piece(int rank, int file){
            return pieces[index_board(rank, file)];
        }

        public Piece index_piece(int index){
            return pieces[index];
        }

        //!!UNSAFE!! grabbing promotion of piece at coords
        public boolean get_promotion(int rank, char file){
            return get_promotion(rank, file-'a');
        }
        public boolean get_promotion(int rank, int file){
            return pieces[index_board(rank, file)].promoted;
        }

    //unverified move
    public boolean move(Piece piece, Point p){
        board[get_board_index(piece.position.rank, piece.position.file)] = -1;
        board[get_board_index(p.rank, p.file)] = piece.id;
        piece.position = p;
        if((p.rank==0 && piece.color==Piece.Color.Black)||(p.rank==7 && piece.color==Piece.Color.Red))
            piece.promoted = true;
        return true;
    }

    //unverified take
    public boolean take(Point p){
        get_piece(p.rank, p.file).taken = true;
        board[get_board_index(p.rank, p.file)] = -1;
        piece_count--;
        return true;
    }


    //iteration
    public interface countIterLambda{
        int x(int index);
    }
    public int count_iterate(countIterLambda lambda){
        int sum = 0;
        for(int rank = 0; rank < board_size; rank++){
            for(int file = 0; file < board_size; file++){
                sum+=lambda.x(index_board(rank, file));
            }
        }
        return sum;
    }

    public Point[] grab_color(Piece.Color color){
        ArrayList<Point> pnts = new ArrayList<Point>();
        for(Piece p : pieces){
            if(p.color == color && !p.taken){ 
                pnts.add(p.position);
            }
        }
        Point[] mr = new Point[pnts.size()];
        return pnts.toArray(mr);
    }

    // //custom catch for methods that can error
    // public boolean ctch(boolean t){
    //     System.out.println(error_buffer);
    //     error_buffer="";
    //     return t;
    // }

    // //CAN ERROR: moving p1 (file, rank) to p2 (file, rank)
    // //INCLUDE MULTIJUMPS, REWORK THIS FUNCTION
    // public boolean move(Bot.Move m){
    //     if(!move(m.coords[0].rank, m.coords[0].file, m.coords[1].rank, m.coords[1].file))return false;
    //     for(int i = 2; i < m.coords.length; i++){
    //         if(!move(m.coords[i-1].rank, m.coords[i-1].file, m.coords[i].rank, m.coords[i].file))return false;
    //     }
    //     return true;
    // }
    // public boolean move(char file, int rank, char dfile, int drank){
    //     return move(rank, (int)(file-'a'), drank, (int)(dfile-'a'));
    // }
    // public boolean move(int rank, char file, int drank, char dfile){
    //     return move(rank, (int)(file-'a'), drank, (int)(dfile-'a'));
    // }
    // public boolean move(int rank, int file, int drank, int dfile){
    //     Square moved = index_board(rank, file);
    //     Square destination = index_board(drank, dfile);

    //     //catch invalid actions
    //     if(moved == Square.OOB || destination == Square.OOB) {error_buffer+="out of bounds"; return false;}
    //     if(moved == Square.Empty) {error_buffer+="target empty"; return false;}
    //     if(destination != Square.Empty) {error_buffer+="dest not empty"; return false;}
    //     if(moved == Square.Red && drank <= rank) {error_buffer+="red move backward"; return false;}
    //     else if(moved == Square.Black && drank >= rank) {error_buffer+="black move backward"; return false;}

    //     if(Math.abs(rank-drank) != 1 || Math.abs(file-dfile) != 1){
    //         //jumping over
    //         if(Math.abs(rank-drank) != 2 || Math.abs(file-dfile) != 2) {error_buffer+="not valid direction"; return false;}
    //         int jumped_rank = ((drank-rank)/2)+rank;
    //         int jumped_file = ((dfile-file)/2)+file;
    //         Square jumped = index_board(jumped_rank, jumped_file);
    //         if(jumped==Square.Empty) {error_buffer+="jumping empty"; return false;}
    //         if(jumped==moved) {error_buffer+="jumping self"; return false;}
    //         board[get_board_index(jumped_rank, jumped_file)] = Square.Empty;
    //     }
    //     //set current empty, set dest with the piece
    //     board[get_board_index(rank, file)] = Square.Empty;
    //     board[get_board_index(drank, dfile)] = moved;

    //     //success 
    //     return true;
    // }

    public void poll_player_move(Piece.Color player_color){
        boolean invalid = true;
        Move m = new Move(true); 
        while (invalid){
            System.out.print("Player Move:");
            String s = scan.nextLine(); 
            m = new Move(s);
            if(m.pairs.length < 2) continue;
            if(index_color(m.pairs[0].p.rank, m.pairs[0].p.file)==player_color.swapped()) continue;
            invalid = !m.is_legal(this);
            System.out.println(m.error_buffer);
        }
        move(m);
    }

    public boolean poll_bot_move(Bot bot){
        Move m = bot.make_move(this);
        if(m==null) return false;
        if(!m.is_legal(this)){ 
            System.out.println("BOT MADE INVALID MOVE"); 
            System.exit(-1);
        }
        move(m);
        return true;
    }

    //display the checkers board
    public void display(){
        System.out.println("  _________________________");
        for(int rank = board_size-1; rank > -1; rank--){
            System.out.print((/*1+*/rank)+" |");
            for(int file = 0; file < board_size; file++){
                switch(index_color(rank, file)){
                    case Piece.Color.Red: 
                        System.out.print(get_promotion(rank, file) ? "KX" : "XX");
                        break;
                    case Piece.Color.Black:
                        System.out.print(get_promotion(rank, file) ? "K0" : "00");
                        break;
                    case Piece.Color.ERROR:
                        System.out.print("  ");
                        break;
                    default: System.exit(-1);
                }
                System.out.print("|");
            }
            System.out.print("\n");
            //System.out.println("|__________________________|\n");
        }
        System.out.print("  -------------------------\n   ");
        for(int file = 0; file < board_size; file++){
            System.out.print((char)((char)file+'a')+"  ");
        }
        System.out.print("\n");
    } 
}