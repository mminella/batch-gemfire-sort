#!/usr/bin/env bash

DATA_DIR=src/main/resources/data
if [ ! -d $DATA_DIR ]; then
	mkdir $DATA_DIR
fi
rm -f $DATA_DIR/*

CHUNK_SIZE=1050000

for i in `seq 0 9`;
do
   gensort -b"$(($i*$CHUNK_SIZE))" $CHUNK_SIZE $DATA_DIR/part$i
done    
