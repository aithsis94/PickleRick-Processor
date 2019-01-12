package in.picklerick.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Created by ajitesh on 09/11/18.
 */

public class DependencyGraph {

    List<Node> rootList;

    public DependencyGraph(Collection<Node> rootList) {
        this.rootList = new ArrayList<>(rootList);
    }

    public Collection<Node> getRootList() {
        return Collections.unmodifiableList(rootList);
    }

    public static void checkIfCreationalDependencySatisfied(DependencyGraph graph) throws Exception {

        List<Node> rootNodeList = new ArrayList<>(graph.rootList);

        while (rootNodeList.size() != 0) {

            Node nonDependantNode = getFirstNonDependantNode(rootNodeList);

            if (nonDependantNode == null) {
                findAndThrowCyclicDependencyNodesError(rootNodeList);
            } else {

                rootNodeList.remove(nonDependantNode);

                for (Node rootNode : rootNodeList) {
                    rootNode.removeDependency(nonDependantNode);
                }
            }
        }
    }

    private static void findAndThrowCyclicDependencyNodesError(List<Node> rootList) throws Exception {

        if (rootList == null || rootList.size() == 0)
            return;

        List<Node> visitedNodes = new ArrayList<>();
        Stack<Node> traversalStack = new Stack<>();

        Node firstNode = rootList.get(0);
        traversalStack.add(firstNode);

        do {

            Node currNode = traversalStack.pop();

            if (visitedNodes.contains(currNode)) {

                Node dependantNode = visitedNodes.get(visitedNodes.size() - 1);
                throw new Exception(dependantNode.getName() + " and " + currNode.getName() + " are cyclically dependant on each other");

            } else {

                visitedNodes.add(currNode);
                List<Node> dependencies = currNode.getDependencies();
                for (Node dependency : dependencies) {
                    traversalStack.push(dependency);
                }
            }

        } while (traversalStack.size() > 0);
    }

    private static Node getFirstNonDependantNode(List<Node> graph) {

        for (Node node : graph) {

            if (!node.isDependant()) {
                return node;
            }
        }

        return null;
    }
}
