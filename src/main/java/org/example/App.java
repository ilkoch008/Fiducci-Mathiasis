package org.example;

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
                    if(graph.score_mode < 0 || graph.score_mode > 5){
                        System.err.println("Unrecognized balance score mode");
                        System.exit(-1);
                    }
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
        graph.all_to_the_left_partition();
        graph.print_partition_info();
        graph.FM_with_gain_containers();
        graph.wright_to(output_file);
    }
}
