/***
 * Macro to convert all czi files of a directory to tif files and 8-bit
 * 
 * @author Gaelle Letort, CNRS/Institut Pasteur
 * @date 04/03/2022
 */

to8bit = true; // set to false to not convert to 8-bit

requires("1.48");
run("Bio-Formats Macro Extensions");
setBatchMode(true);

inDir = getDirectory("Choose images directory");
list = getFileList(inDir);

// Create a directory to save the converted files in
outDir = inDir + "ConvertedFiles"+ File.separator();
if (!File.isDirectory(outDir)) {
	File.makeDirectory(outDir);
}

for(i = 0; i < list.length; i++) {
	// for all czi or nd files 
	if ((endsWith(list[i], ".czi")) ) {
		file = inDir + list[i];	
		rootName = substring(list[i],0,indexOf(list[i],".czi")); // get file name without extension
		// open file and convert to 8-bit
		run("Bio-Formats Importer", "open=[&file] autoscale color_mode=Default");
		if (to8bit) { run("8-bit"); }
		saveAs("Tiff", outDir+rootName+".tif");
		close();
      }
}

setBatchMode(false);
showStatus("Finished: "+list.length+" files converted");
