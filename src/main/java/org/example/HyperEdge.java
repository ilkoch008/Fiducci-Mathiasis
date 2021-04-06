package org.example;

import java.util.ArrayList;

public class HyperEdge {
    private ArrayList<Node> nodes = null;
    public void init(){
        nodes = new ArrayList<>();
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    public void add_node(Node node){
        nodes.add(node);
    }

    public boolean contains(Node node){
        return nodes.contains(node);
    }

    public boolean all_nodes_on_one_side_with(Node node){
        boolean side = node.getSide();
        for (Node n: nodes) {
            if (n.getSide() != side){
                return false;
            }
        }
        return true;
    }

    public boolean all_nodes_on_the_other_side_of(Node node){ // ПРОВЕРИТЬ!!!
        boolean side = node.getSide();
        int key = node.get_key();
        for (Node n: nodes) {
            if (n.getSide() == side && n.get_key() != key){
                return false;
            }
        }
        return true;
    }
}
