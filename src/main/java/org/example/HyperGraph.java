package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class HyperGraph {
    private static final boolean LEFT = false;
    private static final boolean RIGHT = true;
    public boolean add_to_beginning = false;
    public boolean take_from_end = false;
    public boolean silent_mode = false;
    public boolean equal_gain_choose_mode = false;
    public boolean single_gain_container = false;
    public int score_mode = 0;
    public float balance_weight = 1;
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
    private Map<Integer, ArrayList<Node>> gain_container = null;
    private float balance_score; // = min(left, right)/max(left, right) < 1
    public int num_of_cuts;
    private float cut_cost; // = num of edges with nodes on both sides * 2 / num of edges
    private float partition_score; // = balance_score/cut_cost
    public int lefts;
    public int rights;
    private HashSet<Node> nodes_recomputing;

    private void init(){
        nodes = new HashMap<>();
        nodes_recomputing = new HashSet<>();
        edges = new ArrayList<>();
    }

    public void loadPartitionFrom(String str){
        File file = new File(str);
        if (!file.exists()){
            System.err.println("Input file not found");
        }
        Scanner sc = null;
        try {
            sc = new Scanner(file);
            int side;
            for (int i = 1; i <= this.num_of_nodes; i++){
                side = sc.nextInt();
                if (side == 0){
                    this.nodes.get(i).setSide(LEFT);
                } else {
                    this.nodes.get(i).setSide(RIGHT);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readFrom(String str){
        long start, end;
        start = System.currentTimeMillis();
        File file = new File(str);
        if (!file.exists()){
            System.err.println("Input file not found");
        }
        if(!silent_mode) {
            System.out.println("Reading from file...\r");
        }
        try {
            Scanner sc = new Scanner(file);
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
        if(!silent_mode) {
            System.out.println("Initializing nodes...          \r");
        }
        this.nodes.entrySet().parallelStream().forEach((e) -> {
            Node node = e.getValue();
            for (HyperEdge edge: this.edges) {
                if (edge.contains(node)){
                    node.incident_edges.add(edge);
                    }
                }
            });
        end = System.currentTimeMillis();
        if(!silent_mode) {
            System.out.println("Reading done in " + (float) (end - start) / 1000 + "s                 ");
        }
    }

    public void wright_to(String str){
        create_file(str);
        FileWriter output_writer;
        try {
            output_writer = new FileWriter(str);
            for(int i = 1; i <= this.num_of_nodes; i++){
                if (this.nodes.get(i).getSide() == LEFT){
                    output_writer.write("0\n");
                } else {
                    output_writer.write("1\n");
                }
            }
            output_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static void create_file(String str) {
        File output = new File(str);
        try {
            if(output.createNewFile()){
                System.out.println("Output file created");
            } else {
                System.out.println("Overwriting existing file");
            }
        } catch (IOException e) {
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

    public void FM(){
        if (this.single_gain_container){
            FM_with_one_gain_container();
        } else {
            FM_with_two_gain_containers();
        }
    }

    private void FM_with_two_gain_containers(){
        best_partition_in_steps_score = Integer.MIN_VALUE;
        best_partition_in_pass_score = Integer.MIN_VALUE;
        int iter = 0;
        long start;
        do {
            iter++;
            best_partition_in_steps_score = best_partition_in_pass_score;
            start = System.currentTimeMillis();
            this.save_step_partition();
            this.init_containers();
            this.FM_pass_2c();
            print_afterPass(iter, start);
        } while (best_partition_in_pass_score > best_partition_in_steps_score);
        print_done();
    }

    private void print_done() {
        this.restore_partition(this.best_partition_in_steps);
        this.get_partition_score();
        if(!silent_mode) {
            System.out.println("================== !!! DONE !!! ==================");
            System.out.println("balance_score: " + balance_score);
            System.out.println("num_of_cuts: " + num_of_cuts);
            System.out.println("cut_cost: " + cut_cost);
            System.out.println("partition_score: " + partition_score);
            System.out.println("==================================================");
        }
    }

    private void print_afterPass(int iter, long start) {
        long end;
        this.unlockAllNodes();
        end = System.currentTimeMillis();
        if(!silent_mode) {
            System.out.println("=================== Pass  gone ===================");
            System.out.println("iter " + iter + " took " + (float) (end - start) / 1000 + "s");
            System.out.println("balance_score: " + balance_score);
            System.out.println("num_of_cuts: " + num_of_cuts);
            System.out.println("cut_cost: " + cut_cost);
            System.out.println("partition_score: " + partition_score);
        }
    }

    private void FM_with_one_gain_container(){
        best_partition_in_steps_score = Integer.MIN_VALUE;
        best_partition_in_pass_score = Integer.MIN_VALUE;
        int iter = 0;
        long start;
        do {
            iter++;
            best_partition_in_steps_score = best_partition_in_pass_score;
            start = System.currentTimeMillis();
            this.save_step_partition();
            this.init_container();
            this.FM_pass_1c();
            print_afterPass(iter, start);
        } while (best_partition_in_pass_score > best_partition_in_steps_score);
        print_done();
    }

    public void print_partition_info(){
        this.get_partition_score();
        System.out.println("================= Partition info =================");
        System.out.println("balance_score: " + balance_score);
        System.out.println("num_of_cuts: " + num_of_cuts);
        System.out.println("cut_cost: " + cut_cost);
        System.out.println("partition_score: " + partition_score);
    }

    private void FM_pass_2c(){
        this.save_pass_partition();
        this.best_partition_in_pass_score = this.get_partition_score_lite();
        ArrayList<Node> changes = new ArrayList<>();
        while (!left_gain_container.isEmpty() && !right_gain_container.isEmpty()){
            Node best_move = get_node_for_best_move_2c();
            this.num_of_cuts -= best_move.gain;
            this.move(best_move);
            best_move.lock();
            recompute_gains_for_incident_2c(best_move);
            changes.add(best_move);
            this.get_partition_score_lite();
            if (this.partition_score > this.best_partition_in_pass_score){
                this.best_partition_in_pass_score = this.partition_score;
                this.renew_pass_partition(changes);
            }
        }
        this.restore_partition(this.best_partition_in_pass);
    }

    private void FM_pass_1c(){
        this.save_pass_partition();
        this.best_partition_in_pass_score = this.get_partition_score_lite();
        ArrayList<Node> changes = new ArrayList<>();
        while (!gain_container.isEmpty()){
            Node best_move = get_node_for_best_move_1c();
            this.num_of_cuts -= best_move.gain;
            this.move(best_move);
            best_move.lock();
            recompute_gains_for_incident_1c(best_move);
            changes.add(best_move);
            this.get_partition_score_lite();
            if (this.partition_score > this.best_partition_in_pass_score){
                this.best_partition_in_pass_score = this.partition_score;
                this.renew_pass_partition(changes);
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

    private Node get_node_for_best_move_2c(){
        int best_gain_from_left = Collections.max(left_gain_container.keySet());
        int best_gain_from_right = Collections.max(right_gain_container.keySet());
        Node res;
        if (best_gain_from_left > best_gain_from_right){
            res = get_node_from_container(best_gain_from_left, left_gain_container);
        } else if (best_gain_from_left < best_gain_from_right){
            res = get_node_from_container(best_gain_from_right, right_gain_container);
        } else {
            if (equal_gain_choose_mode) {
                int left_k_g_size = this.left_gain_container.get(best_gain_from_left).size();
                int right_k_g_size = this.right_gain_container.get(best_gain_from_left).size();
                if (left_k_g_size > right_k_g_size) {
                    res = get_node_from_container(best_gain_from_left, left_gain_container);
                } else {
                    res = get_node_from_container(best_gain_from_right, right_gain_container);
                }
            } else {
                if (this.lefts > this.rights) {
                    res = get_node_from_container(best_gain_from_left, left_gain_container);
                } else {
                    res = get_node_from_container(best_gain_from_right, right_gain_container);
                }
            }
        }
        return res;
    }

    private Node get_node_for_best_move_1c(){
        int best_gain = Collections.max(gain_container.keySet());
        return get_node_from_container(best_gain, gain_container);
    }

    private Node get_node_from_container(Integer gain, Map<Integer, ArrayList<Node>> container){
        Node res;
        if (this.take_from_end){
            ArrayList<Node> list = container.get(gain);
            int i = list.size()-1;
            res = list.get(i);
        } else {
            res = container.get(gain).get(0);
        }
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
            ArrayList<Node> nodeList = container.get(node.gain);
            nodeList.remove(node);
            if (nodeList.isEmpty()) {
                container.remove(node.gain);
            }
        }
    }

    private void remove_node_from_container(Node node) {
        if (node.isNotLocked()) {
            ArrayList<Node> nodeList = this.gain_container.get(node.gain);
            nodeList.remove(node);
            if (nodeList.isEmpty()) {
                this.gain_container.remove(node.gain);
            }
        }
    }

    private void init_containers(){
        this.compute_gains();
        this.left_gain_container = new HashMap<>();
        this.right_gain_container = new HashMap<>();
        this.nodes.forEach((k, v) -> containers_add(v));
    }

    private void init_container(){
        this.compute_gains();
        this.gain_container = new HashMap<>();
        this.nodes.forEach((k, v) -> container_add(v));
    }

    private void compute_gains(){
        this.nodes.forEach((k, v) -> compute_gain(v));
    }

    private void recompute_gains_for_incident_2c(Node node){
        check_neighbours(node);
        for(Node n : this.nodes_recomputing){
            if(n.changed_gain()){
                this.remove_node_from_containers(n);
                n.gain = n.new_gain;
                this.containers_add(n);
            }
        }
        this.nodes_recomputing.clear();
    }

    private void recompute_gains_for_incident_1c(Node node){

        check_neighbours(node);
        for(Node n : this.nodes_recomputing){
            if(n.changed_gain()){
                this.remove_node_from_container(n);
                n.gain = n.new_gain;
                this.container_add(n);
            }
        }
        this.nodes_recomputing.clear();
    }

    private void check_neighbours(Node node) {
        for (HyperEdge e: node.incident_edges){
            if(e.all_nodes_on_one_side_with(node)){
                for (Node n : e.getNodes()){
                    if (n.isNotLocked()){
                        n.new_gain--;
                        this.nodes_recomputing.add(n);
                    }
                }
            }
            if (e.all_nodes_on_the_other_side_of(node)){
                for (Node n : e.getNodes()){
                    if (n.isNotLocked()){
                        n.new_gain++;
                        this.nodes_recomputing.add(n);
                    }
                }
            }
            if (e.one_node_on_other_side_of(node)){
                if (e.buffered_node.isNotLocked()) {
                    e.buffered_node.new_gain++;
                    this.nodes_recomputing.add(e.buffered_node);
                }
            }
            if (e.one_node_on_one_side_with(node)){
                if (e.buffered_node.isNotLocked()) {
                    e.buffered_node.new_gain--;
                    this.nodes_recomputing.add(e.buffered_node);
                }
            }
        }
    }

    private void compute_gain(Node node){
        node.gain=0;
        node.incident_edges.forEach((edge) -> {
                if (edge.all_nodes_on_one_side_with(node)){
                    node.gain--;
                }
                if (edge.all_nodes_on_the_other_side_of(node)){
                    node.gain++;
                }
                node.new_gain = node.gain;
            });
    }

    void container_add(Node node) {
        int gain = node.gain;

        if (!gain_container.containsKey(gain)) {
            gain_container.put(gain, new ArrayList<>());
        }
        if (this.add_to_beginning) {
            gain_container.get(gain).add(0, node);
        } else {
            gain_container.get(gain).add(node);
        }

    }

    void containers_add(Node node){
        int gain = node.gain;
        if (node.getSide() == LEFT) {
            if (!left_gain_container.containsKey(gain)) {
                left_gain_container.put(gain, new ArrayList<>());
            }
            if(this.add_to_beginning){
                left_gain_container.get(gain).add(0, node);
            } else {
                left_gain_container.get(gain).add(node);
            }
        } else {
            if (!right_gain_container.containsKey(gain)) {
                right_gain_container.put(gain, new ArrayList<>());
            }
            if(this.add_to_beginning){
                right_gain_container.get(gain).add(0, node);
            } else {
                right_gain_container.get(gain).add(node);
            }
        }
    }

    public float get_partition_score_lite(){
        switch (this.score_mode){
            case 0:
                this.balance_score = ((float) min(lefts, rights))/((float) max(lefts, rights));
                this.cut_cost = (float) this.num_of_cuts * 2 / (float) this.num_of_hyperEdges;
                this.partition_score = (float) Math.pow(this.balance_score, this.balance_weight)/this.cut_cost;
                break;
            case 1:
                this.balance_score = ((float) min(lefts, rights))/((float) max(lefts, rights));
                this.cut_cost = (float) this.num_of_cuts * 2 / (float) this.num_of_hyperEdges;
                this.partition_score = this.balance_score * this.balance_weight - this.cut_cost;
                break;
            case 2:
                this.balance_score = (float) lefts * (float) rights;
                this.cut_cost = (float) this.num_of_cuts;
                this.partition_score = this.balance_score * this.balance_weight - this.cut_cost;
                break;
            case 3: // without balance score
                this.balance_score = ((float) min(lefts, rights))/((float) max(lefts, rights));
                this.cut_cost = (float) this.num_of_cuts * 2 / (float) this.num_of_hyperEdges;
                this.partition_score = 1/this.cut_cost;
                break;
            case 4:
                this.balance_score = (float) lefts * (float) rights / ((float)this.num_of_nodes * (float)this.num_of_nodes/4);
                this.cut_cost = (float) this.num_of_cuts * 2 / (float) this.num_of_hyperEdges;
                this.partition_score = this.balance_score * this.balance_weight - this.cut_cost;
                break;
            default:
                System.err.println("Unrecognized balance score mode");
                System.exit(-1);
        }


        return this.partition_score;
    }

    private void left_right_counter(Node node){
        if (node.getSide() == LEFT){
            lefts++;
        } else {
            rights++;
        }
    }

    public float get_partition_score(){
        this.lefts=0;
        this.rights=0;

        this.nodes.forEach((k, v) -> this.left_right_counter(v));

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
        return this.get_partition_score_lite();
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

    public void all_to_the_left_partition(){
        nodes.forEach((k, v) -> v.moveLeft());
    }

    public void oneLeftOneRight_partition(){
        for (int i=1; i <= this.num_of_nodes; i++){
            nodes.get(i).setSide((i%2)==0);
        }
    }

    private void save_step_partition(){
        if (best_partition_in_steps == null){
            best_partition_in_steps = new ConcurrentHashMap<>();
        }
        nodes.forEach((k, v) -> best_partition_in_steps.put(v, v.getSide()));
        best_partition_in_steps_score = get_partition_score();
    }

    private void save_pass_partition(){
        if (best_partition_in_pass == null){
            best_partition_in_pass = new ConcurrentHashMap<>();
        }
        nodes.forEach((k, v) -> best_partition_in_pass.put(v, v.getSide()));
        best_partition_in_pass_score = get_partition_score_lite();
    }

    private void renew_pass_partition(ArrayList<Node> changes){
        if (best_partition_in_pass == null){
            System.err.println("best_partition_in_pass is not initialized!");
            System.exit(-1);
        }
        changes.forEach((node) -> best_partition_in_pass.put(node, node.getSide()));
        changes.clear();
        best_partition_in_pass_score = get_partition_score_lite();
    }

    private void restore_partition(Map<Node, Boolean> partition){
        partition.forEach(Node::setSide);
        this.get_partition_score();
    }

}
