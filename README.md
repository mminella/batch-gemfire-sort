# Spring Batch File Sort With Gemfire
This project is intended to be a proof of concept (PoC) on using Apache Geode/Pivotal Gemfire as an in memory distributed data structure to be able to do high performance computations on in a traditional batch environment.

In order to generate the input files, `gensort` is used.  You can find it via the following locations:
* The original distribution (available for Linux or Windows: http://www.ordinal.com/gensort.html
* An updated version for OS X: https://github.com/scslab/bad

For intial test set of data (1GB across 10 files) I used the following commands:
```
gensort -b0 1050000 part0
gensort -b1050000 1050000 part1
gensort -b2100000 1050000 part2
gensort -b3150000 1050000 part3
gensort -b4200000 1050000 part4
gensort -b5250000 1050000 part5
gensort -b6300000 1050000 part6
gensort -b7350000 1050000 part7
gensort -b8400000 1050000 part8
gensort -b9450000 1050000 part9
```

To verify the results, use the following commands:
```
valsort -o out0.sum output0.dat
valsort -o out1.sum output1.dat
valsort -o out2.sum output2.dat
valsort -o out3.sum output3.dat
valsort -o out4.sum output4.dat
valsort -o out5.sum output5.dat
valsort -o out6.sum output6.dat
valsort -o out7.sum output7.dat
valsort -o out8.sum output8.dat
valsort -o out9.sum output9.dat
cat out0.sum out1.sum out2.sum out3.sum out4.sum out5.sum out6.sum out7.sum out8.sum out9.sum > all.sum
valsort -s all.sum 
```

# TODO
1. Write file from local partition. - This _may_ be done.  The file size isn't making sense though...
2. Convert to LRPs instead of tasks. - DONE
2. Upload files to S3.
3. Download input files from S3.
4. Run on PCF
5. Scale up
