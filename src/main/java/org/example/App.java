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
                default:
                    System.err.println("Unrecognized flag " + args[i]);
            }
        }
        if (input_file == null){
            System.err.println("input file is not given");
            System.exit(-1);
        }
        graph.readFrom(input_file);

//        File output = new File("balance_modes_log1.txt");
//        try {
//            if(output.createNewFile()){
//                System.out.println("Output file created");
//            } else {
//                System.out.println("Overwriting existing file");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        FileWriter output_writer;
//        long start, end;
//        try {
//            output_writer = new FileWriter("balance_modes_log1.txt");
//            for(int i = 2; i <= 4; i++){
//                graph.score_mode = i;
//                output_writer.write("score mode " + i + " ====================================\n");
//                System.out.print("score mode " + i + " ====================================\n");
//                for (int j = 0; j < 100; j++){
//                    graph.random_partition();
//                    graph.get_partition_score();
//                    output_writer.write(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; ");
//                    System.out.print(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; ");
//                    start = System.currentTimeMillis();
//                    graph.FM_with_gain_containers();
//                    end = System.currentTimeMillis();
//                    output_writer.write(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; " + (end-start) + "\n");
//                    System.out.print(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; " + (end-start) + "\n");
//                }
//            }
//            output_writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        graph.random_partition();
        graph.print_partition_info();
        graph.FM_with_gain_containers();
        graph.wright_to(output_file);
    }
}
