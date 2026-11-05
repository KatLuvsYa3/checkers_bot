package bot;
import checkers.*;

public class BoardState{
    public Move[] bot_moves = new Move[0];
    public Move[] player_moves = new Move[0];
    public double bot_eval = 0;
    public BoardState(){}
    public void print(){
        for(int i = 0; i < Math.max(player_moves.length, bot_moves.length); i++){
            if(i < bot_moves.length){ System.out.print("Bot: "); bot_moves[i].print(); }
            if(i < player_moves.length){System.out.print("  Player: "); player_moves[i].print();}
            System.out.print("\t");
        }
    }
    public Board get_board(Board parent){
        Board state = parent.copy();
        for(int i = 0; i < Math.max(bot_moves.length, player_moves.length); i++){
            if(i < bot_moves.length) state.move(bot_moves[i]);
            if(i < player_moves.length) state.move(player_moves[i]);
        }
        return state;
    }
    public BoardState copy(){
        BoardState b = new BoardState();
        b.bot_moves = bot_moves.clone();
        b.player_moves = player_moves.clone();
        return b;
    }
    public BoardState move(Move m, Piece.Color color, Piece.Color bot_color){
        Move[] temp;
        if(color == bot_color){ 
            temp = bot_moves;
            bot_moves = new Move[bot_moves.length+1];
            for(int i = 0; i < temp.length; i++) bot_moves[i] = temp[i];
            bot_moves[bot_moves.length-1] = m;
        }
        else { 
            temp = player_moves;
            player_moves = new Move[player_moves.length+1];
            for(int i = 0; i < temp.length; i++) player_moves[i] = temp[i];
            player_moves[player_moves.length-1] = m;
        }
        return this;
    }
}