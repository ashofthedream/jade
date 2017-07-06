package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.nodes.Node;

interface ReduceFunction {
    Node reduce(Node a, Node b);
}
