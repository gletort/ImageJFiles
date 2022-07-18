# ImageJFiles   [![DOI](https://zenodo.org/badge/169780722.svg)](https://zenodo.org/badge/latestdoi/169780722)
plugins/macros for ImageJ/Fiji


 * [Visualisation 3D](#tilt-3D)
 * [Align stack](#align-stack)
 * [Select specific Rois](#choose-roi)
 * [Correct bleaching](#unbleach)
 * [Ellipsoid 3D (fit, measures)](#ellipsoid)
 * [LOCO EFA (shape complexity)](#loco-efa)
 * [Curvature (unsigned curvature)](#curvature)
 * [Moran index (spatial auto-correlation](#moran-index)
 * [Cortex segmentation (from cortex marker)](#cortex-segmentation)
 * [Radioak plugin (radius)](#radioak-plugin)
 * [Local texture (GLCM)](#local-texture)

## Tilt 3D
Visualisation of 3D stacks.

## Align stack
Center and rotate each slices of a stack based on image moments so that they will all get the same average position and orientation (global alignement). 
Works best on binary images. 
On non-binay images, adding weight to positive pixels (noise reduction option) helps to be less sensitive to background pixels, otherwise, highly biaised.


## Choose Roi

### Select "best" Roi
When there are several Rois by z-slice, select only one "best" Roi for each slice.
For example, select for each z-slice the Roi that have the highest area among all Rois that are present in each z-slice.
Delete the non-selected Rois.

### Keep specific Rois
Keep all Rois that have a desired property (e.g. circularity between two given values, orientation (angle of the Roi main direction) between two specific values.. ).


## UnBleach

Correct bleaching in a 2D temporal stack by histogram matching. Match all histograms to the first slice. 
Usefull for segmentation afterwards.

## Ellipsoid
Handle 3D ellisoid operations.
From a binary stack, calculate the 3D ellipsoid corresponding to the positive pixels. Print its characteristics: centroid position (Xc, Yc, Zc), semi-axis lengths (A, B, C), volume, and eigen vectors (axis directions) (vA, vB, vC). Draw the 3D ellipsoid on the input image for visualisation of the fit.

## Loco EFA
Measure characteristics of the shape complexity with the LOCO-EFA method (lobe contribution elliptic Fourier analysis), defined in [Sanchez-Corrales et al. (2018)](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5897594/).
Reconstruct the shape with 2 to 50 modes as Roi, and calculates the contribution mode, distance between the shapes and entropy of the shape.

## Curvature
Calculate unsigned curvature along a ROI.

## Moran index
Calculate Moran index (Moran 1950) on first Roi in RoiManager, with weights being 1 if pixel j inside k-neighbors of pixel i (k is a user defined parameter), 0 otherwise.

## Cortex segmentation
Segment cortex from fluorescent marker stack.
Look at local maxima on each angle around the center. 

## Radioak plugin
Extract the values of the radius of a given Roi at n angles around its center.
Allows to quantifiy the shape fluctuations. 
Offers option to visualize these dynamics.

## Local texture
Calculate one of the GLCM textures in a moving window of a given size. 
Output a new image with the value of the chosen texture in each of the moving window.
Windows move with step size in x and y of the parameter step, and calculate the texture in a neighborhood of given size. 
