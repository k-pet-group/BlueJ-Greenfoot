package greenfoot.collision.ibsp;

import greenfoot.Actor;

public class ActorNode
{
    private Actor actor;
    private BSPNode node;
    private ActorNode next;
    private ActorNode prev;
    
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
    }
    
    public Actor getActor()
    {
        return actor;
    }
    
    public BSPNode getBSPNode()
    {
        return node;
    }
    
    public ActorNode getNext()
    {
        return next;
    }
    
    public void remove()
    {
        removed();
        node.actorRemoved(actor);
    }
    
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
