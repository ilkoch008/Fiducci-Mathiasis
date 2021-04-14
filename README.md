# Fiducci-Mathiasis
Implementation of Fiducci-Mathiasis algorithm of hypergraph partitioning.

## Flags

```-i input_file_name``` - sets name of input file. You can check format of input file [here](https://github.com/ilkoch008/Fiducci-Mathiasis/blob/master/manual_hmetis.pdf).

```-o output_file_name``` - sets name of output file. You can check format of output file [here](https://github.com/ilkoch008/Fiducci-Mathiasis/blob/master/manual_hmetis.pdf).

```-atb``` - with this flag nodes are added at the beginnig of lists in containers. By default they are added at the end of list.

```-tfe``` - with this flag best node to move will come from the end of the list.

```-egcm``` - this flag regulates choosing of best move in case of equal best gains in left and in right gain-containers. By default, 
the node from the more numerous side is taken. With this flag, only the number of elements of the container with the best gain is counted:

```java
if (equal_gain_choose_mode) {
                int left_k_g_size = this.left_gain_container.get(best_gain).size();
                int right_k_g_size = this.right_gain_container.get(best_gain).size();
                if (left_k_g_size > right_k_g_size) {
                    res = get_node_from_container(best_gain_from_left, left_gain_container);
                } else {
                    res = get_node_from_container(best_gain_from_right, right_gain_container);
                }
            }
```

```-sgc``` - with this flag will be used only one gain container. ```-egcm``` flag will have no effect.

```-bw x``` - sets balance weight (x is float). By default x = 1.

```-b N``` - sets the mode of scoring the partition.
 
```N = 0```:
 
```
balance_score = min(lefts, rights) / max(lefts, rights))
cut_cost = num_of_cuts * 2 / num_of_hyperEdges
partition_score = balance_score^balance_weight / cut_cost
```

```N = 1```:
 
```
balance_score = min(lefts, rights) / max(lefts, rights))
cut_cost = num_of_cuts * 2 / num_of_hyperEdges
partition_score = balance_score*balance_weight - cut_cost
```

```N = 2```:
 
```
balance_score = lefts * rights
cut_cost = num_of_cuts
partition_score = balance_score*balance_weight - cut_cost
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
partition_score = balance_score*balance_weight - cut_cost
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
private Map<Integer, ArrayList<Node>> gain_container;
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

You can see all of measurements [here](https://github.com/ilkoch008/Fiducci-Mathiasis/blob/master/misc/results.xlsx) or in log files.

### 1. Different Types of Scoring

All computations was done with ```ISPD98_ibm05.hgr```. 100 measurements for each type of scoring.

In this table you can see average measured values.

|score mode|num of cuts before|min(lefts,rights) |	max(lefts,rights) |	num of cuts after |	min(lefts,rights) | max(lefts,rights) |	time(ms)|
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|0         |18900             |14606             |14741              |6136               |14673              |14674              |12518    |
|1         |18896             |14603             |14744              |6144               |14673              |14674              |12518    |
|2         |18908             |14599             |14748              |6164               |14671              |14675              |5619     |
|3         |18893             |14605             |14742              |6155               |14630              |14716              |11032    |
|4         |18911             |14607             |14740              |6067               |14671              |14675              |12070    |

We can see here that score mode 2 is faster then others. This is because this score has only one multiplication and one sum inside.
But mode 4 performs better so it will be used further.

It can also be noted that due to the peculiarities of the work of the gain containers, 
even in the case of a score that does not take into account the balance, everything is good with the balance.

### 2. Gain container logics

#### 2.1 Different types of adding/popping items in gain container

|flags     |num of cuts before|min(lefts,rights) |max(lefts,rights)  |num of cuts after  |	min(lefts,rights)|max(lefts,rights)  |time(ms) |
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|default   |18904             |14606             |14741              |6073               |14667              |14680              |13148    |
|-tfe      |18906             |14598             |14749              |6585               |14671              |14675              |18002    |
|-atb      |18908             |14602             |14745              |6640               |14672              |14675              |16458    |
|-tfe -atb |18899             |14604             |14743              |6130               |14671              |14675              |14501    |

We can see that all flags make partition quality worse. And also they take more time.

#### 2.2 ```-egcm``` flag

The same as previous one but with ```-egcm``` flag:

|flags     |num of cuts before|min(lefts,rights) |max(lefts,rights)  |num of cuts after  |	min(lefts,rights)|max(lefts,rights)  |time(ms) |
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|default   |18896             |14599             |14748              |5330               |13902              |15445              |6512     |
|-tfe      |18899             |14609             |14738              |4170               |12882              |16465              |8681     |
|-atb      |18907             |14604             |14743              |4135               |12991              |16355              |9893     |
|-tfe -atb |18904             |14610             |14737              |5413               |13185              |16162              |6702     |

Time of execution and number of cuts after execution have decreased greatly. In some particular cases
number of cuts approaches to 2000 (you can check this [here](https://github.com/ilkoch008/Fiducci-Mathiasis/blob/master/misc/results.xlsx)
or in log files). But there is no anything good in balance.

Let's try to fix it with score mode ```0```.

#### 2.3 ```-egcm``` flag with ```0``` score mode

|flags     |num of cuts before|min(lefts,rights) |max(lefts,rights)  |num of cuts after  |	min(lefts,rights)|max(lefts,rights)  |time(ms) |
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|default   |18907             |14599             |14748              |7404               |14547              |14800              |5637     |
|-tfe      |18905             |14611             |14736              |7213               |14578              |14769              |5772     |
|-atb      |18902             |14612             |14735              |7493               |14563              |14783              |4627     |
|-tfe -atb |18908             |14600             |14747              |7409               |14497              |14850              |5214     |

Balance looks good. But number of cuts has increased a lot. Let's try to increase balance weight in ```4``` score mode.

#### 2.3 ```-egcm``` flag with ```4``` score mode (balance weight x2)

|flags     |num of cuts before|min(lefts,rights) |max(lefts,rights)  |num of cuts after  |	min(lefts,rights)|max(lefts,rights)  |time(ms) |
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|default   |18905             |14608             |14739              |6006               |13710              |15637              |7311     |
|-tfe      |18901             |14604             |14743              |4937               |13798              |15549              |10907    |
|-atb      |18907             |14604             |14743              |5262               |13922              |15425              |10273    |
|-tfe -atb |18911             |14612             |14734              |6073               |13863              |15484              |7158     |

Partitioning became more balanced. But also number of cuts increased. So we need to decide what is more important for us:
partition balance or cut size.

### 3. Single gain container

We can put all of nodes in one container. It may cause some disbalance but let's check it.

#### 3.1 ```0``` score mode

|flags     |num of cuts before|min(lefts,rights) |max(lefts,rights)  |num of cuts after  |	min(lefts,rights)|max(lefts,rights)  |time(ms) |
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|default   |18913             |14607             |14737              |6478               |14645              |14702              |14954    |
|-tfe      |18915             |14600             |14747              |6584               |14667              |14769              |17731    |
|-atb      |18904             |14600             |14747              |6589               |14672              |14675              |17060    |
|-tfe -atb |18904             |14610             |14737              |6521               |14644              |14703              |16066    |

Execution began to take significantly longer. Also, you can notice an almost complete indifference to the flags of the gain container.

#### 3.2 ```4``` score mode

|flags     |num of cuts before|min(lefts,rights) |max(lefts,rights)  |num of cuts after  |	min(lefts,rights)|max(lefts,rights)  |time(ms) |
|----------|-----------------:|-----------------:|------------------:|------------------:|------------------:|------------------:|---------|
|default   |18901             |14608             |14739              |6077               |13274              |16073              |22235    |
|-tfe      |18906             |14610             |14737              |6230               |13662              |15685              |31421    |
|-atb      |18914             |14601             |14746              |6286               |13556              |15791              |28056    |
|-tfe -atb |18907             |14613             |14734              |5981               |13173              |16174              |22763    |

This one turned out to be the slowest one.

There are quite expected problems with the balance of the partition. 
However, these problems are slightly less widespread than in p.2.2, even though 2 gain containers were used there.

## Conclusion

The considered algorithm is extremely unstable. A lot depends on ~~weather, wind direction and your mood~~ the initial partitioning. 
There is also a direct correlation between the balance of the partition and the number of cuts: 
the stricter the conditions for balance, the more cuts in the final partition 
(perhaps there really is some relationship between them, this issue needs to be investigated separately). 
But on the other hand, it is always possible to relax the balance requirements and apply the algorithm on a random partition 
until the necessary requirements for the number of cuts and for the balance are achieved. 
Well, or you can come up with some heuristics to get the initial partition, 
on which the Fiducci-Mathiasis algorithm will certainly show good results.
