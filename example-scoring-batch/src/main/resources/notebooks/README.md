### Instructions for Running the Scorecard Jupyter Notebook
_ScorecardNotebook.ipynb_ can be very useful for exploring scores and scorecards after they have been run in batch. 
In order to make the notebook work you must follow the instructions [here for Setting Apache Toree + Scala Kernel for Jupyter on Windows.](https://quantexa.atlassian.net/wiki/spaces/TECH/pages/4128875/Eclipse+and+other+tools+Setup)

After the notebook is setup, you must run the code on project-example. Instructions for doing this can be found [here.](https://quantexa.atlassian.net/wiki/spaces/TECH/pages/45110910/Smoke+Test+-+Latest) Currently, the scorecard data must be local for the ScorecardNotebook to use it, so after running your score in batch download it do a local directory. 
In the notebook there is a variable called **pathToExampleScoreCard** which you must change to point to the ExampleScoreCard parquet. 

You should then be able to run ScorecardNotebook.ipynb, you can run each cell individual by pressing Shift+Enter or under Cell choose "Run All" to run the entire notebook. You can right click on a graph to save as an image. 
