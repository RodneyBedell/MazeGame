import java.awt.Color;
import java.util.*;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;


//method to compare two edges by weight
class EdgeSorter implements Comparator<Edge>
{
    public int compare(Edge e1, Edge e2)
    {
        return e1.weight - e2.weight;
    }
}

//represents a Node in a maze/graph (the cells that a player can walk on)
class Node
{
    Posn p;
    Node parent; // refers to the parent Node
    Node up; // refers to the node in each direction
    Node right;
    Node down;
    Node left;
    int checked; // how many times this node has already been checked by a search algorithm

    // Constructs a Node
    Node(Posn p)
    {
        this.p = p;
        // initializes the Node with all connections pointing to itself
        this.parent = this;
        this.up = this;
        this.right = this;
        this.down = this;
        this.left = this;
        this.checked = 0;
    }
}

//represent an edge (a connection between two nodes in the maze)
class Edge
{
    Node a; // the top/left node
    Node b; // the bottom/right node
    int weight;
    boolean isWall;

    Edge(Node a, Node b, int weight)
    {
        this.a = a;
        this.b = b;
        this.weight = weight;
    }
    public boolean directionIsVertical() // if node A is vertically above node B
    {
        return this.a.p.x == this.b.p.x;
    }
    public WorldImage drawPath()
    {
        Posn nodeA = new Posn(this.a.p.x * Maze.CELL_SIZE + Maze.CELL_SIZE / 2,
                                this.a.p.y * Maze.CELL_SIZE + Maze.CELL_SIZE / 2);
        Posn nodeB = new Posn(this.b.p.x * Maze.CELL_SIZE + Maze.CELL_SIZE / 2,
                                this.b.p.y * Maze.CELL_SIZE + Maze.CELL_SIZE / 2);
        return new LineImage(nodeA, nodeB, Maze.PATH_COLOR);
    }
    public WorldImage drawWallDown() // draw a wall below node A
    {
        Posn left = new Posn(this.a.p.x * Maze.CELL_SIZE,
                             this.a.p.y * Maze.CELL_SIZE + Maze.CELL_SIZE);
        Posn right = new Posn(this.a.p.x * Maze.CELL_SIZE + Maze.CELL_SIZE,
                             this.a.p.y * Maze.CELL_SIZE + Maze.CELL_SIZE);
        return new LineImage(left, right, Maze.WALL_COLOR);
    }
    public WorldImage drawWallRight() // draw a wall to the right of node A
    {
        Posn top = new Posn(this.a.p.x * Maze.CELL_SIZE + Maze.CELL_SIZE,
                             this.a.p.y * Maze.CELL_SIZE);
        Posn bottom = new Posn(this.a.p.x * Maze.CELL_SIZE + Maze.CELL_SIZE,
                                this.a.p.y * Maze.CELL_SIZE + Maze.CELL_SIZE);
        return new LineImage(top, bottom, Maze.WALL_COLOR);
    }


}
class Maze extends World
{
    static final int CELL_SIZE = 20; //the size of a cell in pixels 
    static final int MAZE_HEIGHT = 30; //the height of the maze in walkable cells 
    static final int MAZE_WIDTH = 50; //the width of the maze in walkable cells 
    static final Color BACKGROUND_COLOR = Color.WHITE;
    static final Color WALL_COLOR = Color.BLACK;
    static final Color PATH_COLOR = Color.RED;
    HashMap<Node, Edge> visited = new HashMap<Node, Edge>();
    ArrayList<Node> searched = new ArrayList<Node>();
    boolean finished = false;
    boolean showWalls;
    boolean showPath;
    boolean depth;
    boolean breadth;


    ArrayList<Edge> grid; // the grid of all possible connections between any two nodes
    ArrayList<Edge> cuts; // all the edges that get cut out 
    ArrayList<Edge> leftovers; // the edges leftover after cuts
    ArrayList<ArrayList<Node>> nodes; // all the nodes

    Maze()
    {
        this.showWalls = true;
        this.showPath = false;
        this.depth = false;
        this.breadth = false;
        this.initializeNodes();
        this.initializeGrid();
        this.initializeMaze();
    }

