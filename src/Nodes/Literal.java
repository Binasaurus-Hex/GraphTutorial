package Nodes;

public class Literal<T> implements Node {
    public T value;
    public Literal(T value){this.value = value; }
}
