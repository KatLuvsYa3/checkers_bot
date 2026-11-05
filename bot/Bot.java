package bot;

import java.util.function.Consumer;
import java.util.ArrayList;

import checkers.*;

//add multi jumps and promotion

public class Bot {
    public Board reference_board;
    public Piece.Color bot_color;
    public int initial_depth = 2; //anything more than this is an explosion

    public Bot(Piece.Color _bot_color){bot_color=_bot_color;}

    private class RecordEntry{
        public RecordEntry(double s, Move _m){score = s; m = _m;}
        public double score; public Move m;
        public void print(){
            m.print();
            System.out.println("\t"+score);
        }
    }

    // public Move make_move(Board board){
    //     reference_board = board;
    //     Point[] pnts = board.grab_color(bot_color);
    //     ArrayList<Move> moves = new ArrayList<Move>();
    //     for(Point p : pnts){
    //         Move[] legal = legal_moves(board, p);
    //         for (Move m : legal){
    //             moves.add(m);
    //         }
    //     }

    //     double best_score = -50;
    //     Move best_move = new Move(true);
    //     for(Move m : moves){
    //         BoardState b = new BoardState().move(m, bot_color, bot_color);
    //         double score = evaluate_board(b, bot_color);
    //         if(score > best_score){
    //             best_score = score;
    //             best_move = m;
    //         }
    //     }

    //     return best_move;
    // }

    public Move make_move(Board board){
        reference_board = board;

        //make initial state tree
        BSTree tree = new BSTree();
        boolean available_move = build_board_state_tree(tree.root().id, reference_board, bot_color, tree, initial_depth);
        if(!available_move){return null;}

        //try to evaluate based on best moves of other player
        //RecordEntry bot_move = best_move(board, bot_color, tree);
        RecordEntry[] record = ranked_moves(reference_board, tree, bot_color);

        //return bot_move.m;
        return record[0].m;
    }

    //something broken in here with tree build for opposite color
    private RecordEntry best_move(Board board, Piece.Color color, BSTree tree){
        RecordEntry worst_best_repsonse = null;
        int best_move = 0;
        //make a record of player1's (bot) 3 best move board states
        RecordEntry[] p1_record = ranked_moves(board, tree, color);
        //for each board state, make new tree
        for(int i = 0; i < Math.clamp(p1_record.length, 0, 3); i++){
            BSTree sub_tree = new BSTree();
            BoardState state = new BoardState();
            state.move(p1_record[i].m, color, bot_color);
            boolean available_move = build_board_state_tree(tree.root().id, state.get_board(reference_board), bot_color, sub_tree, initial_depth);

            //...then make fetch the best player2 (player) moves for each state
            RecordEntry[] p2_record = ranked_moves(state.get_board(reference_board), sub_tree, color.swapped());
            if(worst_best_repsonse==null){
                worst_best_repsonse = p2_record[0];
                best_move = i;
            }
            else if((-p2_record[0].score)>(-worst_best_repsonse.score)) {
                worst_best_repsonse = p2_record[0];
                best_move = i;
            }
        }
        //
        return p1_record[best_move];
    }

