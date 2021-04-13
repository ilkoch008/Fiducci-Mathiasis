package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        HyperGraph graph = new HyperGraph();
        String input_file = null;
        String output_file = null;
        for(int i = 0; i < args.length; i++){
            switch (args[i]){
                case "-i":
                    i++;
                    input_file = args[i];
                    output_file = input_file + ".part.2";
                    break;
                case "-o":
                    i++;
                    output_file = args[i] + ".part.2";
                    break;
                case "-atb":
                    graph.add_to_beginning = true;
                    break;
                case "-tfe":
                    graph.take_from_end = true;
                    break;
                case "-b":
                    i++;
                    graph.score_mode = Integer.parseInt(args[i]);
                    if(graph.score_mode < 0 || graph.score_mode > 4){
                        System.err.println("Unrecognized score mode");
                        System.exit(-1);
                    }
                    break;
                case "-s":
                    graph.silent_mode = true;
                    break;
                case "-egcm":
                    graph.equal_gain_choose_mode = true;
                    break;
                case "-sgc":
                    graph.single_gain_container = true;
                    break;
                default:
                    System.err.println("Unrecognized flag " + args[i]);
            }
        }
        if (input_file == null){
            System.err.println("input file is not given");
            System.exit(-1);
        }

        graph.readFrom(input_file);

//        GraphTester tester = new GraphTester();
//        tester.graph = graph;
//        tester.test_1c = true;
//        tester.output_file = "score_modes_1c_log.txt";
//        tester.run_score_modes();
//        tester.output_file = "cont_logic_1c_log.txt";
//        tester.run_containers_logic();
        graph.random_partition();
        graph.print_partition_info();
        graph.FM();
        graph.wright_to(output_file);
        graph.loadPartitionFrom(output_file);
        graph.print_partition_info();
    }
}
