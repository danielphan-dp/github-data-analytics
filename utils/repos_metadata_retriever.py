import json
import os
from pathlib import Path
import requests


class ReposMetadataRetriever:
    def __init__(self, file_path, github_token):
        self.file_path = file_path
        self.data = self.read()
        self.repos_metadata = {}

    def read(self):
        with open(self.file_path, 'r') as file:
            data = json.load(file)
        return data

    def fetch_metadata(self):
        # google_apis_repos = [repo for repo in self.data if "googleapis/" in repo['repo_name']]
        repos = self.data

        for repo in repos:
            # Retrieve username and repo name from repo.
            temp = repo['repo_name'].split('/')
            org = temp[0]
            repo = temp[1]

            # Create the url.
            url = f"https://api.github.com/repos/{org}/{repo}"

            # Create headers with the GitHub API access token.
            headers = {}
            if github_token:
                headers['Authorization'] = f'token {github_token}'

            # Make API request to get data of current repo.
            response = requests.get(url, headers=headers)
            response_json = response.json()

            self.repos_metadata[f"{org}/{repo}"] = response_json

            # Perform extra parsing with the http response.
            if response.status_code == 200:
                print(org, repo, response_json.get("stargazers_count", 0))
            else:
                print(f"Error {response.status_code}: {response.json().get('message')}")

        # Sort them based on stargazers_count
        self.repos_metadata = dict(sorted(
            self.repos_metadata.items(),
            key=lambda x: x[1]["stargazers_count"],
            reverse=True
        ))

    def print_metadata(self):
        print(self.repos_metadata)

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
    repos_metadata_retriever.fetch_metadata()
    repos_metadata_retriever.print_metadata()

    # Save fetched results to file.
    repos_metadata_retriever.save_data_to_json('./results.json')
