R E T U R N   O F   T H E   G R E E P S 
 - 

Improve the "Greeps" implementation to help the Greeps collect tomatoes!
It's important. (At least to them...)

This scenario is part of the SIGCSE'09 Greenfoot Programming Competition.

Your task is to program the Greeps to collect as many tomatoes as possible 
and to do it faster than the opponent Greeps.

The rules are:

Rule 1 
Only change the class ‘MyGreep’. No other classes may be modiﬁed or created. 

Rule 2 
You cannot extend the Greeps’ memory. That is: you are not allowed to add 
ﬁelds (other than ﬁnal ﬁelds) to the class. Some general purpose memory is
provided. (The ship can also store data.) 

Rule 3 
You can call any method deﬁned in the "Greep" superclass, except act(). 

Rule 4 
Greeps have natural GPS sensitivity. You can call getX()/getY() on any object
and get/setRotation() on yourself any time. Friendly greeps can communicate. 
You can call getMemory() and getFlag() on another greep to ask what they know. 

Rule 5 
No creation of objects. You are not allowed to create any scenario objects 
(instances of user-deﬁned classes, such as MyGreep). Greeps have no magic 
powers – they cannot create things out of nothing. 

Rule 6 
You are not allowed to call any methods (other than those listed in Rule 4)
of any other class in this scenario (including Actor and World). 
 

If you change the name of the 'MyGreep' class you should also change it in 
Ship.createGreep().


Please do not publish your solution anywhere. We might want to run this
competition again, or it might be used by teachers to run in a class, and
that would be ruined if solutions were available.

Version: 1.0
Written by: Michael Kolling, Feb 2009
Copyright (c) Michael Kolling
License: Free for any non-commercial use, modification and distribution.
