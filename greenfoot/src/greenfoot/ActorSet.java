/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;

import threadchecker.OnThread;
import threadchecker.Tag;

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
    
    /** Sum of sequence numbers of contained actors */
    private int myHashCode = 0;


    @OnThread(value = Tag.Simulation, ignoreParent = true)
    @Override
    public int hashCode()
    {
        return myHashCode;
    }

    @OnThread(value = Tag.Simulation, ignoreParent = true)
    @Override
    public boolean add(Actor actor)
    {
        if (containsActor(actor)) {
            return false;
        }
        
        numActors++;
        ListNode newNode = new ListNode(actor, listHeadTail.prev);
        
        int seq = ActorVisitor.getSequenceNumber(actor);
        if (numActors >= 2 * hashMap.length) {
            // grow the hashmap
            resizeHashmap();
        }
        else {
            int hash = seq % hashMap.length;
            ListNode hashHead = hashMap[hash];
            hashMap[hash] = newNode;
            newNode.setHashListHead(hashHead);
        }
        
        myHashCode += seq;
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

    @OnThread(value = Tag.Simulation, ignoreParent = true)
    public boolean containsActor(Actor actor)
    {
        return getActorNode(actor) != null; 
    }

    @OnThread(value = Tag.Simulation, ignoreParent = true)
    @Override
    public boolean contains(Object o)
    {
        if (o instanceof Actor) {
            Actor a = (Actor) o;
            return containsActor(a);
        }
        return false;
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
            myHashCode -= ActorVisitor.getSequenceNumber(actor);
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

    @OnThread(value = Tag.Simulation, ignoreParent = true)
    @Override
    public int size()
    {
        return numActors;
    }

    @OnThread(value = Tag.Simulation, ignoreParent = true)
    @Override
    public Iterator<Actor> iterator()
    {
        return new ActorSetIterator();
    }
    
    @OnThread(Tag.Simulation)
    private class ListNode
    {
        Actor actor;
        ListNode next;
        ListNode prev;
        
        // The node also appears in a linked list representing the hash bucket 
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
        
        /**
         * Set this node as the new head node in a hash bucket list.
         * @param oldHead  The original head node in the bucket
         */
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
    
    @OnThread(Tag.Simulation)
    private class ActorSetIterator implements Iterator<Actor>
    {
        ListNode currentNode;
        
        public ActorSetIterator()
        {
            currentNode = listHeadTail;
        }

        @OnThread(value = Tag.Simulation, ignoreParent = true)
        @Override
        public boolean hasNext()
        {
            return currentNode.next != listHeadTail;
        }

        @OnThread(value = Tag.Simulation, ignoreParent = true)
        @Override
        public Actor next()
        {
            currentNode = currentNode.next;
            return currentNode.actor;
        }

        @OnThread(value = Tag.Simulation, ignoreParent = true)
        @Override
        public void remove()
        {
            ActorSet.this.remove(currentNode);
        }
    }
}
