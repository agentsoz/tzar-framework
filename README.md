# Tzar Framework 

## Git integration notes

### 30 Nov 2017, Dhi

Git cloning from a local git repository works as of commit 
`93d16a2`. Try something like:
 ```
java -jar out/tzar.jar execlocalruns ../example-java --repotype=GIT --numruns=1
```
where `../example-java` is the path to a local Git repository.

As of commit `787670b`, cloning a remote repository also works. You can now do something like:
```concept
java -jar out/tzar.jar execlocalruns https://github.com/dhixsingh/tzar-example-java.git --repotype=GIT --numruns=1

```

