# Contribute

## Add your own repair tool

Repairnator use a Java SPI to automatically discover the available repair tools step.

Then, in order to add a new repair tool in Repairnator you have to:

  1. Create a Java class which inherits from `fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep`. This latter class is located in `repairnator-pipeline`.
  2. Define an empty constructor
  3. Implement the methods `getRepairToolName` and `businessExecute`
  4. Declare the new concrete repair step in a service file `META-INF/services/fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep`
  
For more information about Java SPI have a look on this [tutorial in french](http://thecodersbreakfast.net/index.php?post/2008/12/26/Java-%3A-pr%C3%A9sentation-du-Service-Provider-API) or at [Oracle Documentation](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html).