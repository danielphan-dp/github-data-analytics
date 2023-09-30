import json
import os
from pathlib import Path


class JsonSorter:

    def __init__(self, file_name):
        self.file_name = file_name
        self.sorted_file_contents = self.load_file()

    def load_file(self):
        try:
            with open(self.file_name, 'r') as f:
                return json.load(f)
        except IOError as e:
            print(f"Error reading {self.file_name}: {e}")
            return {}

    def sort_file(self):
        self.sorted_file_contents = dict(
            sorted(
                self.sorted_file_contents.items(),
                key=lambda x: x[1]["stargazers_count"],
                reverse=True
            )
        )

    def save_to_file(self, output_file=None):
        if output_file is None:
            output_file = self.file_name
        try:
            with open(output_file, 'w') as f:
                json.dump(self.sorted_file_contents, f, indent=4)
        except IOError as e:
            print(f"Error writing to {output_file}: {e}")


if __name__ == '__main__':
    # Create the path to the directory containing the to-be-sorted JSON file.
    current_directory = Path(os.getcwd())
    json_file_directory = current_directory / "_github_api_results_" / "results_merged.json"

    # Sort file.
    json_merger = JsonSorter(json_file_directory)
    json_merger.sort_file()

    # Save results.
    json_merger.save_to_file(current_directory / "_github_api_results_" / "results_merged_and_sorted.json")
