### GitHub Repos Data Analytics

[Official Website](https://console.cloud.google.com/marketplace/product/github/github-repos?pli=1&project=friendly-medley-356508)

#### Description (From the Official Site)

GitHub is how people build software and is home to the largest community of open source developers in the 
world, with over 12 million people contributing to 31 million projects on GitHub since 2008.

This 3TB+ dataset comprises the largest released source of GitHub activity to date. It contains a full snapshot of the 
content of more than 2.8 million open source GitHub repositories including more than 145 million unique commits, over 2 
billion different file paths, and the contents of the latest revision for 163 million files, all of which are searchable 
with regular expressions.

This public dataset is hosted in Google BigQuery and is included in BigQuery's 1TB/mo of free tier processing. This 
means that each user receives 1TB of free BigQuery processing every month, which can be used to run queries on this 
public dataset. Watch this short video to learn how to get started quickly using BigQuery to access public datasets."

#### Notes

- Need Google Cloud account and credentials.

- Both GitHub Activity Data (GoogleBigQuery) and GitHub API are used.

- It is important to decide which part of the data pipeline to be processed using SQL and which part to be processed
using Python Scrips (and perhaps utilizing existing libraries as well as custom data structures).

#### Input and Output

Input: The datasource containing a snapshot of data collected from ~2.9 millions public GitHub repos.

Output: Useful data for training machine learning models.

#### Requirements & Tasks:

- [ ] Capable of selecting the N repos in **star_count** and **watch_count.**

  - For examples, top 100 repos in star_count or watch_count.

- [ ] Capable of selecting the repos that have commits within a time period.

  - For examples, having commits during the last year only.

- [ ] Capable of selecting only repos that have file `pom.xml` (build configuration file for the Maven build system)

- [ ] Capable of computing some metrics:
  - `total_loc` (sum of lines of code in all files)
  - `test_loc` (sum of lines of code in  test files, i.e., whose file name/path contains the case-insensitive word "test")