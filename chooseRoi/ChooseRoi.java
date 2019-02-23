/** 
 * \brief Plugin to select only Rois of interest by z-slice
 *
 * \details For each z-slice, when there are several Rois associated to that slice, select the "best" one (e.g. largest area)
 *
 * \author G. Letort, College de France
 * \date created on 2019/02/22
 * */

package chooser;
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

public class ChooseRoi implements PlugIn 
{
	ImagePlus imp;
	Calibration cal;

	/** \brief Select only one Roi by z-slice
	 *
	 * @param which Criteria to choose a winner Roi to keep by z (e.g. biggest area)*/
	public void keepRois( int which )
	{
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null || rm.getCount() == 0 )
		{
			IJ.error("No Rois in Manager");
			return;
		}

		rm.runCommand( imp, "Sort" );
		Roi[] allRois = rm.getRoisAsArray();
		int curz;
		int	wini = 0;
		int doingz = -1;
		double refarea;
		switch ( which )
		{
			case 0:
				refarea = 0;
				break;
			case 1:
				refarea = imp.getWidth()*imp.getHeight() + 2;
				break;
			default:
				refarea = 0;
				break;
		}
		double marea = refarea;
		Vector tokeep = new Vector();
		tokeep.clear();
		for ( int i = 0; i < allRois.length; i++ )
		{
			Roi curroi = allRois[i];
			curz = curroi.getPosition();
			if ( doingz == -1 ) doingz = curz;
			if ( curz > doingz )
			{
				tokeep.add( wini );
				marea = refarea;
				doingz = curz;
			}
			rm.select(i);
			double tmparea = imp.getStatistics().area; 
			switch ( which )
			{
				case 0:
					// biggest area wins
					if ( tmparea > marea )
					{
						wini = i;
						marea = tmparea;
					}
					break;
				case 1:
					// smallest area wins
					if ( tmparea < marea )
					{
						wini = i;
						marea = tmparea;
					}
					break;
				default:
					break;
			}
		}
		// add last z
		tokeep.add( wini );
		marea = 0;

		int ndel = allRois.length-tokeep.size();
		if ( ndel > 0 )
		{
			int[] dels = new int[ndel];
			int filled = 0;
			for ( int j = 0; j < allRois.length; j++ )
			{
				if ( !tokeep.contains(j) )
				{
					dels[filled] = j;
					filled = filled + 1;
				}
			}
			rm.setSelectedIndexes(dels);
			rm.runCommand("Delete");
		}
	}	
	
	public void circularRois()
	{
		GenericDialog gd = new GenericDialog("Circularity");
		gd.addNumericField("min_circularity:", 0.5, 2);
		gd.addNumericField("max_circularity:", 1, 2);

		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		double minc = gd.getNextNumber();
		double maxc = gd.getNextNumber();
		keepCircularRois( minc, maxc );
	}

	public void keepCircularRois( double circmin, double circmax )
	{
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null || rm.getCount() == 0 )
		{
			IJ.error("No Rois in Manager");
			return;
		}


		Roi[] allRois = rm.getRoisAsArray();
		int curz;
		Vector tokeep = new Vector();
		tokeep.clear();
		for ( int i = 0; i < allRois.length; i++ )
		{
			Roi curroi = allRois[i];
			curroi.setImage(imp);
			imp.setPosition(curroi.getPosition() );
			rm.select(i);
			ImageProcessor ip = imp.getProcessor();
			ip.setRoi( curroi.getPolygon() );
			ImageStatistics stats = ImageStatistics.getStatistics(ip, Measurements.AREA, cal);
			double perimeter = curroi.getLength();
			double circ = perimeter==0.0?0.0:4.0*Math.PI*(stats.area/(perimeter*perimeter));
			if ( circ >= circmin && circ <= circmax )
				tokeep.add( i );
		}

		int ndel = allRois.length-tokeep.size();
		if ( ndel > 0 )
		{
			int[] dels = new int[ndel];
			int filled = 0;
			for ( int j = 0; j < allRois.length; j++ )
			{
				if ( !tokeep.contains(j) )
				{
					dels[filled] = j;
					filled = filled + 1;
				}
			}
			rm.setSelectedIndexes(dels);
			rm.runCommand("Delete");
		}
	}	

	/** \brief Select one roi by z-slice (biggest area, smallest area)*/
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

		switch ( arg.toLowerCase() )
		{
			case "biggest": 
				keepRois(0);
				break;
			case "smallest": 
				keepRois(1);
				break;
			case "circ":
				circularRois();
				break;
			default:
				keepRois(0);
				break;
		}
	}		

}
