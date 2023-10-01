import json
import os
from pathlib import Path
import subprocess


class GitBunkCloner:
    def __init__(self, initial_repos, initial_destination_directory):
        self.repos = initial_repos
        self.destination_directory = initial_destination_directory

    @staticmethod
    def clone_repo(repo_clone_url, target_dir):
        command = ['git', 'clone', repo_clone_url, target_dir]
        try:
            subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True
            )
        except Exception as e:
            print(f'Error cloning {repo_clone_url}: {e}')

    def clone_repos(self):
        for repo_url, repo_clone_url in self.repos.items():
            new_repo_name = repo_url.replace('/', '_')
            self.clone_repo(
                repo_clone_url=repo_clone_url,
                target_dir=Path(os.getcwd()) / "_github_repos_" / new_repo_name
            )


if __name__ == "__main__":
    # Select top 10 repos.
    current_directory = Path(os.getcwd())
    file_path = current_directory / "_github_api_results_" / "results_merged_and_sorted.json"
    with open(file_path, 'r') as f:
        repos = json.load(f)
        repos = dict(
            [(url, metadata['clone_url']) for url, metadata in repos.items()][:100]
        )

    # Initialize a GitBunkCloner.
    git_bunk_cloner = GitBunkCloner(
        repos,
        current_directory / "_github_repos_"
    )

    # Clone.
    git_bunk_cloner.clone_repos()
