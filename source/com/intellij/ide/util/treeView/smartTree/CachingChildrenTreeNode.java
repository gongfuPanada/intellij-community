package com.intellij.ide.util.treeView.smartTree;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;

import java.util.*;

public abstract class CachingChildrenTreeNode <Value> extends AbstractTreeNode<Value> {
  protected List<CachingChildrenTreeNode> myChildren;
  protected List<CachingChildrenTreeNode> myOldChildren = null;
  protected final TreeModel myTreeModel;

  public CachingChildrenTreeNode(Project project, Value value, TreeModel treeModel) {
    super(project, value);
    myTreeModel = treeModel;
  }

  public Collection<AbstractTreeNode> getChildren() {
      ensureChildrenAreInitialized();
      return new ArrayList<AbstractTreeNode>(myChildren);
  }

    private void ensureChildrenAreInitialized() {
        if (myChildren == null) {
          myChildren = new ArrayList<CachingChildrenTreeNode>();
          rebuildSubtree();
        }
    }

    public void addSubElement(CachingChildrenTreeNode node) {
    ensureChildrenAreInitialized();
    myChildren.add(node);
    node.setParent(this);
  }

  public void setChildren(Collection<AbstractTreeNode> children) {
    clearChildren();
    for (Iterator<AbstractTreeNode> iterator = children.iterator(); iterator.hasNext();) {
      AbstractTreeNode node = iterator.next();
      myChildren.add((CachingChildrenTreeNode)node);
      node.setParent(this);
    }
  }

  private static class CompositeComparator implements Comparator<AbstractTreeNode> {
    private final Sorter[] mySorters;

    public CompositeComparator(final Sorter[] sorters) {
      mySorters = sorters;
    }

    public int compare(final AbstractTreeNode o1, final AbstractTreeNode o2) {
      final Object value1 = o1.getValue();
      final Object value2 = o2.getValue();
      for (int i = 0; i < mySorters.length; i++) {
        Sorter sorter = mySorters[i];
        final int result = sorter.getComparator().compare(value1, value2);
        if (result != 0) return result;
      }
      return 0;
    }
  }

  public void sortChildren(Sorter[] sorters) {
    Collections.sort(myChildren, new CompositeComparator(sorters));

    for (Iterator<CachingChildrenTreeNode> iterator = myChildren.iterator(); iterator.hasNext();) {
      CachingChildrenTreeNode child = iterator.next();
      if (child instanceof GroupWrapper) {
        child.sortChildren(sorters);
      }
    }

  }

  public void filterChildren(Filter[] filters) {
    Collection children = getChildren();
    for (int i = 0; i < filters.length; i++) {
      Filter filter = filters[i];
      for (Iterator<AbstractTreeNode> eachNode = children.iterator(); eachNode.hasNext();) {
        TreeElementWrapper eachChild = (TreeElementWrapper)eachNode.next();
        if (!filter.isVisible(eachChild.getValue())) {
          eachNode.remove();
        }
      }
    }
    setChildren(children);
  }

  public void groupChildren(Grouper[] groupers) {
    for (int i = 0; i < groupers.length; i++) {
      Grouper grouper = groupers[i];
      groupElements(grouper);
    }
  }

  public void groupElements(Grouper grouper) {

    ArrayList<AbstractTreeNode> ungrouped = new ArrayList<AbstractTreeNode>();
    Collection<AbstractTreeNode> children = getChildren();
    for (Iterator<AbstractTreeNode> iterator = children.iterator(); iterator.hasNext();) {
      CachingChildrenTreeNode node = (CachingChildrenTreeNode)iterator.next();      
      if (node instanceof TreeElementWrapper) {
        ungrouped.add(node);
      } else {
        node.groupElements(grouper);
      }
    }

    processUngrouped(ungrouped, grouper);


    Collection<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
    for (Iterator<AbstractTreeNode> iterator = children.iterator(); iterator.hasNext();) {
      AbstractTreeNode child = iterator.next();
      AbstractTreeNode parent = child.getParent();
      if (parent != this){
        if (!result.contains(parent)) result.add(parent);
      } else {
        result.add(child);
      }
    }
    setChildren(result);
  }

