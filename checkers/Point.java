package checkers;

public class Point{
    public int rank, file;
    public Point(int r, char f){
        rank = r;
        file = (int)(f-'a');
        //safety();
    }
    public Point(int r, int f){
        rank = r;
        file = f;
        //safety();
    }
    public Point(String s){
        rank = s.charAt(1)-'0';
        file = s.charAt(0)-'a';
        //safety();
    }

    public String to_string(){
        //safety();
        return ((char)(file+(int)'a'))+""+((char)(rank+(int)'0'));
    }

    private void safety(){
        if(file<0||file>7||rank>7||rank<0){
            System.out.print(file);
            System.out.print(rank);
            System.out.println("");
            System.exit(-1);
        }
    }

    public void print(){
        System.out.print(to_string());
    }

    public Point copy(){
        return new Point(rank, file);
    }
}