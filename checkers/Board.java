package checkers;

import bot.Bot;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.ArrayList;

public class Board {
    public enum Square {
        Empty, Black, Red, OOB; //out of bounds
        public Square swapped(){
            return switch(this){
                case Black -> Red;
                case Red -> Black;
                default -> this;
            };
        }
    };
    private int board_size = 8; // must be even
    private Square[] board = new Square[board_size*board_size]; 
        public void override_board(Square[] _b){board = _b.clone();}
    private String error_buffer = "";
    private Scanner scan = new Scanner(System.in);

    public Board(){
        //fill out board
        for(int rank = 0; rank < board_size; rank++){
            for(int file = 0; file < board_size; file++){
                int index = get_board_index(rank, file);
                board[index] = (file+rank*(board_size+1)) % 2 != 0 ? 
                    //on the diagonals
                    rank < 3 ? Square.Red : ( rank > 4) ? Square.Black : Square.Empty
                :
                    //off the diagonals
                    Square.Empty;
            }
        }
    }

    private Board(boolean x){}

    public Board copy(){
        Board b = new Board(true);
        b.override_board(board);
        return b;
    }

    //bot game
    public void bot_game(){
        Bot black = new Bot(Board.Square.Black);
        Bot red = new Bot(Board.Square.Red);
        boolean game_running = true;
        // while(game_running){
        for(int i = 0; i < 50; i++){
            if(!poll_bot_move(red)) break;
            if(!poll_bot_move(black)) break;
            display();
        }
        display();
        int score = count_iterate((s) -> {
            return switch(s){
                case Board.Square.Empty -> 0; 
                case Board.Square.Red -> 1;
                case Board.Square.Black -> -1;
                default -> -50;
            };
        });
        if(score == 0) System.out.println("DRAW!");
        else if(score < 0) System.out.println("BLACK WINS!");
        else System.out.println("RED WINS!");
    }

    //run game
    public void run(Square player_color, Bot bot){
        display();
        boolean game_running = true;
        while(game_running){
            //turn order
            switch(player_color){
                case Square.Red:
                    poll_player_move(player_color);
                    display();
                    poll_bot_move(bot);
                    display();
                    break;
                case Square.Black:
                    poll_bot_move(bot);
                    display();
                    poll_player_move(player_color);
                    display();
                    break;
                default: System.exit(-1);
            }
        }
    }

    //indexing 
    public int get_board_index(int rank, char file){
        return get_board_index(rank, (int)(file-'a'));
    }
    public int get_board_index(int rank, int file){
        return file+rank*board_size;
    }

    //grabbing state of coords
    public Square index_board(int rank, char file){
        return index_board(rank, file-'a');
    }
    public Square index_board(int rank, int file){
        if ((rank<0||rank>board_size-1) || (file<0||file>board_size-1))
            return Square.OOB;
        return board[get_board_index(rank, file)];
    }

    //iteration
    public interface countIterLambda{
        int x(Square s);
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

    public Bot.Point[] grab_color(Square color){
        ArrayList<Bot.Point> pnts = new ArrayList<Bot.Point>();
        for(int rank = 0; rank < board_size; rank++){
            for(int file = 0; file < board_size; file++){
                if(index_board(rank, file)==color){
                    Bot.Point p = new Bot.Point(rank, file);
                    pnts.add(p);
                }
            }
        }
        Bot.Point[] mr = new Bot.Point[pnts.size()];
        return pnts.toArray(mr);
    }

    //custom catch for methods that can error
    public boolean ctch(boolean t){
        System.out.println(error_buffer);
        error_buffer="";
        return t;
    }

    //CAN ERROR: moving p1 (file, rank) to p2 (file, rank)
    public boolean move(Bot.Move m){
        return move(m.coords[0].rank, m.coords[0].file, m.coords[1].rank, m.coords[1].file);
    }
    public boolean move(char file, int rank, char dfile, int drank){
        return move(rank, (int)(file-'a'), drank, (int)(dfile-'a'));
    }
    public boolean move(int rank, char file, int drank, char dfile){
        return move(rank, (int)(file-'a'), drank, (int)(dfile-'a'));
    }
    public boolean move(int rank, int file, int drank, int dfile){
        Square moved = index_board(rank, file);
        Square destination = index_board(drank, dfile);

        //catch invalid actions
        if(moved == Square.OOB || destination == Square.OOB) {error_buffer+="out of bounds"; return false;}
        if(moved == Square.Empty) {error_buffer+="target empty"; return false;}
        if(destination != Square.Empty) {error_buffer+="dest not empty"; return false;}
        if(moved == Square.Red && drank <= rank) {error_buffer+="red move backward"; return false;}
        else if(moved == Square.Black && drank >= rank) {error_buffer+="black move backward"; return false;}

        if(Math.abs(rank-drank) != 1 || Math.abs(file-dfile) != 1){
            //jumping over
            if(Math.abs(rank-drank) != 2 || Math.abs(file-dfile) != 2) {error_buffer+="not valid direction"; return false;}
            int jumped_rank = ((drank-rank)/2)+rank;
            int jumped_file = ((dfile-file)/2)+file;
            Square jumped = index_board(jumped_rank, jumped_file);
            if(jumped==Square.Empty) {error_buffer+="jumping empty"; return false;}
            if(jumped==moved) {error_buffer+="jumping self"; return false;}
            board[get_board_index(jumped_rank, jumped_file)] = Square.Empty;
        }
        //set current empty, set dest with the piece
        board[get_board_index(rank, file)] = Square.Empty;
        board[get_board_index(drank, dfile)] = moved;

        //success 
        return true;
    }

    public boolean poll_bot_move(Bot bot){
        Bot.Move m = bot.make_move(this);
        if(m==null) return false;
        move(m);
        return true;
    }

    public void poll_player_move(Square player_color){
        boolean invalid = true;
        while (invalid){
            int rank, file, drank, dfile;
            while(true){
                System.out.print("Moved piece: ");
                String s = scan.nextLine();
                if(s.length()>3) continue;
                if(s.charAt(0)<'a'||s.charAt(0)>'h') continue;
                if(s.charAt(1)<'0'||s.charAt(1)>'7') continue;
                file = (int)(s.charAt(0)-'a');
                rank = (int)(s.charAt(1)-'0');
                if(index_board(rank, file)!=player_color) continue;
                break; 
            }

            while(true){
                System.out.print("Destination: ");
                String s = scan.nextLine();
                if(s.length()>3) continue;
                if(s.charAt(0)<'a'||s.charAt(0)>'h') continue;
                if(s.charAt(1)<'0'||s.charAt(1)>'7') continue;
                dfile = (int)(s.charAt(0)-'a');
                drank = (int)(s.charAt(1)-'0');
                break; 
            }
            invalid = !ctch(move(rank, file, drank, dfile));
        }
    }

    //display the checkers board
    public void display(){
        System.out.println("  _________________________");
        for(int rank = board_size-1; rank > -1; rank--){
            System.out.print((/*1+*/rank)+" |");
            for(int file = 0; file < board_size; file++){
                switch(index_board(rank, file)){
                    case Square.Red: 
                        System.out.print("XX");
                        break;
                    case Square.Black:
                        System.out.print("00");
                        break;
                    case Square.Empty:
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
