import os
import pip
import argparse

try:
    import pandas as pd
except ImportError:
    pip.main(["install", "pandas"])
    import pandas as pd

class tc:
    def black(text):
        return f'\033[30m{text}\033[0m'
    def red(text):
        return f'\033[31m{text}\033[0m'
    def green(text):
        return f'\033[32m{text}\033[0m'
    def yellow(text):
        return f'\033[33m{text}\033[0m'
    def blue(text):
        return f'\033[34m{text}\033[0m'
    def magenta(text):
        return f'\033[35m{text}\033[0m'
    def cyan(text):
        return f'\033[36m{text}\033[0m'
    def white(text):
        return f'\033[37m{text}\033[0m'

def get_csv_fps(input_folder):
    csv_filepaths = []
    for root, _, files in os.walk(input_folder):
        for file in files:
            if file.endswith('.csv'):
                csv_filepaths.append(os.path.join(root, file))
    return csv_filepaths

def sort_filepaths(filepaths):
    # Create a dictionary to store file paths based on common parts of filenames
    files_dict = {}

    # Iterate through the list of file paths
    for filepath in filepaths:
        # Get the filename without the directory path
        filename = os.path.basename(filepath)
        # Determine if it's a "spots" or "tracks" file
        if "spots" in filename:
            # Remove "spots" from filename to get the common part
            common_part = filename.replace("spots", "")
            if common_part not in files_dict:
                files_dict[common_part] = [None, None, None]
            files_dict[common_part][0] = filepath
        elif "tracks" in filename:
            # Remove "tracks" from filename to get the common part
            common_part = filename.replace("tracks", "")
            if common_part not in files_dict:
                files_dict[common_part] = [None, None, None]
            files_dict[common_part][1] = filepath

    # Create a list of tuples from the dictionary
    sorted_filepaths = []
    for common_part, paths in files_dict.items():
        spots_path, tracks_path, _ = paths
        if spots_path:
            # Generate the output filepath
            output_filename = os.path.basename(spots_path).replace("spots", "merged")
            output_filepath = os.path.join(os.path.dirname(spots_path), output_filename)
            sorted_filepaths.append((spots_path, tracks_path, output_filepath))

    return sorted_filepaths

def parse_filter_param(param_str):
    try:
        lower_str, upper_str = param_str.split(";")
        lower = float('inf') if lower_str.lower() == 'inf' else float(lower_str)
        upper = float('inf') if upper_str.lower() == 'inf' else float(upper_str)
        return lower, upper
    except Exception as e:
        raise ValueError(f"Invalid format for --displacement_filter_params: '{param_str}'. Expected format: 'float_or_inf;float_or_inf'") from e

def create_merged_df (spots_fp, tracks_fp):
    # Load and process spots
    spots_df = pd.read_csv(spots_fp, low_memory=False)
    spots_df = spots_df.drop([0, 1, 2]) # Remove the first three rows after the column headers as they are just secondary headers, not useful values
    # Convert all the columns into numbers as datatypes
    for column in spots_df.columns:
        if column != 'LABEL':
            spots_df[column] = pd.to_numeric(spots_df[column])

    # Load and process tracks
    tracks_df = pd.read_csv(tracks_fp, low_memory=False)
    tracks_df = tracks_df.drop([0, 1, 2]) # Remove the first three rows after the column headers as they are just secondary headers, not useful values
    # Convert all the columns into numbers as datatypes
    for column in tracks_df.columns:
        if column != 'LABEL':
            tracks_df[column] = pd.to_numeric(tracks_df[column])


    # Group by TRACK_ID and calculate the average AREA for each group
    spots_areaAvg = spots_df.groupby('TRACK_ID')['AREA'].mean().reset_index()

    merged_df = pd.merge(tracks_df, spots_areaAvg, on='TRACK_ID', how='right')

    # Rename the column
    merged_df = merged_df.rename(columns={'AREA': 'AREA_AVERAGED'})

    # Add calculated columns:
    merged_df["AREA_AVERAGED_X2"] = merged_df["AREA_AVERAGED"] * 2
    merged_df["DISPLACEMENT_MINUS_AREA_AVERAGED_X2"] = merged_df["TRACK_DISPLACEMENT"] - merged_df["AREA_AVERAGED_X2"]

    #print(merged_df)
    # Export the data into a CSV
    #merged_df.to_csv(output_fp, index=False)
    return merged_df


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Script that merges spots table into tracks table (one per operation)."
    )
    parser.add_argument('--csvlist', required=True, type=str, help="\"spot table fp; tracks csv fp\"")
    parser.add_argument('--displacement_filter_params', required=False, default=None, type=str, help="Formatted as: \"lower limit; upper limit\". Values should be floats (demical numbers), or \"inf\" for no limit e.g. \"28.6, inf\" means any value above 28.6. Values are inclusive.")
    args = parser.parse_args()

