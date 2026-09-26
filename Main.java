import java.util.Scanner;
import checkers.*;
import bot.*;

public class Main{
    public static void main(String[] args){
        System.out.println("Hello World!");
        Board board = new Board();
        board.bot_game();
        // Bot bot = new Bot(Board.Square.Black);
        // board.run(Board.Square.Red, bot);
        //bot.make_move(board);
    }
}