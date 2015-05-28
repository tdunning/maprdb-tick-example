
# Instructions

## Get it

    git clone https://github.com/vicenteg/maprdb-tick-example.git

## Build it

    ./build.sh

## Configure it

Edit the `table_refresh.sh` script to your liking.

## Generate some data

Assuming log-synth is installed to /opt, this generates 1 billion rows of phony tick data with real symbols from
NASDAQ. Modify the output directory to your liking. This will take a long time. You can cut this down to 100 million
or less if you like.

    /opt/log-synth/synth \
        -schema src/test/resources/eoddata.json \
        -count $((1 * 10**9)) \
        -format json \
        -threads 100 \
        -output /mapr/se1/user/vgonzalez/eoddata/2015-05-18

## Create some CSV files

Have a look at the ruby script `scripts/split_by_symbol.rb`. This will look at the symbols list and generate some SQL
for use in Drill. The SQL will take your generated data, and use CTAS to create multiple CSV files with data for
multiple symbols. The number of files is hard-coded, and can be adjusted by editing the ruby script.

Run the script like this. Note the paths. It is important to run it as written here, because the ruby script uses
relative paths to get to the symbol list:

    $ ruby scripts/split_by_symbol.rb

Run the script, have a look at the output. You will want to redirect the output to a file, then add something like
the following at the top:

```
use maprfs.vgonzalez;
alter session set `store.format`='csv';
```

You will need to have set up your storage plugin in Drill (exercise for the reader).

## Run it

    ./table_refresh.sh

At the end of the run, you can try this Drill query (or something like it):

    use maprfs.vgonzalez;
    select convert_from(row_key, 'UTF8') as row_key, convert_from(convert_from(t.cf1.data, 'UTF8'), 'JSON') as data from `ticks` t limit 1;

You should get back something intelligible.