    Maze(String dummy) // dummy constructor for testing
    {
        this.nodes = new ArrayList<ArrayList<Node>>();
        this.grid = new ArrayList<Edge>();
        this.leftovers = new ArrayList<Edge>();
        this.cuts = new ArrayList<Edge>();
        this.visited = new HashMap<Node, Edge>();
        this.searched = new ArrayList<Node>();
    }

    public void initializeNodes()
    {
        nodes = new ArrayList<ArrayList<Node>>();
        for (int h = 0; h < MAZE_HEIGHT; h++)
        {
            nodes.add(new ArrayList<Node>());
            for (int w = 0; w < MAZE_WIDTH; w++)
            {
                Node newNode = new Node(new Posn(w, h));
                if ( h == 0 && w == 0)
                {
                    newNode.checked = 2;
                }
                nodes.get(h).add(newNode);
            }
        }
        this.setVertical();
        this.setHorizontal();
        searched.add(nodes.get(0).get(0));
    }
    public void setVertical()
    {
        for (int h = 1; h < MAZE_HEIGHT; h++)
        {
            for (int w = 0; w < MAZE_WIDTH; w++)
            {
                nodes.get(h).get(w).up = nodes.get(h - 1).get(w);
                nodes.get(h - 1).get(w).down = nodes.get(h).get(w);
            }
        }
    }
    public void setHorizontal()
    {
        for (int h = 0; h < MAZE_HEIGHT; h++)
        {
            for (int w = 1; w < MAZE_WIDTH; w++)
            {
                nodes.get(h).get(w).left = nodes.get(h).get(w - 1);
                nodes.get(h).get(w - 1).right = nodes.get(h).get(w);
            }
        }
    }

    // populates the empty grid with all the possible connections between nodes
    public void initializeGrid() 
    {
        grid = new ArrayList<Edge>();
        Random rand = new Random();
        //double for loop, initialize all the edges between nodes except for the top and left lines
        for (int h = 1; h < MAZE_HEIGHT; h++) 
        {
            for (int w = 1; w < MAZE_WIDTH; w++) 
            {
                Node temp = nodes.get(h).get(w);
                grid.add(new Edge(temp.up, temp, rand.nextInt(100)));
                grid.add(new Edge(temp.left, temp, rand.nextInt(100)));
            }
        }
        // initialize the first line of edges on the left
        for (int h = 1; h < MAZE_HEIGHT; h++) 
        {
            Node temp = nodes.get(h).get(0);
            grid.add(new Edge(temp.up, temp, rand.nextInt(100)));
        }
        // initialize the first line of edges on the top
        for (int w = 1; w < MAZE_WIDTH; w++)
        {
            Node temp = nodes.get(0).get(w);
            grid.add(new Edge(temp.left, temp, rand.nextInt(100)));
        }
    }

    //Create maze using Kruskal's algorithm to cut edges out of the maze
    public void initializeMaze()
    {
        Comparator<Edge> edgeSort = new EdgeSorter();
        Collections.sort(grid, edgeSort);
        UnionFind uf = new UnionFind(nodes);
        cuts = new ArrayList<Edge>(); // the Spanning tree - the path through the maze
        leftovers = new ArrayList<Edge>(grid); // the walls still in the maze
        while (grid.size() > 0)
        {
            Edge temp = grid.get(0); // the random Edge
            if (!(uf.find(temp.a).equals(uf.find(temp.b))))
            // if the parent of the two Nodes are not equal
            {
                cuts.add(temp); // add the Edge to the Spanning Tree
                leftovers.remove(temp); // the Edge is connected, so remove the wall
                // sets the Parent of A to the Parent of B
                uf.union(temp.a, temp.b);
            }
            grid.remove(0);
        }
        for (Edge e: leftovers)
        {
            e.isWall = true;
            if (e.directionIsVertical())
            {
                e.a.down = e.a;
                e.b.up = e.b;
            }
            else
            {
                e.a.right = e.a;
                e.b.left = e.b;
            }
        }
        for (Edge e: cuts)
        {
            e.isWall = false;
            if (e.directionIsVertical())
            {
                e.a.down = e.b;
                e.b.up = e.a;
            }
            else
            {
                e.a.right = e.b;
                e.b.left = e.a;
            }
        }
    }

