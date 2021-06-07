# Green 1 Accelerators 1 - Jupyter Notebook Scripts

After running `green/green-1/accelerators-1/terraform/managed-infrastructure` in https://gitlab.com/Quantexa/dev-ops/google/QuantexaGoogleEnvironments repo, run the following command:

```bash
terraform output -json >> notebookVariables.json

# Move this 'notebookVariables.json' to the `hosting/google/green-1-accelerators-1/scripts` folder in `project-example` repo

mv notebookVariables.json ~/quantexa/project-example/hosting/google/green-1-accelerators-1/scripts/
```
