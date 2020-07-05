/** 
 * \brief Plugin to calculate the radius of an Roi all around the Roi center, usefull to calculate shape fluctuations.
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
	ImagePlus imp;
	Calibration cal;
	RoiManager rm;
	Radii rad;
	int nangle = 100;
	double scalexy = 0.1135;
	boolean drawShortLong;
	int slr;
	int slg;
	int slb;
	int shr;
	int shg;
	int shb;
	boolean drawMeanShape;
	int msr;
	int msg;
	int msb;
	boolean drawDynamic;
	double threshold;
	boolean reload;

	/** \brief Dialog window */
	public boolean getParameters()
	{
		GenericDialog gd = new GenericDialog("Options", IJ.getInstance() );
		Font boldy = new Font("SansSerif", Font.BOLD, 12);
		
		gd.addNumericField("number_of_angles:", nangle);
		gd.addToSameRow();
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

		gd.showDialog();
		if (gd.wasCanceled()) return false;

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
	
	public void run(String arg)
	{
		// prepare all
		if ( !getParameters() ) { return; }
		IJ.run("Close All");

		rm = RoiManager.getInstance();
		if ( rm == null )
			rm = new RoiManager();
		rm.reset();
		
		
		// open image and be sure all is reset
		 OpenDialog od = new OpenDialog("Open", "");
		String dir = od.getDirectory();
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

		File directory = new File(dir+"/radioakres");
		if (! directory.exists())
		          directory.mkdir();

		// open Rois
		rm.runCommand("Open", "");
		// get radius
		
		if (!reload)
		{
			rad = new Radii( rm.getCount(), nangle, imp, rm );
			rad.getRadii( dir, name, scalexy );
		}
		else
		{
			Radii radin = new Radii(1,1,imp,rm);
			nangle = radin.getNumberAngle(dir+"/radioakres/"+name+"_anglesRadii.csv" );
			rad = new Radii( rm.getCount(), nangle, imp, rm );
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
		//imp.changes = false;
		//imp.close();
	}
}