    public void depthFirst()
    {
        Node current = this.searched.get(0);
        this.searched.remove(0);
        current.checked = 1;

        if (current.up != current)
        {
            this.searched.add(0, current.up);
            Node upNode = current.up;
            upNode.down = upNode;
            visited.put(upNode, new Edge(current, upNode, 1));
        }
        if (current.down != current)
        {
            this.searched.add(0, current.down);
            Node downNode = current.down;
            downNode.up = downNode;
            visited.put(downNode, new Edge(current, downNode, 1));
        }
        if (current.left != current)
        {
            this.searched.add(0, current.left);
            Node leftNode = current.left;
            leftNode.right = leftNode;
            visited.put(leftNode, new Edge(current, leftNode, 1));
        }
        if (current.right != current)
        {
            this.searched.add(0, current.right);
            Node rightNode = current.right;
            rightNode.left = rightNode;
            visited.put(rightNode, new Edge(current, rightNode, 1));
        }

    }
    public void breadthFirst()
    {
        Node current = this.searched.get(0);
        this.searched.remove(0);
        current.checked = 1;

        if (current.up != current)
        {
            this.searched.add(current.up);
            Node upNode = current.up;
            upNode.down = upNode;
            visited.put(upNode, new Edge(current, upNode, 1));
        }
        if (current.down != current)
        {
            this.searched.add(current.down);
            Node downNode = current.down;
            downNode.up = downNode;
            visited.put(downNode, new Edge(current, downNode, 1));
        }
        if (current.left != current)
        {
            this.searched.add(current.left);
            Node leftNode = current.left;
            leftNode.right = leftNode;
            visited.put(leftNode, new Edge(current, leftNode, 1));
        }
        if (current.right != current)
        {
            this.searched.add(current.right);
            Node rightNode = current.right;
            rightNode.left = rightNode;
            visited.put(rightNode, new Edge(current, rightNode, 1));
        }

    }
    public void backTrack() // once you hit the end of the maze, show the path
    {
        Edge curEdge = visited.get(nodes.get(MAZE_HEIGHT - 1).get(MAZE_WIDTH - 1));
        curEdge.b.checked = 2;
        Node curNode = curEdge.a;

        while (curNode != nodes.get(0).get(0))
        {
            curNode.checked = 2;
            curEdge = this.visited.get(curNode);
            curNode = curEdge.a;
        }
        curNode.checked = 2;
    }
    public void manual(String key)
    {
        Node current = this.searched.get(0);
        current.checked = 0;
        this.searched.remove(0);
        if (key.equals("right"))
        {
            Node nRight = current.right;
            current.checked = 1;
            current = nRight;
            nRight.checked = 2;
            this.searched.add(nRight);
        }
        if (key.equals("left"))
        {
            Node nLeft = current.left;
            current.checked = 1;
            current = nLeft;
            nLeft.checked = 2;
            this.searched.add(nLeft);
        }
        if (key.equals("up"))
        {
            Node nUp = current.up;
            current.checked = 1;
            current = nUp;
            nUp.checked = 2;
            this.searched.add(nUp);
        }
        if (key.equals("down"))
        {
            Node nDown = current.down;
            current.checked = 1;
            current = nDown;
            nDown.checked = 2;
            this.searched.add(nDown);
        }
    }

