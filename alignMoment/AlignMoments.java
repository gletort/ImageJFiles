/** 
 * \brief Plugin to align (translate and rotate) images based on their intensity moments
 *
 * \details For each z-slice, calculate the intensity moments, extract baricenter and main orientation values from the moments.
 * Translate the image to center it on the moments barycenter, and rotate it to get a 0 degrees main-orientation.
 *
 * \author G. Letort, College de France
 * \date created on 2019/02/28
 * */

package align;
import ij.*;
import ij.util.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.measure.*;
import java.io.*;
import java.util.*;
import java.awt.*;

public class AlignMoments implements PlugIn 
{
	ImagePlus imp;
	Calibration cal;
	boolean translate;
	boolean rotate;
	boolean stack;
	boolean print;
	boolean invisible;
	double pow; 

	public void getOptions()
	{
		GenericDialog gd = new GenericDialog("Align from image moments");
		gd.addCheckbox("Translate images (center them)", true);
		gd.addCheckbox("Rotate images (horizontal main orientation)", true);
		gd.addCheckbox("Process all stack", true);
		gd.addCheckbox("Show values", false);
		gd.addCheckbox("Batch mode (hide during processing)", false);
		gd.addNumericField("Noise reduction (1-15, non binary image)", 1, 1);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		translate = gd.getNextBoolean();
		rotate = gd.getNextBoolean();
		stack = gd.getNextBoolean();
		print = gd.getNextBoolean();
		invisible = gd.getNextBoolean();
		pow = gd.getNextNumber();
	}

	public void go()
	{
		if ( print )
			IJ.log("Slice \t CenterX \t CenterY \t Angle \n" );
		if (!stack)
			doOneImage(imp.getSlice());
		else
		{
			int nslices = imp.getNSlices();
			for (int z = 1; z <= nslices; z++)
			{
				IJ.showStatus( "Align slice "+z+"/"+nslices);
				IJ.showProgress( z, nslices );
				imp.setSlice(z);
				doOneImage(z);
			}
		}
	}
	
	public void doOneImage(int z)
	{
		OneImageMoments oneMoment = new OneImageMoments(imp);
		oneMoment.getMoments(pow);
		if ( print )
			oneMoment.printValues(z);
		if ( translate )
			oneMoment.translate();
		if ( rotate )
			oneMoment.rotate();

	}

	
	public void run(String arg) 
	{
		imp = IJ.getImage();

		// Measure everything in pixels
		cal = imp.getCalibration();
		if (cal == null ) 
		{
			cal = new Calibration(imp);
		}
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		cal.pixelDepth = 1;

		getOptions();

		if (invisible)
			imp.hide();
		go();
		imp.show();
	}		

}
