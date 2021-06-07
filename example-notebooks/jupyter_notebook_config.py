""" This config file makes jupyter save identical .py and .html files when it saves.
    For this to work you must run `jupyter notebook` at this level in the directory structure.
    This way we can version control a .py file to get diffs of notebooks and a html file to see an output.
    Reference: https://svds.com/jupyter-notebook-best-practices-for-data-science/
"""

import os
from subprocess import check_call


def post_save(model, os_path, contents_manager):
    """post-save hook for converting notebooks to .py scripts"""
    if model['type'] != 'notebook':
        return # only do this for notebooks
    d, fname = os.path.split(os_path)
    check_call(['jupyter', 'nbconvert', '--to', 'script', fname], cwd=d)
    check_call(['jupyter', 'nbconvert', '--to', 'html', fname], cwd=d)


c.FileContentsManager.post_save_hook = post_save
c.IPKernelApp.no_stderr = True