    public WorldImage makeImage()
    {
        //canvas
        WorldImage maze = new OverlayImages(new RectangleImage(new Posn(0, 0),
                                                               MAZE_WIDTH * CELL_SIZE,
                                                               MAZE_HEIGHT * CELL_SIZE,
                                                               BACKGROUND_COLOR),
                                            new FrameImage(new Posn(0, 0),
                                                           MAZE_WIDTH * CELL_SIZE * 2,
                                                           MAZE_HEIGHT * CELL_SIZE * 2,
                                                           WALL_COLOR));
        for (ArrayList<Node> a: nodes)
        {
            for (Node n: a)
            {
                if (n.checked == 1)
                {
                    Posn cell = new Posn(n.p.x * CELL_SIZE + CELL_SIZE / 2,
                                            n.p.y * CELL_SIZE + CELL_SIZE / 2);
                    maze = maze.overlayImages(
                                new RectangleImage(cell, CELL_SIZE, CELL_SIZE, Color.CYAN));
                }
                if (n.checked == 2)
                {
                    Posn cell = new Posn(n.p.x * CELL_SIZE + CELL_SIZE / 2,
                                            n.p.y * CELL_SIZE + CELL_SIZE / 2);
                    maze = maze.overlayImages(
                                new RectangleImage(cell, CELL_SIZE, CELL_SIZE, Color.BLUE));
                }
            }
        }
        for (Edge e: leftovers)
        {
            if (this.showWalls)
            {
                if (e.directionIsVertical())
                {
                    maze = maze.overlayImages(e.drawWallDown());
                }
                else
                {
                    maze = maze.overlayImages(e.drawWallRight());
                }
            }
        }
        for (Edge e: cuts)
        {
            if (this.showPath)
            {
                maze = maze.overlayImages(e.drawPath());
            }
        }
        return maze;
    }

    public void onKeyEvent(String ke)
    {
        if (ke.equals("left") || ke.equals("up") || ke.equals("down") || ke.equals("right"))
        {
            this.manual(ke);
        }
        if (ke.equals("w"))
        {
            this.showWalls = !this.showWalls;
        }
        if (ke.equals("q"))
        {
            this.showPath = !this.showPath;
        }
        if (ke.equals("m")) // restart with manual control
        {
            this.initializeNodes();
            this.initializeGrid();
            this.initializeMaze();
            this.breadth = false;
            this.depth = false;
        }
        if (ke.equals("b")) // restart with a breadth first search
        {
            this.initializeNodes();
            this.initializeGrid();
            this.initializeMaze();
            this.breadth = true;
            this.depth = false;
        }
        if (ke.equals("d")) // restart with a breadth first search
        {
            this.initializeNodes();
            this.initializeGrid();
            this.initializeMaze();
            this.depth = true;
            this.breadth = false;
        }

    }
    public void onTick()
    {
        if (visited.containsKey(nodes.get(MAZE_HEIGHT - 1).get(MAZE_WIDTH - 1)))
        {
            this.finished = true;
            this.backTrack();
            this.breadth = false;
            this.depth = false;
        }
        if (this.breadth)
        {
            this.breadthFirst();
        }
        if (this.depth)
        {
            this.depthFirst();
        }
    }
    public WorldEnd worldEnd()
    {
        if (this.finished)
        {
            this.backTrack();
            return new WorldEnd(true, new OverlayImages(this.makeImage(),
                    (new TextImage(new Posn(100, 100),
                            "You Solved the Maze!", 36,
                            Color.RED))));
        }
        else
        {
            return new WorldEnd(false, this.makeImage());
        }
    }

}
//the union/find data structure

class UnionFind
{
    HashMap<Node, Node> parents;
    UnionFind(ArrayList<ArrayList<Node>> nodes) // takes in a 2D ArrayList of nodes
    {
        this.parents = new HashMap<Node, Node>();
        // double loop through the 2D list to get to all the nodes
        for (ArrayList<Node> a: nodes)
        {
            for (Node n: a)
            {
                this.parents.put(n, n); // map each node to itself
            }
        }
    }
    public Node find(Node node)
    {
        if (parents.get(node).equals(node)) // if the node is mapped to itself
        {
            return node;
        }
        else // if the node is mapped to a different node
        {
            // continue along the chain of nodes until we find the head of the tree
            return this.find(parents.get(node));
        }
    }
    public void union(Node start, Node end)
    {
        parents.put(this.find(start), this.find(end));
    }
}
class ExamplesMazeWorld
{
    Node n1 = new Node(new Posn(0, 0));
    Node n2 = new Node(new Posn(0, 1));
    Node n3 = new Node(new Posn(1, 0));
    Node n4 = new Node(new Posn(1, 1));

