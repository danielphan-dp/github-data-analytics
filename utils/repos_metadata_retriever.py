import json
import os
import time
from tqdm import tqdm
from pathlib import Path
import requests


class ReposMetadataRetriever:
    def __init__(self, file_path, github_token):
        self.file_path = file_path
        self.github_token = github_token
        self.api_rate = 5000
        self.data = self.read()
        self.repos_metadata = {}

    def read(self):
        with open(self.file_path, 'r') as _file_:
            data = json.load(_file_)
        return data

    def fetch_metadata(self, start=0, end=10):
        """
        Fetch metadata of all repos in range [start, end].
        Then, output to the file "results_[start]_end.json".

        :param start: The start point of the region (inclusive).
        :param end: The end point of the region (exclusive).
        :return: None
        """

        # Boundaries check
        if not (0 <= start < len(self.data) and
                0 <= end < len(self.data) and
                start < end):
            print(f"Invalid range. Cannot fetch data for entries in range [{start} , {end})")
            return

        # Fetch data
        repos = self.data
        for repo in tqdm(repos[start: end], desc="Fetching Data from GitHub API"):
            # Retrieve username and repo name from repo.
            temp = repo['repo_name'].split('/')
            org = temp[0]
            repo = temp[1]

            # Create the url.
            url = f"https://api.github.com/repos/{org}/{repo}"

            # Create headers with the GitHub API access token.
            headers = {}
            if github_token:
                headers['Authorization'] = f'token {self.github_token}'

            # Make API request to get data of current repo.
            response = requests.get(url, headers=headers)
            response_json = response.json()

            # Perform extra parsing with the http response.
            if response.status_code == 200:  # OK.
                self.repos_metadata[f"{org}/{repo}"] = response_json
            elif response.status_code == 403:  # API Rate exceeded.
                sleep_time = (1 * 3600) + (1 * 60)
                print(f"Waiting {sleep_time}s until the GitHub API is ready again.")
                time.sleep(sleep_time)
            else:
                print(f"Error {response.status_code}: {response.json().get('message')} | Repo: {org}/{repo}")

        # Sort them based on stargazers_count
        self.repos_metadata = dict(sorted(
            self.repos_metadata.items(),
            key=lambda x: x[1]["stargazers_count"],
            reverse=True
        ))

        # Save the results to a JSON file
        json_file_name = f"./_github_api_results_/results_{start}_{end + 1}.json"
        try:
            with open(json_file_name, "a+") as outfile:
                json.dump(self.repos_metadata, outfile, indent=4)
        except IOError as e:
            print(f"Error occurred: {e}")

    def save_data_to_json(self, json_file_name):
        with open(json_file_name, "w") as outfile:
            json.dump(self.repos_metadata, outfile, indent=4)


if __name__ == "__main__":
    # Create path to JSON file contain a list of all repos to be working with.
    current_directory = Path(os.getcwd())
    file_path = current_directory.parent / "GitHub-Data-Analytics" / "_precomputed_results_" / "java_repos.json"

    # Load the environment variable.
    # TODO: Fix this to using load_env from a .env file. Running into error atm.
    github_token_file_path = current_directory.parent / "GitHub-Data-Analytics" / "config" / "env.json"
    github_token = None
    with open(github_token_file_path, 'r') as file:
        file_data = json.load(file)
        github_token = file_data['GITHUB_TOKEN']

    # Parse the repos.
    repos_metadata_retriever = ReposMetadataRetriever(
        file_path=file_path,
        github_token=github_token
    )

    # Print the JSON data using the handler
    repos_metadata_retriever.fetch_metadata(start=0, end=1000)

    # repos_metadata_retriever.fetch_metadata(start=0, end=1000)
    # repos_metadata_retriever.fetch_metadata(start=1000, end=2000)
    # repos_metadata_retriever.fetch_metadata(start=2000, end=3000)
    # repos_metadata_retriever.fetch_metadata(start=3000, end=4000)
    # repos_metadata_retriever.fetch_metadata(start=4000, end=5000)
    # repos_metadata_retriever.fetch_metadata(start=5000, end=6000)

    # TODO: Implement more efficient logic, since the GitHub API is limited to 5000 requests / hour.

    # Save fetched results to file.
    # repos_metadata_retriever.save_data_to_json('./results.json')
