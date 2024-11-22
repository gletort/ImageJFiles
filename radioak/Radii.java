package radio;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import java.text.*;
import java.util.Locale;
import java.io.*;
import java.util.*;

public class Radii
{
	double[][] radi;
	int ntime;
	int nang;
	double dang;
	ImagePlus imp;
	RoiManager rm;
	double[] mrad;
	double[] minrad;
	double[] maxrad;
	int intensity_rad;

	/**\brief Constructor */
	public Radii( ImagePlus ip, RoiManager rman, int intensity_radius )
	{
		imp = ip;
		rm = rman;
		intensity_rad = intensity_radius;
	}
	
	/**\brief Initialization */
	public void set_parameters( int nt, int na )
	{
		radi = new double[nt][na];
		mrad = new double[na];
		minrad = new double[na];
		maxrad = new double[na];
		ntime = nt;
		nang = na;
		dang = 2*Math.PI/nang; // discretisatin in nang angles

		for (int i = 0; i < ntime; i++ )
		{
			for ( int j = 0; j < nang; j ++ )
				radi[i][j] = 0;
		}
		for ( int j = 0; j < nang; j ++ )
		{
			mrad[j] = 0;
			minrad[j] = 10000;
			maxrad[j] = 0;
		}
	}

	/**\brief get the number of angles to have a given arc length*/
	public int calculateNbAngleFromArcLength( double arc_length )
	{
		// Get the first ROI
		Roi first = rm.getRoi(0);
		// Get its perimeter and divide it by the arc_length
		double perim = first.getLength();
		int nangle = (int) Math.round( perim/arc_length );
		return nangle;
	}

	/**\brief get Roi radius at a given angle around its center */
	public void getRadii( String dir, String purname, double scalexy )
	{
		double ang = 0;
		double x, y;
		double rad = 0;
		ResultsTable myrt = new ResultsTable();
		DecimalFormatSymbols nf = new DecimalFormatSymbols(Locale.US);
		DecimalFormat df = new DecimalFormat("0.00000", nf);
		ImagePlus meanimg = imp.duplicate();
		IJ.run(meanimg, "Mean...", "radius="+intensity_rad);
			
		// get radius
		Roi cur;
		double[] cent = new double[2];
		int pos;
		int nrois = rm.getCount();
		for (int i=0; i < nrois; i++)
		{
			cur = rm.getRoi(i);
			// get center coordinates
			cent = cur.getContourCentroid(); //xcent, ycent
			pos = cur.getPosition(); // which slice (time point)
			// if Roi not associated with slice, assume ordered Rois
			if ( pos == 0 )
			{
				pos = i+1;
				cur.setPosition(pos);
				rm.setRoi(cur, i);
			}
			ang = 0;
			for ( int a = 0; a < nang; a++ )
			{
				rad = 20;
				x = (cent[0]+rad*Math.cos(ang));
				y = (cent[1]+rad*Math.sin(ang));
				// started to far already, goes back to center
				if ( !cur.contains((int)x,(int)y) )
				{
					rad = 0;
					x = cent[0];
					y = cent[1];
				}
				double dcang = 0.05*Math.cos(ang);
				double dsang = 0.05*Math.sin(ang);
				// look for Roi limit in the current radius
				while ( cur.contains((int)x,(int)y) ) 
				{
					x += dcang;
					y += dsang;
					rad = rad + 0.05;
				}
				myrt.incrementCounter();
				myrt.addValue("Slice", pos);
				myrt.addValue("Angle", df.format(ang));
				myrt.addValue("Radius", rad*scalexy);
				int inten = meanimg.getPixel((int)x,(int)y)[0];
				myrt.addValue("Intensity", inten);
				ang = ang + dang;
				radi[pos-1][a] = rad;
				mrad[a] += rad;
				if ( rad > maxrad[a] )
					maxrad[a] = rad;
				if ( rad < minrad[a] )
					minrad[a] = rad;
			}
			IJ.showProgress(i, ntime);
			myrt.addResults();
		}
		for ( int i = 0; i < nang; i++ )
			mrad[i] /= nrois;
		myrt.save(dir+"/radioakres/"+purname+"_anglesRadii.csv");
		myrt.reset();
		meanimg.changes = false;
		meanimg.close();
	}

	/** \brief Draw shortest and longest radius in each slice */
	public void drawShortLong(int longr, int longg, int longb, int shortr, int shortg, int shortb)
	{
		int pmin = 0;
		int pmax = 0;
		int pos;
		double[] cent = new double[2];
		double vmin, vmax;
		Roi cur;
		for ( int i = 0; i < rm.getCount(); i++ )
		{
			cur = rm.getRoi(i);
			cent = cur.getContourCentroid();
			pos = cur.getPosition(); // in case not ordered
			imp.setPosition(pos);

			// find min and max radius for each slice
			pmin = 0;
			pmax = 0;
			vmin = 10000;
			vmax = 0;
			for ( int j = 0; j < nang; j++ )
			{
				if ( radi[i][j] > vmax )
				{
					vmax = radi[i][j];
					pmax = j;
				}
				if ( radi[i][j] < vmin )
				{
					vmin = radi[i][j];
					pmin = j;
				}
			}

			// draw corresponding radius
			IJ.setForegroundColor(shortr, shortg, shortb);	
			Line minline = new Line( cent[0], cent[1], cent[0]+vmin*Math.cos(dang*pmin), cent[1]+vmin*Math.sin(dang*pmin) );
			imp.setRoi( minline );
			IJ.run(imp, "Draw", "slice");		
			IJ.setForegroundColor(longr, longg, longb);	
			Line maxline = new Line( cent[0], cent[1], cent[0]+vmax*Math.cos(dang*pmax), cent[1]+vmax*Math.sin(dang*pmax) );
			imp.setRoi( maxline );
			IJ.run(imp, "Draw", "slice");		
		}	
	}

