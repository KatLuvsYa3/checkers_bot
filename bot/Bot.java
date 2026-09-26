package bot;

import java.util.function.Consumer;
import java.util.ArrayList;

import checkers.*;

public class Bot {
    // structs
    public static class Point{
        public Point(int r, int f){
            rank = r; file = f;
        }
        public void print(){System.out.print((char)(file+'a')+""+rank);}
        public int rank, file;
    }
    public static class Move{
        public Move(){}
        public Move(Point[] c){coords=c;}
        public Move(int j, Point[] c){jump_count = j; coords=c;}
        public void print(){
            System.out.print("{");
            for(Point pnt : coords){pnt.print(); System.out.print(", ");}
            System.out.print("}");
        }
        public int jump_count = 0;
        public Point[] coords;
    }
    public static class BoardState{
        public Move[] bot_moves = new Move[0];
        public Move[] player_moves = new Move[0];
        public double bot_eval = 0;
        public void print(){
            for(int i = 0; i < bot_moves.length; i++){
                System.out.print("Bot: "); bot_moves[i].print();
                if(i < player_moves.length){System.out.print("\nPlayer: "); player_moves[i].print();}
                System.out.print("\n");
            }
        }
        public Board get_board(Board parent){
            Board state = parent.copy();
            for(int i = 0; i < bot_moves.length; i++){
                state.move(bot_moves[i]);
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
        public BoardState move(Move m, Board.Square color, Board.Square bot_color){
            Move[] temp;
            if(color == bot_color){ 
                temp = bot_moves;;
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

    public static class BSTree{
        public BSTree(){
            nodes = new ArrayList<>();
            add(new BoardState());
        }

        public static class Node { 
            public BSTree tree;
            public BoardState bs; 
            public final ArrayList<Integer> children = new ArrayList<>();
            public int id, parent = -1;
            public Node(){} public Node(BoardState _bs, BSTree _tree){bs = _bs; tree = _tree;}
            public void connect(int c){children.add(c); tree.index(c).parent = id;}
            public double eval_branch(Board ref, Bot bot){
                if (children.size()==0){
                    double v = bot.evaluate_board(bs, bot.bot_color);
                    // if(v!=0){
                    //     bs.get_board(ref).display();
                    //     bs.print();
                    // }
                    return v;
                }

                double sum = 0;
                for( int c : children ){
                    double v = tree.index(c).eval_branch(ref, bot);
                    sum += v;
                }
                //return (sum+bs.bot_eval)/(children.size()+1);
                return sum/children.size();
            }
            public void print(String tabs){
                System.out.print(id + " ");
                bs.print();
                System.out.print("\nchildren");
                for( int c : children ){
                    tree.index(c).print(tabs+"\t");
                }
            }
        }
        private ArrayList<Node> nodes;
        public Node index(int i){return nodes.get(i);}
        public Node add(BoardState bs){
            nodes.add(new Node(bs.copy(), this)); 
            nodes.get(nodes.size()-1).id=nodes.size()-1;
            return nodes.get(nodes.size()-1);
        }
        public Node last(){return nodes.get(nodes.size()-1);}
        public Node root(){return nodes.get(0);}
        public void print(){
            root().print("");
        }
    }

    private Board reference_board;
    public Board.Square bot_color;
    public int initial_depth = 2;

    public Bot(Board.Square _bot_color){bot_color=_bot_color;}

    private class RecordEntry{
        public RecordEntry(double s, Move _m){score = s; m = _m;}
        public double score; public Move m;
        public void print(){
            m.print();
            System.out.println("\t"+score);
        }
    }
    public Move make_move(Board board){
        reference_board = board;

        //make initial state tree
        BSTree tree = new BSTree();
        boolean available_move = build_board_state_tree(tree.root().id, reference_board, bot_color, tree, initial_depth);
        if(!available_move){return null;}

        //make trimmed tree and take best moves from
        //  try to evaluate based on best moves of other player
        RecordEntry[] bot_record = trimmed_tree_record(board, bot_color, tree);
        // System.out.println("TOP 3 BEST");
        // for(RecordEntry e : bot_record){
        //     e.print();
        // }

        // for( RecordEntry e : bot_record ){
        //     BoardState state = new BoardState();
        //     state.move(e.m, bot_color, bot_color);
        //     BSTree ntree = new BSTree();
        //     build_board_state_tree(ntree.root().id, state.get_board(reference_board), bot_color.swapped(), ntree, initial_depth);
        //     RecordEntry[] player_record = trimmed_tree_record(state.get_board(reference_board), bot_color.swapped(), ntree);
        //     System.out.println("PLAYER LAST");
        //     for(RecordEntry en : player_record){
        //         en.print();
        //     }
        // }

        return bot_record[0].m;
    }

    private RecordEntry[] trimmed_tree_record(Board board, Board.Square color, BSTree tree){
        //make trimmed tree
        BSTree trimmed_tree = new BSTree();
        // System.out.println("P1");
        RecordEntry[] p1_record = ranked_moves(board, tree, color);
        for(int i = 0; i < Math.clamp(p1_record.length, 0, 3); i++){
            BoardState state = new BoardState();
            state.move(p1_record[i].m, color, bot_color);
            trimmed_tree.add(state);
            int p1_bs_index = trimmed_tree.last().id;
            trimmed_tree.root().connect(p1_bs_index);

            // System.out.print("P2 from");
            // state.print();
            RecordEntry[] p2_record = ranked_moves(state.get_board(reference_board), tree, color.swapped());
            for(int j = 0; j < Math.clamp(p2_record.length, 0, 3); j++){
                BoardState pstate = state.copy();
                pstate.move(p2_record[j].m, color.swapped(), bot_color);
                trimmed_tree.add(pstate);
                trimmed_tree.index(p1_bs_index).connect(trimmed_tree.last().id);
            }
        }
        // System.out.println("FINAL");
        return ranked_moves(board, trimmed_tree, color);
    }

    private RecordEntry[] ranked_moves(Board board, BSTree tree, Board.Square color){
        ArrayList<RecordEntry> record = new ArrayList<>();
        for(int id : tree.root().children){
            double v = tree.index(id).eval_branch(board, this);
            RecordEntry e = new RecordEntry(v, tree.index(id).bs.bot_moves[0]);
            int i = 0;
            for(; i < record.size(); i++){
                if (color == bot_color) {
                    if(record.get(i).score<e.score){ break; }
                }
                else {
                    if(record.get(i).score>e.score){ break; }
                }
            }
            record.add(i, e);
        }

        // for(RecordEntry e : record){
        //     e.print();
        // }
        RecordEntry[] arr = new RecordEntry[record.size()];
        return record.toArray(arr);
    }

    private boolean build_board_state_tree(int bs, Board board, Board.Square color, BSTree tree, int depth){
        if(depth == 0) {
            tree.index(bs).bs.bot_eval = evaluate_board(tree.index(bs).bs, bot_color);
            return true;
        }
        Board old_board = tree.index(bs).bs.get_board(board);
        Point[] pieces = old_board.grab_color(color);
        boolean available_move = false;
        for(Point pnt : pieces){
            Move[] legal = legal_moves(old_board, pnt);
            if(legal.length == 0) continue;
            for( Move move : legal ){
                if(legal == null) continue;
                BoardState new_bs = tree.index(bs).bs.copy().move(move, color, bot_color);
                tree.add(new_bs).bs.bot_eval = evaluate_board(new_bs, bot_color);
                tree.index(bs).connect(tree.last().id);
                build_board_state_tree(tree.last().id, board, color.swapped(), tree, color != bot_color ? depth-1 : depth);
            }
            available_move = true;
        }
        return available_move;
    }

    private Move[] legal_moves(Board board, /*Piece p,*/ Point pnt){
        ArrayList<Move> moves = new ArrayList<Move>();
        Board.Square color = board.index_board(pnt.rank, pnt.file);
        int differentiator = color == Board.Square.Red ? 1 : -1;
        Point[] temp_ps;

        //moving forward, no jump
        if (board.index_board(pnt.rank+differentiator, pnt.file+1)==Board.Square.Empty){
            temp_ps = new Point[] {pnt, new Point(pnt.rank+differentiator, pnt.file+1)};
            moves.add(new Move(temp_ps));
        }
        if (board.index_board(pnt.rank+differentiator, pnt.file-1)==Board.Square.Empty){
            temp_ps = new Point[] {pnt, new Point(pnt.rank+differentiator, pnt.file-1)};
            moves.add(new Move(temp_ps));
        }

        //moving forward, single jump
        if (
            board.index_board(pnt.rank+differentiator, pnt.file+1)==color.swapped() &&
            board.index_board(pnt.rank+differentiator*2, pnt.file+2)==Board.Square.Empty
        ){
            temp_ps = new Point[] {pnt, new Point(pnt.rank+differentiator*2, pnt.file+2)};
            moves.add(new Move(1, temp_ps));
        }
        if (
            board.index_board(pnt.rank+differentiator, pnt.file-1)==color.swapped() &&
            board.index_board(pnt.rank+differentiator*2, pnt.file-2)==Board.Square.Empty
        ){
            temp_ps = new Point[] {pnt, new Point(pnt.rank+differentiator*2, pnt.file-2)};
            moves.add(new Move(1, temp_ps));
        }

        //do multi-jump

        Move[] mr = new Move[moves.size()];
        return moves.toArray(mr);
    }

    private double evaluate_board(BoardState bs, Board.Square color){
        Board board = bs.get_board(reference_board);
        //material
        int material = (bot_color == Board.Square.Black ? -1 : 1) * board.count_iterate((s) -> {
            return switch(s){
                case Board.Square.Empty -> 0; 
                case Board.Square.Red -> 1;
                case Board.Square.Black -> -1;
                default -> -50;
            };
        });

        //tatical 
        Point[] opp_pieces = board.grab_color(color.swapped());
        int opp_jumps = 0;
        for( Point pnt : opp_pieces ){
            Move[] opp_legal = legal_moves(board, pnt);
            for( Move m : opp_legal ){
                opp_jumps+=m.jump_count;
            }
        }

        return material*3 - (opp_jumps > 1 ? 1.0+opp_jumps*1.3 : opp_jumps);
    }

    // private class BoardPattern{
    //     int size;

    // }
    // private boolean pattern_search(Board board, BoardPattern pattern){

    // }
}
