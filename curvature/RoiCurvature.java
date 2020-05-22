

/** 
 * \brief Plugin to compute shape curvature 
 *
 * \details For a given Roi, calculate the curvature and curvature radius along it
 *
 * \author G. Letort, College de France
 * \date created on 2020/05/22
 * */

package curv;
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

public class RoiCurvature implements PlugIn
{
	public double firstd( float[] z, int ind, int n)
	{
		int pind = (ind-1+n)%n;
		int nind = (ind+1)%n;
		return (double) ((z[nind]-z[pind])/2.0);
	}
	
	public double secondd( float[] z, int ind, int n)
	{
		int pind = (ind-1+n)%n;
		int nind = (ind+1)%n;
		return (double) ((z[nind]-2.0*z[ind]+z[pind])/1.0);
	}
	
	public double meanCircularWindow( double[] tab, int mid, int size)
	{
		double res = 0;
		int nb = 0;
		//middle (current value, counted 2 times)
		for ( int k = 0; k <= size; k++ )
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

	public double[] smoothPts( double[] tab)
	{
		double[] res = new double[tab.length];
		int size = 1; 
		for ( int k = 0; k < tab.length; k++ )
		{
			double locmean = meanCircularWindow( tab, k, size);
			res[k] = locmean;
		}
		return res;
	}

	public void curvature( FloatPolygon fp )
	{
		IJ.log( "XPos \t YPos \t Curvature \t CurvatureRadius \t isFlat"); 
		int n = fp.npoints;
		double[] xp = new double[n];
		double[] yp = new double[n];
		double[] xpp = new double[n];
		double[] ypp = new double[n];
		for ( int i = 0; i < n; i++ )
		{
			xp[i] = firstd( fp.xpoints, i, n); 
			yp[i] = firstd( fp.ypoints, i, n); 
			xpp[i] = secondd( fp.xpoints, i, n); 
			ypp[i] = secondd( fp.ypoints, i, n); 
			//System.out.println(i+" "+xp[i]+" "+yp[i]+" "+xpp[i]);
		}
		// smooth
		xp = smoothPts(xp);
		yp = smoothPts(yp);
		xpp = smoothPts(xpp);
		ypp = smoothPts(ypp);

		double[] curv = new double[n];
		double maxc = 0;
		double minc = 10000;
		double flat = 0;  // proportion of nearly flat curv
		double perim = 0;
		for ( int i = 0; i < n; i++ )
		{
			curv[i] = Math.abs( xp[i]*ypp[i] - yp[i]*xpp[i] );
			curv[i] /= Math.pow( xp[i]*xp[i] + yp[i]*yp[i], 1.5); 
			if ( curv[i] > maxc )
				maxc = curv[i];
			if ( curv[i] < minc )
				minc = curv[i];
			double dist = distance( fp.xpoints, fp.ypoints, i);
		   perim += dist;	
			if ( curv[i] <= 0.001 ) // nearly flat
				flat += dist;
			IJ.log( fp.xpoints[i]+" \t "+fp.ypoints[i]+" \t "+curv[i]+" \t "+(1.0/curv[i])+" \t "+( curv[i]<=0.001) ); 
		}
		flat /= perim;

		double mean = tabMean( curv );
		double sd = tabStd( curv, mean );
		IJ.log("Perimeter="+perim);
		IJ.log("MeanCurv="+mean);
		IJ.log("SdCurv="+sd);
		IJ.log("MaxCurv="+maxc);
		IJ.log("MinCurv="+minc);
		IJ.log("FlatProp="+flat);
	}

	public double tabMean( double[] tab )
	{
		double res = 0.0;
		for ( int i = 0; i < tab.length; i++ )
			res += tab[i];
		return res/tab.length;
	}

	public double tabStd( double[] tab, double mean)
	{
		double res = 0.0;
		for ( int i = 0; i < tab.length; i++ )
			res += (tab[i]-mean)*(tab[i]-mean);
		return Math.sqrt(res/tab.length);
	}

	public double distance( float[] x, float[] y, int i)
	{
		int n = x.length;
		double res = Math.pow((double)(x[(i+1)%n]-x[i]), 2);
		res += Math.pow( (double) (y[(i+1)%n]-y[i]), 2);
		double resp = Math.pow((double)(x[(i-1+n)%n]-x[i]), 2);
		resp += Math.pow( (double) (y[(i-1+n)%n]-y[i]), 2);
		return (Math.sqrt(res)+Math.sqrt(resp))/2.0;
	}

	public void run(String arg)
	{
		RoiManager rm = RoiManager.getInstance();
		if ( rm == null || rm.getCount() <= 0 )
		{
			IJ.error("Must have a Roi to analyze");
		}
		Roi shape = rm.getRoi(0);

		ImagePlus imp = IJ.getImage();
		imp.setRoi(shape);
		IJ.run(imp, "Interpolate", "interval=1 smooth adjust");
		IJ.run(imp, "Fit Spline", "");
		IJ.run(imp, "Interpolate", "interval=5 smooth adjust");
		Roi splined = imp.getRoi();
		FloatPolygon poly = splined.getFloatPolygon();
		curvature(poly);

	}
}
