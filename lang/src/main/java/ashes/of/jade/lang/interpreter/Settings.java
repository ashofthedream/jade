package ashes.of.jade.lang.interpreter;

import java.io.PrintStream;
import java.util.concurrent.ForkJoinPool;

public class Settings {

    private static final int DEFAULT_PARALLELISM_MIN_SIZE = 65536;

    private PrintStream out = System.out;

    private int mapParallelismSize = DEFAULT_PARALLELISM_MIN_SIZE;
    private int reduceParallelismSize = DEFAULT_PARALLELISM_MIN_SIZE;


    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public int getMapParallelismSize() {
        return mapParallelismSize;
    }

    public void setMapParallelismSize(int mapParallelismSize) {
        this.mapParallelismSize = mapParallelismSize;
    }

    public int getReduceParallelismSize() {
        return reduceParallelismSize;
    }

    public void setReduceParallelismSize(int reduceParallelismSize) {
        this.reduceParallelismSize = reduceParallelismSize;
    }
}
