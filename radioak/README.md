# Radioak ImageJ plugin   [![DOI](https://zenodo.org/badge/169780722.svg)](https://zenodo.org/badge/latestdoi/169780722)

This plugin extracts the values of the radius of a given Region of Interest (ROI) at `n` angles around its center. `n` is a parameter to choose in the dialog interface that opens when running the plugin. It allows to quantifiy the shape fluctuations and offers an option to visualize these dynamics.

To install it, download and put the `Radioak_.jar` file in Fiji/imageJ `plugins` folder and restart Fiji.

## Updates:
* 28/09/2022: Add intensity measurement with each radius

* 22/11/2024: Add option to calculate the number of angle to get a constant arc length for each movie.
Check the parameter `Use_arc_length` to use this option: in that case, the parameter `number_of_angle` will be ignored and calculated from the first contour perimeter, divided by the desired arc length defined in `arc_length_(pixels)`.


