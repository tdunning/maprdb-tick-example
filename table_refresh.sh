#!/bin/sh -ex

TABLE_PATH="/user/vgonzalez/ticks"
CFNAME="cf1"

NTHREADS=$(lscpu  | grep '^CPU(s):' | awk '{ print $2 }')

function join { local IFS="$1"; shift; echo "$*"; }

function new_table {
    maprcli table delete -path "$TABLE_PATH"

    # splits are hard coded. This is based on the NASDAQ symbol list.
    # 12 regions, may want to increase the number of regions (by resplitting the symbol list) with bigger
    # machines. The machine I'm currently using has 12 physical cores (24 threads).
    splits="['AAIT','ATRO','CEMI','CZNC','FDML','HBNC','JUNO','MLVF','OVLY','RDUS','SPAN','UCBI','ZIOP',]"

    cat <<EOF | hbase shell >/dev/null
    create '$TABLE_PATH', '$CFNAME', { SPLITS => $splits }
EOF
}

function run {
    java -cp $CP:`hbase classpath` com.mapr.hadoop.HBaseExample $*
}

CP=`join : $(ls target/jackson*.jar target/HBaseExample*.jar target/async-*jar target/asynchbase*.jar)`

echo "10 million rows"
new_table
run "$CFNAME" "$TABLE_PATH" "/mapr/se1/user/vgonzalez/s20150518_10M/0_0_0.csv" $NTHREADS
