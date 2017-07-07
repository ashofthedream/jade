package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.nodes.Node;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;


public class ReduceRecursiveTask extends RecursiveTask<Node> {

    /**
     * Minimum sequence length below which algorithm won't partition the task
     */
    private final int minParallelSize;

    /**
     * Sequence
     */
    private final Node[] seq;

    /**
     * Left bound of current task
     */
    private final int left;

    /**
     * Left bound of current task
     */
    private final int right;

    /**
     * Reduce function
     */
    private final ReduceFunction f;

    public ReduceRecursiveTask(int minParallelSize, Node[] seq, int left, int right, ReduceFunction f) {
        this.minParallelSize = minParallelSize;
        this.seq = seq;
        this.left = left;
        this.right = right;
        this.f = f;
    }

    @Override
    protected Node compute() {
        int length = right - left;

        if (length <= minParallelSize)
            return reduce(seq, left, right, f);

        ReduceRecursiveTask l = new ReduceRecursiveTask(minParallelSize, seq, left, left + length / 2,  f);
        ReduceRecursiveTask r = new ReduceRecursiveTask(minParallelSize, seq, left + length / 2, right, f);

        ForkJoinTask<Node> fl = l.fork();
        ForkJoinTask<Node> fr = r.fork();
        return f.reduce(fl.join(), fr.join());
    }


    private Node reduce(Node[] seq, int left, int right, ReduceFunction f) {
        Node acc = seq[left];
        for (int i = left + 1; i < right; i++)
            acc = f.reduce(acc, seq[i]);

        return acc;
    }
}
