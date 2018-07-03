#!/usr/bin/env bash
DATA_DIR=src/main/resources/data
for i in `seq 0 9`;
do
   valsort -o $DATA_DIR/out$i.sum $DATA_DIR/output$i.dat
done
cat out0.sum out1.sum out2.sum out3.sum out4.sum out5.sum out6.sum out7.sum out8.sum out9.sum > all.sum
valsort -s all.sum



