package Nodes;

import java.util.ArrayList;
import java.util.List;

public class Procedure implements Node {
    public String name;
    public List<Node> inputs = new ArrayList<>();
    public Node output;

    public List<Node> body = new ArrayList<>();
}
