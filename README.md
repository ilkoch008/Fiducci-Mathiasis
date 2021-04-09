# Fiducci-Mathiasis
Implementation of Fiducci-Mathiasis algorithm of hypergraph partitioning.

## Flags

```-i input_file_name``` - sets name of input file. You can check format of input file [here](https://github.com/ilkoch008/Fiducci-Mathiasis/blob/master/manual_hmetis.pdf).

```-o output_file_name``` - sets name of output file. You can check format of output file [here](https://github.com/ilkoch008/Fiducci-Mathiasis/blob/master/manual_hmetis.pdf).

```-atb``` - with this flag nodes are added at the beginnig of lists in containers. By default they are added at the end of list.

```-tfe``` - with this flag best node to move will come from the end of the list.

```-b N``` - sets the mode of scoring the partition.
 
```N = 0```:
 
```
balance_score = min(lefts, rights) / max(lefts, rights))
cut_cost = num_of_cuts * 2 / num_of_hyperEdges
partition_score = balance_score / cut_cost
```

```N = 1```:
 
```
balance_score = min(lefts, rights) / max(lefts, rights))
cut_cost = num_of_cuts * 2 / num_of_hyperEdges
partition_score = balance_score - cut_cost
```

```N = 2```:
 
```
balance_score = lefts * rights
cut_cost = num_of_cuts
partition_score = balance_score - cut_cost
```

```N = 3```: this mode does not take into account the balancing of the partitioning.
 
```
balance_score = min(lefts, rights) / max(lefts, rights))
cut_cost = num_of_cuts * 2 / num_of_hyperEdges
partition_score = 1 / cut_cost
```

```N = 4```:
 
```
balance_score = lefts * rights / (num_of_nodes * num_of_nodes)
cut_cost = num_of_cuts * 2 / num_of_hyperEdges
partition_score = balance_score - cut_cost
```
 
```-s``` - turns on the silent mode.


## General Algorithm Description 
The task is to split the hypergraph (there can be more than two incident vertices on each edge) into to subgraphs. 
Moreover, the two resulting subgraphs should have as few common edges as possible (hereinafter we will call number of common edges the number of cuts).

Two main methods:
1. ```public void FM_with_gain_containers()```
``` java
public void FM_with_gain_containers(){
        best_partition_in_steps_score = Integer.MIN_VALUE;
        best_partition_in_pass_score = Integer.MIN_VALUE;
        int iter = 0;
        long start, end;
        do {
            iter++;
            best_partition_in_steps_score = best_partition_in_pass_score;
            start = System.currentTimeMillis();
            this.save_step_partition(); // this can be removed since FM_pass() won't make it worse
            this.init_containers();
            this.FM_pass();
            this.unlockAllNodes();
            end = System.currentTimeMillis();
        } while (best_partition_in_pass_score > best_partition_in_steps_score);
        this.restore_partition(this.best_partition_in_steps);
        this.get_partition_score();
    }
```

Container structure:
```java
private Map<Integer, ArrayList<Node>> left_gain_container;
```
Where key value is the change in number of cuts after changing side of node (nodes are stored in ArrayList).

2. ```private void FM_pass()``` - the heart of this algorithm
```java
private void FM_pass(){
        this.save_pass_partition();
        this.best_partition_in_pass_score = this.get_partition_score_lite();
        ArrayList<Node> changes = new ArrayList<>(); // we will keep changes here till we improve the quality of partition
                                                     // after that we will save them
        while (!left_gain_container.isEmpty() && !right_gain_container.isEmpty()){
            Node best_move = get_node_for_best_move(); // here we take node with the biggest gain
            this.num_of_cuts -= best_move.gain;
            this.move(best_move);
            best_move.lock(); // locking the choosed node so we won't move it till the end of this pass
            recompute_gains_for_incident(best_move); // some of incident nodes could change their gains so we need to recompute them
            changes.add(best_move); // storing the last move in list
            this.get_partition_score_lite();
            if (this.partition_score > this.best_partition_in_pass_score){
                this.best_partition_in_pass_score = this.partition_score;
                this.renew_pass_partition(changes); // saving the changes that were made to the partitioning and clearing changes list
            }
        }
        this.restore_partition(this.best_partition_in_pass); // restoring the best partition that was achieved in this pass
    }
```

A few things that was made in order to make this thing move faster:
1. Each node contains information about incident nodes and edges. This reduced the execution time by more than five times.
2. Previous item lead to need in initializing of nodes in beginning. This task is performed in parallel.
3. Intermediate states of the hypergraph are not completely saved. The changes are saved in a separate list and, if necessary, they are transferred to the save later.

Here you can see final flame graph:

![](https://raw.githubusercontent.com/ilkoch008/Fiducci-Mathiasis/master/misc/flamegraph.png)

## Experiments
