/** 
 * \brief Plugin to calculate the radius of an Roi all around the Roi center, usefull to calculate shape fluctuations.
 * Options to visualise the radii variation
 *
 *
 * \author G. Letort, College de France
 * \date created on 2020/06/21
 * */

package radio;

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
import java.text.*;
import ij.io.*;

public class Radioak implements PlugIn
{
	private static final long MEGABYTE = 1024L * 1024L;
	Radii rad;
	ImagePlus imp;
	Calibration cal;
	RoiManager rm;
	int nangle = 100; // angular resolution
	double arclength = 5; // arc length
	boolean useArc = false; // use arc length or number of angles
	double scalexy = 0.1135; // image scale
	boolean drawShortLong; // option: draw shortest and longest radius on each slice
	// colors options
	int slr; 
	int slg;
	int slb;
	int shr;
	int shg;
	int shb;
	// option draw average shape of cortex
	boolean drawMeanShape;
	// colors
	int msr;
	int msg;
	int msb;
	boolean drawDynamic; // draw radii variations
	double threshold; // threshold to consider as increasing or decreasing
	boolean reload; // read a radii file
	int intensity_radius; // average local intensity

	/** \brief Dialog window */
	public boolean getParameters()
	{
		GenericDialog gd = new GenericDialog("Options", IJ.getInstance() );
		Font boldy = new Font("SansSerif", Font.BOLD, 12);
		
		gd.addMessage("Choose a number of angles to measure or a constant arc length:");
		gd.addCheckbox("Use_arc_length", false);
		gd.addNumericField("arc_length_(pixels):", arclength);
		gd.addToSameRow();
		gd.addNumericField("number_of_angles:", nangle);
		gd.addNumericField("scale:", scalexy, 4);
		gd.addToSameRow();
		gd.addMessage("(1 pixel=..µm)");
		gd.addCheckbox("Load radius from file", false);
		gd.addMessage("----------------------------------------------------------------------------------- ");
		gd.addCheckbox("Draw shortest/longest radii", true);
		//gd.addToSameRow();
		gd.addNumericField("Color_short (RGB): R", 255);	
		gd.addToSameRow();
		gd.addNumericField("G", 100);	
		gd.addToSameRow();
		gd.addNumericField("B", 0);	
		gd.addNumericField("Color_long: R", 0);	
		gd.addToSameRow();
		gd.addNumericField("G3", 255);	
		gd.addToSameRow();
		gd.addNumericField("B3", 100);	
		gd.addMessage("----------------------------------- ");
		gd.addCheckbox("Draw mean shape", false);
		//gd.addToSameRow();
		gd.addNumericField("Color2 (RGB): R", 0);	
		gd.addToSameRow();
		gd.addNumericField("G2", 0);	
		gd.addToSameRow();
		gd.addNumericField("B2", 255);	
		gd.addMessage("----------------------------------- ");
		gd.addCheckbox("Draw dynamic", true);
		//gd.addToSameRow();
		gd.addNumericField("Threshold for growing/shrinking", 0.01, 2);	
		gd.addMessage("----------------------------------- ");
		gd.addNumericField("Local intensity smoothing", 1, 0);	

		gd.showDialog();
		if (gd.wasCanceled()) return false;

		useArc = gd.getNextBoolean();
		arclength = (double) gd.getNextNumber();
		nangle = (int) gd.getNextNumber();
		scalexy = gd.getNextNumber();
		reload = gd.getNextBoolean();
		drawShortLong = gd.getNextBoolean();
		shr = (int) gd.getNextNumber();
		shg = (int) gd.getNextNumber();
		shb = (int) gd.getNextNumber();
		slr = (int) gd.getNextNumber();
		slg = (int) gd.getNextNumber();
		slb = (int) gd.getNextNumber();

		drawMeanShape = gd.getNextBoolean();
		msr = (int) gd.getNextNumber();
		msg = (int) gd.getNextNumber();
		msb = (int) gd.getNextNumber();
		drawDynamic = gd.getNextBoolean();
		threshold = gd.getNextNumber();
		intensity_radius = (int) gd.getNextNumber();
		return true;
	}

	/** Be sure there s no calibration */
	public void initCalibration()
	{
		// Measure everything in pixels
		cal = imp.getCalibration();
		if (cal == null ) 
		{
			cal = new Calibration(imp);
		}
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		cal.pixelDepth = 1;
	}