# print(tc.cyan("\n\nEnter your folder containing spots + tracks files.\nEach file should be named [cell line number]_XXXX_[\"tracks\" or \"spots\"][integer].csv"))
# while True:
#     csv_folder = input(tc.yellow("\nInput folder: "))
#     if os.path.isdir(csv_folder):
#         break
#     else:
#         print(tc.red(f"ERROR {csv_folder} was not recognized as a folder"))

# csv_fps = get_csv_fps(csv_folder)

# Sort and display input CSVs 
# print(tc.cyan("\nFilepaths to merge:"))
# sorted_fps = sort_filepaths(csv_fps)
# for index, filepaths in enumerate(sorted_fps):
#     spots_fp, tracks_fp, output_fp = filepaths
#     spotname = os.path.basename(spots_fp)
#     trackname = os.path.basename(tracks_fp)
#     outputname = os.path.basename(output_fp)
    #print(f"{index + 1}. {spotname} + {trackname} --> {outputname}")

# Confirm CSV inputs
# confirmation = input(tc.yellow("Press Y to confirm, or any other key to exit: "))
# if confirmation.lower() != "y":
#     print(tc.red("User exit the script.\n"))

# Run merge and generate output CSVs
# for number, set in enumerate(sorted_fps):
    # spots_fp, tracks_fp, output_fp = set
    # merged_df = create_merged_df(spots_fp, tracks_fp)
    # merged_df.to_csv(output_fp, index=False)
    # print(tc.cyan(f"{number + 1}. Completed merge: {os.path.basename(output_fp)}"))

print("Starting Track-Spot Merger")

# Grab the CSV list string and replace the quotation marks
csv_file_string = args.csvlist
print(f"\tcsv_file_string: {csv_file_string}")
# print(csv_file_string)

csv_file_string.replace("\"", "")
# print("csv_file_string after quote replacement: ")
# print(csv_file_string)

# Get the source file paths
spots_fp, tracks_fp = csv_file_string.split(";")
# Sanity check
print(f"\t\tSpot Table Filepath:{spots_fp}\n\t\tTrack Table Filepath: {tracks_fp}")

# Create a new folder for the merged files, and the merged + displacement filtered files.
merged_output_folder = os.path.join(os.path.dirname(spots_fp), "merged")
os.makedirs(merged_output_folder, exist_ok=True)
merged_filtered_output_folder = os.path.join(os.path.dirname(spots_fp), "merged_filtered")
os.makedirs(merged_filtered_output_folder, exist_ok=True)
# merged_output_path = spots_fp.replace("_spottable_auto", "_merged_auto") # OLD

# New names for each file sourced from the old spot filepath basename
merged_filename = os.path.basename(spots_fp).replace("_spottable_auto", "_merged_auto")
merged_filtered_filename = os.path.basename(spots_fp).replace("_spottable_auto", "_merged_displacement_filtered_auto")

merged_output_path = os.path.join(merged_output_folder, merged_filename)
merged_filtered_output_path = os.path.join(merged_filtered_output_folder, merged_filtered_filename)
# merged_filtered_output_path = ""

if not os.path.isfile(spots_fp):
    print(f"\tERROR Track-Spot_Merger_Auto.py: {spots_fp} is not a valid filepath")
    exit(1)
if not os.path.isfile(tracks_fp):
    print(f"\tERROR Track-Spot_Merger_Auto.py: {tracks_fp} is not a valid filepath")
    exit(1)

# Output the merged file
merged_df = create_merged_df(spots_fp, tracks_fp)
merged_df.to_csv(merged_output_path, index=False)

# Add displacement filter and generate output
if args.displacement_filter_params is not None:
    # Tease out the limit values
    lower_limit, upper_limit = parse_filter_param(args.displacement_filter_params)
    print(f"\tdisplacement_filter_params:\n\t\tLower:{lower_limit}\n\t\tUpper: {upper_limit}")

    filtered_df = merged_df
    if lower_limit != float('inf'):  # lower limit is a real number
        filtered_df = filtered_df[filtered_df["TRACK_DISPLACEMENT"] >= lower_limit]
    if upper_limit != float('inf'):  # upper limit is a real number
        filtered_df = filtered_df[filtered_df["TRACK_DISPLACEMENT"] <= upper_limit]
        
    filtered_df.to_csv(merged_filtered_output_path, index=False)

else:
    print("\tWARNING: displacement_filter_params arguments were not provided. Track displacement filtered CSV will not be generated.")


# Validate the merge CSV
if os.path.isfile(merged_output_path):  
    print(f"\tSuccessfully completed merge: {merged_output_path}")
else:
    print(f"\tERROR Track-Spot_Merger_Auto.py: {merged_output_path} is not a valid filepath.")
    exit(1)

# Validate the merge filtered CSV
if os.path.isfile(merged_filtered_output_path):  
    print(f"\tSuccessfully completed merge filter: {merged_filtered_output_path}")
else:
    print(f"\tERROR Track-Spot_Merger_Auto.py: {merged_filtered_output_path} is not a valid filepath.")
    exit(1)


exit(0)

