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
		double refmes = 0;
		int mesure = 0;
		switch ( which )
		{
			case 0:
				refmes = 0;
				mesure = Measurements.AREA;
				break;
			case 1:
				refmes = imp.getWidth()*imp.getHeight() + 2;
				mesure = Measurements.AREA;
				break;
			case 2:
				refmes = 0;
				mesure = Measurements.MEAN;
				break;
			default:
				refmes = 0;
				mesure = Measurements.AREA;
				break;
		}
		double mmes = refmes;
		Vector tokeep = new Vector();
		tokeep.clear();
		for ( int i = 0; i < allRois.length; i++ )
		{
			Roi curroi = allRois[i];
			imp.setRoi(curroi);
			if ( imp.getNSlices() > 1 )
			{
				curz = curroi.getPosition();
				if ( doingz == -1 ) doingz = curz;
				if ( curz > doingz )
				{
					tokeep.add( wini );
					mmes = refmes;
					doingz = curz;
				}
			}
			rm.select(i);
			imp.setRoi(curroi);
			
			double tmpmes;
			switch ( which )
			{
				case 0:
					// biggest area wins
					tmpmes	= imp.getStatistics(mesure).area; 
					if ( tmpmes >= mmes )
					{
						wini = i;
						mmes = tmpmes;
					}
					break;
				case 1:
					// smallest area wins
					tmpmes	= imp.getStatistics(mesure).area; 
					if ( tmpmes <= mmes )
					{
						wini = i;
						mmes = tmpmes;
					}
					break;
				case 2:
					tmpmes	= imp.getStatistics(mesure).mean; 
					// best mean wins
					if ( tmpmes >= mmes )
					{
						wini = i;
						mmes = tmpmes;
					}
					break;
				default:
					break;
			}
		}
		// add last z
		tokeep.add( wini );

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
	
	public void rangeRois(String what)
	{
		GenericDialog gd = new GenericDialog(what);
		double dmin = 0.5;
		double dmax = 1;
		switch ( what )
		{
			case "circularity":
				dmin = 0.5;
				dmax = 1;
				break;
			case "angle":
				dmin = 0;
				dmax = 45;
				break;
			default:
				break;
		}
		gd.addNumericField("min_"+what, dmin, 2);
		gd.addNumericField("max_"+what, dmax, 2);

		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		double mini = gd.getNextNumber();
		double maxi = gd.getNextNumber();
		keepRangeRois( what, mini, maxi );
	}

	public void keepRangeRois( String what, double vmin, double vmax )
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
			double val = 0;
			ImageStatistics stats;	
			switch( what )
			{
				case "circularity":
					stats = ImageStatistics.getStatistics(ip, Measurements.AREA, cal);
					double perimeter = curroi.getLength();
					val = perimeter==0.0?0.0:4.0*Math.PI*(stats.area/(perimeter*perimeter));
					break;
				case "angle":
					stats = ImageStatistics.getStatistics(ip, Measurements.ELLIPSE, cal);
					val = stats.angle;
					break;
				default:
					val = 0;
					break;
			}
			
			if ( val >= vmin && val <= vmax )
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

	public void listPixelInsideRois()
	{
		ResultsTable rt = new ResultsTable();
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null || rm.getCount() == 0 )
		{
			IJ.error("No Rois in Manager");
			return;
		}


		Roi[] allRois = rm.getRoisAsArray();
		for ( int i = 0; i < allRois.length; i++ )
		{
			Roi curroi = allRois[i];
			Polygon poly = curroi.getPolygon();
			Rectangle rect = poly.getBounds();
			for ( int x = (int) rect.getX(); x < (int) rect.getX()+rect.getWidth(); x++ )
			{
				for ( int y = (int) rect.getY(); y < (int) rect.getY()+rect.getHeight(); y++ )
				{
					if ( poly.contains(x,y) )
					{
						rt.incrementCounter();
						rt.addValue("RoiNum", i);
						rt.addValue("X", x);
						rt.addValue("Y", y);
						rt.addValue("Z", curroi.getPosition());
						rt.addResults();
					}
				}
			}
		}

		rt.updateResults();
	}
	
	public void sortBySlice()
	{
		ResultsTable rt = new ResultsTable();
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null || rm.getCount() == 0 )
		{
			IJ.error("No Rois in Manager");
			return;
		}

		Roi[] allRois = rm.getRoisAsArray();
		rm.reset();
		for ( int z = 1; z <= imp.getNSlices(); z++ )
		{
			imp.setSlice(z);
			for ( int i = 0; i < allRois.length; i++ )
			{
				Roi curroi = allRois[i];
				if ( curroi.getPosition() == z )
					rm.addRoi(curroi);	
			}
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
			case "bestmean": 
				keepRois(2);
				break;
			case "circ":
				rangeRois("circularity");
				break;
			case "orient":
				rangeRois("angle");
				break;
			case "inside":
				listPixelInsideRois();
				break;
			case "sort":
				sortBySlice();
				break;
			default:
				keepRois(0);
				break;
		}
	}		

}
