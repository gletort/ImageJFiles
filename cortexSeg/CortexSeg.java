/** 
 * \brief Plugin for segmentation of cortex signal 
 *
 * \details ah 
 *
 * \author G. Letort, College de France
 * \date created on 2020/07/07
 * */

package cortexer;
import ij.*;
import ij.io.*;
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

public class CortexSeg implements PlugIn 
{

	ImagePlus imp;
	RoiManager rm;
	String dir;
	int mode;
	
	public float[] angleMax( float cx, float cy, float radius, double ang )
	{
		IJ.run(imp, "Select None", "");
		double meanvals = imp.getStatistics().mean;
		Line myline = new Line(cx, cy, cx+radius*Math.cos(ang), cy+radius*Math.sin(ang));
		myline.setStrokeWidth(50);
		imp.setRoi(myline);
		double[] vals = myline.getPixels();
		float[] res = new float[2];
		res[0] = 0;
		res[1] = 0;
		double max = 0;

		// look for highest maxima
		if ( mode == 0 )
		{
			for ( int i = 1; i < (vals.length-1); i++ )
			{
				if ( vals[i] > max )
				{
					max = vals[i];
					res[0] = ((float)cx+(float)(radius*Math.cos(ang)*i/vals.length));	
					res[1] = ((float)cy+(float)(radius*Math.sin(ang)*i/vals.length));	
				}
			}
			return res;
		}
		
		// look for first maxima
		if ( mode == 1 )
		{
			vals = smoothTab(vals);
			int step = 20;
			double refdiff = Math.abs(vals[step]-vals[0])/step;
			for ( int j =0; j < 9; j++)
			{
				refdiff += Math.abs(vals[step+j]-vals[j])/step;
			}
			refdiff /= 10;
			
			int i = step;
			while (i < (vals.length-1) )
			{
				// go to local maxima
				while ( (i<vals.length-2) & (vals[i+1] > vals[i]) ) i++;
			
				// high enough
				if ( vals[i] > meanvals*1.25 )
				{
					// high slope
					double diff = (vals[i] - vals[i-step])/step;
					if ( diff > refdiff*5 )
					{
						res[0] = ((float)cx+(float)(radius*Math.cos(ang)*i/vals.length));	
						res[1] = ((float)cy+(float)(radius*Math.sin(ang)*i/vals.length));	
						return res;
					}
				}
				// continue
				i++;
			}
		}
		res[0] = ((float)cx+(float)(radius*Math.cos(ang)));	
		res[1] = ((float)cy+(float)(radius*Math.sin(ang)));	
		return res;
	}

	public double meanVal( double[] tab )
	{
		double res = 0.0;
		for ( int i=0; i < tab.length; i++)
		{
			res += tab[i];
		}
		return res/tab.length;
	}
	
	public void findContour()
	{
		// center of image
		float centx = (float)(imp.getWidth()/2.0);
		float centy = (float)(imp.getHeight()/2.0);
		float radius = (float) Math.max(imp.getWidth()/2.0, imp.getHeight()/2.0);	
		
		for ( int z = 1; z <= imp.getNSlices(); z++ )
		{
			imp.setSlice(z);	
			int nang = 360;	
			double ang = 0;
			double dang = 2*Math.PI/nang;
			float[] xpts = new float[nang];
			float[] ypts = new float[nang];
			double[] prevPos = new double[nang];

			// find angular max
			for (int j=0; j<nang; j++)
			{	
				ang = ang + dang;
				float[] res = angleMax(centx, centy, radius, ang);
				xpts[j] = res[0];
				ypts[j] = res[1];
			}
		
			// smooth	
			float[] rads = new float[xpts.length];
			for ( int m = 0; m < xpts.length; m++ )
			{
				rads[m] = (float) Math.sqrt( (double) ((xpts[m]-centx)*(xpts[m]-centx) + (ypts[m]-centy)*(ypts[m]-centy)));
			}
			ang = 0;
			for (int j=0; j<nang; j++)
			{	
				ang = ang + dang;
				float[] res = smoothRadius(centx, centy, radius, ang, rads, j);
				xpts[j] = res[0];
				ypts[j] = res[1];
			}
			
			Roi contour = new PolygonRoi(xpts, ypts, Roi.POLYGON);
			contour.setImage(imp);
			contour.setPosition(z);
			imp.setRoi(contour);
			rm.addRoi(contour);
		}

	}

	public double[] smoothTab( double[] tab )
	{
		double[] res = new double[tab.length];
		int vois = 5;
		for ( int i = 0; i < tab.length; i++ )
		{
			int	imin = (int) Math.max(i-vois, 0);
			int imax = (int) Math.min(i+vois, tab.length-1);

			res[i] = meanWindow( tab, imin, imax );
		}
		return res;
	}
	
	public double meanWindow( double[] tab, int dep, int end )
	{
		double res = 0;
		int n = 0;
		for ( int i = dep; i <= end; i++ )
		{
			res += tab[i];
			n++;
		}
		return res/n;
	}

	public float meanCircularWindow( float[] tab, int mid, int size, boolean self)
	{
		float res = 0;
		int nb = 0;
		// middle point counted 2 times (higher coef)
		int deb = 0;
		if ( !self )
			deb = 1;
		for ( int k = deb; k < size; k++ )
		{
			int ind = (mid+k)%(tab.length);
			res += tab[ind];
			nb ++;
			ind = (tab.length+mid-k)%(tab.length);
			res += tab[ind];
			nb++;
		}

		res /= nb;
		return res;
	}
	public float[] smoothRadius( float cx, float cy, float radius, double ang, float[] rads, int ind )
	{
		float[] res = new float[2];
		res[0] = (float) 0.0;
		res[1] = (float) 0.0;

		int size = 5;
		double crad = meanCircularWindow( rads, ind, size, true);

		res[0] =  ((float) cx + (float) (crad*Math.cos(ang)));	
		res[1] =  ((float) cy + (float) (crad*Math.sin(ang)));
		return res;
	}

	public void run(String arg)
	{
		//IJ.run("Close All");

		rm = RoiManager.getInstance();
		if ( rm == null )
			rm = new RoiManager();
		rm.reset();
		
		ImagePlus impi = IJ.getImage();
		imp = impi.duplicate();
		//imp.show();
		IJ.run(imp, "Subtract Background...", "rolling=100 stack");
		//imp.show();
		rm.runCommand(imp,"Deselect");
		IJ.run(imp, "Select None", "");
		//IJ.run("Select None");
		IJ.run(imp, "Remove Overlay", "");

		/**FileInfo inf = imp.getFileInfo();	
		dir = inf.getFilePath();
		File directory = new File(dir+"/cortex");
		if (! directory.exists())
		        directory.mkdir();
*/
		mode = 0;
		if ( arg.equals("min") )
		{
			mode = 1;
		}
		findContour();
		//rm.runCommand(imp,"Deselect");
		//String imname = imp.getTitle();
		//String purname = imname.substring(0, imname.lastIndexOf('.'));
		//rm.runCommand("Save", dir+"/cortex/"+purname+"_Cortex.zip");
		//imp.show();
		imp.close();
	}
}