	public void getRadii(String dir, String name)
	{
		Radii rad;	
		if (!reload)
		{
			rad = new Radii( imp, rm, intensity_radius );
			if (useArc)
			{
				nangle = rad.calculateNbAngleFromArcLength( arclength );
				System.out.println("Nb angle "+nangle);
			}
			rad.set_parameters( rm.getCount(), nangle );
			rad.getRadii( dir, name, scalexy );
		}
		else
		{
			Radii radin = new Radii( imp,rm, intensity_radius );
			nangle = radin.getNumberAngle(dir+"/radioakres/"+name+"_anglesRadii.csv" );
			rad = new Radii( imp, rm, intensity_radius );
			rad.set_parameters( rm.getCount(), nangle );
			rad.readRadioakFile( dir+"/radioakres/"+name+"_anglesRadii.csv", scalexy );
		}

		IJ.run(imp, "RGB Color", "");
		if ( drawShortLong)
		{
			//IJ.run(imp, "Line Width...", "line=2");
			rad.drawShortLong(slr, slg, slb, shr, shg, shb);	
		}
		if ( drawMeanShape )
		{
			IJ.setForegroundColor(msr, msg, msb);	
			//IJ.run(imp, "Line Width...", "line=2");
			rad.drawMeanShape();	
			IJ.setForegroundColor((int)msr/2, (int)msg/2, (int)msb/2);	
			//IJ.run(imp, "Line Width...", "line=1");
			rad.drawMinMaxShape();	
		}
		if ( drawDynamic )
		{
			rad.drawDynamic(threshold);
		}
	}

	public void run(String arg)
	{
		// prepare all
		if ( !getParameters() ) { return; }
		IJ.run("Close All");

		rm = RoiManager.getInstance();
		if ( rm == null )
			rm = new RoiManager();
		rm.reset();
		
		if ( arg.equals("fold") )
		{
			String dir = IJ.getDirectory("Choose images directory");	
			File thedir = new File(dir); 
			File[] fileList = thedir.listFiles(); 
			File directory = new File(dir+"/radioakres");
			if (! directory.exists())
		          directory.mkdir();
			for ( int i = 0; i < fileList.length; i++ )
			{
				File fily = fileList[i];
				if ( fily.isFile() )
				{
					String fileName = fily.getName();
					int j = fileName.lastIndexOf('.');
					if (j > 0) 
					{
						String extension = fileName.substring(j);
						if ( extension.equals(".tif") | extension.equals(".TIF") )
						{
							IJ.log("Doing "+dir+fileName);
							IJ.run("Close All", "");
							rm.reset();
							String roiname = "contours/"+(fileName.substring(0,j))+"_UnetCortex.zip";
							File rois = new File(dir+roiname);
							if( !rois.exists() ) 
							{
								// previous version of Oocytor output
								roiname = "cortex/"+(fileName.substring(0,j))+"_UnetCortex.zip";
							}
							if ( rois.exists() )
							{
								IJ.open(dir+fileName);
								imp = IJ.getImage();
								imp.setTitle(fileName+"_radioak.tif");
								initCalibration();
								imp.show();
								rm.runCommand(imp,"Deselect");
								IJ.run(imp, "Select None", "");
								IJ.run("Select None");
								IJ.run(imp, "Remove Overlay", "");
								rm.runCommand("Open", dir+roiname);
								getRadii(dir, fileName);
								IJ.run(imp, "Select None", "");
								IJ.saveAs(imp, "Tiff", dir+"radioakres/"+fileName+"_radius.tif");	
								imp.changes = false;
								imp.close();
							}
							else
							{
								IJ.log("Roi file "+dir+roiname+" not found");
							}
						}
					}
				}	
			}
		}
		else
		{	
			// open image and be sure all is reset
			OpenDialog od = new OpenDialog("Open", "");
			String dir = od.getDirectory();
			File directory = new File(dir+"/radioakres");
			if (! directory.exists())
		          directory.mkdir();
			String name = od.getFileName();
			IJ.open(dir+name);
			imp = IJ.getImage();
			imp.setTitle(name+"_radioak.tif");
			initCalibration();
			imp.show();
			rm.runCommand(imp,"Deselect");
			IJ.run(imp, "Select None", "");
			IJ.run("Select None");
			IJ.run(imp, "Remove Overlay", "");
		
			// open Rois
			int j = name.lastIndexOf('.');
			String roiname = "contours/"+(name.substring(0,j))+"_UnetCortex.zip";
			File rois = new File(dir+roiname);
			if( !rois.exists() )
			{
				roiname = IJ.getFilePath("Choose cortex roi file");
				rois = new File(roiname);
			}	
			rm.runCommand("Open", rois.getPath());
			getRadii(dir, name);
		}
    }
}
