#!/bin/bash -x
# Run test against data loaded into UI

###------ START TESTS ------###

echo '

    ____                    _                ______          __
   / __ \__  ______  ____  (_)___  ____ _   /_  __/__  _____/ /______
  / /_/ / / / / __ \/ __ \/ / __ \/ __ `/    / / / _ \/ ___/ __/ ___/
 / _, _/ /_/ / / / / / / / / / / / /_/ /    / / /  __(__  ) /_(__  ) _ _
/_/ |_|\__,_/_/ /_/_/ /_/_/_/ /_/\__, /    /_/  \___/____/\__/____(_|_|_)
                                /____/
'

# TEST: Check if gateway and app-search work
echo "Checking to see if gateway and app-search work...."
auth=$(curl -k -X POST -F 'username=quantexa-demo' -F 'password=xPlor3r' https://10.4.0.100/authenticate)
searchResponse=$(curl -k 'https://10.4.0.100/search/search' -H 'Pragma: no-cache' -H 'Origin: https://10.4.0.100' -H 'Accept-Encoding: gzip, deflate' -H 'Accept-Language: en-GB,en-US;q=0.9,en;q=0.8' -H "Authorization: Bearer $auth" -H 'Content-Type: application/json' -H 'Accept: application/json, text/plain, */*' -H 'Cache-Control: no-cache' -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36' -H 'Connection: keep-alive' -H 'Referer: https://10.4.0.100/ui/' -H 'Expires: -1' --data-binary '{"dataSource":"customer","runAggregations":true,"entitiesToResolve":["business","individual"],"pageNumber":0,"searchParameters":[{"parameterName":"forenames","parameterValue":"Ryan","searchType":{"type":"MatchAndQuery"}}]}' --compressed)
if [[ $(echo $searchResponse | jq -r .totalHits) -gt 0 ]]; then
    echo "SEARCH TEST PASSED"
else
    echo "SEARCH TEST FAILED"
    echo $searchResponse
    exit 1
fi

# TEST: Check if app-resolve works using entity search
echo "Checking to see if app-resolve works...."
auth=$(curl -k -X POST -F 'username=quantexa-demo' -F 'password=xPlor3r' https://10.4.0.100/authenticate)
entityResponse=$(curl -k 'https://10.4.0.100/entity-search/search' -H 'Pragma: no-cache' -H 'Origin: https://10.4.0.100' -H 'Accept-Encoding: gzip, deflate, br' -H 'Accept-Language: en-GB,en-US;q=0.9,en;q=0.8' -H "Authorization: Bearer $auth" -H 'Content-Type: application/json' -H 'Accept: application/json, text/plain, */*' -H 'Cache-Control: no-cache' -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36' -H 'Connection: keep-alive' -H 'Referer: https://10.4.0.100/ui/' -H 'Expires: -1' --data-binary '{"queryString":null,"scoreSetName":"default","expansionForScoring":"default-entity","scoresFilters":{"scoreIds":[],"scoreTags":["ui-entity-search"]},"entitiesToResolve":["business","individual"],"dataSources":["customer","hotlist"],"searchParameters":[{"parameterName":"surname","parameterValue":"Greene","searchType":{"type":"FuzzyQuery"}},{"parameterName":"forenames","parameterValue":"Michael","searchType":{"type":"FuzzyQuery"}}],"searchFacets":{"customer":[],"hotlist":[]}}' --compressed)
entities=$(echo $entityResponse | jq -r .entities.individual[0].entityId)

if [[ "$entities" = "null" ]] || [[ -z $entities ]]; then
    echo "RESOLVER TEST FAILED"
    exit 1
else
    echo "RESOLVER TEST PASSED"
    echo $entities
fi
