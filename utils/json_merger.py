import json
import os
from pathlib import Path


class JsonMerger:
    def __init__(self, files):
        self.files = files
        self.merged_file_contents = {}

    @staticmethod
    def load_file(file_name):
        try:
            with open(file_name, 'r') as f:
                return json.load(f)
        except IOError as e:
            print(f"Error reading {file_name}: {e}")
            return {}

    def merge_files(self):
        for file in self.files:
            data = self.load_file(file)
            self.merged_file_contents.update(data)

    def save_to_file(self, output_file):
        try:
            with open(output_file, 'a+') as f:
                json.dump(self.merged_file_contents, f, indent=4)
        except IOError as e:
            print(f"Error writing to {output_file}: {e}")


if __name__ == '__main__':
    # Create the path to the directory with
    current_directory = Path(os.getcwd())
    json_files_directory = current_directory / "_github_api_results_"

    # Find all files matching the pattern.
    files = list(json_files_directory.glob('results_*_*.json'))

    # Merge the files.
    json_merger = JsonMerger(files)
    json_merger.merge_files()

    # Save the merged content to separate files.
    json_merger.save_to_file(current_directory / "_github_api_results_" / "results_merged.json")
