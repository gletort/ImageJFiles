package ellip;
import ij.*;
import ij.util.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.measure.*;
import java.io.*;
import java.util.Vector;
import java.util.Locale;
import java.text.*;

public class Ellipsoid3D implements PlugIn 
{

	ImagePlus imp;
	ImagePlus impBin;
	ImagePlus ipcol;
	Calibration cal;
	RoiManager rm;
	boolean show = true;
	Ellipsoid elli;

	double scalex = 0.0895;
	double scaley = 0.0895;
	double scalez = 4;
	double fact = 1;
	
	boolean clearRes;
	boolean drawRes;


	/** Ask for specific options */
	public void getOptions()
	{
		GenericDialog gd = new GenericDialog("Options");
		Font boldy = new Font("SansSerif", Font.BOLD, 12);
	
		gd.addMessage("-------------------------------------------");
		gd.addMessage("Images scale:", boldy);	
		cal = imp.getCalibration();
		if (cal == null ) 
		{
			cal = new Calibration(imp);
		}
		if ( !cal.scaled() )
		{
			cal.pixelWidth = scalex;
			cal.pixelHeight = scaley;
			cal.pixelDepth = scalez;
		}
		gd.addNumericField("In_x: 1 pixel =", cal.pixelWidth, 4);
		gd.addNumericField("In_y: 1 pixel =", cal.pixelHeight, 4);
		gd.addNumericField("In_z: 1 pixel =", cal.pixelDepth, 4);
		gd.addMessage("-------------------------------------------");
		gd.addCheckbox( "Clear Results", false );
		gd.addCheckbox( "Draw ellipsoid", true );

		gd.showDialog();
		if (gd.wasCanceled()) return;
		scalex = gd.getNextNumber();	
		scaley = gd.getNextNumber();	
		scalez = gd.getNextNumber();
		
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		cal.pixelDepth = 1;
		
		clearRes = gd.getNextBoolean();
		drawRes = gd.getNextBoolean();
	}

	/** Close, clear, deselect...*/
	public void init()
	{
		imp = IJ.getImage();
		if ( show ) imp.show();
		getOptions();
		
		cal = imp.getCalibration();
		if (cal == null ) 
		{
			cal = new Calibration(imp);
		}
		// scaling is done in the Ellipsoid calculs
		cal.pixelWidth = 1;
		cal.pixelHeight = 1;
		cal.pixelDepth = 1;
		if ( drawRes )
			ipcol = new Duplicator().run(imp);

		rm = RoiManager.getInstance();
		if ( rm == null )
			rm = new RoiManager();
		rm.runCommand(imp,"Deselect");
		rm.reset();
	}

	public void doOne(boolean bined)	
	{
		init();
	
		if ( !bined )
			IJ.log("not implemented yet");
		else
			impBin = imp;

		// clean image	
		Prefs.blackBackground = false;
		buildEllipsoid();
		if ( !bined )
		{
			impBin.changes = false;
			impBin.close();	
		}
	}
	

	/** From binary image, fit and draw ellipsoid */
	public void buildEllipsoid()
	{
		elli = new Ellipsoid();
		elli.setScale( scalex, scaley, scalez );

		if ( impBin.isInvertedLut() )
			IJ.run(impBin, "Invert LUT", "");
		for ( int z = 1; z <= impBin.getNSlices(); z++ )
		{
			impBin.setSlice(z);
			for ( int x = 0; x < impBin.getWidth(); x++ )
			{
				for ( int y = 0; y < impBin.getHeight(); y++ )
				{
					if ( impBin.getPixel( x, y )[0] > 0 )
					{
						elli.addPoint( (double) x, (double) y, z );
					}
				}
			}
		}
		
		String res = elli.getEllipsoid();
		imp.changes = false;
		imp.close();
		if ( drawRes )
			drawFitEllipse( elli, "red" );
		writeResults();	
	}

	public void writeResults()
	{
		ResultsTable rt = ResultsTable.getResultsTable();
		if ( clearRes )
			rt.reset();
		rt.incrementCounter();
		//rt.addResults();
		rt.addValue( "FileName", imp.getTitle() );
		Point3D cent = elli.getCentroid();
		rt.addValue( "XC", cent.x() );		
		rt.addValue( "YC", cent.y() );		
		rt.addValue( "ZC", cent.z() );
		double[] lengths = elli.getLengths();		
		rt.addValue( "A", lengths[0] );		
		rt.addValue( "B", lengths[1] );		
		rt.addValue( "C", lengths[2] );
		double vol = 4.0/3.0 * Math.PI*lengths[0]*lengths[1]*lengths[2];
		rt.addValue( "Volume", vol );
		double[][] mat = elli.getMatrix();			
		rt.addValue( "vAx", mat[0][0]  );		
		rt.addValue( "vAy", mat[1][0]  );		
		rt.addValue( "vAz", mat[2][0]  );		
		rt.addValue( "vBx", mat[0][1]  );		
		rt.addValue( "vBy", mat[1][1]  );		
		rt.addValue( "vBz", mat[2][1]  );		
		rt.addValue( "vCx", mat[0][2]  );		
		rt.addValue( "vCy", mat[1][2]  );		
		rt.addValue( "vCz", mat[2][2]  );		
		rt.addResults();
		rt.updateResults();
	}
	

	/** Draw ellipsoid contour on each slice, and z-project for visualisation*/
	public void drawFitEllipse(Ellipsoid ellip, String col)
	{
		IJ.run(ipcol, "8-bit", "");
		if ( show ) ipcol.show();
		IJ.run(ipcol, "RGB Color", "");
		IJ.resetThreshold(ipcol);
		IJ.resetMinAndMax(ipcol);
		IJ.run(ipcol, "Colors...", "foreground="+col+" background=black selection=blue");
		
		for ( int z = 1; z <= ipcol.getStackSize(); z++ )
		{
			ipcol.setSlice(z);
			for ( int i = 1; i <= ipcol.getWidth(); i=i+2 )
			{
				for ( int j = 1; j <= ipcol.getHeight(); j=j+2 )
				{
					boolean yes = ellip.isEllipsoidContour( fact*i, fact*j, fact*z);
					if (yes )
					{
						Roi roi = new Roi( i, j, 1, 1 );
						ipcol.setRoi(roi);
						roi.setStrokeColor( new Color(1,0,0) );
						roi.setImage(ipcol);
						IJ.run(ipcol, "Draw", "slice");
					}
				}
			}
		}
	}
	
	
	/** Main call */
	public void run(String arg)
	{
		doOne(true);
	}


}
