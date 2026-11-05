package checkers;

/**
 * _____MOVE_FORMAT_____
 * list of points with spaces
 *      Ex: " c2 b3"
 * when taking another piece, put an x, the coord of the taken piece, then the destination
 *      Ex: " c2xb3 a4"
 * multijump example
 *      Ex: " c2xb3 a4xb5 c6"
 */
public class Move{
    public Mpair[] pairs;
    String error_buffer = "";

    //struct that indicates if a coordindate is to be taken
    static public class Mpair{
        public Point p;
        public boolean taken = false;
        public Mpair(){}
        public Mpair(String s){
            if(s.charAt(0)=='x')taken = true;
            p = new Point(s.substring(1, 3));
        }
        public Mpair(Point _p){p = _p;}
        public String to_string(){
            return (taken ? 'x' : ' ')+p.to_string();
        }
    }

    public Move(String compressed){
        pairs = new Mpair[compressed.length()/3];
        for(int i = 0; i < compressed.length(); i+=3){
            pairs[i/3] = new Mpair(compressed.substring(i, i+3));
        }
    }
    public Move(Point[] pnts){ //DOES NOT ACCOUNT FOR TAKES
        pairs = new Mpair[pnts.length];
        for(int i = 0; i < pnts.length; i++){
            pairs[i] = new Mpair(pnts[i]);
        }
    }
    public Move(boolean b){} //FORCED NULL CONSTRUCTOR

    public String to_string(){
        String s = "";
        for(Mpair mp : pairs){
            s+=mp.to_string();
        }
        return s;
    }

    public Move copy(){
        return new Move(to_string());
    }

    public Move new_appended(Move m){
        String s = m.to_string();
        return new Move(to_string() + s.substring(3, s.length()));
    }

    public int get_jump_count(){
        int sum = 0;
        for(Mpair mp : pairs){
            if(mp.taken) sum++;
        }
        return sum;
    }

    public void print(){
        System.out.print(to_string());
    }

    public boolean is_legal(Board board){
        Board hypothetical_board = board.copy();

        for(int i = 0; i < pairs.length-1; i++){
            int piece_index = hypothetical_board.index_board(pairs[i].p.rank, pairs[i].p.file);
            if( piece_index < 0 ) return false;
            Piece piece = hypothetical_board.get_piece(pairs[i].p.rank, pairs[i].p.file);
            int destination = hypothetical_board.index_board(pairs[i+1].p.rank, pairs[i+1].p.file);

            //catch invalid actions
            if(destination == -2) {error_buffer+="out of bounds"; return false;}
            if(pairs[i+1].p.rank < piece.position.rank){
                if(piece.promoted && piece.color == Piece.Color.Black) 
                    {error_buffer+="promoted black moving backward"; return false;}
                else if(!piece.promoted && piece.color == Piece.Color.Red) 
                    {error_buffer+="red moving backward"; return false;}
            }
            else if(pairs[i+1].p.rank > piece.position.rank){
                if(piece.promoted && piece.color == Piece.Color.Red) 
                    {error_buffer+="promoted red moving backward"; return false;}
                else if(!piece.promoted && piece.color == Piece.Color.Black) 
                    {error_buffer+="black moving backward"; return false;}
            }
            if(Math.abs(pairs[i].p.rank-pairs[i+1].p.rank) != Math.abs(pairs[i].p.file-pairs[i+1].p.file)) 
                {error_buffer+="non-diagonal move"; return false;}

            if(pairs[i+1].taken){
                //checking if heading is consistent and distancing is consistent
                if(pairs[i+1].p.rank-pairs[i+2].p.rank != (pairs[i].p.rank-pairs[i+2].p.rank)/2 
                    || pairs[i+1].p.file-pairs[i+2].p.file != (pairs[i].p.file-pairs[i+2].p.file)/2) {error_buffer+="not valid jump"; return false;}
                //bounding jump to heading of 2
                if(Math.abs(pairs[i].p.rank-pairs[i+2].p.rank) != 2 || Math.abs(pairs[i].p.file-pairs[i+2].p.file) != 2) {error_buffer+="not valid jump"; return false;}
                //check if jumping over opposing piece
                if(destination == -1)
                    {error_buffer+="jumping over nothing"; return false;}
                else if(hypothetical_board.get_piece(pairs[i+1].p.rank, pairs[i+1].p.file).color == piece.color) 
                    {error_buffer+="jumping over ally"; return false;}
                //move piece on hypothetical board for multijump
                hypothetical_board.move(piece, pairs[i+2].p);
                hypothetical_board.take(pairs[i+1].p);
                i++; //skip over take point
            }
            else {
                //has to be single jump
                if(destination != -1) {error_buffer+="filled destination"; return false;}
                if(Math.abs(pairs[i].p.rank-pairs[i+1].p.rank) != 1 || Math.abs(pairs[i].p.file-pairs[i+1].p.file) != 1) {error_buffer+="non-single move"; return false;}
                if(i!=0) {error_buffer+="single move as not first move"; return false;}
            }
        }
        //is legal
        return true;
    }

    public boolean apply_to(Board board){
        Piece piece = board.get_piece(pairs[0].p.rank, pairs[0].p.file);
        for(int i = 1; i < pairs.length; i++){
            Mpair mp = pairs[i];
            if(mp.taken)
                { if(!board.take(mp.p)) return false; }
            else if(!board.move(piece, mp.p)) return false;
        }
        return true;
    }
}