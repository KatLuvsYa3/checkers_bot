import java.util.Scanner;
import checkers.*;
import bot.*;

public class Main{
    public static void main(String[] args){
        System.out.println("Hello World!");
        Board board = new Board();
        // // mid game with promotion vs player
        // board.dev_move(" c2 d3");
        // board.dev_move(" f5 e4");
        // board.dev_move(" d3xe4 f5");
        // board.dev_move(" h5 g4");
        // board.dev_move(" e2 f3");
        // board.dev_move(" g6 h5");
        // board.dev_move(" a2 b3");
        // board.dev_move(" h7 g6");
        // board.dev_move(" f5xg6 h7");
        // board.dev_move(" d5 c4");
        Bot bot = new Bot(Piece.Color.Black);
        // bot.reference_board = board;
        board.run(Piece.Color.Red, bot);

        // board.bot_game();

        // Point p = new Point("a0");
        // System.out.println(p.to_string());
        // Move m = new Move(" a0xb1 c2");
        // System.out.println(m.to_string());
        // board.bot_game();
        //Bot bot = new Bot(Board.Square.Black);
        //board.run(Board.Square.Red, bot);
    }
}