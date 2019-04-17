# Adding an option

Differs according to which module you inted to update

## realtime scanner

Add option to `repairnator.cfg` and `launch_rtscanner.sh`

Add to `fr.inria.spirals.repairnator.config.RepairnatorConfig.java`
	* [ ] With a setter and getter as a private attribute

Add to `fr.inria.spirals.repairnator.realtime.RTLauncher.java`:
	* [ ] Add a new `FlaggedOption`
	* [ ] Read from the arguments given to set value in the config
          (`this.config.set"newOption"(arguments.getObject("option"))`)
		  
If it needs to be initialized, add a method to
`fr.inria.spirals.repairnator.realtime.RTScanner.java` and possibly to 
`fr.inria.spirals.repairnator.LauncherUtils.java` depending on the complexity.
		  
