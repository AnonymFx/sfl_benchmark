# Benchmark System for Static Feature Location
During our thesis 'Implementation and Evaluation of Static Feature Location in Practice', we developed a benchmarking system for static feature location techniqes.
This repository was created to share it with other researchers, who want to evaluate the performance of their algorithms.
The evaluation metrics that are calculated are: precision, recall, F1-measure and top-5 precision (precision for the top-5 results).


## Getting started
Currently there is no build of the benchmark system available, therefore you need to clone/download the sources to use it.
Make sure to also include [Apache's Common IO lib](https://commons.apache.org/proper/commons-io/) as dependency.

Classes that do not need to be included (necessarily) for you benchmarks:
	- TfidfFeatureLocation
	- TfidfConfiguration
	
For running benchmarks, use the `BenchmarkSuite` class.

## How to add benchmarks
Benchmarks are added by adding folders to the benchmark suite root folder used as parameter of the constructor of `BenchmarkSuite`.
The folder structure should look as follows:
```
Benchmarks
+-- Benchmark1
	+-- GoldSets
		+-- GoldSet1
		+-- GoldSet2
	+-- Queries
		+-- Query1
		+-- Query2
	+-- Sources
		+-- Source
		+-- Source2
+-- Benchmark2
	+-- ...
```
The benchmark system will iterate over all queries (textual descriptions of features) in the 'Queries' folder and try to find a matching gold set (expected results) and source.
If either the gold set or a source is not found, the system will skip the query when running the benchmark.
If only one source version exists for all queries, we also support a default source folder (named 'Source') as fallback for queries with no 'Source<ID>' directory.

The gold sets must contain a class in every line.

## How to add a Feature Location Technique
1. Let you feature location technique implement the interface `FeatureLocationTechnique`
2. Pass it as argument in the constructor of `BenchmarkSuite`


## I don't understand, more explanation pls!!1!
For a more detailed explanation, please look at the example provided in the `Main` class or refer to our paper. If you can't find the paper, contact the owner of this repository.