    private RecordEntry[] ranked_moves(Board board, BSTree tree, Piece.Color color){
        ArrayList<RecordEntry> record = new ArrayList<RecordEntry>();
        for(int id : tree.root().children){
            double v = tree.index(id).eval_branch(board, this);
            if(tree.index(id).bs.bot_moves.length==0){tree.index(id).print("");} //BREAKS HERE WHEN RUNNING FROM PLAYER PROP
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
    
    //add a given board state and all its legal moves up to depth to tree
    private boolean build_board_state_tree(int bs, Board board, Piece.Color color, BSTree tree, int depth){
        //when end of depth, get evaluation of the board
        if(depth == 0) {
            tree.index(bs).bs.bot_eval = evaluate_board(tree.index(bs).bs, bot_color);
            return true;
        }
        Board old_board = tree.index(bs).bs.get_board(board);
        Point[] pieces = old_board.grab_color(color);
        boolean available_move = false;
        //for all pieces find all the legal moves for that piece
        for(Point pnt : pieces){
            Move[] legal = legal_moves(old_board, pnt);
            if(legal.length == 0) continue;
            //for each legal move, add that new board to the board state tree
            //evaluate that board state
            //build_board_state_tree of new board
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

    private Move[] legal_moves(Board board, Point pnt){
        ArrayList<Move> moves = new ArrayList<Move>();
        Board hypothetical_board = board.copy();

        int piece_index = hypothetical_board.index_board(pnt.rank, pnt.file);
        Piece piece = hypothetical_board.get_piece(pnt.rank, pnt.file);

        int differentiator = (
            (piece.color == Piece.Color.Red && !piece.promoted)
            || (piece.color == Piece.Color.Black && piece.promoted)
        )
         ? 1 : -1;

        //moving forward, no jump
        if (board.index_board(pnt.rank+differentiator, pnt.file+1)==-1){
            moves.add(new Move(
                new Point[]{pnt, new Point(pnt.rank+differentiator, pnt.file+1)}
            ));
        }
        if (board.index_board(pnt.rank+differentiator, pnt.file-1)==-1){
            moves.add(new Move(
                new Point[]{pnt, new Point(pnt.rank+differentiator, pnt.file-1)}
            ));
        }

        Move[] jumps = legal_jumps(board, pnt);
        for(Move m : jumps){
            moves.add(m);
        }

        Move[] mr = new Move[moves.size()];
        return moves.toArray(mr);
    }

    // recursively find all single and double jumps
    private Move[] legal_jumps(Board board, Point pnt){
        ArrayList<Move> moves = new ArrayList<Move>();
        Piece.Color color = board.index_color(pnt.rank, pnt.file);

        Piece piece = board.get_piece(pnt.rank, pnt.file);
        int differentiator = (
            (piece.color == Piece.Color.Red && !piece.promoted)
            || (piece.color == Piece.Color.Black && piece.promoted)
        )
         ? 1 : -1;

        //moving forward, single jump
        if (
            board.index_board(pnt.rank+differentiator, pnt.file+1)!=-1 &&
            board.index_color(pnt.rank+differentiator, pnt.file+1)==color.swapped() &&
            board.index_board(pnt.rank+differentiator*2, pnt.file+2)==-1 
        ){
            Move m = new Move(
                " " + pnt.to_string() + 
                "x" + (new Point(pnt.rank+differentiator, pnt.file+1)).to_string() + 
                " " + (new Point(pnt.rank+differentiator*2, pnt.file+2)).to_string()
            );
            if(!m.is_legal(board)){
                System.out.println("CAUGHT!");
                m.print();
                System.out.println("\n"+(board.index_board(pnt.rank+differentiator, pnt.file+1)!=-1));
                System.out.println(board.index_color(pnt.rank+differentiator, pnt.file+1)==color.swapped());
                System.out.println(board.index_board(pnt.rank+differentiator*2, pnt.file+2)==-1);
                System.exit(-1);
            }
            moves.add(m);
        }
        if (
            board.index_board(pnt.rank+differentiator, pnt.file-1)!=-1 &&
            board.index_color(pnt.rank+differentiator, pnt.file-1)==color.swapped() &&
            board.index_board(pnt.rank+differentiator*2, pnt.file-2)==-1
        ){  
            Move m = new Move(
                " " + pnt.to_string() + 
                "x" + (new Point(pnt.rank+differentiator, pnt.file-1)).to_string() + 
                " " + (new Point(pnt.rank+differentiator*2, pnt.file-2)).to_string()
            );
            if(!m.is_legal(board)){
                System.out.println("CAUGHT!");
                m.print();
                System.out.println("\n"+(board.index_board(pnt.rank+differentiator, pnt.file-1)!=-1));
                System.out.println(board.index_color(pnt.rank+differentiator, pnt.file-1)==color.swapped());
                System.out.println(board.index_board(pnt.rank+differentiator*2, pnt.file-2)==-1);
                System.exit(-1);
            }
            moves.add(m);
        }

        if(moves.size()==0) return new Move[0];

        //check for multi-jumps
        ArrayList<Move> multijumps = new ArrayList<Move>();
        int i = 0;
        for(Move m : moves){
            Board new_board = board.copy();
            new_board.move(m);
            Move[] new_jumps = legal_jumps(new_board, m.pairs[m.pairs.length-1].p);
            for(Move new_jump : new_jumps){
                i++;
                multijumps.add(m.new_appended(new_jump));
            }
            i++;
        }
        for(Move m : multijumps){
            moves.add(m);
        }

        Move[] mr = new Move[moves.size()];
        return moves.toArray(mr);
    }

    public double evaluate_board(BoardState bs, Piece.Color color){
        Board board = bs.get_board(reference_board);
        //material
        int material = (bot_color == Piece.Color.Black ? -1 : 1) * board.count_iterate((index) -> {
            return index == -1 ? 0 : 
            switch(board.index_piece(index).color){
                case Piece.Color.Red -> 1;
                case Piece.Color.Black -> -1;
                default -> -50;
            };
        });

        //tatical 
        Point[] opp_pieces = board.grab_color(color.swapped());
        int opp_jumps = 0;
        for(Point pnt : opp_pieces){
            Move[] opp_legal = legal_moves(board, pnt);
            for( Move m : opp_legal ){
                opp_jumps+=m.get_jump_count();
            }
        }

        return material*3 - (opp_jumps > 1 ? 1.0+opp_jumps*1.3 : opp_jumps);
    }
}