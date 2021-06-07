# AC-XXXX Merge request

## REVIEWEE SECTION
### Merge request type
<!-- Please try to limit your merge request to one type, submit multiple merge requests if needed. -->

Please check the type of change your PR introduces:

- [ ] Bugfix
- [ ] Feature
- [ ] Code style update (formatting, renaming)
- [ ] Refactoring (no functional changes, no API changes)
- [ ] Build related changes
- [ ] Documentation content changes
- [ ] Other (please describe):

### Merge request checklist
<!--If raising a merge request for part of a piece of work and more will follow, 
    (i.e.merge request doesn't close out a JIRA), documentation can be completed later -->
Please check that your PR fulfils the following requirements:

- [ ] All changes have been pushed
- [ ] Tests for the changes 
  - [ ] Have been added (for bug fixes / features) OR 
  - [ ] Tests are not required because <EXPLANATION> 
- [ ] Docs have been reviewed (by reviewee) and added / updated if needed (for bug fixes / features), and linked below.
- [ ] JIRA Summary has been updated to be easily readable as what was completed
- [ ] Release Highlights are:
  - [ ] Not Required
  - [ ] Have been written and are linked below
- [ ] Build (`gradlew build`, `npm run build`) was run locally on changed projects and dependent projects
- [ ] In addition to the feature branch CICD build, changes have been tested by the following (select as appropriate):
  - [ ] Changes have been tested by running through subset test data on dev-cluster
  - [ ] Changes have been tested by running through full test data on dev-cluster
  - [ ] Changes have been tested by the daily build triggered on the feature branch
  - [ ] Started up the UI and verified changes

###Documentation <!-- Include links to documentation that has been created updated, including new JIRAs--> 
* 

### Does this require release highlights
- [ ] Yes
- [ ] No

<!-- If this requires release highlights, please include a link to the release highlights in confluence-->

### Does this introduce a breaking change

- [ ] Yes
- [ ] No

<!-- If this introduces a breaking change, please include a link to the migration notes in confluence-->

### Gif screen recording / Screenshot(s)
<!-- Add any screenshots here if appropriate i.e. a UI change. -->

### Other information
<!-- Any other information that is important to this MR. -->

##REVIEWER SECTION

- [ ] Code follows best practice
- [ ] Updated Merge commit message (if required, prior to merge)
- [ ] Reviewed JIRA summary (for release notes)
- [ ] Documentation Signed off

###Tasks to be completed post merge by Reviewer
* Documentation Moved out of WIP into published space
* Updated Accelerators Landing page with Documentation links
* JIRA progressed to done