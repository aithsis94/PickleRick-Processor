package in.picklerick.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.TypeElement;

/**
 * Created by ajitesh on 09/11/18.
 */

class Node {

    private TypeElement typeElement;
    private String name;

    public List<Node> dependencies = new ArrayList<>();

    public Node(TypeElement typeElement) {
        this.typeElement = typeElement;
        this.name = typeElement.getQualifiedName().toString();
    }

    public TypeElement getTypeElement() {
        return typeElement;
    }

    public String getName() {
        return name;
    }

    public List<Node> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addDependency(Node toBeAdded) {

        String toBeAddedDependencyName = toBeAdded.getName();
        boolean alreadyPresent = false;

        for (Node currDependency : this.dependencies) {

            String currName = currDependency.getName();

            if (toBeAddedDependencyName.equals(currName)) {
                alreadyPresent = true;
                break;
            }
        }

        if (!alreadyPresent) {
            this.dependencies.add(toBeAdded);
        }
    }

    public void removeDependency(Node toBeRemoved) {

        String toBeAddedDependencyName = toBeRemoved.getName();
        int toBeRemovedIndex = -1;

        for (int i = 0; i < this.dependencies.size(); i++) {

            Node currDependency = this.dependencies.get(i);
            String currName = currDependency.getName();

            if (toBeAddedDependencyName.equals(currName)) {
                toBeRemovedIndex = i;
                break;
            }
        }

        if (toBeRemovedIndex > -1) {
            this.dependencies.remove(toBeRemovedIndex);
        }
    }

    public boolean isDependant(){
        return this.dependencies.size() > 0;
    }

    public int getDependencyCount(){
        return this.dependencies.size();
    }

    public boolean isDependantOn(Node node){

        String nodeName = node.getName();

        for(Node dependency : this.dependencies){

            if(dependency.getName().equals(nodeName)){
                return true;
            }
        }

        return false;
    }
}
