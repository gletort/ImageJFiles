# Choose Roi

 * [Best Roi by z-slice](#select-best-roi)
 * [Installation](#installation)

## Select best Roi
When there are several Rois by z-slice, select only one "best" Roi for each slice.

### Biggest area
Select for each z-slice the Roi that have the highest area among all Rois that are present in each z-slice.
Delete the non-selected Rois.

### Smallest area
Select for each z-slice the Roi that have the smallest area among all Rois that are present in each z-slice.
Delete the non-selected Rois.

### Circularity
Keep all Rois that have circularity between two given values, asked in a prompted dialog.


## Installation
Copy the ```.jar``` file in the plugins forlder of ImageJ (e.g. ```~/.imageJ/plugins``` on linux) and restart ImageJ.
Or compile and install from sources (```.java``` files).
