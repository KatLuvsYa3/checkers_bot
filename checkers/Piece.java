package checkers;

public class Piece{
    public int id;
    public Point position;
    public Color color;
    public boolean promoted = false;
    public boolean taken = false;

    public static enum Color {
        Black, Red, ERROR; //out of bounds

        public Color swapped(){
            return switch(this){
                case Black -> Red;
                case Red -> Black;
                default -> this;
            };
        }
    };

    public Piece(Color c, Point p, int i){
        position = p;
        color = c;
        id = i;
    }

    public Piece(int i, Point p, Color c, boolean pr, boolean _taken){
        id = i; position = p.copy(); color = c; promoted = pr; taken = _taken;
    }

    public Piece copy(){
        return new Piece(id, position, color, promoted, taken);
    }

    public void print(){
        System.out.println((taken? "TAKEN " : "")+id+" "+color+" "+promoted+"@"+position.to_string());
    }
}