package greenfoot.collision.ibsp;

import greenfoot.Actor;

public final class ActorNode
{
    private Actor actor;
    private BSPNode node;
    private ActorNode next;
    private ActorNode prev;
    private boolean mark;
    
    public ActorNode(Actor actor, BSPNode node)
    {
        this.actor = actor;
        this.node = node;
        
        // insert into linked list
        ActorNode first = IBSPColChecker.getNodeForActor(actor);
        this.next = first;
        IBSPColChecker.setNodeForActor(actor, this);
        if (next != null) {
            next.prev = this;
        }
        
        mark = true;
    }
    
    public void clearMark()
    {
        mark = false;
    }
    
    public void mark()
    {
        mark = true;
    }
    
    public boolean checkMark()
    {
        boolean markVal = mark;
        mark = false;
        return markVal;
    }
    
    public Actor getActor()
    {
        return actor;
    }
    
    public BSPNode getBSPNode()
    {
        return node;
    }
    
    /**
     * Get the next ActorNode for the same actor. Returns null if this
     * is the last ActorNode for the actor.
     */
    public ActorNode getNext()
    {
        return next;
    }
    
    /**
     * Remove this actor node. The node is removed from both the BSPNode
     * which contains it, and the linked list of actor nodes for the actor.
     */
    public void remove()
    {
        removed();
        node.actorRemoved(actor);
    }
    
    /**
     * Notify this actor node that it has been removed from the BSPNode.
     * It must remove itself from the linked list of actor nodes for the
     * actor.
     */
    public void removed()
    {
        if (prev == null) {
            IBSPColChecker.setNodeForActor(actor, next);
        }
        else {
            prev.next = next;
        }
        
        if (next != null) {
            next.prev = prev;
        }
    }
}
