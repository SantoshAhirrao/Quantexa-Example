if [ -z $configVersion ]
then
    configVersion=dev
fi

echo -e "\e[32mImport Customer Raw to Parquet\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.customer.ImportRawToParquet -e $configVersion -r etl.customer
echo -e "\e[32mImport Hotlist Raw to Parquet\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.hotlist.ImportRawToParquet -e $configVersion -r etl
echo -e "\e[32mImport Transaction Raw to Parquet\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.transaction.ImportRawToParquet -e $configVersion -r etl.transaction
echo -e "\e[32mImport Country Corruption Raw to Parquet\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.countrycorruption.ImportRawToParquet -e $configVersion -r etl -l metricsConfig.conf
echo -e "\e[32mImport High Risk Country Codes Raw to Parquet\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.highriskcountrycodes.ImportRawToParquet -e $configVersion -r etl -l metricsConfig.conf
echo -e "\e[32mImport Postcode Prices Raw to Parquet\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.postcodeprices.ImportRawToParquet -e $configVersion -r etl -l metricsConfig.conf

echo -e "\e[32mCreate Case Class Customer\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.customer.CreateCaseClass -e $configVersion -r etl.customer
echo -e "\e[32mCreate Case Class Hotlist\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.hotlist.CreateCaseClass -e $configVersion -r etl
echo -e "\e[32mCreate Case Class Transaction\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.transaction.CreateCaseClass -e $configVersion -r etl.transaction

echo -e "\e[32mCleanse Case Class for Customer\e[0m"
./runQSSWithLibpostal.sh -s com.quantexa.example.etl.projects.fiu.customer.CleanseCaseClass -e $configVersion -r etl.customer
echo -e "\e[32mCleanse Case Class for Hotlist\e[0m"
./runQSSWithLibpostal.sh -s com.quantexa.example.etl.projects.fiu.hotlist.CleanseCaseClass -e $configVersion -r etl
echo -e "\e[32mCleanse Case Class for Transaction\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.transaction.CleanseCaseClass -e $configVersion -r etl.transaction

echo -e "\e[32mCreate Aggregated Transaction Document\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.transaction.CreateAggregationClass -e $configVersion -r etl.transaction

echo -e "\e[32mCreate Compounds for Customer\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.customer.CreateCompounds -e $configVersion -r compounds.customer
echo -e "\e[32mCreate Compounds for Hotlist\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.hotlist.CreateCompounds -e $configVersion -r compounds.hotlist
echo -e "\e[32mCreate Compounds for Transaction\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.transaction.CreateCompounds -e $configVersion -r compounds.transaction

echo -e "\e[32mLoad Elastic for Customer\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.customer.LoadElastic -r elastic.customer --config-files external.conf
echo -e "\e[32mLoad Elastic for Hotlist\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.hotlist.LoadElastic -r elastic.hotlist --config-files external.conf
echo -e "\e[32mLoad Elastic for Transaction\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.transaction.LoadElastic -r elastic.transaction --config-files external.conf
echo -e "\e[32mLoad Elastic for Research\e[0m"
./runQSS.sh -s com.quantexa.example.etl.projects.fiu.intelligence.CreateElasticIndices -r elastic.research --config-files external.conf


