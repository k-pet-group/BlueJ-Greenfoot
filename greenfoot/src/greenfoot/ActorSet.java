package greenfoot;

import java.util.AbstractSet;
import java.util.Iterator;

/**
 * This is an ordered set. 
 * 
 * @author Davin McCall
 */
public class ActorSet extends AbstractSet<Actor>
{
    private ListNode listHeadTail = new ListNode();
    
    private ListNode [] hashMap = new ListNode[0];
    
    private int numActors = 0;
    
    public boolean add(Actor actor)
    {
        if (contains(actor)) {
            return false;
        }
        
        numActors++;
        ListNode newNode = new ListNode(actor, listHeadTail.prev);
        
        if (numActors >= 2 * hashMap.length) {
            // grow the hashmap
            resizeHashmap();
        }
        else {
            int seq = ActorVisitor.getSequenceNumber(actor);
            int hash = seq % hashMap.length;
            ListNode hashHead = hashMap[hash];
            hashMap[hash] = newNode;
            newNode.setHashListHead(hashHead);
        }
        
        return true;
    }

    private void resizeHashmap()
    {
        hashMap = new ListNode[numActors];
        ListNode currentActor = listHeadTail.next;
        while (currentActor != listHeadTail) {
            int seq = ActorVisitor.getSequenceNumber(currentActor.actor);
            int hash = seq % numActors;
            ListNode hashHead = hashMap[hash];
            hashMap[hash] = currentActor;
            currentActor.setHashListHead(hashHead);
            
            currentActor = currentActor.next;
        }
    }
    
    public boolean contains(Actor actor)
    {
        return getActorNode(actor) != null; 
    }
    
    /**
     * Get the list node for an actor (null if the actor is not in the set).
     */
    private ListNode getActorNode(Actor actor)
    {
        if (hashMap.length == 0) {
            return null;
        }
        
        int seq = ActorVisitor.getSequenceNumber(actor);
        int hash = seq % hashMap.length;
        ListNode hashHead = hashMap[hash];
        
        if (hashHead == null) {
            return null;
        }
        else if (hashHead.actor == actor) {
            return hashHead;
        }
        
        ListNode curNode = hashHead.nextHash;
        while (curNode != hashHead) {
            if (curNode.actor == actor) {
                return curNode;
            }
            curNode = curNode.nextHash;
        }
        
        return null;
    }
    
    public boolean remove(Actor actor)
    {
        ListNode actorNode = getActorNode(actor);
        
        if (actorNode != null) {
            remove(actorNode);
            return true;
        }
        else {
            return false;
        }
    }
    
    private void remove(ListNode actorNode)
    {
        int seq = ActorVisitor.getSequenceNumber(actorNode.actor);
        int hash = seq % hashMap.length;
        if (hashMap[hash] == actorNode) {
            hashMap[hash] = actorNode.nextHash;
            if (hashMap[hash] == actorNode) {
                // The circular list had only one element
                hashMap[hash] = null;
            }
        }
        
        actorNode.remove();
        numActors--;
        if (numActors <= hashMap.length / 2) {
            // shrink the hashMap
            resizeHashmap();
        }
    }
    
    public int size()
    {
        return numActors;
    }
    
    @Override
    public Iterator<Actor> iterator()
    {
        return new ActorSetIterator();
    }
    
    private class ListNode
    {
        Actor actor;
        ListNode next;
        ListNode prev;
        
        ListNode nextHash;
        ListNode prevHash;
        
        public ListNode()
        {
            // actor, next, prev = null: this is the head/tail node
            next = this;
            prev = this;
        }
        
        /**
         * Create a new list node and insert it at the tail of the list.
         * @param actor
         * @param listTail
         */
        public ListNode(Actor actor, ListNode listTail)
        {
            this.actor = actor;
            next = listTail.next;
            prev = listTail;
            listTail.next = this;
            next.prev = this;
        }
        
        public void setHashListHead(ListNode oldHead)
        {
            if (oldHead == null) {
                nextHash = this;
                prevHash = this;
            }
            else {
                nextHash = oldHead;
                prevHash = oldHead.prevHash;
                oldHead.prevHash = this;
                prevHash.nextHash = this;
            }
        }
        
        public void remove()
        {
            next.prev = prev;
            prev.next = next;
            nextHash.prevHash = prevHash;
            prevHash.nextHash = nextHash;
        }
    }
    
    private class ActorSetIterator implements Iterator<Actor>
    {
        ListNode currentNode;
        
        public ActorSetIterator()
        {
            currentNode = listHeadTail;
        }
        
        public boolean hasNext()
        {
            return currentNode.next != listHeadTail;
        }
        
        public Actor next()
        {
            currentNode = currentNode.next;
            return currentNode.actor;
        }
        
        public void remove()
        {
            ActorSet.this.remove(currentNode);
        }
    }
}
