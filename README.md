# clj-battlesnake
API implementing BAttlesnake interface in Clojure

in order to deploy make sure you build a new jar:

``` Bash
lein ring uberjar
```

And then commit and push it to git (the way we're using ebcli bases
the deploy off of the git history)

Finally deploy with:

``` Bash
eb deploy -v
```
