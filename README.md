# Tzar Framework 

## Git integration notes

### 30 Nov 2017, Dhi

Git cloning from a local git repository now basically works as of commit 
`93d16a2`. The command I used to test it is, 
```
java -jar out/tzar.jar execlocalruns ../example-java --repotype=GIT --numruns=1
```
where `../example-java` is the path to a local Git repository (in my case it is 
a local git repo with the same contents as `./example-projects/example-java`).

If the destination directory for the git clone exists, the code assumes it was 
a previous clone so tries to clean. If that fails, you get an exception with 
some semi-useful message. If the destination directory does not exist, it is 
created and the Git repository is cloned in there.

Here is the output from the above command:
```
Created 2 runs. 
Model exists at /home/dsingh/tzar/modelcode so will attempt to git clean it now 
Done. Model at /home/dsingh/tzar/modelcode is a clean working copy now 
Outputdir: /home/dsingh/tzar/outputdata/Example_Project/default_runset/1_Example_scenario.inprogress 
Running model: /home/dsingh/tzar/modelcode, run_id: 1, Project name: Example Project, Scenario name: Example scenario, Flags: --jarpath=dist/lib/ExampleJavaRunner.jar --classname=au.edu.rmit.rdvtest.ExampleJavaRunner 
Retrieving model from https://github.com/river-jade/tzar-framework/blob/master/java/version.properties to /home/dsingh/tzar/modelcode/version_library_292008521 
Entering example java runner. 
Here are the parameters: Parameters{parameters={test.variable.1=/home/dsingh/tzar/modelcode/foobar, test.variable.3=xyz, test.variable.2=456, random.seed=1, test.variable.5=789, test.variable.4=abc, version.library.name=/home/dsingh/tzar/modelcode/version_library_292008521}} 
Exiting example java runner. 
/home/dsingh/tzar/outputdata/Example_Project/default_runset/1_Example_scenario.inprogress/single_result.csv 
Latest tzar version is:  
Run 1 succeeded. Final output directory: /home/dsingh/tzar/outputdata/Example_Project/default_runset/1_Example_scenario 
Model exists at /home/dsingh/tzar/modelcode so will attempt to git clean it now 
Done. Model at /home/dsingh/tzar/modelcode is a clean working copy now 
Outputdir: /home/dsingh/tzar/outputdata/Example_Project/default_runset/2_Example_scenario_2.inprogress 
Running model: /home/dsingh/tzar/modelcode, run_id: 2, Project name: Example Project, Scenario name: Example scenario 2, Flags: --jarpath=dist/lib/ExampleJavaRunner.jar --classname=au.edu.rmit.rdvtest.ExampleJavaRunner 
Retrieving model from https://github.com/river-jade/tzar-framework/blob/master/java/version.properties to /home/dsingh/tzar/modelcode/version_library_292008521 
Entering example java runner. 
Here are the parameters: Parameters{parameters={test.variable.1=789, test.variable.3=xyz, test.variable.2=456, random.seed=2, test.variable.4=abc, version.library.name=/home/dsingh/tzar/modelcode/version_library_292008521}} 
Exiting example java runner. 
/home/dsingh/tzar/outputdata/Example_Project/default_runset/2_Example_scenario_2.inprogress/single_result.csv 
Latest tzar version is:  
Run 2 succeeded. Final output directory: /home/dsingh/tzar/outputdata/Example_Project/default_runset/2_Example_scenario_2 
Executed 2 runs: 2 succeeded. 0 failed 

```