	/** \brief Color the points at each radius according to its dynamic with previous slice (increasing, decreasing, stable) */
	public void drawDynamic(double th)
	{
		int xpoint, ypoint;
		int r,g,b;
		double[] cent = new double[2];
		Roi cur;	

		for (int i =0; i < rm.getCount(); i++)
		{
			cur = rm.getRoi(i);
			cent = cur.getContourCentroid();
			imp.setPosition(cur.getPosition());
			
			for ( int j = 0; j < nang; j++ )
			{

				xpoint = (int) (cent[0] + radi[i][j]*Math.cos( dang*j ));
				ypoint = (int) (cent[1] + radi[i][j]*Math.sin( dang*j ));
		
				r = 255;
				b = 255;
				g = 255;	
				if (i > 0 )
				{
				 if ( radi[i-1][j] < radi[i][j]*(1-th) )
				 {
					b = 0;
					g = 0;
				 }
				 if ( radi[i-1][j] > radi[i][j]*(1+th) )
				 {
					r = 0;
					b = 0;
				 }
				}	
				IJ.setForegroundColor(r,g,b);	
				imp.setRoi(new OvalRoi(xpoint-5,ypoint-5,10,10) );
				IJ.run(imp, "Fill", "slice");		
			}
		}
	}

/** \brief Draw the average shape over all time points */	
	public void drawMeanShape()
	{
		int[] xpoints = new int[nang];
		int[] ypoints = new int[nang];
		for (int i =0; i < rm.getCount(); i++)
		{
			Roi cur = rm.getRoi(i);
			double[] cent = cur.getContourCentroid();
			imp.setPosition(cur.getPosition());

			for ( int j = 0; j < nang; j++ )
			{
				xpoints[j] = (int) (cent[0] + mrad[j]*Math.cos( dang*j ));
				ypoints[j] = (int) (cent[1] + mrad[j]*Math.sin( dang*j ));
			
			}
			imp.setRoi(new PolygonRoi(xpoints,ypoints,nang,Roi.POLYGON));
			IJ.run(imp, "Draw", "slice");		
		}

	}

	public void drawMinMaxShape()
	{
		int[] xpoints = new int[nang];
		int[] ypoints = new int[nang];
		int[] xmpoints = new int[nang];
		int[] ympoints = new int[nang];
		double[] cent = new double[2];
		for (int i =0; i < rm.getCount(); i++)
		{
			Roi cur = rm.getRoi(i);
			cent = cur.getContourCentroid();
			imp.setPosition(cur.getPosition());

			for ( int j = 0; j < nang; j++ )
			{
				xpoints[j] = (int) (cent[0] + minrad[j]*Math.cos( dang*j ));
				ypoints[j] = (int) (cent[1] + minrad[j]*Math.sin( dang*j ));
				xmpoints[j] = (int) (cent[0] + maxrad[j]*Math.cos( dang*j ));
				ympoints[j] = (int) (cent[1] + maxrad[j]*Math.sin( dang*j ));
			}
			imp.setRoi(new PolygonRoi(xpoints,ypoints,nang,Roi.POLYGON));
			IJ.run(imp, "Draw", "slice");		
			imp.setRoi(new PolygonRoi(xmpoints,ympoints,nang,Roi.POLYGON));
			IJ.run(imp, "Draw", "slice");		
		}

	}

	public int getNumberAngle(String infile)
	{
		int res = 0;
		try {
			/* open the file */
			BufferedReader r = new BufferedReader(new FileReader(infile));
			int row = 0;
			String line = r.readLine();  // skip title line
			line = line.trim();
			String[] cur = line.split(",");
			res = cur.length-1;
			r.close();
		}
		catch (Exception e) 
		{
			IJ.error(e.getMessage());
		}
		return res;
	}

/**\brief load a Radioak file */	
public void readRadioakFile(String infile, double scalexy)
	{
		NumberFormat nf = NumberFormat.getInstance();
		try {
			/* open the file */
			BufferedReader r = new BufferedReader(new FileReader(infile));
			String line = r.readLine();  // skip title line
			double rad;
			int i;
			while ((line = r.readLine()) != null) 
			{
				line = line.trim();
				String[] cur = line.split(",");
				i = (int)(nf.parse(cur[0]).doubleValue());
				for ( int j = 0; j < cur.length-1; j++ )
				{
					rad = nf.parse(cur[j+1]).doubleValue()/scalexy ;
					radi[i-1][j] = rad;
					mrad[j] += rad;
					if ( rad > maxrad[j] )
						maxrad[j] = rad;
					if ( rad < minrad[j] )
						minrad[j] = rad;
				}
			}
			r.close();
		}
		catch (Exception e) 
		{
			IJ.error(e.getMessage());
		}
		
		for ( int i = 0; i < nang; i++ )
			mrad[i] /= rm.getCount();
	}
}
