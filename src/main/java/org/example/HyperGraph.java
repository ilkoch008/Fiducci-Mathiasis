package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class HyperGraph {
    private static final boolean LEFT = false;
    private static final boolean RIGHT = true;
    private Map<Integer, Node> nodes = null;
    private ArrayList<HyperEdge> edges = null;
    private int num_of_nodes = 0;
    private int num_of_hyperEdges = 0;
    private Map<Node, Boolean> best_partition_in_pass = null; // bool represents left or right side
    private float best_partition_in_pass_score;
    private Map<Node, Boolean> best_partition_in_steps = null; // with every step we make pass;
                                         // i.e. best_partition_in_steps is more global then best_partition_in_pass
    private float best_partition_in_steps_score;
    private Map<Integer, ArrayList<Node>> left_gain_container = null; // key is gain
    private Map<Integer, ArrayList<Node>> right_gain_container = null;
    private float balance_score; // = min(left, right)/max(left, right) < 1
    private int num_of_cuts;
    private float cut_cost; // = num of edges with nodes on both sides * 2 / num of edges
    private float partition_score; // = balance_score/cut_cost
    public int lefts;
    public int rights;

    private void init(){
        nodes = new HashMap<>();
        edges = new ArrayList<>();
    }

    public void readFrom(String str){
        File file = new File(str);
        Scanner sc = null;
        try {
            sc = new Scanner(file);
            sc.useDelimiter("\n");
            Scanner line_sc = new Scanner(sc.next());
            this.num_of_hyperEdges = line_sc.nextInt();
            this.num_of_nodes = line_sc.nextInt();
            this.init();
            for (int i = 0; i < this.num_of_nodes; i++) {
                this.getNodes().put(i + 1, new Node.Builder().setKey(i + 1).build());
            }

            while (sc.hasNext()){
                HyperEdge new_edge = new HyperEdge();
                new_edge.init();
                line_sc = new Scanner(sc.next());
                while (line_sc.hasNextInt()) {
                    new_edge.add_node(this.nodes.get(line_sc.nextInt()));
                }
                edges.add(new_edge);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public Map<Integer, Node> getNodes() {
        return nodes;
    }


    public ArrayList<HyperEdge> getEdges() {
        return edges;
    }

    public void unlockAllNodes(){
        this.nodes.forEach((k, v) -> v.unlock());
    }

    public void FM_with_gain_containers(){
        best_partition_in_steps_score = 0;
        best_partition_in_pass_score = 0;
        int iter = 0;
        long start, end;
        do {
            iter++;
            best_partition_in_steps_score = best_partition_in_pass_score;
            start = System.currentTimeMillis();
            this.save_step_partition();
            this.init_containers();
            this.FM_pass();
            this.unlockAllNodes();
            end = System.currentTimeMillis();
            System.out.println("=================== Pass  gone ===================");
            System.out.println("iter " + iter + " took " + (float)(end-start)/1000 + "s");
            System.out.println("balance_score: " + balance_score);
            System.out.println("num_of_cuts: " + num_of_cuts);
            System.out.println("cut_cost: " + cut_cost);
            System.out.println("partition_score: " + partition_score);
        } while (best_partition_in_pass_score > best_partition_in_steps_score);
        this.restore_partition(this.best_partition_in_steps);
        System.out.println("================== !!! DONE !!! ==================");
        System.out.println("balance_score: " + balance_score);
        System.out.println("num_of_cuts: " + num_of_cuts);
        System.out.println("cut_cost: " + cut_cost);
        System.out.println("partition_score: " + partition_score);
    }

    public void print_partition_info(){
        this.get_partition_score();
        System.out.println("================= Partition info =================");
        System.out.println("balance_score: " + balance_score);
        System.out.println("num_of_cuts: " + num_of_cuts);
        System.out.println("cut_cost: " + cut_cost);
        System.out.println("partition_score: " + partition_score);
    }

    private void FM_pass(){
        this.save_pass_partition();
        this.best_partition_in_pass_score = this.get_partition_score_light();
        while (!left_gain_container.isEmpty() && !right_gain_container.isEmpty()){
            Node best_move = get_node_for_best_move();
            this.num_of_cuts -= best_move.gain;
            this.move(best_move);
            best_move.lock();
            recompute_gains_for_incident(best_move);
//            for (Node n: incident_nodes) {
//                this.remove_node_from_containers(n);
//                this.containers_add(n);
//            }
            this.get_partition_score_light();
            if (this.partition_score > this.best_partition_in_pass_score){
                this.best_partition_in_pass_score = this.partition_score;
                this.save_pass_partition();
            }
        }
        this.restore_partition(this.best_partition_in_pass);
    }

    private void move(Node node){
        if (node.getSide() == LEFT){
            lefts--;
            rights++;
        } else {
            rights--;
            lefts++;
        }
        node.move();
    }

    private Node get_node_for_best_move(){
        int best_gain_from_left = Collections.max(left_gain_container.keySet());
        int best_gain_from_right = Collections.max(right_gain_container.keySet());
        Node res;
        if (best_gain_from_left > best_gain_from_right){
            res = get_node_from_container(best_gain_from_left, left_gain_container);
        } else if (best_gain_from_left < best_gain_from_right){
            res = get_node_from_container(best_gain_from_right, right_gain_container);
        } else {
            if (this.lefts > this.rights){
                res = get_node_from_container(best_gain_from_left, left_gain_container);
            } else {
                res = get_node_from_container(best_gain_from_right, right_gain_container);
            }
        }
        return res;
    }

    private Node get_node_from_container(Integer gain, Map<Integer, ArrayList<Node>> container){
        Node res = container.get(gain).get(0);
        container.get(gain).remove(res);
        if (container.get(gain).isEmpty()){
            container.remove(gain);
        }
        return res;
    }

    private void remove_node_from_containers(Node node) {
        if (node.isNotLocked()) {
            Map<Integer, ArrayList<Node>> container;
            container = (node.getSide() == LEFT) ? left_gain_container : right_gain_container;
            if (container == null) {
                System.out.println();
            }
            if (container.get(node.gain) == null) {
                System.out.println();
            }
            container.get(node.gain).remove(node);
            if (container.get(node.gain).isEmpty()) {
                container.remove(node.gain);
            }
        }
    }

    private void init_containers(){
        this.compute_gains();
        this.left_gain_container = new HashMap<>();
        this.right_gain_container = new HashMap<>();
        this.nodes.forEach((k, v) -> containers_add(v));
    }

    private void compute_gains(){
        this.nodes.forEach((k, v) -> compute_gain(v));
    }

    private void recompute_gains_for_incident(Node node){
        //Set<Node> incident_nodes = new HashSet<>();
        for (HyperEdge edge: this.edges) {
            if (edge.contains(node)){
                for (Node n: edge.getNodes()) {
                    if (!n.equals(node) && n.isNotLocked()) {
                        this.remove_node_from_containers(n);
                        compute_gain(n);
                        this.containers_add(n);
                        //incident_nodes.add(n);
                    }
                }
            }
        }
    }

    private void compute_gain(Node node){
        node.gain=0;
        for (HyperEdge edge: this.edges) {
            if (edge.contains(node)){
                if (edge.all_nodes_on_one_side_with(node)){
                    node.gain--;
                }
                if (edge.all_nodes_on_the_other_side_of(node)){
                    node.gain++;
                }
            }
        }
    }

    void containers_add(Node node){
        int gain = node.gain;
        if (node.getSide() == LEFT) {
            if (!left_gain_container.containsKey(gain)) {
                left_gain_container.put(gain, new ArrayList<>());
            }
            left_gain_container.get(gain).add(node);
        } else {
            if (!right_gain_container.containsKey(gain)) {
                right_gain_container.put(gain, new ArrayList<>());
            }
            right_gain_container.get(gain).add(node);
        }
    }

    public float get_partition_score_light(){
        this.balance_score = ((float) min(lefts, rights))/((float) max(lefts, rights));
        this.cut_cost = (float) this.num_of_cuts * 2 / (float) this.num_of_hyperEdges;
        this.partition_score = this.balance_score/this.cut_cost;
        return this.partition_score;
    }

    public float get_partition_score(){
        this.lefts=0;
        this.rights=0;
        for(int i=1; i <= this.num_of_nodes; i++){
            if (this.nodes.get(i).getSide() == LEFT){
                lefts++;
            } else {
                rights++;
            }
        }
        this.balance_score = ((float) min(lefts, rights))/((float) max(lefts, rights));
        this.num_of_cuts=0;
        for (HyperEdge edge: edges) {
            int i = 0;
            boolean first_side=true;
            for (Node node: edge.getNodes()) {
                if(i==0){
                    first_side = node.getSide();
                    i++;
                }
                if(node.getSide() != first_side){
                    this.num_of_cuts++;
                    break;
                }
            }
        }

        this.cut_cost = (float) num_of_cuts * 2 / (float) this.num_of_hyperEdges;
        this.partition_score = this.balance_score/this.cut_cost;
        return this.partition_score;
    }

    float getBalance_score(){
        return balance_score;
    }

    float getCut_cost(){
        return cut_cost;
    }

    public void random_partition(){
        Random r = new Random();
        nodes.forEach((k, v) -> v.setSide(r.nextBoolean()));
    }

    public void oneLeftOneRight_partition(){
        for (int i=1; i <= this.num_of_nodes; i++){
            nodes.get(i).setSide((i%2)==0);
        }
    }

    private void save_step_partition(){
        if (best_partition_in_steps == null){
            best_partition_in_steps = new HashMap<>();
        }
        nodes.forEach((k, v) -> best_partition_in_steps.put(v, v.getSide()));
        best_partition_in_steps_score = get_partition_score();
    }

    private void save_pass_partition(){
        if (best_partition_in_pass == null){
            best_partition_in_pass = new HashMap<>();
        }
        nodes.forEach((k, v) -> best_partition_in_pass.put(v, v.getSide()));
        best_partition_in_pass_score = get_partition_score();
    }

    private void restore_partition(Map<Node, Boolean> partition){
        partition.forEach(Node::setSide);
        this.get_partition_score();
    }

}
