#!/usr/bin/env bash

ELASTIC_URL=elastic.toothless.svc.cluster.local:9200

# If the indexes exist, delete them
function delete_index_if_exist() {
    local index=$1
    if [[ $(curl -o /dev/null -I -L -s -w "%{http_code}" "$ELASTIC_URL/$index") = "200" ]]; then
        curl -X DELETE "$ELASTIC_URL/$index"
    fi
}

declare -a indexes=("ci-test-search-customer"
                    "ci-test-resolver-customer-telephone"
                    "ci-test-resolver-customer-individual"
                    "ci-test-resolver-customer-doc2rec"
                    "ci-test-resolver-customer-business"
                    "ci-test-resolver-customer-address"
                    "ci-test-resolver-customer-account"
                    "ci-test-search-hotlist"
                    "ci-test-resolver-hotlist-telephone"
                    "ci-test-resolver-hotlist-individual"
                    "ci-test-resolver-hotlist-doc2rec"
                    "ci-test-resolver-hotlist-business"
                    "ci-test-resolver-hotlist-address"
                    "ci-test-search-transaction"
                    "ci-test-resolver-transaction-individual"
                    "ci-test-resolver-transaction-doc2rec"
                    "ci-test-resolver-transaction-business"
                    "ci-test-resolver-transaction-account"
                    "ci-test-search-research"
                    "ci-test-resolver-research-telephone"
                    "ci-test-resolver-research-individual"
                    "ci-test-resolver-research-doc2rec"
                    "ci-test-resolver-research-business"
                    "ci-test-resolver-research-address"
                    "ci-test-resolver-research-account"
                    "search-ci-test-txncustomerdatescoretocustomerrollup"
                    "search-ci-test-txnscoretocustomerrollup"
                    )

for i in ${indexes[@]}; do
    printf "\n\nDeleting index ${i}...\n\n"
    delete_index_if_exist ${i}
done
