#!/bin/sh

/usr/local/bin/etcd \
-name etcd \
-advertise-client-urls http://$HOSTNAME:2379,http://$HOSTNAME:4001 \
-listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001 \
-initial-advertise-peer-urls http://$HOSTNAME:2380 \
-listen-peer-urls http://0.0.0.0:2380 \
-initial-cluster-token etcd-cluster-1 \
-initial-cluster etcd=http://$HOSTNAME:2380 \
-initial-cluster-state new
