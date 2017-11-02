# Tzar Framework 

## Git integration notes

### 2 Nov 2017, Dhi

Git cloning from a local git repository now basically works as of commit 
`93d16a2`. The command I used to test it is, 
```
java -cp example-projects/example-java/dist/lib/ExampleJavaRunner.jar:out/tzar.jar au.edu.rmit.tzar.commands.Main execlocalruns ../example-java --repotype=GIT --numruns=1 --runset=demo
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
Outputdir: /home/dsingh/tzar/outputdata/Example_Project/demo/6_Example_scenario.inprogress 
Running model: /home/dsingh/tzar/modelcode/Example_Project_586523363, run_id: 6, Project name: Example Project, Scenario name: Example scenario, Flags: --jarpath=dist/lib/ExampleJavaRunner.jar --classname=au.edu.rmit.rdvtest.ExampleJavaRunner 
Retrieving model from https://github.com/river-jade/tzar-framework/blob/master/java/version.properties to /home/dsingh/tzar/modelcode/version_library_292008521 
Entering example java runner. 
Here are the parameters: Parameters{parameters={test.variable.1=/home/dsingh/tzar/modelcode/Example_Project_586523363/foobar, test.variable.3=xyz, test.variable.2=456, random.seed=6, test.variable.5=789, test.variable.4=abc, version.library.name=/home/dsingh/tzar/modelcode/version_library_292008521}} 
Exiting example java runner. 
/home/dsingh/tzar/outputdata/Example_Project/demo/6_Example_scenario.inprogress/single_result.csv 
Latest tzar version is:  
Run 6 succeeded. Final output directory: /home/dsingh/tzar/outputdata/Example_Project/demo/6_Example_scenario 
Model exists at /home/dsingh/tzar/modelcode so will attempt to git clean it now 
Done. Model at /home/dsingh/tzar/modelcode is a clean working copy now 
Outputdir: /home/dsingh/tzar/outputdata/Example_Project/demo/7_Example_scenario_2.inprogress 
Running model: /home/dsingh/tzar/modelcode/Example_Project_586523363, run_id: 7, Project name: Example Project, Scenario name: Example scenario 2, Flags: --jarpath=dist/lib/ExampleJavaRunner.jar --classname=au.edu.rmit.rdvtest.ExampleJavaRunner 
Retrieving model from https://github.com/river-jade/tzar-framework/blob/master/java/version.properties to /home/dsingh/tzar/modelcode/version_library_292008521 
Entering example java runner. 
Here are the parameters: Parameters{parameters={test.variable.1=789, test.variable.3=xyz, test.variable.2=456, random.seed=7, test.variable.4=abc, version.library.name=/home/dsingh/tzar/modelcode/version_library_292008521}} 
Exiting example java runner. 
/home/dsingh/tzar/outputdata/Example_Project/demo/7_Example_scenario_2.inprogress/single_result.csv 
Latest tzar version is:  
Run 7 succeeded. Final output directory: /home/dsingh/tzar/outputdata/Example_Project/demo/7_Example_scenario_2 
Concatenator output file /home/dsingh/tzar/outputdata/Example_Project/demo/all_results2.csv exists from previous job. Appending. 
Executed 2 runs: 2 succeeded. 0 failed 

```
