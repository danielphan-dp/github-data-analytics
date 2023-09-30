import os
from pathlib import Path
import subprocess


class GitRepoParser:
    def __init__(self, initial_github_repos_dir=Path(""), initial_github_repo_name=Path("")):
        self.github_repos_dir = initial_github_repos_dir
        self.github_repo_name = initial_github_repo_name
        self.total_loc = None
        self.test_loc = None

    def compute_total_loc(self):
        self.total_loc = 0

    def compute_test_loc(self):
        self.test_loc = 0

    def print_analytics(self):
        print(f"{self.github_repo_name} | {self.total_loc} | {self.test_loc}")


if __name__ == '__main__':
    github_repos_dir = Path(os.getcwd()) / "_github_repos_"
    github_repos = os.listdir(github_repos_dir)

    # Create parsers for each repo. Each repo will have 1 parser.
    parsers = [GitRepoParser()] * len(github_repos)
    for i, github_repo_name in enumerate(github_repos, start=0):
        github_repo_absolute_path = github_repos_dir / github_repo_name
        parsers[i] = GitRepoParser(github_repos_dir, github_repo_name)
        parsers[i].compute_total_loc()
        parsers[i].compute_test_loc()

    # Print some analytics.
    print("Repo | Total Loc | Test Loc")
    for parser in parsers:
        parser.print_analytics()
