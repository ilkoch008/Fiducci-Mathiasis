package org.example;

import java.util.HashSet;
import java.util.Set;

public class Node {
    private static final boolean LEFT = false;
    private static final boolean RIGHT = true;
    private boolean side;
    private boolean locked = false;
    private Integer key;
    public int gain;
    public int new_gain;
    public Set<HyperEdge> incident_edges;

    public Node(){
        incident_edges = new HashSet<>();
    }

    public boolean isNotLocked(){
        return !locked;
    }

    public void unlock(){
        locked = false;
    }

    public void lock(){
        locked = true;
    }

    public int get_key(){
        return this.key;
    }

    public boolean getSide() {
        return side;
    }

    public void setSide(boolean side) {
        this.side = side;
    }

    public void moveLeft() {
        this.side = LEFT;
    }

    public void moveRight() {
        this.side = RIGHT;
    }

    public void move() {
        this.side = !this.side;
    }

    public boolean changed_gain(){
        return !(new_gain == gain);
    }

    public static class Builder {

        private Node newNode;

        public Builder(){
            newNode = new Node();
        }

        public Builder setKey(Integer key) {
            newNode.key = key;
            return this;
        }

        public Builder setSide(boolean side) {
            newNode.side = side;
            return this;
        }

        public Node build(){
            return newNode;
        }
    }

}
