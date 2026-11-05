package bot;

import java.util.ArrayList;
import checkers.*;

public class BSTree{
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
            System.out.print(tabs + id + " ");
            bs.print();
            if(children.size()!=0)System.out.print("\tchildren:\n");
            for( int c : children ){
                tree.index(c).print(tabs+"\t");
                System.out.print("\n");
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