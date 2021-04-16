package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class GraphTester {
    public String output_file;
    public HyperGraph graph;
    public void run_containers_logic(){
        FileWriter output_writer;
        HyperGraph.create_file(output_file);
        long start, end;
        try {
            output_writer = new FileWriter(output_file);
            for(int i = 0; i <= 3; i++){
                switch (i){
                    case 0:
                        graph.take_from_end = false;
                        graph.add_to_beginning = false;
                        output_writer.write("default mode ====================================\n");
                        System.out.print("default mode ====================================\n");
                        break;
                    case 1:
                        graph.take_from_end = true;
                        graph.add_to_beginning = false;
                        output_writer.write("mode -tfe ====================================\n");
                        System.out.print("mode -tfe ====================================\n");
                        break;
                    case 2:
                        graph.take_from_end = false;
                        graph.add_to_beginning = true;
                        output_writer.write("mode -atb ====================================\n");
                        System.out.print("mode -atb ====================================\n");
                        break;
                    case 3:
                        graph.take_from_end = true;
                        graph.add_to_beginning = true;
                        output_writer.write("mode -tfe -atb ====================================\n");
                        System.out.print("mode -tfe -atb ====================================\n");
                        break;
                }

                test(output_writer);
            }
            output_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run_score_modes(){
        FileWriter output_writer;
        HyperGraph.create_file(output_file);
        long start, end;
        try {
            output_writer = new FileWriter(output_file);
            for(int i = 0; i <= 4; i++){
                graph.score_mode = i;
                output_writer.write("score mode " + i + " ====================================\n");
                System.out.print("score mode " + i + " ====================================\n");
                test(output_writer);
            }
            output_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void test(FileWriter output_writer) throws IOException {
        long start;
        long end;
        for (int j = 0; j < 100; j++){
            graph.random_partition();
            graph.get_partition_score();
            output_writer.write(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; ");
            System.out.print(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; ");
            start = System.currentTimeMillis();

            graph.FM();

            end = System.currentTimeMillis();
            output_writer.write(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; " + (end-start) + "\n");
            System.out.print(graph.num_of_cuts + "; " + graph.lefts + "; " + graph.rights + "; " + (end-start) + "\n");
        }
    }
}
