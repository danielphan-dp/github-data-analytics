import os
from collections import defaultdict
from pathlib import Path
import subprocess

from tqdm import tqdm


class GitRepoParser:
    def __init__(self, initial_github_repo_dir=Path(""), initial_github_repo_name=Path("")):
        self.github_repo_dir = initial_github_repo_dir
        self.github_repo_name = initial_github_repo_name
        self.total_loc = None
        self.language_loc = defaultdict(lambda: 0)
        self.test_loc = None

    def compute_total_loc(self):
        # Step 1: Get the list of all files inside a directory.
        command = ['git', 'ls-files']
        result = subprocess.run(
            command,
            cwd=self.github_repo_dir / self.github_repo_name,
            stdout=subprocess.PIPE,
            text=True
        )
        files = result.stdout.split('\n')

        # Step 2: For each file in the given list, compute line count then add to total_loc.
        total_loc = 0
        for file in files:
            file_absolute_path = self.github_repo_dir / self.github_repo_name / file
            if os.path.isfile(
                    file_absolute_path):  # Make sure this is a file, otherwise os will raise 'Permission Denied'
                with open(
                        file_absolute_path,
                        'r',
                        encoding='utf-8',
                        errors='ignore'
                ) as f:
                    total_loc += len(f.readlines())

        # Step 3: Update total_loc.
        self.total_loc = total_loc

    def compute_language_loc(self, language='Java'):
        # TODO: Use namespace for languages.
        language_loc = 0

        if language == 'Java':
            for file in Path(self.github_repo_dir / self.github_repo_name).glob('**/*.java'):
                if os.path.isfile(file):
                    with open(file, 'r', encoding='utf-8', errors='ignore') as f:
                        language_loc += len(f.readlines())
            self.language_loc['Java'] = language_loc

        else:  # Not supported yet.
            # TODO: Implement general logic to handle all languages.
            pass

    def compute_test_loc(self):
        test_loc = 0
        for file in Path(self.github_repo_dir / self.github_repo_name).glob('**/*test*.java'):
            if os.path.isfile(file):
                with open(file, 'r', encoding='utf-8', errors='ignore') as f:
                    test_loc += len(f.readlines())
        self.test_loc = test_loc

    def compute_metrics(self):
        pass

    def print_metrics(self):
        print(
            f"{self.github_repo_name} | "
            f"{self.total_loc} | "
            f"{self.language_loc['Java']} | "
            f"{self.test_loc}"
        )


if __name__ == '__main__':
    github_repos_dir = Path(os.getcwd()) / "_github_repos_"
    github_repos = os.listdir(github_repos_dir)

    # Create parsers for each repo. Each repo will have 1 parser.
    parsers = [GitRepoParser()] * len(github_repos)
    for i, github_repo_name in enumerate(tqdm(github_repos, desc="Processing GitHub Repos"), start=0):
        github_repo_absolute_path = github_repos_dir / github_repo_name
        parsers[i] = GitRepoParser(github_repos_dir, github_repo_name)
        parsers[i].compute_total_loc()
        parsers[i].compute_language_loc('Java')
        parsers[i].compute_test_loc()

    # Print some analytics.
    print("Repo | Total Loc | Java Loc | Test Loc")
    for parser in parsers:
        parser.print_metrics()
