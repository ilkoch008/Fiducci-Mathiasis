package org.example;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        assert false;
        HyperGraph graph = new HyperGraph();
        graph.readFrom("benchs/ISPD98_ibm01.hgr");

        graph.random_partition();
        graph.print_partition_info();
        graph.FM_with_gain_containers();
    }
}
