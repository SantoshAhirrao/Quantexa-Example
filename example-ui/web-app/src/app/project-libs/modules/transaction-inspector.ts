import {
    TransactionInspectorLib,
    QueryFieldType,
    QueryOperator,
    QuickFilter,
    ProjectLib as CoreLib,
    AggregationSize,
    AggregationChartType,
    AggregationDefinition
} from '@quantexa/explorer-web-core/ui-transaction-inspector';


const quickFilters: QuickFilter[] = [
    {
        name: 'Michael or Greene',
        filters: [
            {
                conditions: [
                    {
                        op: QueryOperator.CONTAINS,
                        value: 'michael'
                    },
                    {
                        op: QueryOperator.CONTAINS,
                        value: 'greene'
                    }
                ]
            }
        ]
    },
    {
        name: 'Since 2016',
        filters: [
            {
                conditions: [
                    {
                        field: 'posting_dt',
                        type: QueryFieldType.DATE,
                        startOp: QueryOperator.MORE_THAN,
                        start: QuickFilterLastMonthStartValueProvider
                    }
                ]
            }
        ]
    }
];

export function QuickFilterLastMonthStartValueProvider() {
    return '2016-01-01';
}

const aggregations: AggregationDefinition[] = [
    {
        name: 'Transactions Over Time',
        data: {
            count: {
                name: 'Txn Over Time Month',
                path: [ 'count' ]
            },
            amount: {
                name: 'Txn Over Time Month',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.TWO_THIRDS,
        chartConfig: {
            count: {
                type: AggregationChartType.LINE,
                yAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.LINE,
                yAxisFormat: '~s'
            }
        }
    },
    {
        name: 'Top Beneficiaries',
        data: {
            count: {
                name: 'Top Count Beneficiaries',
                path: [ 'count' ]
            },
            amount: {
                name: 'Top Amount Beneficiaries',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.ONE_THIRD,
        chartConfig: {
            count: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            }
        }
    },
    {
        name: 'Top Originators',
        data: {
            count: {
                name: 'Top Count Originators',
                path: [ 'count' ]
            },
            amount: {
                name: 'Top Amount Originators',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.ONE_THIRD,
        chartConfig: {
            count: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            }
        }
    },
    {
        name: 'Top Beneficiary Countries',
        data: {
            count: {
                name: 'Top Count Beneficiary Countries',
                path: [ 'count' ]
            },
            amount: {
                name: 'Top Amount Beneficiary Countries',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.ONE_THIRD,
        chartConfig: {
            count: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            }
        }
    },
    {
        name: 'Top Originator Countries',
        data: {
            count: {
                name: 'Top Count Originator Countries',
                path: [ 'count' ]
            },
            amount: {
                name: 'Top Amount Originator Countries',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.ONE_THIRD,
        chartConfig: {
            count: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            }
        }
    },
    {
        name: 'Top Counterparties',
        data: {
            count: {
                name: 'Top Count Counterparties',
                path: [ 'count' ]
            },
            amount: {
                name: 'Top Amount Counterparties',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.ONE_THIRD,
        chartConfig: {
            count: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            }
        }
    },
    {
        name: 'Transaction Type',
        data: {
            count: {
                name: 'Txn Type',
                path: [ 'count' ]
            },
            amount: {
                name: 'Txn Type',
                path: [ 'subAggregations', 'txTotals' ]
            }
        },
        size: AggregationSize.ONE_THIRD,
        chartConfig: {
            count: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            },
            amount: {
                type: AggregationChartType.ROW,
                xAxisFormat: '~s'
            }
        }
    }
];

const presets = [
    {
        name: 'Default',
        columns: [
            'txn_source_typ_cde',
            'orig_acc_no',
            'orig_name',
            'orig_ctry',
            'bene_acc_no',
            'bene_name',
            'bene_bank_ctry',
            'txn_amt_fipl_norm',
            'posting_dt',
            'txn_desc'
        ]
    },
    {
        name: 'My preset',
        columns: [
            'orig_name',
            'posting_dt',
            'txn_amt_fipl_norm',
            'txn_desc',
            'txn_source_typ_cde'
        ]
    }
];

export const lib: TransactionInspectorLib = {
    ...CoreLib,
    configurationName: 'aggregatedwire',
    pageSizes: [25, 50, 75, 100],
    quickFilters: quickFilters,
    aggregations: aggregations,
    presets: presets
};
