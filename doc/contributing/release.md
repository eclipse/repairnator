# How to release repairnator? 

Releasing repairnator follows 3 main steps:
  1. you need to release the jars on Maven Central
  2. you need to release the jars on Github
  3. you need to release the docker images

## Release on Maven
### Prepare the m2/settings.xml

You need to specify the credentials information such as:
```
<settings>
  <servers>
    <server>
      <id>ossrh-repairnator</id>
      <username>repairnator</username>
      <password>password</password>
    </server>
  <servers>
</settings>
```

You also need to have an available gpg2 key available and setup in the settings. (more info on Spoon release)

### Export Github OAuth Token

Some tests of Repairnator needs the Github OAuth token, so export your token before calling the Maven commands:
```
export GITHUB_OAUTH=<token>
```

### Launch prepare release

```
cd repairnator
mvn release:prepare -DignoreSnapshots
```

Don't forget about `-DignoreSnapshots` as some of our repair tools are only deployed as snapshots... 
:warning: This step takes time and will ask for your password, so keep an eye on the process ;)

### Release the jars

```
mvn release:perform -DignoreSnapshots
```

## Release on Github

After the Maven release a tag has been created on git and pushed on Github. Just open the tag on Github, and upload the jars with dependencies there.
Don't forget also to zip the `scripts` folder and to add it to the Github release.

## Release the docker image

You have to build the docker images, to tag them with the right tag, and then to deploy them.
First ensure your are logged in with repairnator user:

```
docker login -u repairnator
```

Then follow each step for each docker image you want to deploy (e.g. pipeline-dockerimage, bears-dockerimage, ...).

Don't forget to check that the `version.ini` file contains the right version number in the docker image, and to edit it if needed:

```
$ cat docker-images/pipeline-dockerimage/version.ini
2.2
```

Then launch the image build with the right version number as a tag: 
```
docker build -t spirals/repairnator:<version> docker-images/pipeline-dockerimage
```

And finally deploy the image:
```
docker push spirals/repairnator:<version>
```
