

[![alt text](https://raw.githubusercontent.com/ms123s/simpl4-deployed/master/etc/images/simpl4_logo.png  "simpl4 logo")] (http://www.simpl4.org) building
=================

These steps are only required if you modify the source

If you're only interested in a installation, go to [*simpl4-deployed*](https://github.com/ms123s/simpl4-deployed)


####Requirement
* java jdk1.8.0  
* git

----

####Cloning this repo
```bash
$ git clone https://github.com/ms123s/simpl4-src.git simpl4-src
```
----

####Going to sourcerepository and start the build
```bash
$ cd simpl4-src
$ gradlew
```
clone *simpl4-deploy*, parallel to *simpl4-src*   
Directory arrangment:  
simpl4-src  
simpl4-deploy

and now update the "deploy directory"
```bash
$ cd simpl4-src
$ gradlew deploy 
```
----

####[*Website*](http://www.simpl4.org/de) and [*Demos*](http://web.simpl4.org?lang=de)

