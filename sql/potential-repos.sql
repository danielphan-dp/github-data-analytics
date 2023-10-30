WITH
    -- Temp Table: All repositories containing Java codes.
    JavaRepos AS
        (SELECT DISTINCT l.repo_name
         FROM `bigquery-public-data.github_repos.languages` l,
              UNNEST(l.language) AS lang
         WHERE lang.name = 'Java'),

    -- Temp Table: Repositories with commits during a given time period.
    RecentCommits AS
        (SELECT DISTINCT repo
         FROM `bigquery-public-data.github_repos.commits ` c,
              UNNEST(c.repo_name) AS repo
         WHERE DATE_DIFF(CURRENT_DATE
                   , DATE(TIMESTAMP_SECONDS(c.committer.date.seconds))
                   , YEAR) >= 0
           AND DATE_DIFF(CURRENT_DATE
                   , DATE(TIMESTAMP_SECONDS(c.committer.date.seconds))
                   , YEAR) <= 1)

SELECT f.repo_name,
       SUM(c.size) AS total_loc,
       SUM(CASE
               WHEN REGEXP_CONTAINS(LOWER(f.path), r '\btest\b')
                   THEN c.size
               ELSE 0
           END)    AS test_loc

FROM `bigquery-public-data.github_repos.files` f
         JOIN JavaRepos jr ON f.repo_name = jr.repo_name
         JOIN RecentCommits rc ON f.repo_name = rc.repo
         LEFT JOIN `bigquery-public-data.github_repos.contents` c ON f.id = c.id

WHERE f.path = 'pom.xml'
GROUP BY f.repo_name;