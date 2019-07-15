Test submit - will be removed later
[![Build Status](https://travis-ci.org/Spirals-Team/repairnator.svg?branch=master)](https://travis-ci.org/Spirals-Team/repairnator)

# Repairnator: a program repair bot for continuous integration

Repairnator is a software development bot that automatically repairs build failures on continuous integration.
It monitors failing Travis CI builds in Java projects, tries to locally reproduce the failing builds and finally attempts to repair them with the state-of-the-art of [automated program repair research](https://en.wikipedia.org/wiki/Automatic_bug_fixing). Repairnator is a community effort, driven by Martin Monperrus at KTH Royal Institute of Technology. 

Want to join the Repairnator community? To receive news about Repairnator, shoot an email at <mailto:repairnator.subscribe@4open.science>!

-- [The Repairnator team](https://github.com/Spirals-Team/repairnator/issues/760)

## Recruit Luc Esape

If you want that Luc Esape [@lucesape](http://github.com/lucesape), the coolest artificial software developer, joins your team, simply add him as collaborator to your Github project!

## Learn about Repairnator

* **Post:** [Human-competitive Patches in Automatic Program Repair with Repairnator](https://medium.com/@martin.monperrus/human-competitive-patches-in-automatic-program-repair-with-repairnator-359042e00f6a), Medium, Oct. 16 2018
* **Youtube:** [Talk about Repairnator at Hasso-Plattner-Institut](https://hal.inria.fr/hal-01691496/document), April 19 2018

## Resources

### Press

* [The mysterious life of Luc Esape, bug fixer extraordinaire. His big secret? He’s not human (Thomas Claburn), The Register, Oct 17 2018](https://www.theregister.co.uk/2018/10/17/luc_esape_bug_fixer/)
* [Repairnator, an autonomous robot to repair computer bugs (Sophie Timsit) inria.fr, Sep 4 2018](https://www.inria.fr/en/centre/lille/news/repairnator-an-autonomous-robot-to-repair-computer-bugs)
* See all media news about Repairnator in <https://github.com/Spirals-Team/repairnator/issues/771>

### Academic papers

* [Human-competitive Patches in Automatic Program Repair with Repairnator](http://arxiv.org/abs/1810.05806v1) (Martin Monperrus, Simon Urli, Thomas Durieux, Matias Martinez, Benoit Baudry, Lionel Seinturier) arXiv 1810.05806, 2018

* [How to Design a Program Repair Bot? Insights from the Repairnator Project](https://hal.archives-ouvertes.fr/hal-01691496/document) (Simon Urli, Zhongxing Yu, Lionel Seinturier, Martin Monperrus). In Proceedings of 40th International Conference on Software Engineering, Track Software Engineering in Practice (SEIP), 2018. [(bibtex)](https://www.monperrus.net/martin/bibtexbrowser.php?key=urli%3Ahal-01691496&bib=monperrus.bib)

### Talks

* Repairnator & Descartes: the future of testing and continuous integration (Martin Monperrus), Webinar at Sopra-Steria, April 4 2019
* [Repairnator: Automatisk buggrättning med hjälp av AI (Martin Monperrus), Claremont Tech Talks, March 19 2019](https://www.meetup.com/Claremont-Tech-Labs/events/259387546/)
* ["How to Design a Program Repair Bot? Insights from the Repairnator Project" (Martin Monperrus), Software Technology Exchange Workshop, STEW, 2018, Malmö, Oct 17 2018](https://www.swedsoft.se/event/stew-2018/)
* "How to Design a Program Repair Bot? Insights from the Repairnator Project" (Martin Monperrus), Talk at SAAB, Järfälla, Sep 21 2018
* ["How to Design a Program Repair Bot? Insights from the Repairnator Project" (Simon Urli), International Conference on Software Engineering, Gothenburg, June 1st 2018](https://www.icse2018.org/program/program-icse-2018)
* "How to Design a Program Repair Bot for Travis CI?", (Simon Urli, Martin Monperrus) Webinar at Travis CI, May 15 2018
* ["The Future of Automated Program Repair" (Martin Monperrus), 13th Annual Symposium on Future Trends in Service-Oriented Computing, Hasso Plattner Institute, Postdam, April 19 2018](https://hpi.de/veranstaltungen/wissenschaftliche-konferenzen/research-school/2018/symposium-on-future-trends-in-service-oriented-computing.html)
* ["How to Design a Program Repair Bot? Insights from the Repairnator Project" (Simon Urli) 58th CREST Open Workshop - Automating Programmers’ Programming Experiments for Analytic Result Reporting in Code Review and Continuous Integration, London, February 27 2018](http://crest.cs.ucl.ac.uk/cow/58/)

## Usage

[See the usage section of our documentation](doc/jars.md).

If you want to bring your own tool in Repairnator, have a look on [contribution guidelines](/doc/README.md) :smile:

## Content of the repository

This repository is organized as follows:

  * [doc](/doc) contains some documentation about Repairnator and its usage
  * [repairnator](/repairnator) is the main program dedicated to this project: it can automatically scan large set of projects, detect failing builds, reproduce them and try to repair them using our tools
  * [bears-usage](/bears-usage) is a side project dedicated to gather data from repairnator.json files
  * [resources](/resources) contains mainly data produced by Repairnator and scripts to retrieve those data. It also contain the schema of repairnator.json files.
  * [website](/website) contains all data to produce repairnator website
  
Each directory contains its own Readme explaining its own internal organization.

## Releases

* Github releases: https://github.com/Spirals-Team/repairnator/releases
* Maven releases: https://search.maven.org/search?q=repairnator
* DockerHub releases: https://hub.docker.com/r/spirals/repairnator/

## License

The content of this repository is licensed under the MIT terms. 

