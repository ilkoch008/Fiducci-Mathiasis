package org.example;

import java.util.ArrayList;
import java.util.Collection;

public class HyperEdge {
    private ArrayList<Node> nodes = null;
    public Node buffered_node;
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

    public void add_nodes(Collection<Node> nodes){
        this.nodes.addAll(nodes);
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

    public boolean all_nodes_on_the_other_side_of(Node node){
        boolean side = node.getSide();
        for (Node n: nodes) {
            if (n.getSide() == side && !n.equals(node)){
                return false;
            }
        }
        return true;
    }

    public boolean one_node_on_other_side_of(Node node) {
        boolean other_side = !node.getSide();
        int others = 0;
        for (Node n : this.nodes){
            if (n.getSide() == other_side){
                this.buffered_node = n;
                others++;
            }
            if (others == 2){
                return false;
            }
        }
        return others == 1;
    }

    public boolean one_node_on_one_side_with(Node node) {
        boolean side = node.getSide();
        int neighbours = 0;
        for (Node n : this.nodes){
            if (n.getSide() == side){
                neighbours++;
                if (!n.equals(node)){
                    this.buffered_node = n;
                }
            }
            if (neighbours == 3){
                return false;
            }
        }
        return neighbours == 2;
    }
}
