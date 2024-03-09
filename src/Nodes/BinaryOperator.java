package Nodes;

public class BinaryOperator implements Node {
    public enum Operation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }

    public Operation operation;
    public Node left;
    public Node right;
}
