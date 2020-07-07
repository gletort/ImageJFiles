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
	
	public float[] angleMax( float cx, float cy, float radius, double ang )
	{
		Line myline = new Line(cx, cy, cx+radius*Math.cos(ang), cy+radius*Math.sin(ang));
		myline.setStrokeWidth(40);
		imp.setRoi(myline);
		double[] vals = myline.getPixels();
		float[] res = new float[2];
		res[0] = 0;
		res[1] = 0;
		double max = 0;

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
		IJ.run("Close All");

		rm = RoiManager.getInstance();
		if ( rm == null )
			rm = new RoiManager();
		rm.reset();
		
		imp = IJ.openImage();
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
		findContour();
		rm.runCommand(imp,"Deselect");
		String imname = imp.getTitle();
		//String purname = imname.substring(0, imname.lastIndexOf('.'));
		//rm.runCommand("Save", dir+"/cortex/"+purname+"_Cortex.zip");
		imp.show();
	}
}
