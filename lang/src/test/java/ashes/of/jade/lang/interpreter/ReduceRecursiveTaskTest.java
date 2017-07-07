package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.nodes.IntNode;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.SequenceNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static org.junit.Assert.*;

public class ReduceRecursiveTaskTest {

    private ForkJoinPool pool;
    private SequenceNode seq;

    @Before
    public void setUp() throws Exception {
        pool = new ForkJoinPool(4);
        seq = new SequenceNode(Location.EMPTY, 0, 10000);
    }

    @After
    public void tearDown() throws Exception {
        pool.shutdownNow();
    }

    @Test
    public void taskShouldReduceSequenceToOneNode() throws Exception {
        ReduceFunction f = (a, b) -> new IntNode(a.toInteger() + b.toInteger());

        ForkJoinTask<Node> reduced = pool
                .submit(new ReduceRecursiveTask(4, seq.seq, 0, seq.seq.length, f));


        Node join = f.reduce(new IntNode(13), reduced.join());

        assertEquals(50005013, join.toInteger());
    }

    @Test
    public void taskShouldParallelExecutionIfBoundsIsGreaterThanMinLen() throws Exception {
        Set<String> threads = new CopyOnWriteArraySet<>();

        ReduceFunction f = (a, b) -> {
            threads.add(Thread.currentThread().getName());
            return new IntNode(a.toInteger() + b.toInteger());
        };

        ForkJoinTask<Node> reduced = pool
                .submit(new ReduceRecursiveTask(10, seq.seq, 0, seq.seq.length, f));


        Node join = f.reduce(new IntNode(13), reduced.join());

        assertEquals(50005013, join.toInteger());
        // main, FJP-1-worker-XXX, ...
        assertTrue(threads.size() > 2);
    }



    @Test
    public void taskShouldNotParallelExecutionIfBoundsIsLessThanMinLen() throws Exception {
        Set<String> threads = new CopyOnWriteArraySet<>();

        ReduceFunction f = (a, b) -> {
            threads.add(Thread.currentThread().getName());
            return new IntNode(a.toInteger() + b.toInteger());
        };

        ForkJoinTask<Node> reduced = pool
                .submit(new ReduceRecursiveTask(100000, seq.seq, 0, seq.seq.length, f));


        Node join = f.reduce(new IntNode(13), reduced.join());

        assertEquals(50005013, join.toInteger());
        // main, FJP-1-worker-1
        assertEquals(2, threads.size());
    }
}