    Edge e1 = new Edge(n1, n2, 1);
    Edge e2 = new Edge(n1, n3, 1);
    Edge e3 = new Edge(n2, n4, 2);

    ArrayList<ArrayList<Node>> quad = new ArrayList<ArrayList<Node>>();

    Maze testMaze = new Maze();

    void testEdgeSorter(Tester t)
    {
        EdgeSorter test = new EdgeSorter();
        t.checkExpect(test.compare(e1, e2), 0);
        t.checkExpect(test.compare(e1, e3), -1);
        t.checkExpect(test.compare(e3, e2), 1);
    }
    void testUnionFind(Tester t)
    {
        quad.add(new ArrayList<Node>());
        quad.add(new ArrayList<Node>());
        quad.get(0).add(n1);
        quad.get(0).add(n2);
        quad.get(1).add(n3);
        quad.get(1).add(n4);

        UnionFind testUF = new UnionFind(quad);
        t.checkExpect(testUF.find(n1), n1);
        t.checkExpect(testUF.find(n2), n2);
        testUF.union(n2, n4);
        t.checkExpect(testUF.find(n2), n4);
    }

    void testDirectionIsVertical(Tester t)
    {
        t.checkExpect(e1.directionIsVertical(), true);
        t.checkExpect(e3.directionIsVertical(), false);
    }
    void testSetVertical(Tester t)
    {
        testMaze.setVertical();
        for (Edge e: testMaze.grid)
        {
            if (e.directionIsVertical())
            {
                t.checkExpect(e.a.down, e.b);
                t.checkExpect(e.b.up, e.a);
            }
        }
    }
    void testSetHorizontal(Tester t)
    {
        testMaze.setHorizontal();
        for (Edge e: testMaze.grid)
        {
            if (!e.directionIsVertical())
            {
                t.checkExpect(e.a.right, e.b);
                t.checkExpect(e.b.left, e.a);
            }
        }
    }
    Node a = new Node(new Posn(1, 1));

    Node b = new Node(new Posn(1, 2));

    Node c = new Node(new Posn(2, 2));



    Edge ab = new Edge(a, b, 10);

    Edge ac = new Edge(a, c, 20);



    boolean testdirectionIsVertical(Tester t)
    {

        return t.checkExpect(ab.directionIsVertical(), true) &&

                t.checkExpect(ac.directionIsVertical(), false);

    }
    Maze dummyworld = new Maze("dummy");

    void testinitializeNodes(Tester t)
    {
        t.checkExpect(dummyworld.nodes.size() > 0, false);
        dummyworld.initializeNodes();
        t.checkExpect(dummyworld.nodes.size() > 0, true);
    }
    void testinitializeGrid(Tester t)
    {
        t.checkExpect(dummyworld.grid.size() > 0, false);
        dummyworld.initializeNodes();
        dummyworld.initializeGrid();
        t.checkExpect(dummyworld.grid.size() > 0, true);
    }
    void testinitializeMaze(Tester t)
    {
        t.checkExpect(dummyworld.leftovers.size() > 0, false);
        t.checkExpect(dummyworld.cuts.size() > 0, false);
        dummyworld.initializeNodes();
        dummyworld.initializeGrid();
        t.checkExpect(dummyworld.grid.size() > 0, true);
        dummyworld.initializeMaze();
        t.checkExpect(dummyworld.leftovers.size() > 0, true);
        t.checkExpect(dummyworld.cuts.size() > 0, true);
        t.checkExpect(dummyworld.grid.size() > 0, false);
    }

    void testMountain(Tester t)
    {
        Maze game = new Maze();
        game.bigBang(Maze.MAZE_WIDTH * Maze.CELL_SIZE + 1,
                        Maze.MAZE_HEIGHT * Maze.CELL_SIZE + 1, .00001);
    }
}