  private void processUngrouped(ArrayList<AbstractTreeNode> ungrouped,
                                                                   Grouper grouper) {
    Collection<TreeElement> ungroupedObjects = collectValues(ungrouped);
    Collection<Group> groups = grouper.group(ungroupedObjects);

    Map<Group, GroupWrapper> groupNodes = createGroupNodes(groups);

    for (Iterator<Group> eachGroup = groups.iterator(); eachGroup.hasNext();) {
      Group group = eachGroup.next();
      for (Iterator<AbstractTreeNode> eachUngrNode = ungrouped.iterator(); eachUngrNode.hasNext();) {
        AbstractTreeNode node = eachUngrNode.next();
        if (group.contains((TreeElement)node.getValue())) {
          GroupWrapper groupWrapper = groupNodes.get(group);
          groupWrapper.addSubElement((CachingChildrenTreeNode)node);
          node.setParent(groupWrapper);
          eachUngrNode.remove();
        }
      }
    }
  }

  private Collection<TreeElement> collectValues(ArrayList<AbstractTreeNode> ungrouped) {
    ArrayList<TreeElement> objects = new ArrayList<TreeElement>();
    for (Iterator<AbstractTreeNode> iterator = ungrouped.iterator(); iterator.hasNext();) {
      objects.add((TreeElement)iterator.next().getValue());
    }
    return objects;
  }

  private Map<Group, GroupWrapper> createGroupNodes(Collection<Group> groups) {
    com.intellij.util.containers.HashMap<Group, GroupWrapper> result = new com.intellij.util.containers.HashMap<Group, GroupWrapper>();
    for (Iterator<Group> iterator = groups.iterator(); iterator.hasNext();) {
      Group group = iterator.next();
      result.put(group, new GroupWrapper(getProject(), group, myTreeModel));
    }
    return result;
  }


  private void rebuildSubtree() {
    initChildren();
    performTreeActions();

    synchronizeChildren();

  }

  protected void synchronizeChildren() {
    if (myOldChildren != null && myChildren != null) {
      for (Iterator<CachingChildrenTreeNode> iterator = myOldChildren.iterator(); iterator.hasNext();) {
        CachingChildrenTreeNode oldInstance = iterator.next();
        final int newIndex = getIndexOfPointerToTheSameValue(oldInstance);
        if (newIndex >= 0) {
          final CachingChildrenTreeNode newInstance = myChildren.get(newIndex);
          oldInstance.copyFromNewInstance(newInstance);
          oldInstance.setValue(newInstance.getValue());
          myChildren.set(newIndex, oldInstance);
        }
      }
    }
  }

  private int getIndexOfPointerToTheSameValue(final CachingChildrenTreeNode oldInstance) {
    for (int i = 0; i < myChildren.size(); i++) {
      CachingChildrenTreeNode newInstance = myChildren.get(0);

      if (newInstance instanceof TreeElementWrapper) {
        final StructureViewTreeElement newElement = ((StructureViewTreeElement)newInstance.getValue());
        if (oldInstance instanceof TreeElementWrapper) {
          final StructureViewTreeElement oldElement = ((StructureViewTreeElement)oldInstance.getValue());
          if (newElement.getValue() != null) {
            if (Comparing.equal(newElement.getValue(), oldElement.getValue())) return i;
          }
        }
      } else {
        if (newInstance.equals(oldInstance)) return i;
      }
    }
    return -1;
  }

  protected abstract void copyFromNewInstance(final CachingChildrenTreeNode newInstance);

  protected abstract void performTreeActions();

  protected abstract void initChildren();

  protected void clearChildren() {
    if (myChildren != null) {
      myChildren.clear();
    } else {
      myChildren = new ArrayList<CachingChildrenTreeNode>();
    }
  }

  public void rebuildChildren() {
    if (myChildren != null) {
      myOldChildren = myChildren;
      for (Iterator<CachingChildrenTreeNode> iterator = myChildren.iterator(); iterator.hasNext();) {
        iterator.next().rebuildChildren();
      }
      myChildren = null;
    }
  }


}
