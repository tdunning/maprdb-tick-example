#!/bin/sh -ex

TABLE_PATH="/user/vgonzalez/ticks"

function join { local IFS="$1"; shift; echo "$*"; }

function new_table {
    splits="['AAIT','ATRO','CEMI','CZNC','FDML','HBNC','JUNO','MLVF','OVLY','RDUS','SPAN','UCBI','ZIOP',]"
    maprcli table delete -path "$TABLE_PATH"
#    maprcli table create -path "$TABLE_PATH"
#    maprcli table cf create -path "$TABLE_PATH" -cfname cf1
    cat <<EOF | hbase shell
create '$TABLE_PATH', 'cf1', { SPLITS => $splits }
EOF
}


CP=`join : $(ls target/jackson*.jar target/HBaseExample*.jar target/async-*jar target/asynchbase*.jar)`
#for SYNC_OR_ASYNC in async sync; do
for SYNC_OR_ASYNC in async ; do
    #echo "100K rows: $SYNC_OR_ASYNC"
    #new_table
    #java -cp $CP:`hbase classpath` com.mapr.hadoop.HBaseExample "$TABLE_PATH" "/mapr/se1/user/vgonzalez/s20150518_100K/0_0_0.csv" "$SYNC_OR_ASYNC"

    #echo "1 million rows: $SYNC_OR_ASYNC"
    #new_table
    #java -cp $CP:`hbase classpath` com.mapr.hadoop.HBaseExample "$TABLE_PATH" "/mapr/se1/user/vgonzalez/s20150518_1M/0_0_0.csv" "$SYNC_OR_ASYNC"

    echo "10 million rows: $SYNC_OR_ASYNC"
    new_table
    java -cp $CP:`hbase classpath` com.mapr.hadoop.HBaseExample "$TABLE_PATH" "/mapr/se1/user/vgonzalez/s20150518_10M/0_0_0.csv" "$SYNC_OR_ASYNC"

    echo "100 million rows: $SYNC_OR_ASYNC"
    new_table
    java -cp $CP:`hbase classpath` com.mapr.hadoop.HBaseExample "$TABLE_PATH" "/mapr/se1/user/vgonzalez/s20150518/0_0_0.csv" "$SYNC_OR_ASYNC"
done
