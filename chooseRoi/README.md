# Choose Roi

 * [Best Roi by z-slice](#select-best-roi)
 * [Keep Roi according to criteria](#keep-roi)
 * [Pixels inside Rois](#pixels-inside-rois)
 * [Installation](#installation)

## Select best Roi
When there are several Rois by z-slice, select only one "best" Roi for each slice.

### Biggest area
Select for each z-slice the Roi that have the highest area among all Rois that are present in each z-slice.
Delete the non-selected Rois.

### Smallest area
Select for each z-slice the Roi that have the smallest area among all Rois that are present in each z-slice.
Delete the non-selected Rois.

### Best mean intensity
Select for each z-slice the Roi that have the maximal mean intensity among all Rois that are present in the slice.
Delete the non-selected Rois.

## Keep Roi

### Circularity
Keep all Rois that have circularity between two given values, asked in a prompted dialog.

### Orientation
Keep all Rois that have a main direction (of the contour, not intensities) between two given values, asked in a prompted dialog.

## Pixels inside Rois

### List Pixels inside Rois
List all the pixels that are inside each Roi currently in the RoiManager (inside the shape). 
Gives the number of the Roi in the Manager and the (X,y,Z) position of the pixels inside it.

## Installation
Copy the ```.jar``` file in the plugins forlder of ImageJ (e.g. ```~/.imageJ/plugins``` on linux) and restart ImageJ.
Or compile and install from sources (```.java``` files).
