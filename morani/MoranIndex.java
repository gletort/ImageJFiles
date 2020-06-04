
/** 
 * \brief Plugin to calculte Moran index (spatial autocorrelation), Moran 1950.
 *
 *
 * \author G. Letort, College de France
 * \date created on 2020/05/18
 * */

package imoran;
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

public class MoranIndex implements PlugIn
{
	ImagePlus imp;
	RoiManager rm;

	double ksize = 10;
	
	/** Be sure there s no calibration */
	public void initCalibration()
	{
		// Measure everything in pixels
		Calibration cal = imp.getCalibration();
		if (cal == null ) 
		{
			cal = new Calibration(imp);
		}
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		cal.pixelDepth = 1;
	}
	
	/** \brief Dialog window */
	public boolean getParameters()
	{

		GenericDialog gd = new GenericDialog("Options", IJ.getInstance() );
		Font boldy = new Font("SansSerif", Font.BOLD, 12);
		gd.addNumericField("kneighbor distance (pixel)", ksize, 1);
		gd.showDialog();
		if (gd.wasCanceled()) return false;

		ksize = gd.getNextNumber();
		return true;
	}

	public double distance( int x1, int y1, int x2, int y2 )
	{
		return Math.sqrt( Math.pow(x1-x2,2) + Math.pow(y1-y2,2) );
	}

	public void calcIndex()
	{
		double index = 0;
		double sumw = 0;
		double sumsq = 0;
		int n = 0;

		ImageProcessor ip = imp.getProcessor();	
		Roi roi = rm.getRoi(0);
		imp.setRoi(roi);
		ImageStatistics mystat = imp.getAllStatistics();
		double mu = mystat.mean; // mean value inside ROI

		Rectangle rec = roi.getBounds();
		// For each point inside the roi, xi
		for ( int xi = rec.x; xi < (rec.x+rec.width); xi++ )
		{
			for ( int yi = rec.y; yi < (rec.y+rec.height); yi++ )
			{
				if ( roi.contains(xi,yi) )
				{
					n++;
					double vi = ip.getPixel(xi,yi);
							sumsq += Math.pow(vi-mu,2);
					
					// Look for neighboring pixels inside ROI
					for ( int xj = (int) Math.max(rec.x, xi-ksize-1); xj <= (int) Math.min(rec.x+rec.width, xi+ksize+1); xj++ )
				{	
				for ( int yj = (int) Math.max(rec.y, yi-ksize-1); yj <= (int) Math.min(rec.y+rec.height, yi+ksize+1); yj++ )
				{
					// exclude outside ROI and self pixel
					if ( roi.contains(xj,yj) && ((xi!=xj)||(yi!=yj)))
					{
						// inside neighboring
						if ( distance( xi,yi, xj, yj) <= ksize )
						{
							sumw += 1;
							double vj = ip.getPixel(xj,yj);
							index += (vi-mu)*(vj-mu);
						}				
					}
				}
			}
			}
		}	
	}
	index = n/sumw * index/sumsq;
	
	IJ.log("Moran's index, neighboring distance "+ksize+" pixels :"+index);
	}

	public void run(String arg)
	{
		imp = IJ.getImage();
		rm = RoiManager.getInstance();
		if ( rm == null )
		{
			IJ.error("Must have a Roi to analyze");
		}
		getParameters();
		initCalibration();
		calcIndex();
	}
}

