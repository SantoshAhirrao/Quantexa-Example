#!/bin/bash

# Install Jupyter Kernels and restart Jupyter Notebook

set -exo pipefail

readonly ROLE="$(/usr/share/google/get_metadata_value attributes/dataproc-role)"

if [[ "${ROLE}" == 'Master' ]]; then
    # Install Spylon Kernel: https://github.com/Valassis-Digital-Media/spylon-kernel
    pip install spylon-kernel
    python -m spylon_kernel install

    # Install Toree Kernel: https://github.com/apache/incubator-toree
    pip install toree
    SPARK_OPTS="--master yarn --deploy-mode client"
    /opt/conda/bin/jupyter toree install \
        --spark_opts="${SPARK_OPTS}" \
        --spark_home="/usr/lib/spark" \
        --toree_opts='--nosparkcontext' \
        --kernel_name="Toree" \
        --interpreters="Scala,SQL"
    systemctl restart jupyter-notebook
fi
