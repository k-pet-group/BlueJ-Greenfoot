package greenfoot.collision;

import greenfoot.Actor;

public class ClassQuery implements CollisionQuery
{
    private Class<?> cls;
    private CollisionQuery subQuery;
    
    public ClassQuery(Class<?> cls, CollisionQuery subQuery)
    {
        this.cls = cls;
        this.subQuery = subQuery;
    }
    
    public boolean checkCollision(Actor actor)
    {
        if (cls.isInstance(actor)) {
            return subQuery.checkCollision(actor);
        }
        else {
            return false;
        }
    }

}
